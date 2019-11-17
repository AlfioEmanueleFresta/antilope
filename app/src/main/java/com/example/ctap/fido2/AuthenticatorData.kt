package com.example.ctap.fido2

import android.util.Log
import com.example.ctap.Utils.byteArrayToHex
import com.example.ctap.ctap2.Serializable
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator
import com.google.common.primitives.Ints
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * The authenticator data structure encodes contextual bindings made by the authenticator.
 *
 * https://www.w3.org/TR/webauthn/#sec-authenticator-data
 */
class AuthenticatorData(var rpIdHash: ByteArray,
                        var userPresent: Boolean,
                        var userVerified: Boolean,
                        var signCount: Int,
                        var credentialData: CredentialData? = null) : Serializable() {

    private val TAG = Authenticator::class.qualifiedName;

    @Throws(IOException::class)
    override fun serialize(): ByteArray {
        var flags = 0x00
        flags += if (userPresent)    0b00000001 else 0     // Bit 0
        flags += if (userVerified)   0b00000100 else 0     // Bit 2
        flags += if (credentialData != null)
                                     0b01000000 else 0     // Bit 6
        Log.d(TAG, "AuthenticatorData.flags=$flags");

        assert(rpIdHash.size == 4)
        assert(Ints.toByteArray(signCount).size == 4)

        // TODO this only works for ECDSA
        val authData = byteArrayOf(
                *rpIdHash,
                flags.toByte(),
                *Ints.toByteArray(signCount),
                *(if (credentialData != null)
                    credentialData!!.serialize()
                        else
                    byteArrayOf()
                )
        );
        Log.d(TAG, "AuthenticatorData.serialize()=" + byteArrayToHex(authData));
        return authData;

    }

    @Throws(IOException::class)
    override fun serializeCBOR(gen: CBORGenerator,
                               output: ByteArrayOutputStream) {
        throw NotImplementedError("This isn't a CBOR object.")
    }
}
