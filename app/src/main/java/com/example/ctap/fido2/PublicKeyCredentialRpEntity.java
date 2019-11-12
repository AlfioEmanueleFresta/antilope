package com.example.ctap.fido2;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PublicKeyCredentialRpEntity {
    @JsonProperty("id") public String id;
    @JsonProperty("name") public String name;
    @JsonProperty(value = "icon", required = false) public String icon;
}
