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
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.ui.EditorTextField
import com.intellij.ui.JBSplitter
import com.intellij.ui.SingleSelectionModel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBPanel
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.components.BorderLayoutPanel
import org.jetbrains.annotations.NotNull
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JPanel
import javax.swing.ListSelectionModel


class MKConfigureComponent {

    private val mainPanel: JPanel = BorderLayoutPanel()
    private val scriptTable: JBTable = JBTable()
    private val scripts = mutableListOf<ScriptModel>()
    private val state = MKStateService.getInstance()
    private val editor = EditorPanel()
    private var selectRow = -1;

    init {

        // table
        scripts.addAll(state.getScripts())

        println("state.getScripts(), ${state.getScripts()}")
        scriptTable.tableHeader = null;
//        scriptTable.setShowColumns(true)
        scriptTable.setDefaultRenderer(ScriptTableModelCell::class.java, ScriptTableModelCell())
        scriptTable.setDefaultRenderer(ScriptTableModelControlCell::class.java, ScriptTableModelControlCell())
        scriptTable.model = ScriptTableModel(scripts)
        scriptTable.rowHeight = 80
        scriptTable.dragEnabled = false
        scriptTable.isStriped = true
        scriptTable.selectionModel = SingleSelectionModel()
        scriptTable.setShowGrid(false)
        scriptTable.autoscrolls = true
        scriptTable.columnModel.getColumn(1).width = 40;
        val selectionModel: ListSelectionModel = scriptTable.selectionModel
        selectionModel.addListSelectionListener {
            println("ListSelectionListener ${scriptTable.selectedRow}")
            processChangedScript()
            reselectTable()
        }

        val toolbarDecorator: ToolbarDecorator = ToolbarDecorator.createDecorator(scriptTable)
        // add

        val newLuaAction: AnAction = object : DumbAwareAction("Add lua script") {
            override fun actionPerformed(e: AnActionEvent) {
                println("action1 actionPerformed ${e}")

                scripts.add(ScriptModel("lua"))
                scriptTable.removeRowSelectionInterval(selectRow, selectRow)
                refreshTable()
                scriptTable.setRowSelectionInterval(scripts.size, scripts.size)
            }
        }

        val newJsAction: AnAction = object : DumbAwareAction("Add js Script") {
            override fun actionPerformed(e: AnActionEvent) {
                println("action1 actionPerformed ${e}")
                scripts.add(ScriptModel("lua"))
                scriptTable.removeRowSelectionInterval(selectRow, selectRow)
                refreshTable()
                scriptTable.setRowSelectionInterval(scripts.size, scripts.size)
            }
        }

        // browser
        val browserAction: AnAction = object : DumbAwareAction(AllIcons.Actions.Show) {
            override fun actionPerformed(e: AnActionEvent) {
                println("e.project, ${e.project}")
                if (BrowserDialog().showAndGet()) {
                    // todo
                    println("dialog closed")
                }
            }
        }

        val copyAction: AnAction = object : DumbAwareAction(AllIcons.Actions.Copy) {
            override fun actionPerformed(e: AnActionEvent) {
                println(e)
            }
        }

        val removeAction: AnAction = object : DumbAwareAction(AllIcons.General.Remove) {
            override fun actionPerformed(e: AnActionEvent) {
                println(e)
            }
        }


        val newActionGroup = DefaultActionGroup(newLuaAction, newJsAction)
        newActionGroup.templatePresentation.icon = AllIcons.General.Add
        newActionGroup.isPopup = true


        val addAction: AnAction = object : DumbAwareAction(AllIcons.General.Add) {
            override fun actionPerformed(e: AnActionEvent) {
                val popup = JBPopupFactory.getInstance().createActionGroupPopup(
                    null,
                    newActionGroup,
                    e.dataContext,
                    JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                    false
                );
                println(" toolbarDecorator.actionsPanel.position, ${toolbarDecorator.actionsPanel.position}")
                popup.show(RelativePoint.getSouthWestOf(toolbarDecorator.actionsPanel))
            }
        }

        val actionGroup = DefaultActionGroup(addAction, browserAction, copyAction, removeAction)
        actionGroup.isPopup = true
        toolbarDecorator.setActionGroup(actionGroup)
        toolbarDecorator.setToolbarPosition(ActionToolbarPosition.BOTTOM)

        val tablePanel = toolbarDecorator.createPanel()
        tablePanel.minimumSize = Dimension(200, 600)

        val splitter = JBSplitter()
        splitter.firstComponent = tablePanel
        splitter.secondComponent = editor
        mainPanel.add(splitter, BorderLayout.CENTER)
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

        var script: ScriptModel? = null

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

        fun setScriptData(s: ScriptModel) {
            script = s
            refresh()
        }

        fun getScriptData(): ScriptModel? {
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