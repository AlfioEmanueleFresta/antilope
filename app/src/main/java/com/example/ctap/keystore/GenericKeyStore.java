package com.example.ctap.keystore;

import java.security.PublicKey;
import java.util.Set;

public interface GenericKeyStore {
    Set<Integer> getSupportedAlgorithms();
    PublicKey createKeyPair(final String alias, final int algorithm, final boolean userRequired);
    byte[] sign(final String alias, final byte[] payload);
}
