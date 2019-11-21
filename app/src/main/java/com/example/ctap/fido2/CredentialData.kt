package com.example.ctap.fido2

import android.util.Log
import com.example.ctap.Utils.byteArrayToHex
import com.google.common.primitives.Shorts
import java.io.IOException


/**
 * Attested credential data
 *
 * https://www.w3.org/TR/webauthn/#sec-attested-credential-data
 *
 */
class CredentialData(var aaguid: ByteArray,
                     var credentialId: ByteArray,
                     var credentialPublicKey: CredentialPublicKey) {

    private val TAG = CredentialData::class.simpleName

    @Throws(IOException::class)
    fun serialize(): ByteArray {
        val credentialIdSize = Shorts.toByteArray(credentialId.size.toShort());

        assert(aaguid.size == 16)
        assert(credentialIdSize.size == 2)
        Log.d(TAG, "credentialPublicKey=[" + byteArrayToHex(credentialPublicKey.serialize()) + "]")

        return byteArrayOf(
                *aaguid,
                *credentialIdSize,
                *credentialId,
                *credentialPublicKey.serialize()
        );
    }

}
