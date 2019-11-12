package com.example.ctap.fido2;

import com.fasterxml.jackson.annotation.JsonProperty;


public class PublicKeyCredentialUserEntity {
    @JsonProperty("id") public byte[] id;
    @JsonProperty("name") public String name;
    @JsonProperty("displayName") public String displayName;
}