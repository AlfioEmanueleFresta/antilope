package com.example.ctap.ctap2;


import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * https://fidoalliance.org/specs/fido-v2.0-ps-20190130/fido-client-to-authenticator-protocol-v2.0-ps-20190130.html#responses
 */
public abstract class Response extends Serializable {
    final static byte CTAP2_SUCCESS = 0x00;
    protected byte fidoStatus = CTAP2_SUCCESS;

    @Override
    public byte[] serialize() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(new byte[] {fidoStatus});  // Header
        output.write(super.serialize());        // Body
        return output.toByteArray();
    }

}
