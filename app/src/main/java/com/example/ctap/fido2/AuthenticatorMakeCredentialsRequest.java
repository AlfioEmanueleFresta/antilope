package com.example.ctap.fido2;

import com.example.ctap.ctap2.Request;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;


public class AuthenticatorMakeCredentialsRequest extends Request {

    @JsonProperty("1") byte[] clientDataHash;
    @JsonProperty("2") PublicKeyCredentialRpEntity rp;
    @JsonProperty("3") PublicKeyCredentialUserEntity user;
    @JsonProperty("4") List<CredentialType> credentialTypes;
    @JsonProperty(value = "5", defaultValue="[]") public List<String> excludeList;
    @JsonProperty(value = "6") public List<String> extensions;
    @JsonProperty(value = "7") public Map<String, Boolean> options;

    static class CredentialType {
        @JsonProperty("alg") int algorithm;
        @JsonProperty("type") String type;
    }

}
