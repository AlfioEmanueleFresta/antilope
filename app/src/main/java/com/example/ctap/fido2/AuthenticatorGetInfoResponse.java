package com.example.ctap.fido2;

import android.util.Pair;

import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;


public class AuthenticatorGetInfoResponse extends Response {

    private String[] versions;
    private byte[] aaguid;
    private Options options;

    public AuthenticatorGetInfoResponse(String[] versions, byte[] aaguid, Options options) {
        this.versions = versions;
        this.aaguid = aaguid;
        this.options = options;
    }

    @Override
    void serializeCBOR(CBORGenerator cborGenerator) throws IOException {
        cborGenerator.writeStartObject(3);

        // Versions
        cborGenerator.writeFieldId(0x01);
        cborGenerator.writeStartArray(versions.length);
        for (final String version: versions) {
            cborGenerator.writeString(version);
        }
        cborGenerator.writeEndArray();

        // AAGUID
        cborGenerator.writeFieldId(0x03);
        cborGenerator.writeBinary(aaguid);

        // Options
        cborGenerator.writeFieldId(0x04);
        cborGenerator.writeStartObject(options.size());
        for (final Pair<String, Boolean> option: options) {
            cborGenerator.writeBooleanField(option.first, option.second);
        }
        cborGenerator.writeEndObject();

        cborGenerator.writeEndObject();
    }


    public static class Options extends ArrayList<Pair<String, Boolean>> {
        Options() {
            super();

            // resident key: Indicates that the device is capable of storing keys on the device
            // itself and therefore can satisfy the authenticatorGetAssertion request with
            // allowList parameter not specified or empty.
            this.add(new Pair<>("rk", true));

            // user presence: Indicates that the device is capable of testing user presence.
            this.add(new Pair<>("up", true));

            // user verification: Indicates that the device is capable of verifying the user within
            // itself. For example, devices with UI, biometrics fall into this category.
            this.add(new Pair<>("uv", true));

            // platform device: Indicates that the device is attached to the client and therefore
            // can’t be removed and used on another client.
            this.add(new Pair<>("plat", false));
        }
    }

}
