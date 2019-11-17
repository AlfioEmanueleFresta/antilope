package com.example.ctap.fido2

import com.example.ctap.ctap2.Serializable
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * https://www.w3.org/TR/webauthn/#packed-attestation
 */
internal class PackedAttestationStatement(var algorithmId: Int,
                                          var signature: ByteArray) :
        Serializable() {

    var attestationFormat = "packed"

    @Throws(IOException::class)
    override fun serializeCBOR(gen: CBORGenerator,
                               output: ByteArrayOutputStream) {
        gen.writeStartObject(2)
        gen.writeNumberField("alg", algorithmId)
        gen.writeBinaryField("sig", signature)
        // TODO attestation certificate(s)
        gen.writeEndObject()
    }

}