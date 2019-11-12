package com.example.ctap.fido2;

import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AuthenticatorMakeCredentialsResponse extends Response {
    public AuthenticatorMakeCredentialsResponse(AuthenticatorData authenticatorData, AttestationStatement attestationStatement) {
        this.authenticatorData = authenticatorData;
        this.attestationStatement = attestationStatement;
    }

    public AuthenticatorData authenticatorData;
    public AttestationStatement attestationStatement;

    @Override
    void serializeCBOR(CBORGenerator gen) throws IOException {
        gen.writeStartObject(3);

        gen.writeFieldId(1);
        gen.writeString(attestationStatement.attestationFormat);

        gen.writeFieldId(2);

        ByteArrayOutputStream blob = new ByteArrayOutputStream();
        blob.write(authenticatorData.rpIdHash);
        blob.write(authenticatorData.flags);
        blob.write(new byte[] {0x00, 0x00, 0x00, 0x00}); // TODO signCount uint32
        if (authenticatorData.credentialData != null) {
            blob.write(authenticatorData.credentialData.serialize());
            // TODO this only works for ECDSA
        }
        gen.writeBinary(blob.toByteArray());

        gen.writeFieldId(3);
        gen.writeStartObject(2);
        gen.writeFieldName("alg");
        gen.writeNumber(attestationStatement.algorithmId);
        gen.writeFieldName("sig");
        gen.writeBinary(attestationStatement.signature);
        gen.writeEndObject();

        gen.writeEndObject();
    }
}
