package com.example.ctap.fido2

import com.example.ctap.ctap2.Serializable
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator
import com.google.common.primitives.Ints
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.experimental.or

/**
 * The authenticator data structure encodes contextual bindings made by the authenticator.
 *
 * https://www.w3.org/TR/webauthn/#sec-authenticator-data
 */
class AuthenticatorData(var rpIdHash: ByteArray,
                        var userPresent: Boolean,
                        var userVerified: Boolean,
                        var signCount: Int,
                        var credentialData: CredentialData) : Serializable() {

    @Throws(IOException::class)
    override fun serialize(): ByteArray {

        var flags: Byte = 0x00
        flags = flags or (if (userPresent)  0b00000001 else 0).toByte() // Bit 0
        flags = flags or (if (userVerified) 0b00000100 else 0).toByte() // Bit 2
        flags = flags or (if (credentialData != null)
                                            0b01000000 else 0).toByte() // Bit 6

        val blob = ByteArrayOutputStream()

        assert(rpIdHash.size == 4)
        blob.write(rpIdHash)

        assert(byteArrayOf(flags).size == 1)
        blob.write(byteArrayOf(flags))

        assert(Ints.toByteArray(signCount).size == 4)
        blob.write(Ints.toByteArray(signCount))

        blob.write(credentialData.serialize())
        // TODO this only works for ECDSA

        return blob.toByteArray()
    }

    @Throws(IOException::class)
    override fun serializeCBOR(gen: CBORGenerator,
                               output: ByteArrayOutputStream) {
        throw NotImplementedError("This isn't a CBOR object.")
    }
}
