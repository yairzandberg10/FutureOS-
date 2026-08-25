package com.future.notes

import android.app.Application
import androidx.room.Room
import com.future.notes.data.NoteDatabase
import com.future.notes.data.NoteRepository

class NotesApp : Application() {
    val database by lazy { 
        Room.databaseBuilder(this, NoteDatabase::class.java, "note_database").build() 
    }
    val repository by lazy { NoteRepository(database.noteDao()) }
}
