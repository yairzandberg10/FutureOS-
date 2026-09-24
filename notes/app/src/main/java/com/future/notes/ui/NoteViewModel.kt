package com.future.notes.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.future.notes.data.Note
import com.future.notes.data.NoteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

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

    suspend fun getNote(id: Int): Note? = repository.get(id)

    /**
     * שמירה אוטומטית מהעורך: פתק חדש (id 0) נוצר בשמירה הראשונה, ומוחזר
     * ה-id שלו כדי שהשמירות הבאות יעדכנו אותו במקום ליצור עוד פתק.
     */
    suspend fun save(note: Note): Int {
        val stamped = note.copy(timestamp = System.currentTimeMillis())
        return if (note.id == 0) {
            repository.insert(stamped).toInt()
        } else {
            repository.update(stamped)
            note.id
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.delete(note)
            note.audioPath?.let { runCatching { File(it).delete() } }
        }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch {
            repository.update(note.copy(isPinned = !note.isPinned))
        }
    }

    /** פתק שהגיע משיתוף (ACTION_SEND) - בדרך כלל מ-FuturePhone אחר דרך הודעות. */
    fun importShared(title: String, content: String, isChecklist: Boolean) {
        viewModelScope.launch {
            repository.insert(Note(title = title, content = content, isChecklist = isChecklist))
        }
    }
}
