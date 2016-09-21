package info.blockchain.wallet.view;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentManager;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import info.blockchain.wallet.model.PendingTransaction;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.ViewUtils;
import info.blockchain.wallet.view.helpers.TransferFundsDataManager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.AlertPromptTransferFundsBinding;
import piuk.blockchain.android.databinding.FragmentBackupCompleteBinding;
import rx.subscriptions.CompositeSubscription;

public class BackupWalletCompleteFragment extends Fragment {

    public static final String TAG = BackupWalletCompleteFragment.class.getSimpleName();
    private static final String KEY_CHECK_TRANSFER = "check_transfer";

    private CompositeSubscription mCompositeSubscription;

    public static BackupWalletCompleteFragment newInstance(boolean checkTransfer) {
        Bundle args = new Bundle();
        args.putBoolean(KEY_CHECK_TRANSFER, checkTransfer);
        BackupWalletCompleteFragment fragment = new BackupWalletCompleteFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentBackupCompleteBinding dataBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_backup_complete, container, false);
        mCompositeSubscription = new CompositeSubscription();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setElevation(ViewUtils.convertDpToPixel(5F, getActivity()));
        }

        long lastBackup = new PrefsUtil(getActivity()).getValue(BackupWalletActivity.BACKUP_DATE_KEY, 0);

        if (lastBackup != 0) {
            @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");
            String date = dateFormat.format(new Date(lastBackup * 1000));
            String message = String.format(getResources().getString(R.string.backup_last), date);

            dataBinding.subheadingDate.setText(message);
        } else {
            dataBinding.subheadingDate.setVisibility(View.GONE);
        }

        dataBinding.buttonBackupAgain.setOnClickListener(v -> {
            getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            getFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new BackupWalletStartingFragment())
                    .addToBackStack(BackupWalletStartingFragment.TAG)
                    .commit();
        });

        if (getArguments() != null && getArguments().getBoolean(KEY_CHECK_TRANSFER)) {
            TransferFundsDataManager fundsHelper = new TransferFundsDataManager(PayloadManager.getInstance());
            mCompositeSubscription.add(
                    fundsHelper.getTransferableFundTransactionListForDefaultAccount()
                            .subscribe(map -> {
                                Map.Entry<List<PendingTransaction>, Pair<Long, Long>> entry = map.entrySet().iterator().next();
                                if (!entry.getKey().isEmpty()) {
                                    showTransferFundsPrompt();
                                }
                            }, Throwable::printStackTrace));
        }

        return dataBinding.getRoot();
    }

    private void showTransferFundsPrompt() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle);
        AlertPromptTransferFundsBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(getActivity()),
                R.layout.alert_prompt_transfer_funds, null, false);
        dialogBuilder.setView(dialogBinding.getRoot());

        AlertDialog alertDialog = dialogBuilder.create();
        dialogBinding.confirmDontAskAgain.setVisibility(View.GONE);
        dialogBinding.confirmCancel.setOnClickListener(v -> alertDialog.dismiss());
        dialogBinding.confirmSend.setOnClickListener(v -> {
            alertDialog.dismiss();
            showTransferFundsConfirmationDialog();
        });

        alertDialog.show();
    }

    private void showTransferFundsConfirmationDialog() {
        ConfirmFundsTransferDialogFragment fragment = ConfirmFundsTransferDialogFragment.newInstance();
        fragment.show(((AppCompatActivity) getActivity()).getSupportFragmentManager(), ConfirmFundsTransferDialogFragment.TAG);
    }

    @Override
    public void onDestroy() {
        mCompositeSubscription.clear();
        super.onDestroy();
    }
}
