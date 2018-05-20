package co.touchlab.knarch.db.sqlite

interface SQLiteTransactionListener{
    /**
     * Called immediately after the transaction begins.
     */
    fun onBegin()

    /**
     * Called immediately before commiting the transaction.
     */
    fun onCommit()

    /**
     * Called if the transaction is about to be rolled back.
     */
    fun onRollback()
}