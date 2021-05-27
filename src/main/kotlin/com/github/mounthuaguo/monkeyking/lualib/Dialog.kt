package com.github.mounthuaguo.monkeyking.lualib

import com.github.mounthuaguo.monkeyking.ui.FormDialog
import com.github.mounthuaguo.monkeyking.ui.InputModel
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.VarArgFunction

class Dialog : TwoArgFunction() {
    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
        val dialog = LuaTable(0, 10)
        dialog["show"] = Show()
        arg2["dialog"] = dialog
        arg2["package"]["loaded"]["dialog"] = dialog
        return dialog
    }

    class Show() : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {

            val num = args.narg()
            val arr = mutableListOf<InputModel>()
            for (i in 1 until num + 1) {
                val table = args.checktable(i)!!
                var oa: Array<String>? = null
                println("options ${table["options"]}, ${table["options"].istable()}")
                if (table["options"].istable()) {
                    val options = mutableListOf<String>()
                    val ot = table["options"].checktable()!!
                    for (idx in 1 until ot.arrayLength) {
                        options.add(ot[idx].checkstring().toString())
                    }
                    oa = options.toTypedArray()
                }
                arr.add(
                    InputModel(
                        type = table["type"].checkstring().toString(),
                        field = table["field"].checkstring().toString(),
                        options = oa,
                    )
                )
            }

            println("dialog arr: $args, $arr")

            val dialog = FormDialog(arr.toTypedArray())
            val result = LuaTable()
            if (dialog.showAndGet()) {
                result["success"] = valueOf(true)
                val data = LuaTable()
                val values = dialog.values()
                values.forEach { (t, u) ->
                    when (u) {
                        is String -> {
                            data[t] = valueOf(u.toString())
                        }
                        is Array<*> -> {
                            val opts = LuaTable()
                            u.forEachIndexed { index, it ->
                                opts[index + 1] = valueOf(it as String)
                            }
                            data[t] = opts
                        }
                        is Int -> {
                            data[t] = valueOf(u)
                        }
                    }
                }
                result["data"] = data
            } else {
                result["success"] = valueOf(false)
            }
            return result
        }
    }
}