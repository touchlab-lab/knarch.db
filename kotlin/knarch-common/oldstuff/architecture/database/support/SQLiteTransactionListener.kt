package co.touchlab.kurgan.architecture.database.support

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