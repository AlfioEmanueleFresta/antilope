package com.example.ctap.keystore.impl;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import com.example.ctap.fido2.Algorithms;
import com.example.ctap.fido2.PublicKeyCredentialDescriptor;
import com.example.ctap.keystore.GenericKeyStore;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.spec.ECGenParameterSpec;
import java.util.HashSet;
import java.util.Set;

public class AndroidKeyStore implements GenericKeyStore {

    private KeyStore ks;
    private KeyPairGenerator kg;

    public AndroidKeyStore() throws KeyStoreException {
        try {
            ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);

            kg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
        } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException | NoSuchProviderException e) {
            // Not much we can do.
            e.printStackTrace();
            throw new KeyStoreException(e);
        }
    }

    @Override
    public Set<Integer> getSupportedAlgorithms() {
        HashSet<Integer> algorithms = new HashSet<>();
        algorithms.add(Algorithms.PUBLIC_KEY_ES256);
        return algorithms;
    }

    private KeyGenParameterSpec getKeyGenParameterSpec(final String alias,
                                                       final int algorithm,
                                                       final boolean userRequired) {
        if (algorithm != Algorithms.PUBLIC_KEY_ES256) {
            return null; // TODO fixme
        }

        return new KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN)
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1")) // TODO pick best curve
                .setUserAuthenticationRequired(userRequired)
                .setUserAuthenticationValidityDurationSeconds(5 * 60) // TODO confirm value is within spec.
                .build();
    }

    public PublicKeyCredentialDescriptor createKeyPair(final String alias,
                                                       final int algorithm,
                                                       final boolean userRequired) {
        final KeyGenParameterSpec spec = getKeyGenParameterSpec(alias, algorithm, userRequired);

        try {
            Log.i("AndroidKeyStore", "Spec=" + spec.toString());
            kg.initialize(spec);

        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
            throw new RuntimeException(e);

        }

        final KeyPair pair = kg.generateKeyPair();
        return (PublicKeyCredentialDescriptor) pair.getPublic();
    }

    public byte[] sign(final String alias,
                       final byte[] payload) {
        try {
            final PrivateKey privateKey = (PrivateKey)
                    ks.getKey(alias, null);

            Signature signer = Signature.getInstance("SHA256withECDSA");
            signer.initSign(privateKey);
            signer.update(payload);
            return signer.sign();

        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableEntryException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
            return null;

        }
    }
}
