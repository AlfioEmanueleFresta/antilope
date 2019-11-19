package com.example.ctap

interface FidoToken {
    suspend fun sendKeepAlive()
}
