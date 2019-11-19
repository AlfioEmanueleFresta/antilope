package com.example.ctap.fido2

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class PublicKeyCredentialRpEntity(
        @JsonProperty("id") var id: String,
        @JsonProperty("name") var name: String,
        @JsonProperty(value="icon", required=false) var icon: String?
)
