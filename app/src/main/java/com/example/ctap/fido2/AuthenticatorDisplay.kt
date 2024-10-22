package com.example.ctap.fido2

import java.util.concurrent.CompletableFuture

interface AuthenticatorDisplay {
    fun confirmMakeCredentials(
            rp: PublicKeyCredentialRpEntity,
            user: PublicKeyCredentialUserEntity
    ): CompletableFuture<Boolean>
}
