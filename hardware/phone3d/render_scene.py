"""Path-traced renders of the real FutureOS Regular phone, from the actual model files (Blender's Cycles through the `bpy` module).

  python3 render_scene.py still hero      out/hero.png      [--samples 200 --scale 1.0]
  python3 render_scene.py still exploded  out/exploded.png
  python3 render_scene.py video teardown  out/frames --seconds 14 --fps 30 [--from N --to M --samples 40]
  python3 render_scene.py list

What is rendered: the whole Regular, exactly as its two OpenSCAD files define it (export-parts.sh -> stl/): the shell (front, and the back with every opening
the mainboard needs), the keypad and side keys, the glass, the LCD module, and the real electronics stack of futureos_mainboard.scad: main board with the Quectel
SoM, connectors, motor, receiver, sensors, IR and both cameras; keyboard board with its gold pads, domes and LEDs and the bottom strip (USB-C, 3.5 mm, SIM,
speaker, microphone); the battery with its PCM; the flex cables. Nothing is redrawn: colours are the SCAD's own colours.
The legends come from print/regular_markings.stl (the SCAD needs Windows' "Segoe MDL2 Assets" for the function-key icons), moved by the same -FRONT_T as assembly().
The screen shows the real launcher: screen.png, drawn from the design-system components (screen_shot.js).
Scene units: 1 Blender unit = 1 metre (the STLs are imported with a 0.001 scale); the scene is Y-up like the model (Y = along the phone, Z = out of the screen).
"""
import argparse
import math
import os
import sys

import bpy
from mathutils import Euler, Matrix, Vector

ROOT = os.path.dirname(os.path.abspath(__file__))
STL = os.path.join(ROOT, "stl")
MM = 0.001
CENTER = Vector((26.5, 72.63, -4.2)) * MM      # middle of the body (W 53 x L 145.27 x T 8.4 mm)


def look_at(loc, target, up=(0, 1, 0)):
    """Euler rotation for an object at `loc` that looks down its -Z toward `target`, with world +Y as up"""
    f = (Vector(target) - Vector(loc)).normalized()
    r = f.cross(Vector(up)).normalized()
    u = r.cross(f)
    return Matrix(((r.x, u.x, -f.x), (r.y, u.y, -f.y), (r.z, u.z, -f.z))).to_euler()


def lin(h):
    """#rrggbb (sRGB) -> linear RGBA"""
    c = [int(h[i:i + 2], 16) / 255 for i in (1, 3, 5)]
    c = [x / 12.92 if x <= 0.04045 else ((x + 0.055) / 1.055) ** 2.4 for x in c]
    return (*c, 1.0)


# ---------------------------------------------------------------- scene
def reset():
    bpy.ops.wm.read_factory_settings(use_empty=True)
    sc = bpy.context.scene
    sc.render.engine = "CYCLES"
    sc.cycles.device = "CPU"
    sc.cycles.use_denoising = True
    sc.cycles.denoiser = "OPENIMAGEDENOISE"
    sc.cycles.use_adaptive_sampling = True
    sc.cycles.adaptive_threshold = float(os.environ.get("ADAPT", 0.02))
    sc.cycles.max_bounces = int(os.environ.get("BOUNCES", 6))
    sc.cycles.diffuse_bounces = 2 if os.environ.get("BOUNCES") else 3
    sc.cycles.glossy_bounces = int(os.environ.get("GLOSSY", 4))
    sc.cycles.transmission_bounces = 2
    sc.cycles.caustics_reflective = False
    sc.cycles.caustics_refractive = False
    sc.cycles.sample_clamp_indirect = 8
    sc.render.image_settings.file_format = "PNG"
    sc.render.image_settings.color_depth = "8"
    sc.view_settings.view_transform = "AgX"
    sc.view_settings.look = "AgX - Medium High Contrast"
    sc.view_settings.exposure = -0.25
    return sc


