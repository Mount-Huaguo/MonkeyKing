package com.github.mounthuaguo.monkeyking.actions

import com.github.mounthuaguo.monkeyking.ui.ScriptDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import org.luaj.vm2.lib.jse.JsePlatform

abstract class ScriptAction(private val language: String) : AnAction() {

    var sourceText: String = ""
    var scriptText: String = ""
    private var targetText: String = ""
    private val dialog: ScriptDialog
    var isSelected: Boolean = false

    init {
        dialog = ScriptDialog(language, sourceText, scriptText) { typ, text ->
            handleCallBack(typ, text)
        }
    }

    private fun showDialog(language: String) {
        dialog.showAndGet()
    }

    private fun handleCallBack(typ: String, text: String) {
        when (typ) {
            "source" -> {
                sourceText = text
                targetText = execScript(sourceText, scriptText)
                dialog.setTargetDocument(targetText)
            }
            "script" -> {
                scriptText = text
                targetText = execScript(sourceText, scriptText)
                dialog.setTargetDocument(targetText)
            }
            "copy" -> {
                // todo copy
            }
            "replace" -> {
                // todo replace
            }
        }
    }

    abstract fun execScript(source: String, script: String): String

}


class LuaScriptAction() : ScriptAction("lua") {

    constructor(template: String) : this() {
        scriptText = templateText.toString()
    }

    override fun execScript(source: String, script: String): String {
        return try {
            val jse = JsePlatform.standardGlobals()
            jse["source"] = source
            val chuck = jse.load(script)
            chuck.call().checkstring().toString()
        } catch (e: Exception) {
            e.toString()
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(LangDataKeys.EDITOR) ?: return

        if (editor.selectionModel.hasSelection()) {
            sourceText = editor.selectionModel.selectedText.toString()
        }
        sourceText = editor.document.text

    }

}


class JSScriptAction() : ScriptAction("js") {

    constructor(template: String) : this() {
        scriptText = templateText.toString()
    }

    override fun execScript(source: String, script: String): String {
        TODO("Not yet implemented")
    }

    override fun actionPerformed(e: AnActionEvent) {

    }
}