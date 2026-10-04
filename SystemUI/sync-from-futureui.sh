#!/usr/bin/env bash
# Mirrors FutureUI (com.future.futureui, the System UI that runs on the device)
# into SystemUI (com.android.sistemui). After this script the two modules are
# the same code; only the package name differs.
#
# Copied: all Kotlin sources, AndroidManifest.xml, res/values/themes.xml and res/xml.
# Not touched: app icons (mipmap-*/drawable) - the project rule is that app icons
# never change - and the Gradle files (they already match except the namespace).
#
# Renamed in the copy: the package com.future.futureui -> com.android.sistemui,
# which also renames the ContentProvider authorities and the signature permission
# (com.android.sistemui.theme / .systemui / .permission.SYSTEM_SETTINGS), so both
# can be installed side by side. The accessibility-service labels become
# "SystemUI ..." so the two are distinguishable in the accessibility list.
#
# Broadcast actions the other apps send stay com.future.futureui.ACTION_* on
# purpose: they come from SharedKeypadNav (SystemUiTarget.PACKAGE), so SystemUI
# answers exactly the same broadcasts FutureUI does.
#
# Usage: bash SystemUI/sync-from-futureui.sh   (from the repo root or anywhere)
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo="$(cd "$here/.." && pwd)"
src="$repo/FutureUI/app/src/main"
dst="$repo/SystemUI/app/src/main"

[ -d "$src/java/com/future/futureui" ] || { echo "FutureUI sources not found at $src" >&2; exit 1; }

# Kotlin sources
rm -rf "$dst/java/com/android/sistemui"
mkdir -p "$dst/java/com/android/sistemui"
cp -r "$src/java/com/future/futureui/." "$dst/java/com/android/sistemui/"
find "$dst/java/com/android/sistemui" -name '*.kt' -print0 | xargs -0 sed -i \
    -e 's/com\.future\.futureui\.ACTION_/@@KEEP_ACTION@@/g' \
    -e 's/com\.future\.futureui/com.android.sistemui/g' \
    -e 's/@@KEEP_ACTION@@/com.future.futureui.ACTION_/g'

# Manifest
sed \
    -e 's/com\.future\.futureui\.ACTION_/@@KEEP_ACTION@@/g' \
    -e 's/com\.future\.futureui/com.android.sistemui/g' \
    -e 's/@@KEEP_ACTION@@/com.future.futureui.ACTION_/g' \
    -e 's/android:label="FutureUI /android:label="SystemUI /g' \
    "$src/AndroidManifest.xml" > "$dst/AndroidManifest.xml"

# Resources (never icons)
cp "$src/res/values/themes.xml" "$dst/res/values/themes.xml"
cp "$src/res/values/strings.xml" "$dst/res/values/strings.xml"
cp "$src/res/values/colors.xml" "$dst/res/values/colors.xml"
rm -rf "$dst/res/xml"
cp -r "$src/res/xml" "$dst/res/xml"

echo "SystemUI now mirrors FutureUI ($(find "$dst/java" -name '*.kt' | wc -l) Kotlin files)."