def material(name, color, rough=0.5, metal=0.0, spec=0.5, coat=0.0, coat_rough=0.05, grain=0.0, emit=None):
    m = bpy.data.materials.new(name)
    m.use_nodes = True
    nt = m.node_tree
    for n in list(nt.nodes):
        nt.nodes.remove(n)
    out = nt.nodes.new("ShaderNodeOutputMaterial")
    b = nt.nodes.new("ShaderNodeBsdfPrincipled")
    b.inputs["Base Color"].default_value = lin(color)
    b.inputs["Roughness"].default_value = rough
    b.inputs["Metallic"].default_value = metal
    b.inputs["Specular IOR Level"].default_value = spec
    b.inputs["Coat Weight"].default_value = coat
    b.inputs["Coat Roughness"].default_value = coat_rough
    if emit:
        b.inputs["Emission Color"].default_value = lin(emit[0])
        b.inputs["Emission Strength"].default_value = emit[1]
    if grain:                                   # MJF PA12: a fine powder grain in the surface
        tc = nt.nodes.new("ShaderNodeTexCoord")
        nz = nt.nodes.new("ShaderNodeTexNoise")
        nz.inputs["Scale"].default_value = 9000
        nz.inputs["Detail"].default_value = 3
        bump = nt.nodes.new("ShaderNodeBump")
        bump.inputs["Strength"].default_value = grain
        bump.inputs["Distance"].default_value = 0.00005
        nt.links.new(tc.outputs["Object"], nz.inputs["Vector"])
        nt.links.new(nz.outputs["Fac"], bump.inputs["Height"])
        nt.links.new(bump.outputs["Normal"], b.inputs["Normal"])
    nt.links.new(b.outputs["BSDF"], out.inputs["Surface"])
    return m


def screen_material(png, strength=1.6):
    m = bpy.data.materials.new("screen")
    m.use_nodes = True
    nt = m.node_tree
    for n in list(nt.nodes):
        nt.nodes.remove(n)
    out = nt.nodes.new("ShaderNodeOutputMaterial")
    em = nt.nodes.new("ShaderNodeEmission")
    tex = nt.nodes.new("ShaderNodeTexImage")
    tex.image = bpy.data.images.load(png)
    tex.image.colorspace_settings.name = "sRGB"
    tex.interpolation = "Cubic"
    tex.extension = "CLIP"
    em.inputs["Strength"].default_value = strength
    nt.links.new(tex.outputs["Color"], em.inputs["Color"])
    nt.links.new(em.outputs["Emission"], out.inputs["Surface"])
    return m


def glass_over_screen():
    """the reflection of the glass over the lit screen: a clear plane that only adds a fresnel-weighted mirror layer"""
    m = bpy.data.materials.new("screen_glass")
    m.use_nodes = True
    nt = m.node_tree
    for n in list(nt.nodes):
        nt.nodes.remove(n)
    out = nt.nodes.new("ShaderNodeOutputMaterial")
    mix = nt.nodes.new("ShaderNodeMixShader")
    tr = nt.nodes.new("ShaderNodeBsdfTransparent")
    gl = nt.nodes.new("ShaderNodeBsdfGlossy")
    gl.inputs["Roughness"].default_value = 0.03
    fr = nt.nodes.new("ShaderNodeFresnel")
    fr.inputs["IOR"].default_value = 1.52
    nt.links.new(fr.outputs["Fac"], mix.inputs["Fac"])
    nt.links.new(tr.outputs["BSDF"], mix.inputs[1])
    nt.links.new(gl.outputs["BSDF"], mix.inputs[2])
    nt.links.new(mix.outputs["Shader"], out.inputs["Surface"])
    return m


def inset_xy(o, mm):
    """the LCD keep-out is a plain box: its top corners stick 0.5 mm out of the body's rounded corners (R 7) and show as a blue sliver. Only for the picture: the box is drawn
    `mm` smaller on every side, which pulls the corners inside the outline; its thickness and position are untouched"""
    xs = [v.co.x for v in o.data.vertices]
    ys = [v.co.y for v in o.data.vertices]
    cx, cy = (max(xs) + min(xs)) / 2, (max(ys) + min(ys)) / 2
    sx = 1 - 2 * mm * MM / (max(xs) - min(xs))
    sy = 1 - 2 * mm * MM / (max(ys) - min(ys))
    for v in o.data.vertices:
        v.co.x = cx + (v.co.x - cx) * sx
        v.co.y = cy + (v.co.y - cy) * sy


