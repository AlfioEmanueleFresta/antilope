package com.example.ctap.fido;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;


public class Authenticator {
    private static final String TAG = Authenticator.class.getCanonicalName();

    private final Context mContext;

    public Authenticator(Context context) {
        mContext = context;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public CompletableFuture<byte[]> request(byte cmd, byte[] payload) {
        return CompletableFuture.supplyAsync(() -> execute(cmd, payload));
    }

    private byte[] execute(byte cmd, byte[] payload) {
        Log.i(TAG, String.format("Executing: cmd=0x%x, payload(%d)=%s",
                cmd, payload.length, Arrays.toString(payload)));

        return new byte[] {};
    }

}
