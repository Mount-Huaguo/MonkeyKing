package com.github.mounthuaguo.monkeyking.settings

import com.github.mounthuaguo.monkeyking.ui.BrowserDialog
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.ui.EditorTextField
import com.intellij.ui.JBSplitter
import com.intellij.ui.SingleSelectionModel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBPanel
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.components.BorderLayoutPanel
import org.jetbrains.annotations.NotNull
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.table.AbstractTableModel


class MKConfigureComponent {

    private val mainPanel: JPanel = BorderLayoutPanel()
    private val scriptTable: JBTable = JBTable()
    private val scripts = mutableListOf<ScriptData>()
    private val state = MKStateService.getInstance()
    private val editor = EditorPanel()
    private var selectRow = -1;

    init {

        // table
        scripts.addAll(state.getScripts())

        println("state.getScripts(), ${state.getScripts()}")

        scriptTable.setShowColumns(true)
        scriptTable.model = object : AbstractTableModel() {

            override fun getRowCount(): Int {
                return scripts.size
            }

            override fun getColumnCount(): Int {
                return 1
            }

            override fun getColumnName(index: Int): String {
                when (index) {
                    0 -> return "NAME"
                    1 -> return "LANGUAGE"
                    2 -> return "DESCRIPTION"
                }
                return "exception"
            }

            override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
                when (columnIndex) {
                    0 -> return scripts[rowIndex].name
                    1 -> return scripts[rowIndex].language
                    2 -> return scripts[rowIndex].description
                }
                return "error"
            }

        }

        scriptTable.rowHeight = 60
        scriptTable.dragEnabled = false
        scriptTable.isStriped = true
        scriptTable.selectionModel = SingleSelectionModel()
        scriptTable.setShowGrid(false)
        scriptTable.autoscrolls = true
        val selectionModel: ListSelectionModel = scriptTable.selectionModel
        selectionModel.addListSelectionListener {
            println("ListSelectionListener ${scriptTable.selectedRow}")
            processChangedScript()
            reselectTable()
        }

        val toolbarDecorator: ToolbarDecorator = ToolbarDecorator.createDecorator(scriptTable)
        // add
        val action1: AnAction = object : DumbAwareAction(AllIcons.General.Add) {
            override fun actionPerformed(e: AnActionEvent) {
                println("action1 actionPerformed ${e}")

                scripts.add(ScriptData("lua"))
                scriptTable.removeRowSelectionInterval(selectRow, selectRow)
                refreshTable()
                scriptTable.setRowSelectionInterval(scripts.size, scripts.size)
            }
        }

        // browser
        val action2: AnAction = object : DumbAwareAction(AllIcons.Actions.Show) {
            override fun actionPerformed(e: AnActionEvent) {
                println("e.project, ${e.project}")
                if (BrowserDialog().showAndGet()) {
                    // todo
                }
            }
        }

        val action4: AnAction = object : DumbAwareAction(AllIcons.General.Remove) {
            override fun actionPerformed(e: AnActionEvent) {
                println(e)
            }
        }

        val ag = DefaultActionGroup(action1, action2, action4)
        toolbarDecorator.setActionGroup(ag)
        toolbarDecorator.setToolbarPosition(ActionToolbarPosition.BOTTOM)

        val tablePanel = toolbarDecorator.createPanel()
        tablePanel.minimumSize = Dimension(200, 600)


        val splitter = JBSplitter()
        splitter.firstComponent = tablePanel
        splitter.secondComponent = editor
        mainPanel.add(splitter, BorderLayout.CENTER)

//        mainPanel.add(tablePanel, GridConstraints(0, 0, 1, 1, 0, 3, 1 or 2, 1 or 2, null, null, null, 0, false))
//        mainPanel.add(editor, GridConstraints(0, 1, 1, 1, 0, 3, 1 or 2, 1 or 2, null, null, null, 0, false))
    }

    private fun reselectTable() {
        selectRow = scriptTable.selectedRow
        val d = scripts[selectRow]
        editor.setScriptData(d)
    }

    fun getPanel(): JPanel {
        return mainPanel
    }

    private fun processChangedScript() {
        val d = editor.getScriptData() ?: return
        scripts[selectRow] = d
    }

    fun refreshTable() {
//        scriptTable.fire
        scriptTable.repaint()
    }

    fun saveScripts() {
        val m = mutableMapOf<String, Unit>()
        for (s in scripts) {
            if (!s.isValid()) {
                return
            }
            // name duplicate
            if (m.containsKey(s.name)) {
                return
            }
            m[s.name] = Unit
        }
        state.setScripts(scripts)
        state.state
    }

    fun isEqual(): Boolean {
        if (state.getScripts().size != scripts.size) {
            return false
        }
        val m = mutableMapOf<String, Unit>()
        val ss = state.getScripts()
        for (s in ss) {
            m[s.raw] = Unit
        }
        for (s in scripts) {
            if (!m.containsKey(s.raw)) {
                return false
            }
        }
        return true
    }


    //
    private class EditorPanel() : JBPanel<JBPanel<*>>(BorderLayout()) {

        var script: ScriptData? = null

        private val editor: Editor
        private val document: Document

        init {
            // editor
            val factory = EditorFactory.getInstance()
            document = factory.createDocument("")
            document.setReadOnly(false)
            document.addDocumentListener(object : DocumentListener {
                override fun documentChanged(event: DocumentEvent) {
//                    sourceTextHasChanged(event.document.text)
                }
            })

            editor = createEditor("", document)


//            editor.setOneLineMode(false)
//            editor.autoscrolls = true
            add(editor.component, BorderLayout.CENTER)
        }

        fun setScriptData(s: ScriptData) {
            script = s
            refresh()
        }

        fun getScriptData(): ScriptData? {
            return script
        }

        fun refresh() {
            val txt = script?.raw.toString() + script?.raw.toString() + script?.raw.toString() + script?.raw.toString()

            document.setText(txt)

//            editor.text =
//                script?.raw.toString() + script?.raw.toString() + script?.raw.toString() + script?.raw.toString()
        }

        private fun createEditor(filename: String, document: Document): Editor {
            val fileExtension: String = FileUtilRt.getExtension(filename)
            val editor = EditorFactory.getInstance().createEditor(
                document,
                null,
                FileTypeManager.getInstance().getFileTypeByExtension(fileExtension),
                false
            )
            editor.component.minimumSize = Dimension(200, 600)
            editor.settings.isLineNumbersShown = false
            editor.settings.isLineMarkerAreaShown = false
            editor.settings.isFoldingOutlineShown = false
            return editor
        }

    }


    inner class MyEditor(text: @NotNull String) : EditorTextField(text) {

        init {

        }

    }


}