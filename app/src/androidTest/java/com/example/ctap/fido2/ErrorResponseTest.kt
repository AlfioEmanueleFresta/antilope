package com.example.ctap.fido2

import org.junit.Assert
import org.junit.Test

class ErrorResponseTest {

    @Test
    fun serialize() {
        val response = ErrorResponse(0x42)
        Assert.assertArrayEquals(response.serialize(),
                byteArrayOf(0x42)
        )
    }

}