package com.github.mounthuaguo.monkeyking.util

import org.junit.jupiter.api.Test

internal class ScanKtTest {

    @Test
    fun scanString() {
        val r = scanString(
            10, 10,
            """
            中文"中文测试，中文测试"
            """.trimIndent()
        )
        assert(r.start == 3)
        assert(r.end == 12)
        assert(r.value == "中文测试，中文测试")
    }

    @Test
    fun scanInt() {
        val r = scanInt(5, 6, "中文123456")
        assert(r.start == 2)
        assert(r.end == 8)
        assert(r.value == 123456)
    }

    @Test
    fun scanHex() {
        val r = scanHex(
            10, 10,
            """
            中文"AFAF1938AF94中文
            """.trimIndent()
        )
        assert(r.start == 3)
        assert(r.end == 15)
        assert(r.value == "AFAF1938AF94")
    }
}
