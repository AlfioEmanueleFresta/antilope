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

    public static class RelyingParty {
        @JsonProperty("id") public String id;
        @JsonProperty("name") public String name;
        @JsonProperty(value = "icon", required = false) public String icon;
    }

    public static class User {
        @JsonProperty("id") public byte[] id;
        @JsonProperty("name") public String name;
        @JsonProperty("displayName") public String displayName;
    }

    static class CredentialType {
        @JsonProperty("alg") int algorithm;
        @JsonProperty("type") String type;
    }

}
