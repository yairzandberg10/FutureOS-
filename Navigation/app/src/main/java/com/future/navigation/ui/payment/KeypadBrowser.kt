package com.future.navigation.ui.payment

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import com.future.navigation.data.payment.PaymentConfig
import com.future.navigation.data.payment.PaymentProfile
import org.json.JSONObject
import kotlin.math.min

/**
 * דפדפן לעמוד התשלום, בשביל מכשיר בלי מסך מגע: WebView עם סמן על המסך.
 *
 * החיצים מזיזים את הסמן (מאיץ כשמחזיקים), OK לוחץ במקום שלו - "נגיעה"
 * מסונתזת שנשלחת ישירות ל-WebView ולכן לא נחסמת ב-dispatchTouchEvent של
 * MainActivity. בקצה המסך הסמן גולל את מה שמתחתיו בגלגלת עכבר מסונתזת,
 * כך שגם אזור גלילה פנימי של העמוד (לא רק העמוד כולו) זז. 2/8 גוללים עמוד
 * שלם - רק כשאין שדה טקסט פעיל, אחרת הספרות הולכות למקלדת. חזור חוזר
 * אחורה בהיסטוריה של העמוד, ובעמוד הראשון סוגר את המסך.
 *
 * בכל עמוד שנטען ממארח מורשה (PaymentConfig.isAutofillAllowed) מוזרק
 * סקריפט שממלא את פרטי המשתמש בשדות הריקים ומשגיח על שינויי DOM, כי טפסי
 * SPA נבנים אחרי onPageFinished.
 */
