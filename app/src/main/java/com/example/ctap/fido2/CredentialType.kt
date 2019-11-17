package com.example.ctap.fido2

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty

@JsonAutoDetect(fieldVisibility=JsonAutoDetect.Visibility.ANY)
data class CredentialType (
    @JsonProperty("alg") val algorithm: Int = 0,
    @JsonProperty("type") val type: String? = null
)
