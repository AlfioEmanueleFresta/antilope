package com.example.ctap.fido2

import com.example.ctap.ctap2.Serializable
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator
import java.io.ByteArrayOutputStream

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class PublicKeyCredentialDescriptor(
        @JsonProperty("type") var type: String = "public-key",
        @JsonProperty("id") var id: ByteArray,
        @JsonProperty("transports") var transports: List<String>? = null
) : Serializable() {

    override fun serializeCBOR(gen: CBORGenerator, output: ByteArrayOutputStream) {

        gen.writeStartObject(2)

        gen.writeFieldName("id")
        gen.writeBinary(id)

        gen.writeFieldName("type")
        gen.writeString(type)

        gen.writeEndObject()
    }
}