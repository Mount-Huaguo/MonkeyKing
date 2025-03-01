package com.github.mounthuaguo.monkeyking

import com.intellij.AbstractBundle
import com.intellij.openapi.util.IconLoader
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE = "messages.MKBundle"

object MonkeyBundle : AbstractBundle(BUNDLE) {

    @Suppress("SpreadOperator")
    @JvmStatic
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
        getMessage(key, *params)

    @Suppress("SpreadOperator")
    @JvmStatic
    fun messagePointer(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
        getLazyMessage(key, *params)
}

object MonkeyIcons {
    @JvmField
    val LUA = IconLoader.getIcon("/asserts/lua.svg", javaClass)
    @JvmField
    val JS = IconLoader.getIcon("/asserts/js.svg", javaClass)
}
