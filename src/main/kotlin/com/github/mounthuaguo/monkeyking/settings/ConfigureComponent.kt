package com.github.mounthuaguo.monkeyking.settings

import com.intellij.icons.AllIcons
import com.intellij.ide.fileTemplates.FileTemplateManager
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
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.LoadingDecorator
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.ui.*
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.util.*
import javax.swing.JEditorPane
import javax.swing.JPanel
import javax.swing.JTabbedPane
import javax.swing.ListSelectionModel


class MKConfigureComponent(private val myProject: Project) : BorderLayoutPanel() {
    private val tabbedPane = JBTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT)
    private val browserPanel = MKConfigureBrowserComponent()
    private val installedPanel = MKConfigureInstalledComponent(myProject)

    init {
        tabbedPane.addTab("Installed", installedPanel)
        tabbedPane.addTab("Browser", browserPanel)
        tabbedPane.addChangeListener {
            println("addChangeListener: $it")
        }
        add(tabbedPane, BorderLayout.CENTER)
    }
}

class MKConfigureInstalledComponent(private val myProject: Project) : BorderLayoutPanel() {

    private var scriptListView: CheckBoxList<ScriptModel> = CheckBoxList()
    private val scripts = mutableListOf<ScriptModel>()
    private val state = ConfigureStateService.getInstance()
    private val editorPanel = ScriptEditorPanel(myProject)
    private val mySpliterator = JBSplitter(false, 0.35f, 0.3f, 0.5f)
    private var toolbarDecorator: ToolbarDecorator? = null

    init {
        // table
        scripts.addAll(state.getScripts())
        scripts.addAll(state.getScripts())
        scripts.addAll(state.getScripts())
        scripts.addAll(state.getScripts())
        scripts.addAll(state.getScripts())
        scripts.addAll(state.getScripts())
        setupUI()

    }

    private fun setupUI() {
        setupScriptListView()
        setupEditorView()
        add(mySpliterator, BorderLayout.CENTER)
    }

    // script list view
    private fun setupScriptListView() {
        val toolbar = ToolbarDecorator.createDecorator(scriptListView)
        toolbar.setActionGroup(anActionGroup())
        toolbar.disableAddAction()
        toolbar.disableRemoveAction()
        toolbar.disableUpAction()
        toolbar.disableUpDownActions()
        toolbar.disableDownAction()
        toolbar.setToolbarPosition(ActionToolbarPosition.BOTTOM)
        toolbarDecorator = toolbar

        val listPanel = toolbar.createPanel()
        mySpliterator.firstComponent = listPanel

        val scriptListModel = ScriptListModel(scripts)
        scriptListView.model = scriptListModel
        scriptListView.fixedCellHeight = 32
        scriptListView.cellRenderer = ScriptListModelCell()
        scriptListView.selectionMode = ListSelectionModel.SINGLE_SELECTION
        scriptListView.setCheckBoxListListener { index, value ->
            println("setCheckBoxListListener, $index, $value")
        }
        scriptListView.addListSelectionListener {
            println("addListSelectionListener $it")
            if (!it.valueIsAdjusting) {
                return@addListSelectionListener
            }
//            editorPanel.setScriptModel(scriptListModel.getElementAt(it.firstIndex))
        }

//        val list = CheckBoxList

        mySpliterator.firstComponent = scriptListView
    }

    private fun setupEditorView() {
        mySpliterator.secondComponent = editorPanel
    }


