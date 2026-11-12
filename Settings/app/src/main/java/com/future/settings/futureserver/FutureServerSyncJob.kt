package com.future.settings.futureserver

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import com.future.settings.feedback.FeedbackRepository
import com.future.settings.update.UpdateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * סנכרון ברקע מול שרת FutureOS, פעם ב-6 שעות כשיש רשת (JobScheduler, בלי GMS):
 * 1. רישום המכשיר אם עוד לא נרשם.
 * 2. שליחת משובים שחיכו בתור (המשתמש כבר אישר אותם בתצוגה המקדימה).
 * 3. משיכת תשובות למשוב ("בטיפול", "תוקן בגרסה X") והתראה.
 * 4. בדיקת עדכונים והתראה. **לא מוריד ולא מתקין** - זה רק בלחיצה של המשתמש.
 *
 * בלי כתובת שרת בבנייה (FUTURE_API_BASE / ro.futureos.api_base ריקים) ה-Job לא מתוזמן בכלל.
 */
class FutureServerSyncJob : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var running: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        running = scope.launch {
            try {
                sync(applicationContext)
            } finally {
                jobFinished(params, false)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        running?.cancel()
        return true // ננסה שוב בפעם הבאה
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val JOB_ID = 71_001
        private const val PERIOD_MS = 6L * 60L * 60L * 1000L
        private const val FLEX_MS = 60L * 60L * 1000L

        /** מתזמן פעם אחת. קריאה חוזרת (כל עלייה של Settings, אחרי אתחול) לא יוצרת Job נוסף. */
        fun schedule(context: Context) {
            if (FutureServerRegistration.resolvedBaseUrl() == null) return
            val js = context.getSystemService(JobScheduler::class.java) ?: return
            if (js.getPendingJob(JOB_ID) != null) return
            val job = JobInfo.Builder(JOB_ID, ComponentName(context, FutureServerSyncJob::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(PERIOD_MS, FLEX_MS)
                .setPersisted(true)
                .build()
            runCatching { js.schedule(job) }
        }

        suspend fun sync(context: Context) {
            FutureServerRegistration.registerInBackgroundIfNeeded(context)
            runCatching { FeedbackRepository.flushOutbox(context) }
            runCatching { FutureNotifications.showFeedbackReplies(context, FeedbackRepository.pollInbox(context)) }
            if (UpdateRepository.autoCheckEnabled(context)) {
                val result = runCatching { UpdateRepository.check(context) }.getOrNull()
                if (result is UpdateRepository.CheckResult.Ok) {
                    FutureNotifications.showUpdates(context, UpdateRepository.takeUnnotified(context, result.offers))
                }
            }
        }
    }
}
