package com.example.ctap.fido2

import com.example.ctap.ctap2.Request
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class AuthenticatorGetAssertionRequest(
    @JsonProperty("1")
    var rpId: String,

    @JsonProperty("2")
    var clientDataHash: ByteArray,

    @JsonProperty(value="3")
    var allowList: List<PublicKeyCredentialDescriptor>?,

    @JsonProperty(value="4")
    var extensions: List<String>?,

    @JsonProperty(value="5")
    var options: Map<String, Boolean>?

    // TODO pin-related data
) : Request()
