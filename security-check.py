#!/usr/bin/env python3
"""
FutureOS security gate. Run before every release build:

    python security-check.py

Scans every app in the repo and fails (exit 1) on regressions that have
already bitten this project once:

  * an exported receiver/service/provider with no android:permission whose
    intent-filter is not a system action (any app on the device could drive it)
  * an exported provider without a permission
  * cleartext HTTP (usesCleartextTraffic / http:// endpoints in code)
  * committed secrets (API keys, private keys, tokens)
  * a mutable PendingIntent wrapping an implicit Intent
  * a trust-all TrustManager / HostnameVerifier, or a WebView JS bridge
  * user-controlled strings concatenated into a root shell command without
    RootShell.quote()

Output is English on purpose: the Windows terminal renders Hebrew reversed.
"""
import pathlib
import re
import sys
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parent
ANDROID = "{http://schemas.android.com/apk/res/android}"
SKIP_DIRS = {"build", ".gradle", "node_modules", ".git", "lib", "output"}

# Actions only the system can send (protected broadcasts) or that the system
# binds with its own permission, so an exported component without a
# permission is expected for them.
SYSTEM_ACTIONS = {
    "android.appwidget.action.APPWIDGET_UPDATE",
    "android.intent.action.BOOT_COMPLETED",
    "android.intent.action.LOCKED_BOOT_COMPLETED",
    "android.intent.action.MY_PACKAGE_REPLACED",
    "android.intent.action.TIME_SET",
    "android.intent.action.TIMEZONE_CHANGED",
    "androidx.media3.session.MediaSessionService",
    "android.media.browse.MediaBrowserService",
}

# Deliberate exceptions, each with the reason it is safe.
ALLOWED_EXPORTS = {
    # Key-press relays stay open so adb `am broadcast` can drive UI tests.
}

# Git-ignored local config: holding keys is exactly their job, so they are not
# scanned. What matters is that they never get committed (see .gitignore).
LOCAL_CONFIG = {"local.properties", "keystore.properties", "google-services.json", "firebase.json"}

SECRET_PATTERNS = [
    (re.compile(r"AIza[0-9A-Za-z_\-]{35}"), "Google API key"),
    (re.compile(r"-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----"), "private key"),
    (re.compile(r"ghp_[A-Za-z0-9]{36}"), "GitHub token"),
    (re.compile(r"sk-(ant-)?[A-Za-z0-9_\-]{32,}"), "API secret key"),
    (re.compile(r"xox[baprs]-[A-Za-z0-9\-]{10,}"), "Slack token"),
]

CODE_RULES = [
    (re.compile(r"checkServerTrusted\s*\([^)]*\)\s*(\{\s*\}|=\s*Unit)"), "trust-all TrustManager"),
    (re.compile(r"HostnameVerifier\s*\{\s*_?,?\s*_?\s*->\s*true"), "HostnameVerifier that accepts everything"),
    (re.compile(r"addJavascriptInterface\s*\("), "WebView JavaScript bridge"),
    (re.compile(r"MODE_WORLD_(READABLE|WRITEABLE)"), "world-accessible file"),
    (re.compile(r"setReadable\(\s*true\s*,\s*false\s*\)"), "world-readable file"),
]

# `pm`/`am`/`settings` with a bare $variable that is not a Boolean/Int switch.
ROOT_CMD = re.compile(r'(runRootCommands?|RootShell\.run|root)\(\s*"(am|pm|settings put|appops)')
ROOT_VAR = re.compile(r'\$\{?([a-zA-Z_]+)')
# Booleans, Ints and constants - nothing an outside caller can shape into shell syntax.
ROOT_SAFE_VARS = {"v", "on", "value", "state", "enabled", "percent", "dpi", "ns", "key",
                  "if", "context", "subscriptionId", "Settings"}


def walk(pattern):
    for path in ROOT.rglob(pattern):
        if any(part in SKIP_DIRS for part in path.relative_to(ROOT).parts):
            continue
        yield path


def check_manifests(problems):
    for manifest in walk("AndroidManifest.xml"):
        if "src/main" not in manifest.as_posix():
            continue
        rel = manifest.relative_to(ROOT).as_posix()
        try:
            tree = ET.parse(manifest)
        except ET.ParseError as e:
            problems.append(f"{rel}: cannot parse ({e})")
            continue
        app = tree.getroot().find("application")
        if app is None:
            continue
        if app.get(ANDROID + "usesCleartextTraffic") == "true":
            problems.append(f"{rel}: usesCleartextTraffic=true")
        if app.get(ANDROID + "debuggable") == "true":
            problems.append(f"{rel}: android:debuggable=true")
        for comp in app:
            kind = comp.tag
            if kind not in ("receiver", "service", "provider"):
                continue
            if comp.get(ANDROID + "exported") != "true" or comp.get(ANDROID + "permission"):
                continue
            name = comp.get(ANDROID + "name", "?")
            if f"{rel}#{name}" in ALLOWED_EXPORTS:
                continue
            if kind == "provider":
                problems.append(f"{rel}: exported provider {name} has no android:permission")
                continue
            actions = {a.get(ANDROID + "name") for a in comp.iter("action")}
            custom = sorted(a for a in actions if a not in SYSTEM_ACTIONS)
            if not actions or custom:
                problems.append(
                    f"{rel}: exported {kind} {name} has no android:permission"
                    + (f" (custom actions: {', '.join(custom)})" if custom else " (no intent-filter)")
                )


def check_sources(problems):
    for path in list(walk("*.kt")) + list(walk("*.java")) + list(walk("*.ts")) + list(walk("*.properties")) + list(walk("*.json")):
        rel = path.relative_to(ROOT).as_posix()
        if "/test/" in rel or rel.endswith("package-lock.json"):
            continue
        if path.name in LOCAL_CONFIG:
            continue
        try:
            text = path.read_text(encoding="utf-8", errors="ignore")
        except OSError:
            continue
        for lineno, line in enumerate(text.splitlines(), 1):
            for rx, label in SECRET_PATTERNS:
                if rx.search(line):
                    problems.append(f"{rel}:{lineno}: possible {label} committed")
            if path.suffix in (".kt", ".java"):
                for rx, label in CODE_RULES:
                    if rx.search(line):
                        problems.append(f"{rel}:{lineno}: {label}")
                if re.search(r'"http://(?!schemas\.android\.com|www\.w3\.org|www\.siri\.org|localhost|127\.0\.0\.1)', line) and "startsWith" not in line:
                    problems.append(f"{rel}:{lineno}: cleartext http:// endpoint")
                if "FLAG_MUTABLE" in line and "setComponent" not in text and "::class.java" not in text:
                    problems.append(f"{rel}:{lineno}: FLAG_MUTABLE PendingIntent without an explicit component")
                if ROOT_CMD.search(line):
                    for var in ROOT_VAR.findall(re.sub(r"\$\{RootShell\.quote\([^)]*\)\}", "", line)):
                        if var not in ROOT_SAFE_VARS:
                            problems.append(f"{rel}:{lineno}: root command interpolates ${var} without RootShell.quote()")


def main():
    problems = []
    check_manifests(problems)
    check_sources(problems)
    if problems:
        print(f"FutureOS security check: {len(problems)} problem(s)")
        for p in problems:
            print("  - " + p)
        return 1
    print("FutureOS security check: OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