@SuppressLint("SetJavaScriptEnabled", "ViewConstructor")
class KeypadBrowser(
    context: Context,
    private val cursorFill: Int,
    private val cursorRing: Int,
    private val onProgress: (Int) -> Unit,
    private val onTitle: (String) -> Unit,
) : FrameLayout(context) {

    var profile: PaymentProfile = PaymentProfile()

    private val density = resources.displayMetrics.density
    private var cursorX = -1f
    private var cursorY = -1f

    val webView: WebView = object : WebView(context) {
        override fun dispatchKeyEvent(event: KeyEvent): Boolean =
            handleKey(event) || super.dispatchKeyEvent(event)
    }

    private val cursorView = object : View(context) {
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cursorFill; style = Paint.Style.FILL }
        private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cursorRing
            style = Paint.Style.STROKE
            strokeWidth = 2f * density
        }

        override fun onDraw(canvas: Canvas) {
            if (cursorX < 0f) return
            canvas.drawCircle(cursorX, cursorY, CURSOR_RADIUS_DP * density, fill)
            canvas.drawCircle(cursorX, cursorY, CURSOR_RADIUS_DP * density, ring)
        }
    }.apply {
        isFocusable = false
        isClickable = false
    }

    init {
        addView(webView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(cursorView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            setGeolocationEnabled(false)
            useWideViewPort = true
            loadWithOverviewMode = true
        }
        // רב-קו אונליין שומר את ההתחברות לחשבון בעוגיות, וגורמי סליקה רצים
        // לפעמים ב-iframe של דומיין אחר - בלי עוגיות צד שלישי התשלום נתקע.
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            // רק http/https. intent:, file:, javascript: וכל השאר נבלעים - דף
            // אינטרנט לא אמור להפעיל מכאן רכיבים של אפליקציות אחרות.
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val scheme = request.url.scheme?.lowercase()
                return scheme != "https" && scheme != "http"
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                onProgress(0)
            }

            override fun onPageFinished(view: WebView, url: String?) {
                injectAutofill(url)
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) = onProgress(newProgress)
            override fun onReceivedTitle(view: WebView, title: String?) {
                if (!title.isNullOrBlank()) onTitle(title)
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (cursorX < 0f && w > 0 && h > 0) {
            cursorX = w / 2f
            cursorY = h / 3f
            cursorView.invalidate()
        }
    }

    fun load(url: String) = webView.loadUrl(url)

    fun destroy() {
        webView.stopLoading()
        webView.destroy()
    }

    private fun handleKey(event: KeyEvent): Boolean {
        val down = event.action == KeyEvent.ACTION_DOWN
        when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> { if (down) moveCursor(0, -1, event.repeatCount); return true }
            KeyEvent.KEYCODE_DPAD_DOWN -> { if (down) moveCursor(0, 1, event.repeatCount); return true }
            KeyEvent.KEYCODE_DPAD_LEFT -> { if (down) moveCursor(-1, 0, event.repeatCount); return true }
            KeyEvent.KEYCODE_DPAD_RIGHT -> { if (down) moveCursor(1, 0, event.repeatCount); return true }
            KeyEvent.KEYCODE_DPAD_CENTER -> { if (down && event.repeatCount == 0) tapAtCursor(); return true }
            // ENTER מגיע גם מהמקלדת כשמקלידים בשדה - שם הוא שייך לעמוד.
            KeyEvent.KEYCODE_ENTER -> if (!webView.onCheckIsTextEditor()) {
                if (down && event.repeatCount == 0) tapAtCursor()
                return true
            }
            KeyEvent.KEYCODE_2 -> if (!webView.onCheckIsTextEditor()) {
                if (down) wheel(vertical = PAGE_TICKS, horizontal = 0f)
                return true
            }
            KeyEvent.KEYCODE_8 -> if (!webView.onCheckIsTextEditor()) {
                if (down) wheel(vertical = -PAGE_TICKS, horizontal = 0f)
                return true
            }
            KeyEvent.KEYCODE_BACK -> if (webView.canGoBack()) {
                if (event.action == KeyEvent.ACTION_UP) webView.goBack()
                return true
            }
        }
        return false
    }

    private fun moveCursor(dx: Int, dy: Int, repeat: Int) {
        if (width == 0 || height == 0) return
        val step = (STEP_DP + min(repeat, MAX_ACCEL_STEPS) * ACCEL_DP) * density
        val margin = EDGE_DP * density
        var x = cursorX + dx * step
        var y = cursorY + dy * step

        // בקצה - הסמן נעצר והתוכן שמתחתיו נגלל במקומו.
        if (y < margin) { wheel(vertical = 1f, horizontal = 0f); y = margin }
        if (y > height - margin) { wheel(vertical = -1f, horizontal = 0f); y = height - margin }
        if (x < margin) { wheel(vertical = 0f, horizontal = -1f); x = margin }
        if (x > width - margin) { wheel(vertical = 0f, horizontal = 1f); x = width - margin }

        cursorX = x
        cursorY = y
        cursorView.invalidate()
    }

    private fun tapAtCursor() {
        if (!webView.isFocused) webView.requestFocus()
        val now = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, cursorX, cursorY, 0)
        val up = MotionEvent.obtain(now, now + TAP_MS, MotionEvent.ACTION_UP, cursorX, cursorY, 0)
        down.setSource(InputDevice.SOURCE_TOUCHSCREEN)
        up.setSource(InputDevice.SOURCE_TOUCHSCREEN)
        webView.dispatchTouchEvent(down)
        webView.dispatchTouchEvent(up)
        down.recycle()
        up.recycle()
    }

    /** גלגלת עכבר במיקום הסמן - Chromium גולל את האלמנט שמתחתיו, ואם אין לו לאן - את העמוד. */
    private fun wheel(vertical: Float, horizontal: Float) {
        val now = SystemClock.uptimeMillis()
        val props = arrayOf(MotionEvent.PointerProperties().apply {
            id = 0
            toolType = MotionEvent.TOOL_TYPE_MOUSE
        })
        val coords = arrayOf(MotionEvent.PointerCoords().apply {
            x = cursorX
            y = cursorY
            setAxisValue(MotionEvent.AXIS_VSCROLL, vertical)
            setAxisValue(MotionEvent.AXIS_HSCROLL, horizontal)
        })
        val event = MotionEvent.obtain(
            now, now, MotionEvent.ACTION_SCROLL, 1, props, coords,
            0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_MOUSE, 0
        )
        webView.dispatchGenericMotionEvent(event)
        event.recycle()
    }

    private fun injectAutofill(url: String?) {
        if (!PaymentConfig.isAutofillAllowed(url)) return
        val p = profile
        if (p.isEmpty) return
        val data = JSONObject()
            .put("first", p.firstName)
            .put("last", p.lastName)
            .put("full", p.fullName)
            .put("id", p.idNumber)
            .put("phone", p.phone)
            .put("email", p.email)
            .put("ravKav", p.ravKavNumber)
        webView.evaluateJavascript(AUTOFILL_SCRIPT.replace("__PROFILE__", data.toString()), null)
    }

    private companion object {
        const val CURSOR_RADIUS_DP = 9f
        const val STEP_DP = 14f
        const val ACCEL_DP = 6f
        const val MAX_ACCEL_STEPS = 8
        const val EDGE_DP = 24f
        const val PAGE_TICKS = 6f
        const val TAP_MS = 60L

        /**
         * ממלא רק שדות ריקים שהמשתמש לא נגע בהם, לפי autocomplete/type/name
         * ולפי התווית בעברית ובאנגלית. לעולם לא סיסמה ולא שדות אשראי
         * (cc-*, תוקף, CVV). מספר רב-קו נכנס רק לשדה שמסומן במפורש כרב-קו -
         * "מספר כרטיס" סתמי עלול להיות כרטיס האשראי. הערכים נכתבים דרך
         * ה-setter המקורי של value ומשוגרים אירועי input/change, אחרת
         * React/Angular לא רואים אותם.
         */
        val AUTOFILL_SCRIPT = """
            (function (p) {
              if (window.__futureFill) { window.__futureFill(); return; }
              function lower(s) { return (s || '').toString().toLowerCase(); }
              function describe(el) {
                var t = '';
                if (el.id) {
                  var l = document.querySelector('label[for="' + CSS.escape(el.id) + '"]');
                  if (l) t += ' ' + l.textContent;
                }
                var parent = el.closest('label');
                if (parent) t += ' ' + parent.textContent;
                t += ' ' + (el.getAttribute('aria-label') || '') + ' ' + (el.placeholder || '') +
                     ' ' + (el.name || '') + ' ' + (el.id || '') + ' ' + (el.getAttribute('formcontrolname') || '');
                return lower(t);
              }
              function kind(el) {
                var type = lower(el.type), ac = lower(el.getAttribute('autocomplete')), t = describe(el);
                if (['password', 'hidden', 'checkbox', 'radio', 'submit', 'button', 'file', 'date'].indexOf(type) >= 0) return null;
                if (ac.indexOf('cc-') === 0 || /cvv|cvc|expir|credit|אשראי|תוקף/.test(t)) return null;
                if (/rav.?kav|רב.?קו/.test(t)) return 'ravKav';
                if (type === 'email' || ac === 'email' || /e-?mail|אימייל|מייל|דוא"?ל/.test(t)) return 'email';
                if (type === 'tel' || ac === 'tel' || /phone|mobile|טלפון|נייד|סלולרי/.test(t)) return 'phone';
                if (/teudat|id.?number|identity|national.?id|תעודת זהות|מספר זהות|ת\.?ז/.test(t)) return 'id';
                if (ac === 'given-name' || /first.?name|שם פרטי/.test(t)) return 'first';
                if (ac === 'family-name' || /last.?name|surname|שם משפחה/.test(t)) return 'last';
                if (ac === 'name' || /full.?name|שם מלא/.test(t)) return 'full';
                return null;
              }
              var setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set;
              function fill() {
                var inputs = document.querySelectorAll('input');
                for (var i = 0; i < inputs.length; i++) {
                  var el = inputs[i];
                  if (el.disabled || el.readOnly || el.value || el.dataset.futureFilled) continue;
                  var k = kind(el);
                  if (!k || !p[k]) continue;
                  setter.call(el, p[k]);
                  el.dispatchEvent(new Event('input', { bubbles: true }));
                  el.dispatchEvent(new Event('change', { bubbles: true }));
                  el.dataset.futureFilled = '1';
                }
              }
              document.addEventListener('input', function (e) {
                if (e.isTrusted && e.target && e.target.dataset) e.target.dataset.futureFilled = '1';
              }, true);
              window.__futureFill = fill;
              fill();
              var timer = null;
              new MutationObserver(function () {
                clearTimeout(timer);
                timer = setTimeout(fill, 300);
              }).observe(document.documentElement, { childList: true, subtree: true });
            })(__PROFILE__);
        """.trimIndent()
    }
}
