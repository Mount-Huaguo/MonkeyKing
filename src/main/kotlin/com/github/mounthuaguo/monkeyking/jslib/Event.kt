package com.github.mounthuaguo.monkeyking.jslib

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.SelectionModel

class Event(event: AnActionEvent) {

    var editor = event.getRequiredData(CommonDataKeys.EDITOR)

    fun selectionModel(): SelectionModel {
        return SelectionModel()
    }

    fun document(): Document {
        return Document()
    }


    inner class SelectionModel() {

        fun selectionStart(): Int {
            return editor.selectionModel.selectionStart
        }

        fun selectionEnd(): Int {
            return editor.selectionModel.selectionEnd
        }

        fun selectedText(): String {
            editor.selectionModel.selectedText?.let {
                return editor.selectionModel.selectedText!!
            }
            return ""
        }

        fun hasSelected(): Boolean {
            return editor.selectionModel.hasSelection()
        }

    }

    inner class Document() {

        fun text(): String {
            return editor.document.text
        }

        fun textLength(): Int {
            return editor.document.textLength
        }

        fun lineCount(): Int {
            return editor.document.lineCount
        }

        fun replaceString(start: Int, end: Int, text: String) {
            ApplicationManager.getApplication().invokeLater({
                runWriteAction {
                    editor.document.replaceString(start, end, text)
                    editor.caretModel.moveToOffset(start + end)
                }
            }, ModalityState.any())
        }

        fun insertString(position: Int, text: String) {
            ApplicationManager.getApplication().invokeLater({
                runWriteAction {
                    editor.document.insertString(position, text)
                    editor.caretModel.moveToOffset(position + text.length)
                }
            }, ModalityState.any())
        }
    }

}