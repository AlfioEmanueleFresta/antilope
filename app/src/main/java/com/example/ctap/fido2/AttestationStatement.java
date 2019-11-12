package com.example.ctap.fido2;

class AttestationStatement {
    public AttestationStatement(String attestationFormat, int algorithmId, byte[] signature) {
        this.attestationFormat = attestationFormat;
        this.algorithmId = algorithmId;
        this.signature = signature;
    }

    public String attestationFormat;
    public int algorithmId;
    public byte[] signature;
}
