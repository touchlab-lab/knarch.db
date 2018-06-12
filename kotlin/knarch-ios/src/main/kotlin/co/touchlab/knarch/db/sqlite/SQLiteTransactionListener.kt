package co.touchlab.knarch.db.sqlite

/**
 * Like the Android version. Because of threading rules, the listener will only
 * be called on the thread that started the transaction.
 */
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