package com.example.ctap.fido2;

import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public abstract class Response {
    final static byte CTAP2_SUCCESS = 0x00;

    public byte fidoStatus = CTAP2_SUCCESS;

    public byte[] serialize() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CBORFactory cborFactory = new CBORFactory();
        CBORGenerator generator = cborFactory.createGenerator(outputStream);
        outputStream.write(new byte[]{ fidoStatus });
        serializeCBOR(generator);
        generator.close();
        return outputStream.toByteArray();
    }

    abstract void serializeCBOR(CBORGenerator cborGenerator) throws IOException;

}
