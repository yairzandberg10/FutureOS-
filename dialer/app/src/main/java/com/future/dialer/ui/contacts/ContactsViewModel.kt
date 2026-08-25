package com.future.dialer.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.future.dialer.data.model.Contact
import com.future.dialer.data.repository.ContactRepository
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

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
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
        val query = _searchQuery.value.lowercase()
        if (query.isEmpty()) {
            _contacts.value = _allContacts.value
        } else {
            _contacts.value = _allContacts.value.filter {
                it.name.lowercase().contains(query) || it.phoneNumber.contains(query)
            }
        }
    }
}
