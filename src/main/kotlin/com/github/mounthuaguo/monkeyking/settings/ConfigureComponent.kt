package com.github.mounthuaguo.monkeyking.settings

import com.intellij.icons.AllIcons
import com.intellij.ide.IdeBundle
import com.intellij.ide.fileTemplates.impl.UrlUtil
import com.intellij.ide.plugins.newui.PluginSearchTextField
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.LoadingDecorator
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.ui.*
import com.intellij.ui.SingleSelectionModel
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import java.awt.*
import java.net.URL
import javax.swing.*


class MKConfigureComponent(val project: Project) : BorderLayoutPanel() {
    private val tabbedPane = JBTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT)
    private val browserPanel = MKConfigureBrowserComponent()
    private val installedPanel = MKConfigureInstalledComponent(project)

    init {
        tabbedPane.addTab("Installed", installedPanel)
        tabbedPane.addTab("Browser", browserPanel)
        tabbedPane.addChangeListener {
            println("addChangeListener: $it")
        }
        add(tabbedPane, BorderLayout.CENTER)
    }
}

class MKConfigureInstalledComponent(val project: Project) : BorderLayoutPanel() {

    private val scriptTable = JBTable()
    private val scripts = mutableListOf<ScriptModel>()
    private val state = ConfigureStateService.getInstance()
    private val editor = EditorPanel(project) {
        scriptHasChanged(it)
    }
    private var scriptTableModel = ScriptTableModel(mutableListOf<ScriptModel>())
    private val toolbarDecorator: ToolbarDecorator

    init {

        // table
        scripts.addAll(state.getScripts())
        scripts.addAll(state.getScripts())
        scripts.addAll(state.getScripts())
        scripts.addAll(state.getScripts())
        scripts.addAll(state.getScripts())
        scripts.addAll(state.getScripts())

        scriptTableModel.setScripts(scripts)

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
        scriptTable.model = scriptTableModel
        scriptTable.rowHeight = 40
        scriptTable.isStriped = true
        scriptTable.setShowGrid(false)

        val selectionModel: ListSelectionModel = scriptTable.selectionModel
        selectionModel.addListSelectionListener {
            println("ListSelectionListener ${it}")
            val script = scriptTableModel.scriptAt(scriptTable.selectedRow)
            editor.setScriptModel(script)
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
        val splitter = JBSplitter(false, 0.35f, 0.3f, 0.5f)
        splitter.firstComponent = tablePanel
        splitter.secondComponent = editor
        splitter.firstComponent.maximumSize = Dimension(160, 100)
        splitter.firstComponent.minimumSize = Dimension(100, 100)
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
                model.removeRow(scriptTable.selectedRow)
            }
        }

        val actionGroup = DefaultActionGroup(addAction, copyAction, removeAction)
        actionGroup.isPopup = true

