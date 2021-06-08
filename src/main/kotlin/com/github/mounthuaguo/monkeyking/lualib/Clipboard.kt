package com.github.mounthuaguo.monkeyking.lualib

import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection

class Clipboard : TwoArgFunction() {
    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
        val clipboard = LuaTable(0, 10)
        clipboard["setContents"] = SetContent()
        clipboard["getContents"] = GetContent()
        arg2["clipboard"] = clipboard
        arg2["package"]["loaded"]["clipboard"] = clipboard
        return clipboard
    }

    private inner class SetContent() : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val selection = StringSelection(arg.toString())
            val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(selection, selection)
            return valueOf(true)
        }
    }

    private inner class GetContent() : ZeroArgFunction() {
        override fun call(): LuaValue {
            val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
            val t = clipboard.getContents(null) ?: return valueOf("")
            return valueOf(t.getTransferData(DataFlavor.stringFlavor) as String)
        }
    }
}
