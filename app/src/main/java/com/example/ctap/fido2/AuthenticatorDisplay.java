package com.example.ctap.fido2;

import java.util.concurrent.CompletableFuture;

public interface AuthenticatorDisplay {
    CompletableFuture<Boolean> confirmMakeCredentials(
            final PublicKeyCredentialRpEntity rp,
            final PublicKeyCredentialUserEntity user
    );


}
