package com.example.ctap.fido2

import com.example.ctap.ctap2.Response
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator
import java.io.ByteArrayOutputStream
import java.io.IOException

internal class AuthenticatorGetInfoResponse(
        private val versions: Array<String>,
        private val aaguid: ByteArray,
        private val options: Options) :
        Response(CTAP2_SUCCESS) {

    @Throws(IOException::class)
    override fun serializeCBOR(gen: CBORGenerator,
                               output: ByteArrayOutputStream) {
        gen.writeStartObject(2)
        // Versions
        gen.writeFieldId(0x01)
        gen.writeStartArray(versions.size)
        for (version in versions) {
            gen.writeString(version)
        }
        gen.writeEndArray()
        // AAGUID
        gen.writeFieldId(0x03)
        gen.writeBinary(aaguid)
        /*
        // Options
        gen.writeFieldId(0x04);
        gen.writeStartObject(options.size());
        for (final Pair<String, Boolean> option: options) {
            gen.writeBooleanField(option.first, option.second);
        }
        gen.writeEndObject();
         */
        gen.writeEndObject()
    }

    internal class Options : ArrayList<Pair<String?, Boolean?>?>() {
        init {
            // resident key: Indicates that the device is capable of storing keys on the device
            // itself and therefore can satisfy the authenticatorGetAssertion request with
            // allowList parameter not specified or empty.
            this.add(Pair("rk", true))
            // user presence: Indicates that the device is capable of testing user presence.
            this.add(Pair("up", false))
            // user verification: Indicates that the device is capable of verifying the user within
            // itself. For example, devices with UI, biometrics fall into this category.
            this.add(Pair("uv", false))
            // platform device: Indicates that the device is attached to the client and therefore
            // can’t be removed and used on another client.
            this.add(Pair("plat", false))
        }
    }

}