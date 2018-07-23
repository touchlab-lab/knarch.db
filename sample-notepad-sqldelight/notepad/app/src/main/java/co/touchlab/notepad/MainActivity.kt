package co.touchlab.notepad

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import co.touchlab.notepad.sqldelight.Note
import co.touchlab.notepad.viewmodel.NoteViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var viewAdapter: NotesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val model = ViewModelProviders.of(this).get(NoteViewModel::class.java)
        val titleEdit = findViewById<EditText>(R.id.title)
        val noteEdit = findViewById<EditText>(R.id.note)
        findViewById<Button>(R.id.addButton).setOnClickListener {
            model.noteModel.insertNote(
                    titleEdit.text.toString(),
                    noteEdit.text.toString()
            )
        }

        viewAdapter = NotesAdapter(this)

        findViewById<ListView>(R.id.noteList).apply {
            adapter = viewAdapter
        }

        model.noteModel.notesLiveData().observe(this, Observer<List<Note>> {
            if(it != null)
                viewAdapter.updateData(it.toTypedArray())
        })

//        model.noteModel.initUpdate {
//            viewAdapter.updateData(it)
//        }

    }

    class NotesAdapter(context:Context): ArrayAdapter<Note>(context, android.R.layout.simple_list_item_1, android.R.id.text1) {
        private var notes : Array<Note> = arrayOf()
        override fun getItem(position: Int): Note = notes[position]
        override fun getCount(): Int = notes.size

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            var cv = convertView
            if(cv == null){
                cv = LayoutInflater.from(context).inflate(R.layout.row_note, null)
            }

            cv!!.findViewById<TextView>(R.id.noteTitle).text = getItem(position).title

            return cv
        }

        fun updateData(notes:Array<Note>){
            this.notes = notes
            notifyDataSetChanged()
        }
    }
}