        return actionGroup
    }

    fun saveScripts() {
        val m = mutableMapOf<String, Unit>()
        for (s in scripts) {
            if (s.validate() != "") {
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

    private fun scriptHasChanged(script: ScriptModel) {
        if (scriptTable.selectedRow == -1) {
            return
        }
        scriptTableModel.updateScript(scriptTable.selectedRow, script)
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
    private class EditorPanel(val myProject: Project, val modelHasChanged: (model: ScriptModel) -> Unit) :
        BorderLayoutPanel(),
        DocumentListener {

        var script: ScriptModel? = null

        private var editor: Editor? = null
        private var document: Document? = null
        private var textarea = JTextArea("")

        init {
            // editor
            ApplicationManager.getApplication().invokeLater {
                val factory = EditorFactory.getInstance()
                document = factory.createDocument("")
                document!!.setReadOnly(false)
                document!!.addDocumentListener(this)
                editor = createEditor("script.lua", document!!)
                add(editor!!.component, BorderLayout.CENTER)
                textarea.foreground = Color.red
                textarea.background = Color(0, 0, 0, 0)
                textarea.isEditable = false
                add(textarea, BorderLayout.SOUTH)
            }

            val myDescriptionComponent = JEditorPane()
            myDescriptionComponent.editorKit = UIUtil.getHTMLEditorKit()
            myDescriptionComponent.text = "<html></html>"
            myDescriptionComponent.isEditable = false
            myDescriptionComponent.addHyperlinkListener(BrowserHyperlinkListener())

            val descriptionPanel = JPanel(GridBagLayout())
            descriptionPanel.add(
                JLabel(IdeBundle.message("label.description")),
                GridBagConstraints(
                    0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                    JBUI.insetsBottom(2), 0, 0
                )
            )
            descriptionPanel.add(
                ScrollPaneFactory.createScrollPane(myDescriptionComponent),
                GridBagConstraints(
                    0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    JBUI.insetsTop(2), 0, 0
                )
            )


            UrlUtil.loadText(URL(""))


        }

        private fun createEditor(@Nullable file: PsiFile): Editor? {
            val editorFactory = EditorFactory.getInstance()
            val doc: Document = createDocument(file)
            val editor = editorFactory.createEditor(doc, myProject)
            val editorSettings = editor.settings
            editorSettings.isVirtualSpace = false
            editorSettings.isLineMarkerAreaShown = false
            editorSettings.isIndentGuidesShown = false
            editorSettings.isLineNumbersShown = false
            editorSettings.isFoldingOutlineShown = false
            editorSettings.additionalColumnsCount = 3
            editorSettings.additionalLinesCount = 3
            editorSettings.isCaretRowShown = false
            editor.document.addDocumentListener(object : DocumentListener {
                override fun documentChanged(@NotNull e: DocumentEvent) {
//                    onTextChanged()
                }
            }, (editor as EditorImpl).disposable)
//            (editor as EditorEx).highlighter = createHighlighter()
            val topPanel = JPanel(BorderLayout(0, 5))
            val southPanel = JPanel(HorizontalLayout(40))
//            southPanel.add(myAdjustBox)
//            southPanel.add(myLiveTemplateBox)
            topPanel.add(southPanel, BorderLayout.SOUTH)
            topPanel.add(editor.getComponent(), BorderLayout.CENTER)
//            mySplitter.setFirstComponent(topPanel)
            return editor
        }

        private fun createDocument(file: PsiFile?): Document {
            val document = if (file != null) PsiDocumentManager.getInstance(file.project).getDocument(file) else null
            return document
                ?: EditorFactory.getInstance().createDocument("")
        }

        fun setScriptModel(s: ScriptModel) {
            script = s
            refresh()
        }

        fun refresh() {
            if (script == null) {
                return
            }
            val txt = script?.raw.toString()
            val err = script?.validate()
            if (err != "") {
                textarea.isVisible = true
                textarea.text = "Error: $err"
            } else {
                textarea.text = ""
                textarea.isVisible = false
            }
            refreshText(txt)
        }

        fun refreshText(text: String) {
            ApplicationManager.getApplication().runWriteAction() {
                println("editor: $editor")
                editor?.document?.setText(text)
            };
        }

        override fun documentChanged(event: DocumentEvent) {
            if (script == null) {
                return
            }
            script = document?.let { ScriptModel(script!!.language, it.text) }
            modelHasChanged(script!!)
            refresh()
        }

        private fun createEditor(filename: String, document: Document): Editor {
            val fileExtension: String = FileUtilRt.getExtension(filename)
            val editor = EditorFactory.getInstance().createEditor(
                document,
                myProject,
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

        add(splitter, BorderLayout.CENTER)

        queryScripts()

    }

    private fun queryScripts() {
//        GlobalScope.launch {
//            val result = runCatching {
////                val response =
////                    URL("https://raw.githubusercontent.com/Mount-Huaguo/MonkeyKingScripts/main/index.lua").readText()
////                println("response: $response")
//            }
//
//            println("result.isSuccess ${result.isSuccess}")
//        }
    }


}
