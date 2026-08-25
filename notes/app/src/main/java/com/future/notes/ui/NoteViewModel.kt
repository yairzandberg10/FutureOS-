package com.future.notes.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.future.notes.data.Note
import com.future.notes.data.NoteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NoteViewModel(private val repository: NoteRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<List<Note>> = _searchQuery
        .map { it.trim() }
        .flatMapLatest { query ->
            if (query.isEmpty()) repository.allNotes
            else repository.searchNotes(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun addOrUpdateNote(id: Int = 0, title: String, content: String, isPinned: Boolean = false) {
        viewModelScope.launch {
            val note = Note(id = id, title = title, content = content, isPinned = isPinned, timestamp = System.currentTimeMillis())
            if (id == 0) repository.insert(note)
            else repository.update(note)
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.delete(note)
        }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch {
            repository.update(note.copy(isPinned = !note.isPinned))
        }
    }
}
