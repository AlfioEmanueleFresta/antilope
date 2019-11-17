package com.example.ctap.fido2

import com.example.ctap.ctap2.Response
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * https://fidoalliance.org/specs/fido-v2.0-ps-20190130/fido-client-to-authenticator-protocol-v2.0-ps-20190130.html#responses
 */
internal class AuthenticatorMakeCredentialsResponse(
        var authenticatorData: AuthenticatorData,
        var attestationStatement: PackedAttestationStatement
) : Response(CTAP2_SUCCESS) {

    @Throws(IOException::class)
    override fun serializeCBOR(gen: CBORGenerator,
                               out: ByteArrayOutputStream) {
        gen.writeStartObject(3)
        gen.writeFieldId(1) // Format
        gen.writeString(attestationStatement.attestationFormat)
        gen.writeFieldId(2) // Auth Data
        gen.writeBinary(authenticatorData.serialize())
        gen.writeFieldId(3) // Attestation Statement
        attestationStatement.serializeCBOR(gen, out)
        gen.writeEndObject()
    }

}