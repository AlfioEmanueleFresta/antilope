package com.example.ctap.fido2

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
internal class PublicKeyCredentialUserEntity (
        @JsonProperty("id") var id: ByteArray,
        @JsonProperty("name") var name: String,
        @JsonProperty("displayName") var displayName: String
)