def import_stl(name, filename, mat, shift=(0, 0, 0), smooth=True):
    before = set(bpy.data.objects)
    bpy.ops.wm.stl_import(filepath=filename, global_scale=MM)
    o = (set(bpy.data.objects) - before).pop()
    o.name = name
    o.data.materials.append(mat)
    o.location = (Vector(shift) * MM) - CENTER
    bpy.context.view_layer.objects.active = o
    o.select_set(True)
    if smooth:
        bpy.ops.object.shade_smooth_by_angle(angle=math.radians(32))
    o.select_set(False)
    return o


STAGES = 4
PARTS = {}


def build(screen_png):
    sc = reset()
    PARTS.clear()
    M = {
        "pa12": material("PA12 body #1c1c1e", "#1c1c1e", rough=0.62, spec=0.45, grain=0.12),
        "keys": material("keys #2a2a2d", "#2a2a2d", rough=0.42, spec=0.55, grain=0.05),
        "legend": material("legend WhiteSmoke", "#f5f5f5", rough=0.5),
        "glass": material("glass", "#020203", rough=0.02, spec=0.6, coat=0.6, coat_rough=0.02),
        "lcd": material("LCD SteelBlue", "#4682b4", rough=0.4, metal=0.35),
        "pcb": material("PCB DarkGreen", "#006400", rough=0.32, spec=0.6, coat=0.3, coat_rough=0.15),
        "black": material("Black", "#0c0c0d", rough=0.4),
        "silver": material("Silver", "#8c8c8c", rough=0.55, metal=0.85),
        "gray": material("Gray", "#808080", rough=0.35, metal=0.6),
        "dimgray": material("DimGray", "#696969", rough=0.4, metal=0.4),
        "teal": material("Teal", "#008080", rough=0.35),
        "darkred": material("DarkRed", "#8b0000", rough=0.3),
        "cam": material("camera #333", "#333333", rough=0.35, metal=0.4),
        "lens": material("lens #111", "#111111", rough=0.05, spec=0.8, coat=1.0, coat_rough=0.02),
        "yellow": material("flash Yellow", "#ffff00", rough=0.3, emit=("#ffff00", 0.4)),
        "purple": material("ALS Purple", "#800080", rough=0.3),
        "white": material("White", "#ffffff", rough=0.4),
        "gold": material("Gold", "#ffd700", rough=0.22, metal=1.0),
        "domes": material("dome Silver", "#c0c0c0", rough=0.25, metal=1.0),
        "led": material("LED White", "#ffffff", rough=0.3, emit=("#ffffff", 3.0)),
        "dim444": material("speaker #444", "#444444", rough=0.4, metal=0.3),
        "goldenrod": material("Goldenrod", "#daa520", rough=0.3, metal=0.9),
        "orange": material("Battery Orange", "#ffa500", rough=0.35),
        "green": material("PCM Green", "#008000", rough=0.35),
    }
    f = lambda n: os.path.join(STL, f"regular_{n}.stl")
    MB, KB = -25, -25
    # key: (stl, material, stage, offset (x, y, z) mm, smooth shading). Stage j leaves while ex goes from j to j+1: back shell first, the keys last.
    spec = {
        "keypad": (f("keypad"), "keys", 3, (0, 0, 54), True),
        "legend": (os.path.join(ROOT, "..", "print", "regular_markings.stl"), "legend", 3, (0, 0, 54), False),
        "front": (f("front_shell"), "pa12", 3, (0, 0, 34), True),
        "glass": (f("glass"), "glass", 3, (0, 0, 17), True),
        "punch": (f("punch"), "glass", 3, (0, 0, 17), True),
        "lcd": (f("lcd"), "lcd", 3, (0, 0, -11), False),
        "mb_pcb": (f("mb_pcb"), "pcb", 2, (0, 0, MB), True), "mb_black": (f("mb_black"), "black", 2, (0, 0, MB), False),
        "mb_silver": (f("mb_silver"), "silver", 2, (0, 0, MB), False), "mb_gray": (f("mb_gray"), "gray", 2, (0, 0, MB), True),
        "mb_dimgray": (f("mb_dimgray"), "dimgray", 2, (0, 0, MB), False), "mb_teal": (f("mb_teal"), "teal", 2, (0, 0, MB), False),
        "mb_darkred": (f("mb_darkred"), "darkred", 2, (0, 0, MB), False), "mb_cams": (f("mb_cams"), "cam", 2, (0, 0, MB), False),
        "mb_lens": (f("mb_lens"), "lens", 2, (0, 0, MB), True), "mb_flash": (f("mb_flash"), "yellow", 2, (0, 0, MB), False),
        "mb_als": (f("mb_als"), "purple", 2, (0, 0, MB), False), "mb_label": (f("mb_label"), "white", 2, (0, 0, MB), False),
        "kb_pcb": (f("kb_pcb"), "pcb", 2, (0, 0, KB), True), "kb_gold": (f("kb_gold"), "gold", 2, (0, 0, KB), False),
        "kb_domes": (f("kb_domes"), "domes", 2, (0, 0, KB), True), "kb_leds": (f("kb_leds"), "led", 2, (0, 0, KB), False),
        "kb_usb": (f("kb_usb"), "silver", 2, (0, 0, KB), False), "kb_jack": (f("kb_jack"), "dimgray", 2, (0, 0, KB), False),
        "kb_spk": (f("kb_spk"), "dim444", 2, (0, 0, KB), False), "kb_mic": (f("kb_mic"), "black", 2, (0, 0, KB), False),
        "kb_sim": (f("kb_sim"), "goldenrod", 2, (0, 0, KB), False),
        "batt_body": (f("batt_body"), "orange", 1, (0, 0, -42), False), "batt_pcm": (f("batt_pcm"), "green", 1, (0, 0, -42), False),
        "batt_fpc": (f("batt_fpc"), "gold", 1, (0, 0, -42), False), "fpcs": (f("fpcs"), "gold", 1, (0, 0, -42), False),
        "side_fpc": (f("side_fpc"), "gold", 0, (20, 0, -24), False),
        "sidekeys": (f("side_keys"), "keys", 0, (26, 0, -14), True),
        "back": (f("back_final"), "pa12", 0, (0, 0, -62), True),
    }
    for key, (path, mk, stage, off, sm) in spec.items():
        shift = (0, 0, -1.0) if key == "legend" else (0, 0, 0)     # assembly() lifts keypad and markings by -FRONT_T (= -1 mm)
        o = import_stl(key, path, M[mk], shift=shift, smooth=sm)
        o["stage"], o["off"] = stage, off
        o["base"] = tuple(o.location)
        if key == "lcd":
            inset_xy(o, 1.0)
        PARTS[key] = o
    # the lit screen: a plane exactly on the display area of the model (49.32 x 73.97 mm), with the real UI as an emissive texture
    x0, y0, x1, y1, z = 1.84, 68.0, 51.16, 141.97, -0.02
    bpy.ops.mesh.primitive_plane_add(size=1)
    scr = bpy.context.active_object
    scr.name = "display"
    mesh = scr.data
    for v, (px, py) in zip(mesh.vertices, [(x0, y0), (x1, y0), (x0, y1), (x1, y1)]):
        v.co = (px * MM, py * MM, 0)
    uv = mesh.uv_layers.new(name="UV")
    for loop in mesh.loops:
        v = mesh.vertices[loop.vertex_index].co
        uv.data[loop.index].uv = ((v.x / MM - x0) / (x1 - x0), (v.y / MM - y0) / (y1 - y0))
    scr.data.materials.append(screen_material(screen_png))
    scr.location = Vector((0, 0, z * MM)) - CENTER
    scr["stage"], scr["off"], scr["base"] = 3, (0, 0, 17), tuple(scr.location)
    PARTS["display"] = scr
    gl = scr.copy()
    gl.data = scr.data.copy()
    gl.data.materials.clear()
    gl.data.materials.append(glass_over_screen())
    gl.name = "display_glass"
    gl.location = scr.location + Vector((0, 0, 0.0004))
    gl["base"] = tuple(gl.location)
    bpy.context.collection.objects.link(gl)
    PARTS["display_glass"] = gl
    # everything hangs from one empty at the middle of the phone, so turning the phone is turning that one object
    root = bpy.data.objects.new("phone", None)
    bpy.context.collection.objects.link(root)
    for o in PARTS.values():
        o.parent = root
    return root


