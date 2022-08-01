package com.github.mounthuaguo.monkeyking.jslib

import com.github.mounthuaguo.monkeyking.util.*
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor

class Event(val event: AnActionEvent) {

  var editor = event.getRequiredData(CommonDataKeys.EDITOR)

  fun selectionModel(): SelectionModel {
    return SelectionModel()
  }

  fun document(): Document {
    return Document()
  }

  inner class SelectionModel {

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

  inner class Document {

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
      CommandProcessor.getInstance().executeCommand(
        event.project,
        {
          ApplicationManager.getApplication().runWriteAction {
            editor.document.replaceString(start, end, text)
            editor.caretModel.moveToOffset(start + end)
          }
        },
        "mk.event.document.replaceString", null
      )
    }

    fun insertString(position: Int, text: String) {
      CommandProcessor.getInstance().executeCommand(
        event.project,
        {
          ApplicationManager.getApplication().runWriteAction {
            editor.document.insertString(position, text)
            editor.caretModel.moveToOffset(position + text.length)
          }
        },
        "mk.event.document.insertString", null
      )
    }

    fun scanInt(): ScanInt {
      val text = editor.document.text
      val start = editor.selectionModel.selectionStart
      val end = editor.selectionModel.selectionStart
      return scanInt(start, end, text)
    }

    fun scanString(): ScanString {
      val text = editor.document.text
      val start = editor.selectionModel.selectionStart
      val end = editor.selectionModel.selectionStart
      return scanString(start, end, text)
    }

    fun scanHex(): ScanString {
      val text = editor.document.text
      val start = editor.selectionModel.selectionStart
      val end = editor.selectionModel.selectionStart
      return scanHex(start, end, text)
    }
  }
}
