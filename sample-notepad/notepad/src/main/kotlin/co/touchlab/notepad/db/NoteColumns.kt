package co.touchlab.notepad.db

class NoteColumns
{
    companion object {
        val NOTES_TABLE_NAME = "notes"

        /**
         * The default sort order for this table
         */
        val DEFAULT_SORT_ORDER = "modified DESC"
        /**
         * The title of the note
         * <P>Type: TEXT</P>
         */
        val TITLE = "title"
        /**
         * The note itself
         * <P>Type: TEXT</P>
         */
        val NOTE = "note"
        /**
         * The timestamp for when the note was created
         * <P>Type: INTEGER (long from System.curentTimeMillis())</P>
         */
        val CREATED_DATE = "created"
        /**
         * The timestamp for when the note was last modified
         * <P>Type: INTEGER (long from System.curentTimeMillis())</P>
         */
        val MODIFIED_DATE = "modified"

        val HI_BLOB = "hiblob"

        /**
         * The unique ID for a row.
         * <P>Type: INTEGER (long)</P>
         */
        val _ID = "_id"

        /**
         * The count of rows in a directory.
         * <P>Type: INTEGER</P>
         */
        val _COUNT = "_count"
    }
}