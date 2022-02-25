package com.example.notesapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.view.*

class NoteAdapter(
    private var noteList: List<Note>,
    private val recyclerViewClickListener: RecyclerViewClickListener
//    private val onItemSelected: (note : Note) -> Unit,
    ): ListAdapter<Note, NoteAdapter.ViewHolder>(DiffUtil()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.note_item, parent, false)
        return ViewHolder(itemView, recyclerViewClickListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val note : Note = noteList[position]
//        onItemSelected(note)
        holder.noteDetail.text = note.noteDetail
        holder.noteDetail.setOnClickListener {
            recyclerViewClickListener.onRecyclerViewItemClicked(note)
        }
    }

//    override fun getItemCount(): Int {
//        return noteList.size
//    }

    class ViewHolder(itemView : View, listener: RecyclerViewClickListener): RecyclerView.ViewHolder(itemView) {
        val noteDetail : TextView = itemView.findViewById(R.id.notesContent)
    }

    class DiffUtil : androidx.recyclerview.widget.DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem == newItem
        }
    }
}