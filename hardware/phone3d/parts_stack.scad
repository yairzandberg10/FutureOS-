// The electronics of the FutureOS Regular, one STL per material, in the same assembly coordinates as parts.scad.
// It re-uses futureos_mainboard.scad unchanged (its own modules and its own layout tables: MB_LAYOUT, KB_STRIP, dome_points()...) and only splits
// what that file draws in one call into groups by colour, because an STL has no colours.
// Usage: openscad -D 'MODEL="regular"' -D 'PART="none"' -D 'MB_PART="none"' -D 'OUT="mb_pcb"' -o stl/regular_mb_pcb.stl parts_stack.scad
include <../openscad/futureos_mainboard.scad>
OUT = "mb_pcb";

module mb_group(col) {
    for (p = MB_LAYOUT) if (part_color(p[0]) == col)
        if (p[0] == "MOTOR") translate([MB_X0 + p[1] + p[3] / 2, MB_Y0 + p[2] + p[4] / 2, MB_BOT - p[5]]) cylinder(d = p[3], h = p[5]);
        else translate([MB_X0 + p[1], MB_Y0 + p[2], MB_BOT - p[5]]) cube([p[3], p[4], p[5]]);
}
module strip_group(name) {
    for (p = KB_STRIP) if (p[0] == name) part_down(p[1], p[2], KB_BOT, p[2][2]);
}

if (OUT == "mb_pcb")        mb_pcb();
if (OUT == "mb_black")      mb_group("Black");
if (OUT == "mb_silver")     mb_group("Silver");
if (OUT == "mb_gray")       mb_group("Gray");
if (OUT == "mb_dimgray")    mb_group("DimGray");
if (OUT == "mb_teal")       mb_group("Teal");
if (OUT == "mb_darkred")    mb_group("DarkRed");
if (OUT == "mb_cams")       {
    translate([CAM_XY[0] - RCAM[0] / 2, CAM_XY[1] - RCAM[1] / 2, RCAM_TOP - RCAM[2]]) cube([RCAM[0], RCAM[1], RCAM[2]]);
    if (HAS_FRONT_CAM) translate([PUNCH_XY[0] - FCAM[0] / 2, PUNCH_XY[1] - FCAM[1] / 2, -0.6 - FCAM[2]]) cube(FCAM);
}
if (OUT == "mb_lens")       translate([CAM_XY[0], CAM_XY[1], RCAM_TOP - RCAM[2] - 0.05]) cylinder(d = RCAM[0] * 0.6, h = 0.05);
if (OUT == "mb_flash")      translate([FLASH_XY[0] - 1.5, FLASH_XY[1] - 1.5, Z_BACK_IN]) cube([3, 3, 0.8]);
if (OUT == "mb_als")        translate([ALS_XY[0] - 1, ALS_XY[1] - 1, -0.6 - 1.0]) cube([2, 2, 1.0]);
if (OUT == "mb_label")      label(MODEL == "mini" ? "SC200E" : "SC680A", mb_center("SOM"), MB_BOT - SOM[2] - 0.06, 3, back = true);

if (OUT == "kb_pcb")        kb_pcb();
if (OUT == "kb_gold")       {
    for (p = dome_points()) translate([p[0], p[1], KB_TOP]) cylinder(d = DOME_D, h = 0.02);
    part_down(KB_B2B, [8, 3.2], KB_BOT, 0.15);
}
if (OUT == "kb_domes")      for (p = dome_points()) translate([p[0], p[1], KB_TOP]) scale([1, 1, 0.3 / (DOME_D / 2)]) sphere(d = DOME_D);
if (OUT == "kb_leds")       for (p = dome_points()) translate([p[0] + DOME_D / 2 + 0.6, p[1] - 0.4, KB_TOP]) cube([0.8, 0.8, 0.4]);
if (OUT == "kb_usb")        strip_group("USB");
if (OUT == "kb_jack")       strip_group("JACK");
if (OUT == "kb_spk")        strip_group("SPK");
if (OUT == "kb_mic")        strip_group("MIC");
if (OUT == "kb_sim")        strip_group("SIM");

if (OUT == "batt_body")     translate([BATT_X0, BATT_Y0, Z_BATT_BOT]) cube(BATT);
if (OUT == "batt_pcm")      { pcm_x = min(mb_center("BATT")[0], BATT_X0 + BATT[0] - 6); translate([pcm_x - 6, BATT_Y1 - 3, Z_BATT_BOT]) cube([12, 3, BATT[2]]); }
if (OUT == "batt_fpc")      { pcm_x = min(mb_center("BATT")[0], BATT_X0 + BATT[0] - 6); translate([pcm_x - 3, BATT_Y1 - 1, MB_BOT - 0.95]) cube([6, MB_Y0 - BATT_Y1 + 3, 0.15]); }
if (OUT == "fpcs")          fpcs_geometry();
if (OUT == "antenna")       antenna_geometry();
if (OUT == "back_final")    translate([0, 0, -BODY_T]) back_shell_final();   // back_shell_final() is in print orientation (inside up): move it to where assembly() puts the back

module fpcs_geometry() {
    z = Z_BATT_TOP + 0.1;
    translate([min(mb_center("LCD")[0], MB_X1 - 4.5) - 4.5, Y_SCR - 1, z]) cube([9, MB_Y0 - Y_SCR + 3, 0.12]);
    translate([min(mb_center("MAIN")[0], MB_X1 - 4) - 4, KB_Y1 - 4, z]) cube([8, MB_Y0 - KB_Y1 + 7, 0.12]);
}
module antenna_geometry() {
    translate([IN_X0, IN_Y0, Z_BACK_IN]) cube([IN_X1 - IN_X0, ANT_H, -Z_BACK_IN - FRONT_T]);
    translate([IN_X0, IN_Y1 - ANT_H, Z_BACK_IN]) cube([IN_X1 - IN_X0, ANT_H, -Z_BACK_IN - FRONT_T]);
}
