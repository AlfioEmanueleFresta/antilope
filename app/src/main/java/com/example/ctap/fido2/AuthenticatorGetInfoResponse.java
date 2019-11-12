package com.example.ctap.fido2;

import android.util.Pair;

import com.example.ctap.ctap2.Response;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;


public class AuthenticatorGetInfoResponse extends Response {

    private String[] versions;
    private byte[] aaguid;
    private Options options;

    AuthenticatorGetInfoResponse(String[] versions, byte[] aaguid, Options options) {
        this.versions = versions;
        this.aaguid = aaguid;
        this.options = options;
    }

    @Override
    public void serializeCBOR(@NotNull CBORGenerator gen, @NotNull ByteArrayOutputStream output) throws IOException {
        gen.writeStartObject(3);

        // Versions
        gen.writeFieldId(0x01);
        gen.writeStartArray(versions.length);
        for (final String version: versions) {
            gen.writeString(version);
        }
        gen.writeEndArray();

        // AAGUID
        gen.writeFieldId(0x03);
        gen.writeBinary(aaguid);

        // Options
        gen.writeFieldId(0x04);
        gen.writeStartObject(options.size());
        for (final Pair<String, Boolean> option: options) {
            gen.writeBooleanField(option.first, option.second);
        }
        gen.writeEndObject();

        gen.writeEndObject();
    }


    static class Options extends ArrayList<Pair<String, Boolean>> {
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
