package com.example.ctap.fido2;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.example.ctap.keystore.GenericKeyStore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;


public class Authenticator {
    private static final String TAG = Authenticator.class.getCanonicalName();

    private static final int AUTHENTICATOR_MAKE_CREDENTIAL = 0x01;
    private static final int AUTHENTICATOR_GET_ASSERTION = 0x02;
    private static final int AUTHENTICATOR_GET_NEXT_ASSERTION = 0x08;
    private static final int AUTHENTICATOR_GET_INFO = 0x04;
    private static final int AUTHENTICATOR_CLIENT_PIN = 0x06;
    private static final int AUTHENTICATOR_RESET = 0x07;

    private static final byte[] AAGUID = new byte[]
            { 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9, 0xA, 0xB, 0xC, 0xD, 0xE, 0xF, 0x10 };


    private final Context mContext;
    private final AuthenticatorDisplay mDisplay;
    private final GenericKeyStore mKeyStore;

    public Authenticator(final Context context,
                         final GenericKeyStore keyStore,
                         final AuthenticatorDisplay display) {
        mContext = context;
        mKeyStore = keyStore;
        mDisplay = display;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public CompletableFuture<byte[]> request(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        int opcode = buffer.get();
        byte[] payload = Arrays.copyOfRange(data, 1, data.length);
        return CompletableFuture.supplyAsync(() -> {
            try {
                return execute(opcode, payload);
            } catch (Exception e) {
                Log.e(TAG, "Authenticator exception", e);
                return new byte[] {};
            }
        });
    }

    private byte[] execute(int cmd, byte[] payload) throws IOException, Exception {
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

    private byte[] makeCredential(byte[] payload) throws IOException, NoSuchAlgorithmException {
        AuthenticatorMakeCredentialsRequest request = (AuthenticatorMakeCredentialsRequest)
                Request.parse(payload, AuthenticatorMakeCredentialsRequest.class);

        Response response = handleMakeCredentialRequest(request);
        return response.serialize();
    }

    private Response handleMakeCredentialRequest(final AuthenticatorMakeCredentialsRequest request) throws NoSuchAlgorithmException, IOException {
        Log.i(TAG, "makeCredential");
        Log.i(TAG, "request.rp.id=" + request.rp.id);
        Log.i(TAG, "request.rp.name=" + request.rp.name);
        Log.i(TAG, "request.rp.icon=" + request.rp.icon);
        Log.i(TAG, "request.user.id=" + Arrays.toString(request.user.id));
        Log.i(TAG, "request.user.name=" + request.user.name);
        Log.i(TAG, "request.user.displayName=" + request.user.displayName);

        for (AuthenticatorMakeCredentialsRequest.CredentialType type:
                request.credentialTypes) {
            Log.i(TAG, "Supported cipher: algorithm=" + type.algorithm + ", type=" + type.type);
        }

        // TODO 1. check exclusions list, fail w/ CTAP2_ERR_CREDENTIAL_EXCLUDED

        // 2. Check supported algorithms
        if (mKeyStore.getSupportedAlgorithms().stream()
                .noneMatch(supportedAlgorithm ->
                        request.credentialTypes.stream().map(type -> type.algorithm)
                                .collect(Collectors.toList()).contains(supportedAlgorithm))) {
            // No supported algorithms
            return new ErrorResponse(ErrorResponse.CTAP2_ERR_UNSUPPORTED_ALGORITHM);
        }

        // TODO 3. Check options (CTAP2_ERR_UNSUPPORTED_OPTION / CTAP2_ERR_INVALID_OPTION)
        // TODO 4. Optionally check extensions.
        // 5-6 are ignored because we don't support PINs.
        // TODO 7. Ensure pinAuth is not part of the request.

        try {
            final boolean confirmed = mDisplay.confirmMakeCredentials(request.rp, request.user).get();
            if (!confirmed) {
                throw new IllegalStateException(); // TODO use user exception
            }

        } catch (IllegalStateException | ExecutionException | InterruptedException e) {
            Log.w(TAG, "User denied credentials creation.");
            e.printStackTrace();
            return new ErrorResponse(ErrorResponse.CTAP2_ERR_OPERATION_DENIED);
        }

        Log.i(TAG, "User confirmed credentials creation.");
        final String alias = getCredentialAlias(request.rp, request.user);
        final int algorithm = -36; // TODO negotiate!!
        final boolean userRequired = false; // TODO
        final PublicKey publicKey = mKeyStore.createKeyPair(alias, algorithm, userRequired);

        final MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(request.rp.id.getBytes());
        final byte[] rpIdHash = md.digest();
        final byte flags = 0x40 | 0x01 | 0x04; // TODO register, uv, up
        final int signCount = 0;

        final ByteArrayOutputStream payload = new ByteArrayOutputStream();
        payload.write(new AuthenticatorData(rpIdHash, flags, signCount,
                new CredentialData(AAGUID, request.user.id, publicKey)).serialize());
        payload.write(rpIdHash);

        final byte[] signature = mKeyStore.sign(alias, payload.toByteArray());
        Log.w(TAG, "Signature: " + Arrays.toString(signature));

        return new AuthenticatorMakeCredentialsResponse(
            new AuthenticatorData(rpIdHash, flags, signCount,
                new CredentialData(AAGUID, request.user.id, publicKey)),
            new AttestationStatement("packed", algorithm, signature)
        );

    }


    private byte[] getInfo(byte[] payload) throws IOException {
        AuthenticatorGetInfoResponse.Options options = new AuthenticatorGetInfoResponse.Options();
        AuthenticatorGetInfoResponse response = new AuthenticatorGetInfoResponse(
                new String[] {"FIDO_2_0"},
                AAGUID,
                options
        );
        return response.serialize();
    }

    private String getCredentialAlias(final AuthenticatorMakeCredentialsRequest.RelyingParty rp,
                                      final AuthenticatorMakeCredentialsRequest.User user) {
        StringBuilder userIdBuilder = new StringBuilder();
        for (byte b: user.id) {
            userIdBuilder.append(String.format("%02X", b));
        }
        return String.format("%s::%s", rp.id, userIdBuilder.toString());
    }

}
