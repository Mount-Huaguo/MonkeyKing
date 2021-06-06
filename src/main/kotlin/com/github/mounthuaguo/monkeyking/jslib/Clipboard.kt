package com.github.mounthuaguo.monkeyking.jslib

import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection

class Clipboard {

    fun getContents(): String {
        val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val t = clipboard.getContents(null) ?: return ""
        return t.getTransferData(DataFlavor.stringFlavor) as String
    }

    fun setContents(msg: String) {
        val selection = StringSelection(msg)
        val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(selection, selection)
    }

}