package com.example.ctap.fido2

import com.fasterxml.jackson.dataformat.cbor.CBORGenerator
import java.io.ByteArrayOutputStream
import java.security.interfaces.ECPublicKey
import java.util.ArrayList


data class EllipticCurvePublicKey(
        val algorithm: Int,
        val publicKey: ECPublicKey):
        PublicKeyCredentialDescriptor() {

    override fun serializeCBOR(gen: CBORGenerator, output: ByteArrayOutputStream) {

        gen.writeStartObject(5)
        gen.writeFieldId(1)
        gen.writeNumber(2)
        gen.writeFieldId(3)
        gen.writeNumber(algorithm)
        gen.writeFieldId(-1)
        gen.writeNumber(1)
        gen.writeFieldId(-2)
        gen.writeBinary(normalize(publicKey.w.affineX.toByteArray()))
        gen.writeFieldId(-3)
        gen.writeBinary(normalize(publicKey.w.affineY.toByteArray()))
        gen.writeEndObject()
    }

    private fun normalize(input: ByteArray): ByteArray {
        val result = ArrayList<Byte>()
        if (input[0] == 0x00.toByte()) {
            result.addAll(input.slice(1 until input.size))
        } else {
            result.addAll(input.toList())
        }
        return result.toByteArray()
    }


}