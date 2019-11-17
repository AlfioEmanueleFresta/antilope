package com.example.ctap.ctap2

import java.io.IOException

/**
 * https://fidoalliance.org/specs/fido-v2.0-ps-20190130/fido-client-to-authenticator-protocol-v2.0-ps-20190130.html#responses
 */
abstract class Response(var fidoStatus: Byte = CTAP2_SUCCESS) :
        Serializable() {

    @Throws(IOException::class)
    override fun serialize(): ByteArray {
        return byteArrayOf(
                fidoStatus,
                *super.serialize()
        )
    }

    companion object {
        internal const val CTAP2_SUCCESS: Byte = 0x00
    }
}