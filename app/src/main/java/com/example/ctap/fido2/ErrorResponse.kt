package com.example.ctap.fido2

import com.example.ctap.ctap2.Response
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator
import java.io.ByteArrayOutputStream

internal class ErrorResponse (errorCode: Byte)
    : Response(errorCode) {

    override fun serializeCBOR(gen: CBORGenerator,
                               output: ByteArrayOutputStream) {
        // No payload.
    }

    companion object {
        internal const val CTAP2_ERR_UNSUPPORTED_ALGORITHM: Byte = 0x26
        internal const val CTAP2_ERR_OPERATION_DENIED: Byte = 0x27
    }
}