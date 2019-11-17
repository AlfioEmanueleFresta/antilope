package com.example.ctap.fido2

import com.example.ctap.ctap2.Request
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty


@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
internal data class AuthenticatorMakeCredentialsRequest (
        @JsonProperty("1")
        var clientDataHash: ByteArray,

        @JsonProperty("2")
        var rp: PublicKeyCredentialRpEntity,

        @JsonProperty("3")
        var user: PublicKeyCredentialUserEntity,

        @JsonProperty("4")
        var credentialTypes: List<CredentialType>,

        @JsonProperty(value="5")
        var excludeList: List<String>?,

        @JsonProperty(value="6")
        var extensions: List<String>?,

        @JsonProperty(value="7")
        var options: Map<String, Boolean>?

) : Request()
