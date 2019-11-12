package com.example.ctap.fido2;

import com.example.ctap.ctap2.Serializable;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;


/**
 * https://www.w3.org/TR/webauthn/#packed-attestation
 */
class PackedAttestationStatement extends Serializable  {
    public PackedAttestationStatement(int algorithmId,
                                      byte[] signature) {
        this.attestationFormat = "packed";
        this.algorithmId = algorithmId;
        this.signature = signature;
    }

    public String attestationFormat;
    public int algorithmId;
    public byte[] signature;

    @Override
    public void serializeCBOR(@NotNull final CBORGenerator gen,
                              @NotNull final ByteArrayOutputStream output) throws IOException {
        gen.writeStartObject(2);

        gen.writeFieldName("alg");
        gen.writeNumber(algorithmId);

        gen.writeFieldName("sig");
        gen.writeBinary(signature);

        // TODO attestation certificate(s)

        gen.writeEndObject();
    }
}
