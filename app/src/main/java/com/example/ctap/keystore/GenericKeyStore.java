package com.example.ctap.keystore;

import com.example.ctap.fido2.CredentialPublicKey;

import java.util.Set;

public interface GenericKeyStore {
    Set<Integer> getSupportedAlgorithms();
    CredentialPublicKey createKeyPair(final String alias,
                                      final int algorithm,
                                      final boolean userRequired);
    byte[] sign(final String alias, final byte[] payload);
}
