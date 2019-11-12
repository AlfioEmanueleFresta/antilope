package com.example.ctap.fido2;

import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;

class CredentialData {
    public byte[] aaguid;
    public byte[] credentialId;
    public PublicKey credentialPublicKey;

    public CredentialData(byte[] aaguid, byte[] credentialId, PublicKey credentialPublicKey) {
        this.aaguid = aaguid;
        this.credentialId = credentialId;
        this.credentialPublicKey = credentialPublicKey;
    }

    public byte[] serialize() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CBORGenerator gen = new CBORFactory().createGenerator(output);

        output.write(aaguid);
        output.write(credentialId);

        gen.writeStartObject(5);
        gen.writeFieldId(1);
        gen.writeNumber(2);
        gen.writeFieldId(3);
        gen.writeNumber(-7);
        gen.writeFieldId(-1);
        gen.writeNumber(1);
        gen.writeFieldId(-2);
        gen.writeBinary(((ECPublicKey) credentialPublicKey).getW().getAffineX().toByteArray());
        gen.writeFieldId(-3);
        gen.writeBinary(((ECPublicKey) credentialPublicKey).getW().getAffineY().toByteArray());
        gen.writeEndObject();

        return output.toByteArray();
    }

}
