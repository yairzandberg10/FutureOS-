package com.future.dialer.ui.dialpad

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.future.dialer.data.model.CallRecord
import com.future.dialer.data.model.Contact
import com.future.dialer.data.repository.CallLogRepository
import com.future.dialer.data.repository.ContactRepository
import com.future.dialer.util.T9Search
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * מסך החיוג וההיסטוריה מאוחדים: כשאין ספרות בשדה החיוג מוצגות השיחות האחרונות,
 * וברגע שמתחילים להקיש מספר המסך עובר להצעות אנשי קשר תואמות (T9).
 */
class DialpadViewModel(
    private val contactRepository: ContactRepository,
    private val callLogRepository: CallLogRepository
) : ViewModel() {

    private val _dialedNumber = MutableStateFlow("")
    val dialedNumber: StateFlow<String> = _dialedNumber.asStateFlow()

    private val _suggestedContacts = MutableStateFlow<List<Contact>>(emptyList())
    val suggestedContacts: StateFlow<List<Contact>> = _suggestedContacts.asStateFlow()

    private val _recentCalls = MutableStateFlow<List<CallRecord>>(emptyList())
    val recentCalls: StateFlow<List<CallRecord>> = _recentCalls.asStateFlow()

    private var allContacts: List<Contact> = emptyList()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            allContacts = contactRepository.getAllContacts()
            filterContacts()
        }
        viewModelScope.launch {
            _recentCalls.value = callLogRepository.getCallLogs()
        }
    }

    fun onDigitPressed(digit: String) {
        _dialedNumber.value += digit
        filterContacts()
    }

    fun onDeletePressed() {
        if (_dialedNumber.value.isNotEmpty()) {
            _dialedNumber.value = _dialedNumber.value.dropLast(1)
            filterContacts()
        }
    }

    fun clearNumber() {
        _dialedNumber.value = ""
        filterContacts()
    }


    private fun filterContacts() {
        val query = _dialedNumber.value
        if (query.isEmpty()) {
            _suggestedContacts.value = emptyList()
            return
        }
        _suggestedContacts.value = allContacts.filter {
            T9Search.matches(it.name, query) || it.phoneNumber.contains(query)
        }
    }
}
