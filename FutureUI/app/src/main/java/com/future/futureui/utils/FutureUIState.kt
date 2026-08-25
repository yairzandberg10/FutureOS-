package com.future.futureui.utils

/**
 * מצב משותף בין שירותי הנגישות של FutureUI (כולם רצים באותו תהליך) - כרגע
 * רק אם מסך הנעילה גלוי כרגע, כדי ששירותים אחרים (כמו שורת המצב) ידעו לא
 * להגיב למקשים ששם כבר יש להם משמעות אחרת (למשל מקש Options במצב עריכה).
 */
object FutureUIState {
    @Volatile
    var isLockScreenVisible: Boolean = false
}
