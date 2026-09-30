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
  * key-press broadcasts (Options / * / #) sent or received without the suite
    signature permission (any app could inject key presses)
  * tel: URIs built by string concatenation ("#" truncates the number)
  * an exported *SettingsActivity without android:permission, and
    allowBackup="true" on the System UI (lock-screen data in backups)
  * a GitHub Actions step not pinned to a commit SHA, or a workflow without a
    top-level permissions: block
  * allowBackup="true" in ANY app (chat database, learned words and shell history
    would be uploaded to cloud backup), not only in the System UI
  * a gradlew / build script committed without the executable bit (CI died with
    "Permission denied" on every job, so the build gate never ran)
  * FutureUIActions.PERMISSION_SYSTEM missing, or not equal to the signature
    permission the shared library manifest declares
  * an exported media3 MediaSessionService that lets any app load content
  * the lock-screen window letting touches through to the windows beneath it
  * personal data (numbers, addresses, PINs) in Log.e/w/i calls, which R8 keeps
  * Firebase rules that are open (`if true`) or accept uploads without a content type
  * an API key baked into BuildConfig straight from local.properties
  * the System UI decoding another app's icon without loadUntrusted() (an
    OutOfMemoryError from a hostile bitmap would crash the lock screen's process)
  * .gitignore missing the entries that keep local keys/config out of git

Output is English on purpose: the Windows terminal renders Hebrew reversed.
"""
import pathlib
import re
import subprocess
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
    # Key-press relays are protected in code (KeyPressBroadcasts): suite
    # signature permission, or android.permission.DUMP so adb still works.
}

KEY_PRESS = re.compile(r"ACTION_(OPTIONS|STAR|POUND)_SHORT_PRESS")
SYSTEM_UI_APPS = {"FutureUI", "SystemUI"}

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
ROOT_CMD = re.compile(r'(runRootCommand(s|Async)?|RootShell\.run|root)\(\s*"(am|pm|settings put|appops)|"am force-stop \$')
ROOT_VAR = re.compile(r'\$\{?([a-zA-Z_]+)')
# Booleans, Ints and constants - nothing an outside caller can shape into shell syntax.
ROOT_SAFE_VARS = {"v", "on", "value", "state", "enabled", "percent", "dpi", "ns", "key", "packageName",
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
        # The shared library manifest has no backup policy of its own; every app does.
        if not rel.startswith("SharedKeypadNav/") and app.get(ANDROID + "allowBackup") != "false":
            problems.append(f"{rel}: allowBackup must be false (app data would be uploaded to cloud backup)")
        for comp in app:
            kind = comp.tag
            if (kind == "activity" and comp.get(ANDROID + "exported") == "true"
                    and comp.get(ANDROID + "name", "").endswith("SettingsActivity")
                    and not comp.get(ANDROID + "permission")
                    and rel.split("/")[0] in SYSTEM_UI_APPS):
                problems.append(f"{rel}: exported {comp.get(ANDROID + 'name')} has no android:permission")
            if kind not in ("receiver", "service", "provider"):
                continue
            if comp.get(ANDROID + "exported") != "true" or comp.get(ANDROID + "permission"):
                continue
            if kind == "provider" and comp.get(ANDROID + "readPermission") and comp.get(ANDROID + "writePermission"):
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
                if re.search(r'Log\.(e|w|i)\(.*\$\{?(number|phone|phoneNumber|address|body|pin)\b', line):
                    problems.append(f"{rel}:{lineno}: personal data in a Log.e/w/i call (R8 only strips v/d; logcat is readable over adb)")
                if re.search(r'Uri\.parse\(\s*"tel:\s*\$', line):
                    problems.append(f"{rel}:{lineno}: tel: URI built by concatenation - use Uri.fromParts(\"tel\", n, null)")
                if KEY_PRESS.search(line) and "sendBroadcast(" in line and "PERMISSION_SYSTEM" not in line and "setPackage(" not in line:
                    problems.append(f"{rel}:{lineno}: key-press broadcast sent without FutureUIActions.PERMISSION_SYSTEM")
                if "RECEIVER_EXPORTED" in line and "PERMISSION" not in line and KEY_PRESS.search(text) and not rel.endswith("KeyPressBroadcasts.kt"):
                    problems.append(f"{rel}:{lineno}: exported key-press receiver - register it through KeyPressBroadcasts")
                if ROOT_CMD.search(line):
                    for var in ROOT_VAR.findall(re.sub(r"\$\{(?:[\w.]+\.)?RootShell\.quote\([^)]*\)\}", "", line)):
                        if var not in ROOT_SAFE_VARS:
                            problems.append(f"{rel}:{lineno}: root command interpolates ${var} without RootShell.quote()")


def check_workflows(problems):
    """CI supply chain (OWASP A03): a tag can be moved to other code, a SHA
    cannot, and GITHUB_TOKEN must not default to write access."""
    for wf in (ROOT / ".github" / "workflows").glob("*.y*ml"):
        rel = wf.relative_to(ROOT).as_posix()
        text = wf.read_text(encoding="utf-8", errors="ignore")
        if not re.search(r"^permissions:", text, re.M):
            problems.append(f"{rel}: no top-level permissions: block (GITHUB_TOKEN gets the repo default)")
        for lineno, line in enumerate(text.splitlines(), 1):
            m = re.search(r"uses:\s*([^\s#]+)@([^\s#]+)", line)
            if m and not m.group(1).startswith("./") and not re.fullmatch(r"[0-9a-f]{40}", m.group(2)):
                problems.append(f"{rel}:{lineno}: action {m.group(1)}@{m.group(2)} not pinned to a commit SHA")


def _git(*args):
    try:
        out = subprocess.run(["git", *args], cwd=ROOT, capture_output=True, text=True, timeout=60)
    except (OSError, subprocess.SubprocessError):
        return None
    return out.stdout if out.returncode == 0 else None


def _code_lines(text):
    """(lineno, line) without whole-line // and * comments."""
    for lineno, line in enumerate(text.splitlines(), 1):
        stripped = line.strip()
        if stripped.startswith("//") or stripped.startswith("*") or stripped.startswith("/*"):
            continue
        yield lineno, line


def check_executable_scripts(problems):
    """A script committed as 100644 fails with "Permission denied" on Linux CI -
    that is exactly how every build job failed and the build gate never ran."""
    listing = _git("ls-files", "-s")
    if listing is None:
        return  # not a git checkout (release tarball): nothing to verify
    for row in listing.splitlines():
        mode, _, rest = row.partition(" ")
        path = rest.split("\t", 1)[-1]
        name = path.rsplit("/", 1)[-1]
        if name == "gradlew" or path in ("build-all.sh", "SystemUI/sync-from-futureui.sh"):
            if mode != "100755":
                problems.append(f"{path}: committed with mode {mode}, needs 100755 (git update-index --chmod=+x)")


def check_gitignore(problems):
    text = (ROOT / ".gitignore").read_text(encoding="utf-8", errors="ignore")
    lines = {ln.strip() for ln in text.splitlines()}
    for entry in ("keystore.properties", "*.jks", "*.keystore", "local.properties", "google-services.json",
                  "Wallpapers/app/src/main/assets/firebase.json"):
        if entry not in lines:
            problems.append(f".gitignore: missing '{entry}' (local keys/config would be committed by `git add -A`)")


def check_system_permission(problems):
    """FutureUIActions.PERMISSION_SYSTEM guards every key-press and call broadcast.
    It was referenced in ten places and defined in none."""
    actions = ROOT / "SharedKeypadNav/sharedkeypadnav/src/main/java/com/future/sharednav/actions/FutureUIActions.kt"
    target = ROOT / "SharedKeypadNav/sharedkeypadnav/src/main/java/com/future/sharednav/systemui/SystemUiTarget.kt"
    manifest = ROOT / "SharedKeypadNav/sharedkeypadnav/src/main/AndroidManifest.xml"
    used = any(
        "PERMISSION_SYSTEM" in path.read_text(encoding="utf-8", errors="ignore")
        for path in walk("*.kt") if path != actions and "/test/" not in path.as_posix()
    )
    text = actions.read_text(encoding="utf-8", errors="ignore")
    m = re.search(r'const val PERMISSION_SYSTEM\s*=\s*"([^"]+)"', text)
    if not m:
        if used:
            problems.append(f"{actions.relative_to(ROOT).as_posix()}: PERMISSION_SYSTEM is used but not defined")
        return
    name = m.group(1)
    pkg = re.search(r'const val PACKAGE\s*=\s*"([^"]+)"', target.read_text(encoding="utf-8", errors="ignore"))
    if pkg:
        name = name.replace("${SystemUiTarget.PACKAGE}", pkg.group(1))
    declared = {
        p.get(ANDROID + "name"): p.get(ANDROID + "protectionLevel")
        for p in ET.parse(manifest).getroot().findall("permission")
    }
    if declared.get(name) != "signature":
        problems.append(f"PERMISSION_SYSTEM = {name} is not declared protectionLevel=signature in {manifest.relative_to(ROOT).as_posix()}")


def check_media_sessions(problems):
    """An exported MediaSessionService accepts any controller. media3's defaults let
    that controller set media items, i.e. make the player open content:// or file://
    of its choosing with the app's storage permissions."""
    for manifest in walk("AndroidManifest.xml"):
        if "src/main" not in manifest.as_posix() or manifest.parts[-5:-4] == ("sharedkeypadnav",):
            continue
        try:
            app = ET.parse(manifest).getroot().find("application")
        except ET.ParseError:
            continue
        if app is None:
            continue
        for svc in app.findall("service"):
            actions = {a.get(ANDROID + "name") for a in svc.iter("action")}
            if ("androidx.media3.session.MediaSessionService" not in actions
                    or svc.get(ANDROID + "exported") != "true" or svc.get(ANDROID + "permission")):
                continue
            cls = svc.get(ANDROID + "name", "").rsplit(".", 1)[-1]
            src = manifest.parents[3]
            found = [f for f in src.rglob("*.kt") if re.search(rf"class\s+{re.escape(cls)}\b", f.read_text(encoding="utf-8", errors="ignore"))]
            ok = any("remove(Player.COMMAND_SET_MEDIA_ITEM)" in f.read_text(encoding="utf-8", errors="ignore") for f in found)
            if not ok:
                problems.append(
                    f"{manifest.relative_to(ROOT).as_posix()}: exported {cls} lets any controller set media items "
                    "- remove Player.COMMAND_SET_MEDIA_ITEM / COMMAND_CHANGE_MEDIA_ITEMS for other packages in onConnect"
                )


def check_lock_window(problems):
    """The lock screen is an accessibility overlay. With FLAG_NOT_TOUCHABLE touches fall
    through to the windows under it (on a touch-capable device: the status-bar shade)."""
    for path in walk("*.kt"):
        rel = path.relative_to(ROOT).as_posix()
        if "/lockscreen/" not in rel and not rel.lower().endswith("lockscreencontroller.kt"):
            continue
        text = path.read_text(encoding="utf-8", errors="ignore")
        if "TYPE_ACCESSIBILITY_OVERLAY" not in text:
            continue
        for lineno, line in _code_lines(text):
            if "FLAG_NOT_TOUCHABLE" in line:
                problems.append(f"{rel}:{lineno}: lock-screen window must not be FLAG_NOT_TOUCHABLE (touch would reach the windows below it)")


def check_firebase(problems):
    for rules in walk("*.rules"):
        rel = rules.relative_to(ROOT).as_posix()
        text = rules.read_text(encoding="utf-8", errors="ignore")
        for m in re.finditer(r"allow\s+([a-z, ]+):\s*if\s+([^;]+);", text):
            methods = {x.strip() for x in m.group(1).split(",")}
            expr = m.group(2).strip()
            line = text[: m.start()].count("\n") + 1
            if expr == "true":
                problems.append(f"{rel}:{line}: open rule `allow {m.group(1)}: if true`")
            if rel.endswith("storage.rules") and methods & {"create", "write"} and expr != "false" and "contentType" not in expr:
                problems.append(f"{rel}:{line}: storage upload rule does not restrict request.resource.contentType")


def check_build_config_keys(problems):
    """A key read from local.properties straight into BuildConfig ends up in the APK,
    and base.apk is readable by every app on the device."""
    for gradle in walk("build.gradle.kts"):
        rel = gradle.relative_to(ROOT).as_posix()
        for lineno, line in enumerate(gradle.read_text(encoding="utf-8", errors="ignore").splitlines(), 1):
            if re.search(r'buildConfigField\([^)]*(API_KEY|SECRET|TOKEN|PASSWORD)[^)]*localProperty\(', line):
                problems.append(f"{rel}:{lineno}: secret baked into BuildConfig from local.properties - use embeddedKey() (Firebase proxy holds the key)")


def check_untrusted_drawables(problems):
    """The System UI decodes icons that belong to other apps. An oversized one raises
    OutOfMemoryError - not an Exception - and `catch (e: Exception)` lets it kill the
    process that hosts the lock screen and key filter (repeatedly, while the notification
    that carries the icon is still posted). Such loads go through loadUntrusted()."""
    for path in walk("*.kt"):
        rel = path.relative_to(ROOT).as_posix()
        if rel.split("/")[0] not in SYSTEM_UI_APPS or "/test/" in rel:
            continue
        text = path.read_text(encoding="utf-8", errors="ignore")
        if "loadUntrusted(" in text or "runCatching" in text:
            continue
        for lineno, line in _code_lines(text):
            if re.search(r"getApplicationIcon\(|\.loadDrawable\(|\.loadIcon\(", line):
                problems.append(f"{rel}:{lineno}: third-party drawable decoded without loadUntrusted() (OutOfMemoryError would crash the System UI)")


def main():
    problems = []
    check_manifests(problems)
    check_sources(problems)
    check_workflows(problems)
    check_executable_scripts(problems)
    check_gitignore(problems)
    check_system_permission(problems)
    check_media_sessions(problems)
    check_lock_window(problems)
    check_firebase(problems)
    check_build_config_keys(problems)
    check_untrusted_drawables(problems)
    if problems:
        print(f"FutureOS security check: {len(problems)} problem(s)")
        for p in problems:
            print("  - " + p)
        return 1
    print("FutureOS security check: OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
