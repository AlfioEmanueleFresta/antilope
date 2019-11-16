package com.example.ctap.fido2;

import com.example.ctap.ctap2.Response;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * https://fidoalliance.org/specs/fido-v2.0-ps-20190130/fido-client-to-authenticator-protocol-v2.0-ps-20190130.html#responses
 */
public class AuthenticatorMakeCredentialsResponse extends Response {
    public AuthenticatorMakeCredentialsResponse(AuthenticatorData authenticatorData, PackedAttestationStatement attestationStatement) {
        this.authenticatorData = authenticatorData;
        this.attestationStatement = attestationStatement;
    }

    public AuthenticatorData authenticatorData;
    public PackedAttestationStatement attestationStatement;

    @Override
    public void serializeCBOR(@NotNull final CBORGenerator gen,
                              @NotNull final ByteArrayOutputStream out)
            throws IOException {
        gen.writeStartObject(3);

        gen.writeFieldId(1); // Format
        gen.writeString(attestationStatement.attestationFormat);

        gen.writeFieldId(2); // Auth Data
        gen.writeBinary(authenticatorData.serialize());

        gen.writeFieldId(3); // Attestation Statement
        attestationStatement.serializeCBOR(gen, out);

        gen.writeEndObject();
    }
}