def smooth(t):
    t = max(0.0, min(1.0, t))
    return t * t * (3 - 2 * t)


def apply_explode(ex, hide=(), ry=0.0):
    """ex in 0..STAGES: step j of the teardown runs while ex goes from j to j+1 (back shell first, front keypad last)
    Seen from the front while the front shell is still on, the LCD keep-out box is not drawn: it is behind the glass anyway, but its corners
    graze the body's rounded corners and leave a blue sliver there."""
    front_view = math.cos(math.radians(ry)) > 0.05
    for name, o in PARTS.items():
        o.hide_render = o.hide_viewport = name in hide or (name == "lcd" and front_view and ex < 3.0)
        k = smooth(ex - o["stage"])
        o.location = Vector(o["base"]) + Vector(o["off"]) * MM * k


# ---------------------------------------------------------------- lights, floor, camera
def studio(sc, floor=True, y_floor=-0.088):
    w = bpy.data.worlds.new("world")
    sc.world = w
    w.use_nodes = True
    bg = w.node_tree.nodes["Background"]
    bg.inputs["Color"].default_value = (0.006, 0.007, 0.009, 1)
    bg.inputs["Strength"].default_value = 1.0

    def area(name, loc, size, power, color=(1, 1, 1), sy=None):
        d = bpy.data.lights.new(name, "AREA")
        d.shape = "RECTANGLE"
        d.size = size
        d.size_y = sy or size
        d.energy = power
        d.color = color
        o = bpy.data.objects.new(name, d)
        bpy.context.collection.objects.link(o)
        o.location = loc
        o.rotation_euler = look_at(loc, (0, 0, 0))
        return o
    # a soft key from above-left, two long strips for rim light on the edges, a cool light from the right, a warm kicker
    area("key", (-0.30, 0.42, 0.42), 0.40, 22, sy=0.30)
    area("strip_r", (0.34, 0.05, -0.18), 0.06, 6, sy=0.55, color=(0.85, 0.95, 1.0))
    area("strip_l", (-0.36, 0.05, -0.15), 0.06, 5, sy=0.55, color=(1.0, 0.93, 0.85))
    area("front_fill", (0.18, -0.05, 0.62), 0.5, 3, sy=0.5)
    area("top_strip", (0.0, 0.55, -0.05), 0.5, 14, sy=0.06)
    if floor:
        bpy.ops.mesh.primitive_plane_add(size=6)
        fl = bpy.context.active_object
        fl.name = "floor"
        fl.rotation_euler = Euler((-math.radians(90), 0, 0))      # a plane's normal is +Z: turn it so it faces up (+Y)
        fl.location = (0, y_floor, 0)
        fl.data.materials.append(material("floor", "#08090b", rough=0.16, spec=0.6))


