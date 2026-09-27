"""Minimal KiCad s-expression reader/writer used by the generator scripts."""
import re

_TOKEN = re.compile(r'\s*(?:(\()|(\))|("(?:[^"\\]|\\.)*")|([^\s()"]+))')


class Sym(str):
    """Unquoted atom (keyword / number). Plain str = quoted string."""


def _unescape(s):
    return re.sub(r'\\(.)', r'\1', s)


def _escape(s):
    return s.replace('\\', '\\\\').replace('"', '\\"')


def parse(text):
    stack, cur = [], []
    pos = 0
    while True:
        m = _TOKEN.match(text, pos)
        if not m or m.end() == pos:
            break
        pos = m.end()
        if m.group(1):
            stack.append(cur)
            cur = []
        elif m.group(2):
            done = cur
            cur = stack.pop()
            cur.append(done)
        elif m.group(3) is not None:
            cur.append(_unescape(m.group(3)[1:-1]))
        else:
            cur.append(Sym(m.group(4)))
    return cur[0]


def fmt(v):
    s = ("%.4f" % v).rstrip("0").rstrip(".")
    return "0" if s in ("-0", "") else s


def dump(node, indent=0):
    if isinstance(node, list):
        if not node:
            return "()"
        if all(not isinstance(x, list) for x in node):
            return "(" + " ".join(dump(x) for x in node) + ")"
        pad = "\t" * (indent + 1)
        out = "(" + dump(node[0])
        for x in node[1:]:
            out += ("\n" + pad + dump(x, indent + 1)) if isinstance(x, list) else (" " + dump(x))
        return out + "\n" + "\t" * indent + ")"
    if isinstance(node, Sym):
        return str(node)
    if isinstance(node, bool):
        return "yes" if node else "no"
    if isinstance(node, (int, float)):
        return fmt(node)
    return '"' + _escape(str(node)) + '"'


def find(node, key):
    return [x for x in node if isinstance(x, list) and x and x[0] == key]


def first(node, key):
    r = find(node, key)
    return r[0] if r else None


def load_symbol(lib_path, name):
    """Return a symbol node from a .kicad_sym, flattening 'extends'."""
    tree = parse(open(lib_path, encoding="utf-8").read())
    syms = {s[1]: s for s in find(tree, "symbol")}
    node = syms[name]
    ext = first(node, "extends")
    if ext:
        base = syms[ext[1]]
        props = {p[1]: p for p in find(node, "property")}
        merged = [Sym("symbol"), name]
        for x in base[2:]:
            if isinstance(x, list) and x[0] == "property" and x[1] in props:
                merged.append(props.pop(x[1]))
            elif isinstance(x, list) and x[0] == "symbol":
                sub = list(x)
                sub[1] = name + sub[1][len(ext[1]):]
                merged.append(sub)
            else:
                merged.append(x)
        # keep derived properties that the base lacks, before the unit sub-symbols
        idx = next(i for i, x in enumerate(merged) if isinstance(x, list) and x[0] == "symbol")
        merged[idx:idx] = list(props.values())
        node = merged
    return node


def symbol_pins(sym):
    pins = []
    for unit in find(sym, "symbol"):
        for p in find(unit, "pin"):
            at = first(p, "at")
            pins.append({
                "number": first(p, "number")[1],
                "name": first(p, "name")[1],
                "type": str(p[1]),
                "x": float(at[1]), "y": float(at[2]),
                "rot": float(at[3]) if len(at) > 3 else 0.0,
                "len": float(first(p, "length")[1]),
            })
    return pins
