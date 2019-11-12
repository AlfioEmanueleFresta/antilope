package com.example.ctap.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import java.util.concurrent.CompletableFuture;

public class CompletableDialog extends DialogFragment {
    private CompletableFuture<Boolean> mSuccessFuture;
    private String mMessage;
    private String mMessageYes;
    private String mMessageNo;

    public CompletableDialog(final CompletableFuture<Boolean> successFuture,
                             final String message,
                             final String messageYes,
                             final String messageNo) {
        mSuccessFuture = successFuture;
        mMessage = message;
        mMessageYes = messageYes;
        mMessageNo = messageNo;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(mMessage)
                .setPositiveButton(mMessageYes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mSuccessFuture.complete(true);
                    }
                })
                .setNegativeButton(mMessageNo, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mSuccessFuture.complete(false);
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
