package com.example.notesapplication

import com.google.firebase.database.Exclude

data class Note(
    var noteDetail: String? = "",
    var id: String? = ""
)
