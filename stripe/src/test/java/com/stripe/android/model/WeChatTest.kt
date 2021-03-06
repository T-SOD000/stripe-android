package com.stripe.android.model

import kotlin.test.Test
import kotlin.test.assertEquals
import org.json.JSONException
import org.json.JSONObject

class WeChatTest {

    @Test
    @Throws(JSONException::class)
    fun fromJson_shouldReturnExpectedObject() {
        val actual = WeChat.fromJson(WE_CHAT_PAY_JSON)

        val expected = WeChat(
            statementDescriptor = "ORDER 123",
            appId = "wxa0dfnoie578ce",
            nonce = "yFNjgfoni3kZEPYID",
            packageValue = "Sign=WXPay",
            partnerId = "2623457",
            prepayId = "wx070440552351e841913701900",
            sign = "1A98A09EA74DCF12349B33DED3FF6BCED1C062C63B43AE773D8",
            timestamp = "1565134055"
        )

        assertEquals(expected, actual)
    }

    companion object {
        private val WE_CHAT_PAY_JSON = JSONObject(
            """
            {
                "statement_descriptor": "ORDER 123",
                "android_appId": "wxa0dfnoie578ce",
                "android_nonceStr": "yFNjgfoni3kZEPYID",
                "android_package": "Sign=WXPay",
                "android_partnerId": "2623457",
                "android_prepayId": "wx070440552351e841913701900",
                "android_sign": "1A98A09EA74DCF12349B33DED3FF6BCED1C062C63B43AE773D8",
                "android_timeStamp": "1565134055"
            }
            """.trimIndent()
        )
    }
}
