package com.github.mounthuaguo.monkeyking.jslib

import com.github.mounthuaguo.monkeyking.ui.FormDialog
import com.github.mounthuaguo.monkeyking.ui.InputModel

class Dialog {

    fun show(vararg models: Any): Any {
        val inputs = mutableListOf<InputModel>()
        models.forEach {
            inputs.add(this.getModel(it))
        }
        val d = FormDialog(inputs.toTypedArray())
        val success = d.showAndGet()
        if (success) {
            val values = d.values()
            return mapOf(
                Pair("success", success),
                Pair("data", values)
            )
        }
        return mapOf<String, Any>(
            Pair("success", success)
        )
    }

    private fun getModel(m: Any): InputModel {
        val meta = m::class.java
        val method = meta.getDeclaredMethod("get", Object::class.java)
        val typ = method.invoke(m, "type").toString()
        val field = method.invoke(m, "field").toString()
        var default = method.invoke(m, "default")
        when (default) {
            null -> {
                // do nothing
            }
            is String -> {
                default = default.toString()
            }
            is Int -> {
                default = default.toString()
            }
            else -> {
                default = this.getArray(default)
            }
        }
        val options = method.invoke(m, "options")
        var oa: Array<String>? = null
        when (options) {
            null -> {
                // do nothing
            }
            else -> {
                oa = this.getArray(options)
            }
        }

        return InputModel(typ, field, options = oa, default = default)
    }

    private fun getArray(a: Any): Array<String> {
        val meta = a::class.java
        val size = meta.getDeclaredMethod("size").invoke(a) as Int
        val method = meta.getDeclaredMethod("get", Object::class.java)
        val options = mutableListOf<String>()
        for (i in 0..size) {
            val option = method.invoke(a, i.toString()).toString()
            options.add(option)
        }
        return options.toTypedArray()
    }
}
