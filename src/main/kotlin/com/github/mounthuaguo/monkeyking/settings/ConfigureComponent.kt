package com.github.mounthuaguo.monkeyking.settings

import com.intellij.icons.AllIcons
import com.intellij.ide.plugins.newui.PluginSearchTextField
import com.intellij.openapi.Disposable
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
import com.intellij.openapi.ui.LoadingDecorator
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.ui.JBSplitter
import com.intellij.ui.SingleSelectionModel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JTabbedPane
import javax.swing.ListSelectionModel


class MKConfigureComponent : BorderLayoutPanel() {
    private val tabbedPane = JBTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT)
    private val browserPanel = MKConfigureBrowserComponent()
    private val installedPanel = MKConfigureInstalledComponent()

    init {
        tabbedPane.addTab("Installed", installedPanel)
        tabbedPane.addTab("Browser", browserPanel)
        add(tabbedPane, BorderLayout.CENTER)
    }
}

class MKConfigureInstalledComponent : BorderLayoutPanel() {

    private val scriptTable = JBTable()
    private val scripts = mutableListOf<ScriptModel>()
    private val state = ConfigureStateService.getInstance()
    private val editor = EditorPanel()
    private var selectRow = -1;
    private val toolbarDecorator: ToolbarDecorator

    init {

        // table
        scripts.addAll(state.getScripts())
        scripts.addAll(state.getScripts())
        scripts.addAll(state.getScripts())
        scripts.addAll(state.getScripts())
        scripts.addAll(state.getScripts())
        scripts.addAll(state.getScripts())
//        scripts.addAll(state.getScripts())
//        scripts.addAll(state.getScripts())
//        scripts.addAll(state.getScripts())
//        scripts.addAll(state.getScripts())
//        scripts.addAll(state.getScripts())
//        scripts.addAll(state.getScripts())
//        scripts.addAll(state.getScripts())

        println("state.getScripts(), ${state.getScripts()}")
        scriptTable.dragEnabled = false
        scriptTable.selectionModel = SingleSelectionModel()
        scriptTable.autoscrolls = true
        val scriptTableModelCell = ScriptTableModelCell { row: Int, checked: Boolean ->
            val model = scriptTable.model as ScriptTableModel
            model.updateScriptEnabled(row, checked)
        }

        scriptTable.setDefaultRenderer(ScriptTableModelCell::class.java, scriptTableModelCell)
        scriptTable.setDefaultEditor(ScriptTableModelCell::class.java, scriptTableModelCell)
        scriptTable.model = ScriptTableModel(scripts)
        scriptTable.rowHeight = 60
        scriptTable.isStriped = true
        scriptTable.setShowGrid(false)

        val selectionModel: ListSelectionModel = scriptTable.selectionModel
        selectionModel.addListSelectionListener {
            println("ListSelectionListener ${it}")
        }

        toolbarDecorator = ToolbarDecorator.createDecorator(scriptTable)
        toolbarDecorator.setActionGroup(anActionGroup())
        toolbarDecorator.disableAddAction()
        toolbarDecorator.disableRemoveAction()
        toolbarDecorator.disableUpAction()
        toolbarDecorator.disableUpDownActions()
        toolbarDecorator.disableDownAction()
        toolbarDecorator.setToolbarPosition(ActionToolbarPosition.BOTTOM)

        val tablePanel = toolbarDecorator.createPanel()
        val splitter = JBSplitter()

        splitter.firstComponent = tablePanel
        splitter.secondComponent = editor
        add(splitter, BorderLayout.CENTER)
    }

    private fun anActionGroup(): DefaultActionGroup {

        val newLuaAction: AnAction = object : DumbAwareAction("Add lua script") {
            override fun actionPerformed(e: AnActionEvent) {
                println("action1 actionPerformed ${e}")
                val script = ScriptModel("lua")
                (scriptTable.model as ScriptTableModel).addScripts(script)
            }
        }

        val newJsAction: AnAction = object : DumbAwareAction("Add js Script") {
            override fun actionPerformed(e: AnActionEvent) {
                println("action1 actionPerformed ${e}")
                val script = ScriptModel("js")
                (scriptTable.model as ScriptTableModel).addScripts(script)
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
//                println(" toolbarDecorator.actionsPanel.position, ${toolbarDecorator.actionsPanel.position}")
                popup.show(RelativePoint.getSouthWestOf(toolbarDecorator.actionsPanel))
            }
        }

        val copyAction: AnAction = object : DumbAwareAction(AllIcons.Actions.Copy) {
            override fun actionPerformed(e: AnActionEvent) {
                println(e)
            }
        }

        val removeAction: AnAction = object : DumbAwareAction(AllIcons.General.Remove) {
            override fun actionPerformed(e: AnActionEvent) {
                println("scriptTable.selectedRow, ${scriptTable.selectedRow}")
                if (scriptTable.selectedRow <= -1) {
                    return
                }
                val model = scriptTable.model as ScriptTableModel
                model.removeAt(scriptTable.selectedRow)
            }
        }

        val actionGroup = DefaultActionGroup(addAction, copyAction, removeAction)
        actionGroup.isPopup = true

        return actionGroup
    }

    private fun reselectTable() {
//        selectRow = scriptTable.selectedRow
//        val d = scripts[selectRow]
//        editor.setScriptData(d)
    }

    private fun processChangedScript() {
        val d = editor.getScriptData() ?: return
        scripts[selectRow] = d
    }

    fun refreshTable() {
//        scriptTable.fire
//        scriptTable.repaint()
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
                }
            })

            editor = createEditor("", document)
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
}

class MKConfigureBrowserComponent : BorderLayoutPanel() {

    private val leftPanel = BorderLayoutPanel()
    private val rightPanel = BorderLayoutPanel()
    private val searchBar = PluginSearchTextField()
    private val table = JBTable()
    private val splitter = JBSplitter()

    init {
        leftPanel.add(searchBar, BorderLayout.NORTH)
        val scripts = ConfigureStateService.getInstance().getScripts()
        table.model = ScriptTableModel(scripts.toMutableList())
        table.setDefaultRenderer(ScriptTableModelCell::class.java, ScriptTableModelCell { i: Int, b: Boolean ->

        })
        table.rowHeight = 80
        table.dragEnabled = false
        table.isStriped = true
        table.selectionModel = SingleSelectionModel()
        table.setShowGrid(false)
        table.autoscrolls = true
        val loadingDecorator = LoadingDecorator(table, Disposable() {

        }, 0)

        val loading = JBLoadingPanel(null, Disposable { })
        loading.startLoading()
        leftPanel.add(loading, BorderLayout.CENTER)

        splitter.firstComponent = leftPanel
        splitter.secondComponent = rightPanel

//        splitter.addHierarchyListener { println("splitter.addHierarchyListener $it") }
//        splitter.addHierarchyBoundsListener(object : HierarchyBoundsListener {
//            override fun ancestorMoved(e: HierarchyEvent?) {
//                println("HierarchyBoundsListener, ancestorMoved, ${e}")
//            }
//
//            override fun ancestorResized(e: HierarchyEvent?) {
//                println(
//                    "HierarchyBoundsListener, ancestorResized,${
//                        splitter.firstComponent.size
//                    }"
//                )
//
//            }
//
//        })

        add(splitter, BorderLayout.CENTER)
    }

}
