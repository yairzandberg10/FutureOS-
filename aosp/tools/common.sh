#!/usr/bin/env bash
# משותף לכל סקריפטי M0. כל הסקריפטים כאן קוראים בלבד: אין write, erase, unlock או flash.
set -euo pipefail

TOOLS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOCK_FILE="$TOOLS_DIR/../../../.claude/coordination/phone.lock"
# mtkclient: git clone https://github.com/bkerler/mtkclient ; pip install -r requirements.txt
MTK="${MTK:-python3 $HOME/mtkclient/mtk.py}"
# התיקייה לפלט. לא על C (כמעט מלא): מומלץ דיסק חיצוני.
OUT="${OUT:-/d/futureos-m0}"

die() { echo "!!! $*" >&2; exit 1; }

# הטלפון משותף לכמה צ'אטים. בלי הדגל, הסקריפט לא נוגע בו.
require_phone_lock() {
    if [[ "${1:-}" != "--i-hold-the-phone-lock" ]]; then
        echo "הסקריפט מתחבר לטלפון. מריצים רק כשה-phone.lock אצלך:"
        [[ -f "$LOCK_FILE" ]] && { echo "--- phone.lock כרגע:"; cat "$LOCK_FILE"; echo; }
        die "הוסף --i-hold-the-phone-lock כדי להמשיך"
    fi
}

# מסרב לכל פקודת mtkclient שאינה קריאה.
mtk_read() {
    case "$1" in
        r|rl|printgpt|gettargetconfig) ;;
        *) die "mtk_read: '$1' אינה פקודת קריאה - חסום" ;;
    esac
    $MTK "$@"
}

sha_all() { (cd "$1" && find . -type f ! -name SHA256SUMS -print0 | sort -z | xargs -0 sha256sum > SHA256SUMS); }
