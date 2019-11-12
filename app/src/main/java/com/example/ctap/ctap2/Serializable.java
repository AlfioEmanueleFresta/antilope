package com.example.ctap.ctap2;

import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public abstract class Serializable {
    public byte[] serialize() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CBORFactory cborFactory = new CBORFactory();
        CBORGenerator generator = cborFactory.createGenerator(outputStream);
        serializeCBOR(generator, outputStream);
        generator.close();
        return outputStream.toByteArray();
    }

    public abstract void serializeCBOR(@NotNull final CBORGenerator gen,
                                       @NotNull final ByteArrayOutputStream output) throws IOException;

}
