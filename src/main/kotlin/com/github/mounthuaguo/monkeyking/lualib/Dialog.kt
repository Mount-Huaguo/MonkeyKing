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

  class Show : VarArgFunction() {
    override fun invoke(args: Varargs): Varargs {

      val num = args.narg()
      val arr = mutableListOf<InputModel>()
      for (i in 1 until num + 1) {
        val table = args.checktable(i)!!
        var oa: Array<String>? = null
        if (table["options"].istable()) {
          val options = mutableListOf<String>()
          val ot = table["options"].checktable()!!
          for (idx in 1 until ot.arrayLength) {
            options.add(ot[idx].checkstring().toString())
          }
          oa = options.toTypedArray()
        }
        val default = table["default"]
        var defaultValue: Any? = null
        when {
          default.isnil() -> {
            // do nothing
          }

          default.isstring() -> {
            defaultValue = default.checkstring().tostring()
          }

          default.isint() -> {
            defaultValue = default.checkint()
          }

          default.islong() -> {
            defaultValue = default.checklong().toInt()
          }

          default.istable() -> {
            val ds = mutableListOf<String>()
            val ot = default.checktable()!!
            for (idx in 1 until ot.arrayLength + 1) {
              ds.add(ot[idx].checkstring().toString())
            }
            defaultValue = ds.toTypedArray()
          }
        }
        arr.add(
          InputModel(
            type = table["type"].checkstring().toString(),
            field = table["field"].checkstring().toString(),
            options = oa,
            default = defaultValue
          )
        )
      }

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
