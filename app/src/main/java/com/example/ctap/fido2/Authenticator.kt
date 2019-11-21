package com.example.ctap.fido2

import android.util.Log
import com.example.ctap.FidoToken
import com.example.ctap.Utils
import com.example.ctap.Utils.byteArrayToHex
import com.example.ctap.ctap2.Request
import com.example.ctap.ctap2.Response
import com.example.ctap.fido2.AuthenticatorGetInfoResponse.Options
import com.example.ctap.keystore.GenericKeyStore
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.IOException
import java.security.MessageDigest
import java.util.function.Function
import java.util.stream.Collectors


class Authenticator(private val mKeyStore: GenericKeyStore,
                    private val mDisplay: AuthenticatorDisplay) {

    suspend fun request(data: ByteArray,
                        token: FidoToken): ByteArray {
        val opcode = data[0].toInt()
        val payload = data.copyOfRange(1, data.size)
        return withContext (Dispatchers.Default) {
            execute(opcode, payload, token);
        }
    }

    private fun execute(cmd: Int, payload: ByteArray, token: FidoToken): ByteArray {
        Log.i(TAG, String.format("Executing: cmd=0x%x, payload(%d)=%s",
                cmd, payload.size, byteArrayToHex(payload)))

        return when (cmd) {
            AUTHENTICATOR_GET_INFO -> getInfo(payload)
            AUTHENTICATOR_MAKE_CREDENTIAL -> makeCredential(payload, token)
            AUTHENTICATOR_GET_ASSERTION -> getAssertion(payload, token)
            else -> invalidCmd()
        }
    }

    private fun invalidCmd(): ByteArray {
        Log.e(TAG, "Unsupported command");
        return ErrorResponse(ErrorResponse.CTAP1_ERR_INVALID_COMMAND)
                             .serialize();
    }

    private fun makeCredential(payload: ByteArray, token: FidoToken): ByteArray {
        val request = Request.parse(payload, AuthenticatorMakeCredentialsRequest::class.java)
                as AuthenticatorMakeCredentialsRequest
        val response = handleMakeCredentialRequest(request, token)
        return response.serialize()
    }

    private fun getAssertion(payload: ByteArray, token: FidoToken): ByteArray {
        val request = Request.parse(payload, AuthenticatorGetAssertionRequest::class.java)
            as AuthenticatorGetAssertionRequest
        val response = handleGetAssertionRequest(request, token)
        return response.serialize()
    }

    private fun handleMakeCredentialRequest(request: AuthenticatorMakeCredentialsRequest,
                                            token: FidoToken): Response {
        Log.i(TAG, "makeCredential request=$request")
        for (type in request.credentialTypes) {
            Log.i(TAG, "Supported cipher: $type")
        }

        // TODO 1. check exclusions list, fail w/ CTAP2_ERR_CREDENTIAL_EXCLUDED
        // 2. Check supported algorithms
        if (mKeyStore.supportedAlgorithms.stream()
                        .noneMatch { supportedAlgorithm: Int? ->
                            request.credentialTypes.stream().map(Function { type: CredentialType -> type.algorithm })
                                    .collect(Collectors.toList()).contains(supportedAlgorithm)
                        }) { // No supported algorithms
            return ErrorResponse(ErrorResponse.CTAP2_ERR_UNSUPPORTED_ALGORITHM)
        }

        // TODO 3. Check options (CTAP2_ERR_UNSUPPORTED_OPTION / CTAP2_ERR_INVALID_OPTION)
        // TODO 4. Optionally check extensions.
        // 5-6 are ignored because we don't support PINs.
        // TODO 7. Ensure pinAuth is not part of the request.
        try {
            val confirmed = runBlocking {
                waitForUserConfirmation(request.rp, request.user, token);
            }
            // TODO use user exception
            check(confirmed)

        } catch (e: Exception) {
            Log.w(TAG, "User denied credentials creation.")
            e.printStackTrace()
            return ErrorResponse(ErrorResponse.CTAP2_ERR_OPERATION_DENIED)
        }

        Log.i(TAG, "User confirmed credentials creation.")
        val alias = getCredentialAlias(request.rp, request.user)

        val algorithm = Algorithms.PUBLIC_KEY_ES256 // TODO negotiate!!
        val userRequired = false // TODO
        val publicKey = mKeyStore.createKeyPair(alias, algorithm, userRequired)

        val md = MessageDigest.getInstance("SHA-256")
        md.update(request.rp.id.toByteArray())
        val rpIdHash = md.digest()

        val signCount = 0
        val authenticatorData = AuthenticatorData(
                rpIdHash = rpIdHash,
                userPresent = true, userVerified = true, signCount = signCount,
                credentialData = CredentialData(AAGUID, request.user.id, publicKey))
        Log.d(TAG, "clientDataHash=[" + Utils.byteArrayToHex(request.clientDataHash) + "]")

        val payload = byteArrayOf(*authenticatorData.serialize(), *request.clientDataHash)
        Log.d(TAG, "payload=[" + Utils.byteArrayToHex(payload) + "]")

        val signature = mKeyStore.sign(alias, payload)
        Log.w(TAG, "signature=[" + Utils.byteArrayToHex(signature) + "]")

        return AuthenticatorMakeCredentialsResponse(
                authenticatorData,
                PackedAttestationStatement(algorithm, signature)
        )
    }

    private fun handleGetAssertionRequest(request: AuthenticatorGetAssertionRequest,
                                          token: FidoToken): Response {
        Log.i(TAG, "getAssertion request=$request");

        val md = MessageDigest.getInstance("SHA-256")
        md.update(request.rpId.toByteArray());
        val digest = md.digest()

        val authenticatorData = AuthenticatorData(
                rpIdHash = digest,
                userPresent =  true, userVerified = false, signCount = 0)
        val payload = byteArrayOf(*authenticatorData.serialize(),
                                            *request.clientDataHash)

        val alias = "barfoo"; // TODO
        val signature = mKeyStore.sign(alias, payload)

        val credential = request.allowList!!.get(0)
        val response = AuthenticatorGetAssertionResponse(credential, authenticatorData,
                signature)

        return response
        // todo check uv and up options
    }

    private suspend fun waitForUserConfirmation(rp: PublicKeyCredentialRpEntity,
                                                user: PublicKeyCredentialUserEntity,
                                                token: FidoToken) : Boolean {
        val channel = Channel<Boolean>();
        GlobalScope.launch {
            val confirmed = mDisplay.confirmMakeCredentials(rp, user).get()
            channel.send(confirmed);
            channel.close();
        }

        var result: Boolean? = null;
        while ( result == null ) {
            Log.i(TAG, "Waiting for user confirmation, sending keep-alive packet...");
            delay(500L);
            token.sendKeepAlive();
            result = channel.poll();
        }

        return result;
    }

    @Throws(IOException::class)
    private fun getInfo(payload: ByteArray): ByteArray {
        val options = Options()
        val response = AuthenticatorGetInfoResponse(arrayOf("FIDO_2_0"),
                AAGUID,
                options
        )
        return response.serialize()
    }

    private fun getCredentialAlias(rp: PublicKeyCredentialRpEntity,
                                   user: PublicKeyCredentialUserEntity): String {
        return "barfoo";
        val userIdBuilder = StringBuilder()
        for (b in user.id) {
            userIdBuilder.append(String.format("%02X", b))
        }
        return String.format("%s::%s", rp.id, userIdBuilder.toString())
    }

    companion object {
        private val TAG = Authenticator::class.java.canonicalName
        private const val AUTHENTICATOR_MAKE_CREDENTIAL = 0x01
        private const val AUTHENTICATOR_GET_ASSERTION = 0x02
        private const val AUTHENTICATOR_GET_NEXT_ASSERTION = 0x08
        private const val AUTHENTICATOR_GET_INFO = 0x04
        private const val AUTHENTICATOR_CLIENT_PIN = 0x06
        private const val AUTHENTICATOR_RESET = 0x07
        private val AAGUID = byteArrayOf(0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9, 0xA, 0xB, 0xC, 0xD, 0xE, 0xF, 0x10)
    }

}