package com.example.notesapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.*
import kotlinx.android.synthetic.main.activity_main.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var noteArrayList: ArrayList<Note>
    private lateinit var adapter: NoteAdapter
    private lateinit var db: FirebaseFirestore

    private val noteCollectionRef = Firebase.firestore.collection("notes")
    private val dbNotes = FirebaseDatabase.getInstance().getReference("notes")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        noteArrayList = arrayListOf()
        adapter = NoteAdapter(noteArrayList)
        recyclerView.adapter = adapter


        adapter.setOnItemClickListener(object : RecyclerViewClickListener{
            override fun onRecyclerViewItemClicked(note: Note) {
                inputNotesId.setText(note.id)
                inputNotesText.setText(note.noteDetail)
            }
        })

        buttonAddNote.setOnClickListener {
            val noteDetail = inputNotesText.text.toString()
            val noteId = dbNotes.push().key
            val note = Note(noteDetail, noteId)
            saveNotes(note)
            adapter.submitList(noteArrayList)
            inputNotesText.setText("")
            inputNotesId.setText("")
        }

        buttonUpdate.setOnClickListener {
            val oldNote = getOldNote()
            val newNoteMap = getNewNoteMap()
            updateNote(oldNote, newNoteMap)
        }

        buttonDelete.setOnClickListener {
            val note = getOldNote()
            deleteNote(note)
        }

        realtimeUpdates()
        eventChangeListner()
    }

    private fun getOldNote(): Note {
        val noteDetail = inputNotesText.text.toString()
        val id = inputNotesId.text.toString()
        return Note(noteDetail, id)
    }

    private fun eventChangeListner() {
        db = FirebaseFirestore.getInstance()
        db.collection("notes").addSnapshotListener(object : EventListener<QuerySnapshot> {
            override fun onEvent(value: QuerySnapshot?, error: FirebaseFirestoreException?) {
                if (error != null) {
                    Log.e("Firestore Error", error.message.toString())
                    return
                }
                noteArrayList.clear()
                value?.let{
                    for (dc: DocumentChange in value.documentChanges) {
                        if (dc.type == DocumentChange.Type.ADDED) {
                            noteArrayList.add(dc.document.toObject(Note::class.java))
                        }
                    }
                }
                adapter.submitList(noteArrayList)
            }
        })
    }

    private fun realtimeUpdates() {
        noteCollectionRef.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
            firebaseFirestoreException?.let {
                Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                return@addSnapshotListener
            }
            querySnapshot?.let {
                val stringBuilder = StringBuilder()
                for (document in it) {
                    val note = document.toObject<Note>()
                    stringBuilder.append("$note\n")
                }
            }
        }
    }

    private fun saveNotes(note: Note) = CoroutineScope(Dispatchers.IO).launch {
        try {
            noteCollectionRef.add(note).await()
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Successfully saved data.", Toast.LENGTH_LONG)
                    .show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getNewNoteMap(): Map<String, Any> {
        val noteDetail = inputNotesText.text.toString()
        val map = mutableMapOf<String, Any>()
        if(noteDetail.isNotEmpty()) {
            map["noteDetail"] = noteDetail
        }
        return map
    }

    private fun updateNote(note: Note, newNoteMap: Map<String, Any>) = CoroutineScope(Dispatchers.IO).launch {
        val personQuery = noteCollectionRef
            .whereEqualTo("id", note.id)
            .get()
            .await()
        if(personQuery.documents.isNotEmpty()) {
            for(document in personQuery) {
                try {
                    noteCollectionRef.document(document.id).set(
                        newNoteMap,
                        SetOptions.merge()
                    ).await()
                    eventChangeListner()
                    Toast.makeText(this@MainActivity, "Updated Successfully", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "No notes matched the query.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun deleteNote(note: Note) = CoroutineScope(Dispatchers.IO).launch {
        val noteQuery = noteCollectionRef
            .whereEqualTo("noteDetail", note.noteDetail)
            .whereEqualTo("id", note.id)
            .get()
            .await()
        if(noteQuery.documents.isNotEmpty()) {
            for(document in noteQuery) {
                try {
                    noteCollectionRef.document(document.id).delete().await()
                    withContext(Dispatchers.Main) {
                        eventChangeListner()
                        Toast.makeText(this@MainActivity, "Deleted Successfully", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "No note matched the query.", Toast.LENGTH_LONG).show()
            }
        }
    }
}