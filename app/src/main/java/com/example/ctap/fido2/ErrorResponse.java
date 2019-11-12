package com.example.ctap.fido2;

import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;

import java.io.IOException;

public class ErrorResponse extends Response {
    final static byte CTAP2_ERR_UNSUPPORTED_ALGORITHM = 0x26;
    final static byte CTAP2_ERR_OPERATION_DENIED = 0x27;

    ErrorResponse(final byte errorCode) {
        fidoStatus = errorCode;
    }

    @Override
    void serializeCBOR(CBORGenerator cborGenerator) throws IOException {
        // No-op.
    }
}
