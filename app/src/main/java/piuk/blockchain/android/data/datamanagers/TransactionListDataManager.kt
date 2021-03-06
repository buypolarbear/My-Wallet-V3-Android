package piuk.blockchain.android.data.datamanagers

import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.util.FormatsUtil
import io.reactivex.Observable
import io.reactivex.Single
import piuk.blockchain.android.data.ethereum.EthDataManager
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.data.stores.TransactionListStore
import piuk.blockchain.android.data.transactions.BtcDisplayable
import piuk.blockchain.android.data.transactions.Displayable
import piuk.blockchain.android.data.transactions.EthDisplayable
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.util.annotations.Mockable
import java.util.*

@Mockable
class TransactionListDataManager(
        private val payloadManager: PayloadManager,
        private val ethDataManager: EthDataManager,
        private val transactionListStore: TransactionListStore
) {

    fun fetchTransactions(itemAccount: ItemAccount, limit: Int, offset: Int): Observable<List<Displayable>> {
        val observable: Observable<List<Displayable>> = when (itemAccount.type) {
            ItemAccount.TYPE.ALL_ACCOUNTS_AND_LEGACY -> getAllTransactionsObservable(limit, offset)
            ItemAccount.TYPE.ALL_LEGACY -> getLegacyObservable(limit, offset)
            ItemAccount.TYPE.ETHEREUM -> getEthereumObservable()
            else -> if (FormatsUtil.isValidXpub(itemAccount.address)) {
                getAccountObservable(itemAccount, limit, offset)
            } else {
                getLegacyObservable(limit, offset)
            }
        }

        return observable.doOnNext { insertTransactionList(it.toMutableList()) }
                .map { transactionListStore.list }
                .doOnError { emptyList<Displayable>() }
                .compose(RxUtil.applySchedulersToObservable())
    }

    /**
     * Returns a list of [Displayable] objects generated by [getTransactionList]
     *
     * @return A list of Txs sorted by date.
     */
    fun getTransactionList(): List<Displayable> = transactionListStore.list

    /**
     * Resets the list of Transactions.
     */
    fun clearTransactionList() {
        transactionListStore.clearList()
    }

    /**
     * Allows insertion of a single new [Displayable] into the main transaction list.
     *
     * @param transaction A new, most likely temporary [Displayable]
     * @return An updated list of Txs sorted by date
     */
    fun insertTransactionIntoListAndReturnSorted(transaction: Displayable): List<Displayable> {
        transactionListStore.insertTransactionIntoListAndSort(transaction)
        return transactionListStore.list
    }

    /**
     * Get total BTC balance from [ItemAccount].
     *
     * @param itemAccount [ItemAccount]
     * @return A BTC value as a long.
     */
    fun getBtcBalance(itemAccount: ItemAccount): Long {
        return when (itemAccount.type) {
            ItemAccount.TYPE.ALL_ACCOUNTS_AND_LEGACY -> payloadManager.walletBalance.toLong()
            ItemAccount.TYPE.ALL_LEGACY -> payloadManager.importedAddressesBalance.toLong()
            ItemAccount.TYPE.SINGLE_ACCOUNT -> payloadManager.getAddressBalance(itemAccount.address).toLong()
            else -> throw IllegalArgumentException("You can't get the BTC balance of an ETH account")
        }
    }

    /**
     * Get a specific [Displayable] from a hash
     *
     * @param transactionHash The hash of the Tx to be returned
     * @return An Observable object wrapping a Tx. Will call onError if not found with a
     * NullPointerException
     */
    fun getTxFromHash(transactionHash: String): Single<Displayable> =
            Observable.fromIterable(getTransactionList())
                    .filter { it.hash == transactionHash }
                    .firstOrError()

    /**
     * Returns a [HashMap] where a [Displayable] hash is used as a key against
     * the confirmation number. This is for displaying the confirmation number in the Contacts page.
     * Please note that this is deliberately not cleared when switching accounts.
     */
    fun getTxConfirmationsMap(): HashMap<String, Int> = transactionListStore.txConfirmationsMap

    private fun insertTransactionList(txList: MutableList<Displayable>) {
        val pendingTxs = getRemainingPendingTransactionList(txList)
        clearTransactionList()
        txList.addAll(pendingTxs)
        transactionListStore.insertTransactions(txList)
    }

    /**
     * Gets list of transactions that have been published but delivery has not yet been confirmed.
     */
    private fun getRemainingPendingTransactionList(newlyFetchedTxs: List<Displayable>): List<Displayable> {
        val pendingMap = HashMap<String, Displayable>()
        transactionListStore.list
                .filter { it.isPending }
                .forEach { pendingMap.put(it.hash, it) }

        if (!pendingMap.isEmpty()) {
            filterProcessed(newlyFetchedTxs, pendingMap)
        }

        return ArrayList(pendingMap.values)
    }

    private fun filterProcessed(
            newlyFetchedTxs: List<Displayable>,
            pendingMap: HashMap<String, Displayable>
    ) {
        newlyFetchedTxs.filter { pendingMap.containsKey(it.hash) }
                .forEach { pendingMap.remove(it.hash) }
    }

    private fun getAllTransactionsObservable(limit: Int, offset: Int): Observable<List<Displayable>> =
            Observable.fromCallable {
                payloadManager.getAllTransactions(limit, offset)
                        .map { BtcDisplayable(it) }
            }

    private fun getLegacyObservable(limit: Int, offset: Int): Observable<List<Displayable>> =
            Observable.fromCallable {
                payloadManager.getImportedAddressesTransactions(limit, offset)
                        .map { BtcDisplayable(it) }
            }

    private fun getAccountObservable(itemAccount: ItemAccount, limit: Int, offset: Int): Observable<List<Displayable>> =
            Observable.fromCallable {
                payloadManager.getAccountTransactions(itemAccount.address, limit, offset)
                        .map { BtcDisplayable(it) }
            }

    private fun getEthereumObservable(): Observable<List<Displayable>> = ethDataManager.getLatestBlock()
            .flatMap { latestBlock ->
                ethDataManager.getEthTransactions()
                        .map {
                            EthDisplayable(
                                    ethDataManager.getEthResponseModel()!!,
                                    it,
                                    latestBlock.blockHeight
                            )
                        }.toList()
                        .toObservable()
            }

}