def camera(sc, loc, lens=70, fstop=0.0):
    cd = bpy.data.cameras.new("cam")
    cd.lens = lens
    cd.sensor_width = 36
    cd.clip_start = 0.01
    cd.clip_end = 20
    if fstop:
        cd.dof.use_dof = True
        cd.dof.aperture_fstop = fstop
        cd.dof.focus_distance = Vector(loc).length
    co = bpy.data.objects.new("cam", cd)
    bpy.context.collection.objects.link(co)
    co.location = loc
    co.rotation_euler = look_at(loc, (0, 0, 0))
    sc.camera = co
    return co


ALL_BUT_BOARDS = ("keypad", "legend", "front", "glass", "punch", "lcd", "display", "display_glass", "batt_body", "batt_pcm", "batt_fpc", "fpcs", "back", "sidekeys", "side_fpc")

SHOTS = {
    # ry: turn about the vertical axis (deg), rx: tilt, ex: teardown progress 0..4, off: the point (mm from the middle of the phone) placed at the frame centre
    "hero":      dict(ry=-30, rx=8, cam=(0.0, 0.03, 0.44), lens=85, ex=0.0, off=(0, 0, 0), size=(1080, 1920)),
    "front":     dict(ry=0, rx=0, cam=(0.0, 0.0, 0.85), lens=110, ex=0.0, off=(0, 0, 0), size=(1080, 1920)),
    "back":      dict(ry=152, rx=8, cam=(0.0, 0.03, 0.44), lens=85, ex=0.0, off=(0, 0, 0), size=(1080, 1920), exposure=-1.35),
    "side":      dict(ry=-78, rx=6, cam=(0.0, 0.02, 0.42), lens=85, ex=0.0, off=(0, 0, 0), size=(1080, 1920)),
    "keys":      dict(ry=-14, rx=34, cam=(0.0, -0.01, 0.24), lens=90, ex=0.0, off=(0, -50, 0), size=(1080, 1920), fstop=5.6),
    "exploded":  dict(ry=-38, rx=16, cam=(0.0, 0.02, 0.66), lens=80, ex=4.0, off=(0, 0, 0), size=(1080, 1920), exposure=-1.0),
    "exploded_wide": dict(ry=-70, rx=7, cam=(0.0, 0.0, 0.54), lens=70, ex=4.0, off=(0, 0, 0), size=(2560, 1440), exposure=-1.0),
    "teardown":  dict(ry=160, rx=14, cam=(0.0, 0.02, 0.56), lens=80, ex=1.7, off=(0, 0, 0), size=(1080, 1920), exposure=-1.6),
    "electronics": dict(ry=180, rx=12, cam=(0.0, 0.0, 0.46), lens=90, ex=0.0, off=(0, 0, 0), size=(1080, 1920), hide=ALL_BUT_BOARDS, exposure=-2.0),
    "electronics_3q": dict(ry=150, rx=28, cam=(0.0, 0.0, 0.42), lens=90, ex=0.0, off=(0, 0, 0), size=(1080, 1920), hide=ALL_BUT_BOARDS, exposure=-2.0),
    "open_back": dict(ry=158, rx=12, cam=(0.0, 0.02, 0.45), lens=85, ex=0.0, off=(0, 0, 0), size=(1080, 1920), hide=("back", "sidekeys"), exposure=-2.4),
    "som":       dict(ry=172, rx=16, cam=(0.0, 0.0, 0.26), lens=90, ex=0.0, off=(0, 40, 0), size=(1080, 1920), hide=ALL_BUT_BOARDS, exposure=-2.5, fstop=6.3),
}


