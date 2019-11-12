package com.example.ctap.ctap2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;

import java.io.IOException;

public class Request {

    public static Request parse(byte[] data, Class clazz) throws IOException {
        CBORFactory cborFactory = new CBORFactory();
        ObjectMapper mapper = new ObjectMapper(cborFactory);
        return (Request) mapper.readValue(data, clazz);
    }

}
