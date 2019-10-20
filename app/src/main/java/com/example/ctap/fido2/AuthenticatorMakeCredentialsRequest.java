package com.example.ctap.fido2;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;


public class AuthenticatorMakeCredentialsRequest extends Request {

    @JsonProperty("1") byte[] clientDataHash;
    @JsonProperty("2") RelyingParty rp;
    @JsonProperty("3") User user;
    @JsonProperty("4") List<CredentialType> credentialTypes;
    @JsonProperty(value = "5", defaultValue="[]") public List<String> excludeList;
    @JsonProperty(value = "6") public List<String> extensions;
    @JsonProperty(value = "7") public Map<String, Boolean> options;

    static class RelyingParty {
        @JsonProperty("id") String id;
        @JsonProperty("name") String name;
        @JsonProperty(value = "icon", required = false) String icon;
    }

    static class User {
        @JsonProperty("id") byte[] id;
        @JsonProperty("name") String name;
        @JsonProperty("displayName") String displayName;
    }

    static class CredentialType {
        @JsonProperty("alg") int algorithm;
        @JsonProperty("type") String type;
    }

}
