package com.example.ctap.fido2

import com.google.common.primitives.Shorts

import java.io.ByteArrayOutputStream
import java.io.IOException


/**
 * Attested credential data
 *
 * https://www.w3.org/TR/webauthn/#sec-attested-credential-data
 *
 */
class CredentialData(var aaguid: ByteArray,
                     var credentialId: ByteArray,
                     var credentialPublicKey: PublicKeyCredentialDescriptor) {

    @Throws(IOException::class)
    fun serialize(): ByteArray {
        val output = ByteArrayOutputStream()

        output.write(aaguid)
        output.write(Shorts.toByteArray(credentialId.size.toShort()))
        output.write(credentialId)
        output.write(credentialPublicKey.serialize());

        return output.toByteArray()
    }

}
