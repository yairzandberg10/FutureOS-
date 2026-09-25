package com.future.dialer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.future.dialer.data.model.CallRecord
import com.future.dialer.data.model.Contact
import com.future.dialer.data.repository.CallLogRepository
import com.future.dialer.data.repository.ContactRepository
import com.future.dialer.util.T9Search
import com.future.dialer.data.model.CallType
import com.future.dialer.data.model.CallFilter
import com.future.dialer.data.model.CallStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * המצב של אפליקציית השיחות (ui_kits/calls): היומן, המספר שמוקלד במקלדת,
 * אנשי הקשר והמועדפים. כל המסכים קוראים מכאן, כך שמועדף שנוסף במסך איש
 * קשר מופיע מיד בטאב המועדפים.
 */
class CallsViewModel(
    private val contactRepository: ContactRepository,
    private val callLogRepository: CallLogRepository,
) : ViewModel() {

    private val _dialedNumber = MutableStateFlow("")
    val dialedNumber: StateFlow<String> = _dialedNumber.asStateFlow()

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _recentCalls = MutableStateFlow<List<CallRecord>>(emptyList())
    val recentCalls: StateFlow<List<CallRecord>> = _recentCalls.asStateFlow()

    val favorites: StateFlow<List<Contact>> = _contacts
        .map { list -> list.filter { it.isFavorite } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * היומן מקובץ לימים, עם הטקסטים של כל שורה כבר מוכנים - מחושב פעם אחת
     * לכל טעינה, ברקע (Default), ולא ב-composition של המסך. עם מאות שיחות
     * הקיבוץ (Calendar לכל שיחה) והפורמט עלו בכל כניסה לטאב על ה-main thread.
     */
    private val _filter = MutableStateFlow(CallFilter.ALL)
    val filter: StateFlow<CallFilter> = _filter.asStateFlow()

    fun setFilter(filter: CallFilter) {
        _filter.value = filter
    }

    /**
     * היומן המסונן, מקובץ לימים. השם של כל שורה: השם השמור ביומן, ואם אין -
     * איש הקשר של המספר (אנשי קשר שנוספו אחרי השיחה), ואם גם אין - המספר
     * עצמו, או "מספר חסוי". כך כל שיחה מוצגת, גם ממספר שאינו שמור.
     */
    val callDays: StateFlow<List<CallDay>> = combine(_recentCalls, _contacts, _filter) { calls, contacts, filter ->
        val byKey = HashMap<String, String>()
        contacts.forEach { c -> c.allNumbers.forEach { n -> matchKey(n)?.let { k -> byKey.putIfAbsent(k, c.name) } } }
        groupByDay(calls.filter { filter.matches(it.type) }) { call ->
            call.name
                ?: matchKey(call.phoneNumber)?.let(byKey::get)
                ?: call.phoneNumber.takeIf { it.isNotBlank() && !call.isPrivate }
                ?: "מספר חסוי"
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** סיכום הדקות של כל היומן - לא תלוי בסינון. */
    val stats: StateFlow<CallStats> = _recentCalls.map { calls ->
        CallStats(
            incomingSeconds = calls.filter { it.type == CallType.INCOMING }.sumOf { it.duration },
            outgoingSeconds = calls.filter { it.type == CallType.OUTGOING }.sumOf { it.duration },
            incomingCount = calls.count { it.type == CallType.INCOMING && it.duration > 0 },
            outgoingCount = calls.count { it.type == CallType.OUTGOING },
            missedCount = calls.count { it.type == CallType.MISSED },
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, CallStats(0, 0, 0, 0, 0))

    /** אנשי קשר שמתאימים למה שהוקלד במקלדת (ספרות במספר או T9 בשם) - עד שישה. */
    val dialSuggestions: StateFlow<List<Contact>> = combine(_dialedNumber, _contacts) { digits, list ->
        if (digits.length < 2) return@combine emptyList()
        val clean = digitsOf(digits)
        list.filter { digitsOf(it.phoneNumber).contains(clean) || T9Search.matchesAnyWord(it.name, digits) }.take(6)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * איש הקשר שהמספר המוקלד שייך לו - מוצג מתחת למספר במקלדת. משלוש ספרות
     * ומעלה, כמו בערכה: קודם התאמה במספר, ואם אין - התאמת T9 על השם.
     */
    val dialMatch: StateFlow<Contact?> = combine(_dialedNumber, _contacts) { digits, list ->
        if (digits.length < 3) return@combine null
        val clean = digitsOf(digits)
        list.firstOrNull { digitsOf(it.phoneNumber).contains(clean) }
            ?: list.firstOrNull { T9Search.matchesAnyWord(it.name, digits) }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        refresh()
    }

    private var contactsJob: Job? = null
    private var callsJob: Job? = null

    /**
     * טוען מחדש את אנשי הקשר והיומן. נקרא מכמה מקומות בפתיחה (init, onResume,
     * סוף שיחה) - טעינה שכבר רצה לא מתחילה שוב, כדי שלא ירוצו שתי שאילתות
     * זהות במקביל על אותו ספק.
     */
    fun refresh() {
        if (contactsJob?.isActive != true) {
            contactsJob = viewModelScope.launch { _contacts.value = contactRepository.getAllContacts() }
        }
        if (callsJob?.isActive != true) {
            callsJob = viewModelScope.launch { _recentCalls.value = callLogRepository.getCallLogs() }
        }
    }

    /**
     * טעינה מחדש של היומן כשהוא משתנה (ContentObserver ב-MainActivity).
     * הטעינה שרצה כבר מבוטלת ומתחילה מחדש - אחרת שיחה שנרשמה ביומן שנייה
     * אחרי שהטעינה התחילה (Telecom כותב את היומן אחרי סוף השיחה) לא הופיעה.
     */
    fun reloadCalls() {
        callsJob?.cancel()
        callsJob = viewModelScope.launch { _recentCalls.value = callLogRepository.getCallLogs() }
    }

    fun reloadContacts() {
        contactsJob?.cancel()
        contactsJob = viewModelScope.launch { _contacts.value = contactRepository.getAllContacts() }
    }

    fun onDigitPressed(digit: String) {
        if (_dialedNumber.value.length < MaxDialLength) _dialedNumber.value += digit
    }

    fun onDeletePressed() {
        _dialedNumber.value = _dialedNumber.value.dropLast(1)
    }

    fun clearNumber() {
        _dialedNumber.value = ""
    }

    /** ממלא את המקלדת במספר קיים בלי לחייג - המשתמש מחייג בעצמו. */
    fun setNumber(number: String) {
        _dialedNumber.value = number
    }

    /** איש הקשר של מספר, לפי תשע הספרות האחרונות (052... מול +97252...). */
    fun contactFor(number: String): Contact? {
        val key = matchKey(number) ?: return null
        return _contacts.value.firstOrNull { c -> c.allNumbers.any { matchKey(it) == key } }
    }

    /** השיחות האחרונות עם מספר - ההיסטוריה שבמסך איש הקשר. */
    fun historyFor(number: String, limit: Int = 30): List<CallRecord> {
        val key = matchKey(number) ?: return emptyList()
        return _recentCalls.value.filter { matchKey(it.phoneNumber) == key }.take(limit)
    }

    fun toggleFavorite(contact: Contact) {
        viewModelScope.launch {
            contactRepository.setFavorite(contact.id, !contact.isFavorite)
            _contacts.value = contactRepository.getAllContacts()
        }
    }

    /** "נקה יומן". false כשהמערכת סירבה (אין הרשאת כתיבה ליומן). */
    fun clearCallLog(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = callLogRepository.clearAll()
            _recentCalls.value = callLogRepository.getCallLogs()
            onResult(ok)
        }
    }

    private companion object {
        /** קיבוץ לפי יום, בסדר יורד - "היום", "אתמול", ואז תאריך מלא. */
        fun groupByDay(calls: List<CallRecord>, titleOf: (CallRecord) -> String): List<CallDay> =
            calls.sortedByDescending { it.timestamp }
                .groupBy { CallFormat.dayTitle(it.timestamp) }
                .map { (title, dayCalls) ->
                    CallDay(
                        title = title,
                        rows = dayCalls.map { call ->
                            CallRow(
                                call = call,
                                title = titleOf(call),
                                summary = CallFormat.summaryOf(call),
                                time = CallFormat.timeOf(call),
                            )
                        },
                    )
                }

        /** מספר טלפון ארוך ביותר שהמקלדת מקבלת (כולל קידומת בינלאומית). */
        const val MaxDialLength = 20

        fun digitsOf(number: String): String = number.filter { it.isDigit() }

        fun matchKey(number: String): String? {
            val digits = digitsOf(number)
            if (digits.isEmpty()) return null
            return if (digits.length > 9) digits.takeLast(9) else digits
        }
    }
}

/** יום אחד ביומן: הכותרת ("היום", "אתמול"...) והשורות שלו. */
data class CallDay(val title: String, val rows: List<CallRow>)

/** שורת יומן עם הטקסטים שלה מוכנים מראש. */
data class CallRow(val call: CallRecord, val title: String, val summary: String, val time: String)
