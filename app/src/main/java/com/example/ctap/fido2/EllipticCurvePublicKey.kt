package com.example.ctap.fido2

import com.fasterxml.jackson.dataformat.cbor.CBORGenerator
import java.io.ByteArrayOutputStream
import java.security.interfaces.ECPublicKey


/**
 * https://www.w3.org/TR/webauthn/#sctn-encoded-credPubKey-examples
 */
data class EllipticCurvePublicKey(
        val algorithm: Int,
        val publicKey: ECPublicKey):
        CredentialPublicKey() {

    override fun serializeCBOR(gen: CBORGenerator, output: ByteArrayOutputStream) {
        // https://tools.ietf.org/html/rfc8152#section-7
        // https://tools.ietf.org/html/rfc8152#section-13
        gen.writeStartObject(5)
        gen.writeFieldId(1)  // kty (Key type)
        gen.writeNumber(2)      // EC2
        gen.writeFieldId(3)
        gen.writeNumber(algorithm)
        gen.writeFieldId(-1) // crv (Curve)
        gen.writeNumber(1)      // NIST P-256 also known as secp256r1
        gen.writeFieldId(-2) // x (x-coordinate)
        gen.writeBinary(normalize(publicKey.w.affineX.toByteArray()))
        gen.writeFieldId(-3) // y (y-coordinate)
        gen.writeBinary(normalize(publicKey.w.affineY.toByteArray()))
        // gen.writeEndObject() TODO removed for some reason.
    }

    private fun normalize(input: ByteArray): ByteArray {
        if (input[0] == 0x00.toByte()) {
            return input.takeLast(input.size - 1).toByteArray();
        }
        return input;
    }


}