    private fun anActionGroup(): DefaultActionGroup {

        val newLuaAction: AnAction = object : DumbAwareAction("Add lua script") {
            override fun actionPerformed(e: AnActionEvent) {
                println("action1 actionPerformed ${e}")
                val script = ScriptModel("lua")
            }
        }

        val newJsAction: AnAction = object : DumbAwareAction("Add js Script") {
            override fun actionPerformed(e: AnActionEvent) {
                println("action1 actionPerformed ${e}")
                val script = ScriptModel("js")
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
                popup.show(RelativePoint.getSouthWestOf(toolbarDecorator!!.actionsPanel))
            }
        }

        val copyAction: AnAction = object : DumbAwareAction(AllIcons.Actions.Copy) {
            override fun actionPerformed(e: AnActionEvent) {
                println(e)
            }
        }

        val removeAction: AnAction = object : DumbAwareAction(AllIcons.General.Remove) {
            override fun actionPerformed(e: AnActionEvent) {
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

    //    DefaultListCellRenderer
    private class ScriptEditorPanel(val myProject: Project) : JPanel(GridBagLayout()) {

        private var myScriptEditorPanel = BorderLayoutPanel()
        private var myScriptEditor: Editor? = null;
        private var myErrorPanel: JEditorPane? = null;
        private var myScriptModel: ScriptModel? = null;
        private val mySpliterator = JBSplitter(true, 0.9f, 0.5f, 0.9f)
        private var luaFieType = FileTypeManager.getInstance().getFileTypeByExtension("lua")
        private var jsFileType = FileTypeManager.getInstance().getFileTypeByExtension("js")

        init {
            setupUI()
        }

        fun setupUI() {
            val panel = JEditorPane()
            panel.editorKit = UIUtil.getHTMLEditorKit()
            panel.isEditable = false
            panel.addHyperlinkListener(BrowserHyperlinkListener())
            myErrorPanel = panel

            val errorComponent = ScrollPaneFactory.createScrollPane(panel)
            mySpliterator.secondComponent = errorComponent

            this.add(
                mySpliterator,
                GridBagConstraints(
                    0, 0, 1, 1, 1.0, 1.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH, JBUI.insetsBottom(2), 0, 0
                )
            )
        }

        fun setScriptModel(script: ScriptModel) {
            myScriptModel = script
            reset()
        }

        private fun reset() {
            myScriptEditor?.let {
                println("release editor: $myScriptEditor")
                EditorFactory.getInstance().releaseEditor(myScriptEditor!!);
            }
            myScriptEditor = createEditor()
            this.revalidate()
            this.repaint()
        }

        private fun createEditor(): Editor {
            val editorFactory = EditorFactory.getInstance()
            val doc: Document = createDocument()
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
                override fun documentChanged(e: DocumentEvent) {
                    scriptHasChanged()
                }
            }, (editor as EditorImpl).disposable)
            myScriptEditorPanel.removeAll()
            myScriptEditorPanel.add(editor.component, BorderLayout.CENTER)
            mySpliterator.firstComponent = myScriptEditorPanel
            return editor
        }

        private fun scriptHasChanged() {
            val text = myScriptEditor?.document?.text
            val scriptModel = text?.let { ScriptModel(myScriptModel?.language ?: "lua", it) }
            val errorMessage = scriptModel?.validate()
            errorMessage?.let {
                myErrorPanel?.text = """
                   $errorMessage 
                """.trimIndent()
            }
        }

        private fun createDocument(): Document {
            println("createDocument myScriptModel?.raw $myScriptModel ")
            return EditorFactory.getInstance().createDocument(myScriptModel?.raw ?: "")
        }


        private fun createFile(text: String): PsiFile? {
            val fileType: FileType = if (myScriptModel?.language == "lua") {
                luaFieType
            } else {
                jsFileType
            }
            if (fileType === FileTypes.UNKNOWN) return null

            val file = PsiFileFactory.getInstance(myProject).createFileFromText("$name.txt.ft", fileType, text, 0, true)
            val properties = Properties()
            properties.putAll(FileTemplateManager.getInstance(myProject).defaultProperties)
            file.viewProvider.putUserData(FileTemplateManager.DEFAULT_TEMPLATE_PROPERTIES, properties)
            return file
        }

        fun disposeUIResources() {
            myScriptEditor?.let {
                EditorFactory.getInstance().releaseEditor(myScriptEditor!!)
            }
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