def place(root, ry, rx, rz=0, off=(0, 0, 0)):
    root.rotation_mode = "XYZ"
    e = Euler((math.radians(rx), math.radians(ry), math.radians(rz)), "XYZ")
    root.rotation_euler = e
    # turn around the middle of the phone (the parts already hold their offset from it), then slide so `off` (mm from the middle) sits at the origin
    root.location = -(e.to_matrix() @ (Vector(off) * MM))


def render(sc, path, w, h, samples):
    sc.render.resolution_x, sc.render.resolution_y = w, h
    sc.render.resolution_percentage = 100
    sc.cycles.samples = samples
    sc.render.filepath = path
    bpy.ops.render.render(write_still=True)


def setup(shot, screen_png, floor=True):
    root = build(screen_png)
    sc = bpy.context.scene
    s = SHOTS[shot]
    apply_explode(s["ex"], tuple(s.get("hide", ())) + tuple(h for h in os.environ.get("HIDE", "").split(",") if h), ry=s["ry"])
    place(root, s["ry"], s["rx"], 0, s["off"])
    studio(sc, floor=floor)
    sc.view_settings.exposure = s.get("exposure", -0.25)
    camera(sc, s["cam"], s["lens"], fstop=s.get("fstop", 0))
    return s


# ---------------------------------------------------------------- the film: one turn of the closed phone, then the teardown, then it all comes back
# Timed for the reel's music (120 BPM, a beat every 0.5 s): the turn takes 3 s (6 beats), each of the four teardown steps takes 1 s (2 beats):
# 3 s back shell, 4 s battery, 5 s boards, 6 s LCD + front + keys; then a hold, then everything snaps back on the last two beats.
def film_state(t, T=9.0):
    if t < 3.0:                                    # 1. the closed phone, one full turn, screen on
        ry, ex, rx = -34 + 360 * smooth(t / 3.0), 0.0, 8
    elif t < 7.0:                                  # 2. teardown, one part group per second
        v = (t - 3.0) / 4.0
        ex = 4.0 * v
        ry = -34 - 26 * smooth(v)
        rx = 8 + 10 * smooth(v)
    elif t < 8.0:                                  # 3. hold, drifting a little
        v = t - 7.0
        ex, ry, rx = 4.0, -60 - 6 * v, 18 + 2 * v
    else:                                          # 4. back together
        v = smooth(t - 8.0)
        ex, ry, rx = 4.0 * (1 - v), -66 + 32 * v, 20 - 12 * v
    dist = 0.44 + 0.30 * smooth(ex / 2.2)
    return ry, rx, ex, dist


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("mode", choices=["still", "video", "list"])
    ap.add_argument("name", nargs="?", default="")
    ap.add_argument("out", nargs="?", default="")
    ap.add_argument("--samples", type=int, default=160)
    ap.add_argument("--seconds", type=float, default=9)
    ap.add_argument("--fps", type=int, default=24)
    ap.add_argument("--from", dest="a", type=int, default=0)
    ap.add_argument("--to", dest="b", type=int, default=-1)
    ap.add_argument("--scale", type=float, default=1.0, help="multiplies the preset size (0.5 for a quick test)")
    ap.add_argument("--screen", default=os.path.join(ROOT, "screen.png"))
    ap.add_argument("--nofloor", action="store_true")
    a = ap.parse_args(sys.argv[1:])
    if a.mode == "list":
        print("\n".join(SHOTS))
        return
    if a.mode == "still":
        s = setup(a.name, a.screen, floor=not a.nofloor)
        os.makedirs(os.path.dirname(os.path.abspath(a.out)), exist_ok=True)
        render(bpy.context.scene, a.out, int(s["size"][0] * a.scale), int(s["size"][1] * a.scale), a.samples)
        return
    root = build(a.screen)
    sc = bpy.context.scene
    studio(sc, floor=not a.nofloor)
    cam = camera(sc, (0, 0.03, 0.44), 85)
    os.makedirs(a.out, exist_ok=True)
    n = int(a.seconds * a.fps)
    for i in range(a.a, n if a.b < 0 else min(a.b, n)):
        ry, rx, ex, dist = film_state(i / a.fps, a.seconds)
        apply_explode(ex, ry=ry)
        place(root, ry, rx, 0, (0, 0, 0))
        cam.location = (0.0, 0.03, dist)
        cam.rotation_euler = look_at(cam.location, (0, 0, 0))
        render(sc, os.path.join(a.out, f"f_{i:05d}.png"), int(1080 * a.scale), int(1920 * a.scale), a.samples)
        print("frame", i, "of", n, flush=True)


if __name__ == "__main__":
    main()
