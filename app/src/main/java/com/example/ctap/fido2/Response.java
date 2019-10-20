package com.example.ctap.fido2;

import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public abstract class Response {
    public byte fidoStatus;

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
