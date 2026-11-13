#!/usr/bin/env python3
"""Future Phone landing page + a real signup list.

    python server.py            # http://localhost:8080
    python server.py 9000       # another port

Serves this folder and accepts POST /api/signup.
Every signup is appended to signups.csv next to this file (open it in Excel).
There is no endpoint that lists signups, so the list is only readable on the machine that runs this.
"""
import csv, json, os, re, sys, threading, time
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer

HERE = os.path.dirname(os.path.abspath(__file__))
CSV = os.path.join(HERE, "signups.csv")
LOCK = threading.Lock()
HITS = {}                      # ip -> [timestamps], simple rate limit
LIMIT, WINDOW = 8, 3600        # 8 signups per hour per address
EMAIL = re.compile(r"^[^@\s]+@[^@\s]+\.[^@\s]+$")
PHONE = re.compile(r"^\+?[0-9][0-9\-\s]{6,16}[0-9]$")


def clean(v, n):
    v = (v or "").strip()
    v = re.sub(r"[\x00-\x1f\x7f]", " ", v)
    # keep spreadsheet formulas from running when the CSV is opened in Excel
    if v[:1] in "=+-@" and not PHONE.match(v):
        v = "'" + v
    return v[:n]


def save(name, contact, ip):
    new = not os.path.exists(CSV)
    with LOCK:
        if os.path.exists(CSV):
            with open(CSV, encoding="utf-8-sig", newline="") as f:
                for row in csv.reader(f):
                    if len(row) > 2 and row[2].strip().lower() == contact.lower():
                        return "exists"
        with open(CSV, "a", encoding="utf-8-sig", newline="") as f:
            w = csv.writer(f)
            if new:
                w.writerow(["זמן", "שם", "טלפון או מייל", "כתובת"])
            w.writerow([time.strftime("%Y-%m-%d %H:%M:%S"), name, contact, ip])
    return "ok"


class Handler(SimpleHTTPRequestHandler):
    def __init__(self, *a, **k):
        super().__init__(*a, directory=HERE, **k)

    def end_headers(self):
        self.send_header("Cache-Control", "no-store")
        super().end_headers()

    def send_json(self, code, obj):
        body = json.dumps(obj, ensure_ascii=False).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        # the list and the server itself are not for visitors
        if self.path.split("?")[0].lower().endswith((".csv", ".py")):
            return self.send_error(404)
        return super().do_GET()

    def do_POST(self):
        if self.path != "/api/signup":
            return self.send_error(404)
        try:
            n = int(self.headers.get("Content-Length") or 0)
            if n > 4096:
                return self.send_json(413, {"error": "too_big"})
            data = json.loads(self.rfile.read(n) or b"{}")
        except Exception:
            return self.send_json(400, {"error": "bad_request"})
        if data.get("website"):                      # honeypot
            return self.send_json(200, {"ok": True})
        name, contact = clean(data.get("name"), 80), clean(data.get("contact"), 120)
        if not (EMAIL.match(contact) or PHONE.match(contact)):
            return self.send_json(422, {"error": "invalid_contact"})
        ip = self.client_address[0]
        now = time.time()
        hits = [t for t in HITS.get(ip, []) if now - t < WINDOW]
        if len(hits) >= LIMIT:
            return self.send_json(429, {"error": "slow_down"})
        HITS[ip] = hits + [now]
        res = save(name, contact, ip)
        return self.send_json(200, {"ok": True, "status": res})

    def log_message(self, fmt, *args):
        sys.stderr.write("%s - %s\n" % (self.address_string(), fmt % args))


if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8080
    print(f"http://localhost:{port}   signups -> {CSV}")
    ThreadingHTTPServer(("0.0.0.0", port), Handler).serve_forever()
