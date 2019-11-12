package com.example.ctap.fido2;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AuthenticatorData {
    public byte[] rpIdHash;
    public byte flags;
    public int signCount;
    public CredentialData credentialData;

    public AuthenticatorData(byte[] rpIdHash, byte flags, int signCount, CredentialData credentialData) {
        this.rpIdHash = rpIdHash;
        this.flags = flags;
        this.signCount = signCount;
        this.credentialData = credentialData;
    }

    public byte[] serialize() throws IOException {
        final ByteBuffer payloadBuffer = ByteBuffer.allocate(
                rpIdHash.length + 1 + 4 + (credentialData != null? credentialData.serialize().length: 0)
        );
        payloadBuffer.put(rpIdHash);
        payloadBuffer.put(flags);
        payloadBuffer.putInt(signCount);
        if (this.credentialData != null) {
            payloadBuffer.put(credentialData.serialize());
        }
        return payloadBuffer.array();
    }
}
