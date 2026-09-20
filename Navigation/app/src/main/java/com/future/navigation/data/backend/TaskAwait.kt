package com.future.navigation.data.backend

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * ממיר Task של Google Play Services ל-suspend, במקום להוסיף תלות נוספת
 * (kotlinx-coroutines-play-services) רק בשביל await אחד. אין כאן ביטול אמיתי
 * של הבקשה - Task לא תומך בזה - אבל הקורוטינה משתחררת מיד כשהיא מבוטלת,
 * וה-Task שנשאר רץ ברקע נזרק בלי תופעות לוואי.
 */
suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (!continuation.isActive) return@addOnCompleteListener
        val error = task.exception
        when {
            error != null -> continuation.resumeWithException(error)
            task.isCanceled -> continuation.cancel()
            else -> {
                @Suppress("UNCHECKED_CAST")
                continuation.resume(task.result as T)
            }
        }
    }
}
