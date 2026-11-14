package com.future.dialer.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.future.dialer.data.model.Contact
import com.future.dialer.data.repository.ContactRepository
import com.future.dialer.util.T9Search
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContactsViewModel(private val repository: ContactRepository) : ViewModel() {

    private val _allContacts = MutableStateFlow<List<Contact>>(emptyList())
    
    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // בלי טעינה ב-init: ה-ViewModel נוצר בפתיחת האפליקציה, אבל הרשימה
    // נחוצה רק במסך החיפוש - והוא טוען אותה בכל כניסה (SearchScreen).
    // כך לא רצה שאילתת אנשי קשר כפולה בזמן שהיומן נטען.

    private var refreshJob: kotlinx.coroutines.Job? = null

    fun refresh() {
        // טעינה חדשה מבטלת את הקודמת - לא שתי שאילתות מתחרות שהישנה מהן דורסת את החדשה
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _allContacts.value = repository.getAllContacts()
            filterContacts()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        filterContacts()
    }

    fun toggleFavorite(contact: Contact) {
        viewModelScope.launch {
            repository.setFavorite(contact.id, !contact.isFavorite)
            refresh()
        }
    }

    private fun filterContacts() {
        val raw = _searchQuery.value.trim()
        val query = raw.lowercase()
        if (query.isEmpty()) {
            _contacts.value = _allContacts.value
        } else if (raw.all { it in '0'..'9' }) {
            // שאילתת ספרות: חיפוש T9 לפי שם (כמו בחייגן), והשוואת מספרים בלי קידומת.
            val key = numberKey(raw)
            _contacts.value = _allContacts.value.filter { contact ->
                T9Search.matchesAnyWord(contact.name, raw) ||
                    contact.allNumbers.any { numberKey(it).contains(key) }
            }
        } else {
            _contacts.value = _allContacts.value.filter {
                it.name.lowercase().contains(query) || it.phoneNumber.contains(query)
            }
        }
    }

    private companion object {
        /** ספרות בלבד, בלי קידומת 972 או 0 מובילה - כדי ש-052 ימצא +972 52. */
        fun numberKey(number: String): String {
            val digits = number.filter { it in '0'..'9' }
            val key = when {
                digits.startsWith("972") -> digits.drop(3)
                digits.startsWith("0") -> digits.drop(1)
                else -> digits
            }
            return key.ifEmpty { digits }
        }
    }
}
