package com.example.ctap.fido2

import com.example.ctap.ctap2.Response
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator
import java.io.ByteArrayOutputStream

data class AuthenticatorGetAssertionResponse(
        val credential: PublicKeyCredentialDescriptor,
        val authenticatorData: AuthenticatorData,
        val signature: ByteArray
) : Response() {

    override fun serializeCBOR(gen: CBORGenerator, output: ByteArrayOutputStream) {
        gen.writeStartObject(3)

        gen.writeFieldId(1)
        credential.serializeCBOR(gen, output)

        gen.writeFieldId(2)
        gen.writeBinary(authenticatorData.serialize())

        gen.writeFieldId(3)
        gen.writeBinary(signature)

        gen.writeEndObject()
    }

}