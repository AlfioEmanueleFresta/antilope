package com.example.ctap.fido2;

import android.content.Context;
import android.os.Build;
import android.util.Log;


import androidx.annotation.RequiresApi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;


public class Authenticator {
    private static final String TAG = Authenticator.class.getCanonicalName();

    private static final int AUTHENTICATOR_MAKE_CREDENTIAL = 0x01;
    private static final int AUTHENTICATOR_GET_ASSERTION = 0x02;
    private static final int AUTHENTICATOR_GET_NEXT_ASSERTION = 0x08;
    private static final int AUTHENTICATOR_GET_INFO = 0x04;
    private static final int AUTHENTICATOR_CLIENT_PIN = 0x06;
    private static final int AUTHENTICATOR_RESET = 0x07;


    private final Context mContext;

    public Authenticator(Context context) {
        mContext = context;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public CompletableFuture<byte[]> request(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        int opcode = buffer.get();
        byte[] payload = Arrays.copyOfRange(data, 1, data.length);
        return CompletableFuture.supplyAsync(() -> {
            try {
                return execute(opcode, payload);
            } catch (IOException e) {
                Log.e(TAG, "IOException", e);
                return new byte[] {}; // TODO
            }
        });
    }

    private byte[] execute(int cmd, byte[] payload) throws IOException {
        Log.i(TAG, String.format("Executing: cmd=0x%x, payload(%d)=%s",
                cmd, payload.length, Arrays.toString(payload)));

        switch(cmd) {
            case AUTHENTICATOR_GET_INFO: return getInfo(payload);
            case AUTHENTICATOR_MAKE_CREDENTIAL: return makeCredential(payload);
            default: {
                Log.e(TAG, "Unsupported cmd");
                return new byte[] {};

            }
        }
    }

    private byte[] makeCredential(byte[] payload) throws IOException {
        AuthenticatorMakeCredentialsRequest request = (AuthenticatorMakeCredentialsRequest)
                Request.parse(payload, AuthenticatorMakeCredentialsRequest.class);

        Log.i(TAG, "makeCredential");
        Log.i(TAG, "request.rp.id=" + request.rp.id);
        Log.i(TAG, "request.rp.name=" + request.rp.name);
        Log.i(TAG, "request.rp.icon=" + request.rp.icon);
        Log.i(TAG, "request.user.id=" + Arrays.toString(request.user.id));
        Log.i(TAG, "request.user.name=" + request.user.name);
        Log.i(TAG, "request.user.displayName=" + request.user.displayName);
        return new byte[] {};
    }

    private byte[] getInfo(byte[] payload) throws IOException {
        AuthenticatorGetInfoResponse.Options options = new AuthenticatorGetInfoResponse.Options();
        AuthenticatorGetInfoResponse response = new AuthenticatorGetInfoResponse(
                new String[] {"FIDO_2_0"},
                new byte[] { 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9, 0xA, 0xB, 0xC,
                             0xD, 0xE, 0xF, 0x10 },
                options
        );
        return response.serialize();
    }

}
