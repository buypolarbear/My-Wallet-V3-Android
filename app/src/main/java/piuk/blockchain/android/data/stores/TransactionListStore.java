package piuk.blockchain.android.data.stores;

import info.blockchain.wallet.multiaddress.TransactionSummary;

import java.util.HashMap;
import java.util.List;

import piuk.blockchain.android.data.transactions.Displayable;
import piuk.blockchain.android.data.transactions.DisplayableDateComparator;

/**
 * Contains both a list of {@link TransactionSummary} objects and also a Map of transaction
 * confirmations keyed to their Transaction's hash.
 */
public class TransactionListStore extends ListStore<Displayable> {

    private HashMap<String, Integer> txConfirmationsMap = new HashMap<>();

    public TransactionListStore() {
        // Empty constructor
    }

    public void insertTransactionIntoListAndSort(Displayable transaction) {
        insertObjectIntoList(transaction);
        getTxConfirmationsMap().put(transaction.getHash(), transaction.getConfirmations());
        sort(new DisplayableDateComparator());
    }

    public void insertTransactions(List<Displayable> transactions) {
        insertBulk(transactions);
        for (Displayable summary : transactions) {
            getTxConfirmationsMap().put(summary.getHash(), summary.getConfirmations());
        }
        sort(new DisplayableDateComparator());
    }

    /**
     * Returns a {@link HashMap} where a {@link TransactionSummary} hash is used as a key against
     * the confirmation number. This is for displaying the confirmation number in the Contacts page.
     */
    public HashMap<String, Integer> getTxConfirmationsMap() {
        return txConfirmationsMap;
    }

}
