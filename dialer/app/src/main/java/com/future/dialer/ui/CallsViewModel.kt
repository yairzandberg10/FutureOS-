package com.future.dialer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.future.dialer.data.model.CallRecord
import com.future.dialer.data.model.Contact
import com.future.dialer.data.repository.CallLogRepository
import com.future.dialer.data.repository.ContactRepository
import com.future.dialer.util.T9Search
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
     * איש הקשר שהמספר המוקלד שייך לו - מוצג מתחת למספר במקלדת. משלוש ספרות
     * ומעלה, כמו בערכה: קודם התאמה במספר, ואם אין - התאמת T9 על השם.
     */
    val dialMatch: StateFlow<Contact?> = combine(_dialedNumber, _contacts) { digits, list ->
        if (digits.length < 3) return@combine null
        val clean = digitsOf(digits)
        list.firstOrNull { digitsOf(it.phoneNumber).contains(clean) }
            ?: list.firstOrNull { T9Search.matchesAnyWord(it.name, digits) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { _contacts.value = contactRepository.getAllContacts() }
        viewModelScope.launch { _recentCalls.value = callLogRepository.getCallLogs() }
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
        return _contacts.value.firstOrNull { matchKey(it.phoneNumber) == key }
    }

    /** השיחות האחרונות עם מספר - ההיסטוריה שבמסך איש הקשר. */
    fun historyFor(number: String, limit: Int = 3): List<CallRecord> {
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
