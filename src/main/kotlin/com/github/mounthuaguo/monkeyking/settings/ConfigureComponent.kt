package com.github.mounthuaguo.monkeyking.settings

import com.intellij.icons.AllIcons
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.plugins.newui.PluginSearchTextField
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
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.luaj.vm2.lib.jse.JsePlatform
import java.awt.*
import java.net.URL
import java.util.*
import javax.swing.*


const val fixedCellHeight = 30;


class MKConfigureComponent(private val myProject: Project) : BorderLayoutPanel() {
    private val tabbedPane = JBTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT)
    private val browserPanel = MKConfigureBrowserComponent(myProject)
    private val installedPanel = MKConfigureInstalledComponent(myProject)

    init {
        tabbedPane.addTab("Installed", installedPanel)
        tabbedPane.addTab("Browser", browserPanel)
        tabbedPane.addChangeListener {
            println("tabbedPane.addChangeListener: $it, ${tabbedPane.selectedIndex}, ${tabbedPane.selectedComponent}")
            if (tabbedPane.selectedIndex == 1) {
                browserPanel.setupUI()
            }
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
        setupListView()

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
    }


    private fun setupListView() {
        object : CheckBoxList<ScriptModel>() {
            override fun adjustRendering(
                rootComponent: JComponent,
                checkBox: JCheckBox,
                index: Int,
                selected: Boolean,
                hasFocus: Boolean
            ): JComponent {
                val panel = JPanel(GridBagLayout())
                panel.border = BorderFactory.createEmptyBorder()

                panel.add(
                    checkBox,
                    GridBagConstraints(
                        0, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH, JBUI.emptyInsets(), 0, 0
                    )
                )

                val icon = JLabel(AllIcons.FileTypes.JavaScript)
                icon.isOpaque = true
                icon.preferredSize = Dimension(25, -1)
                icon.horizontalAlignment = SwingConstants.CENTER
                icon.background = getBackground(selected)

                panel.add(
                    icon,
                    GridBagConstraints(
                        1, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH, JBUI.emptyInsets(), 0, 0
                    )
                )

                val label = JLabel("This coupon is available within the validity")
                label.isOpaque = true
                label.preferredSize = Dimension(100, -1)
                label.background = getBackground(selected)
                panel.add(
                    label,
                    GridBagConstraints(
                        2, 0, 1, 1, 1.0, 1.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH, JBUI.emptyInsets(), 0, 0
                    )
                )

                panel.background = getBackground(false)
                if (!checkBox.isOpaque) {
                    checkBox.isOpaque = true
                }
                checkBox.border = null
                return panel
            }
        }.also { scriptListView = it }
        val scriptListModel = ScriptListModel(scripts)
        scriptListView.model = scriptListModel
        scriptListView.fixedCellHeight = fixedCellHeight
        scriptListView.selectionMode = ListSelectionModel.SINGLE_SELECTION
        scriptListView.setCheckBoxListListener { index, value ->
            println("setCheckBoxListListener, $index, $value")
        }
        scriptListView.addListSelectionListener {
            println("addListSelectionListener $it")
            if (!it.valueIsAdjusting) {
                return@addListSelectionListener
            }
        }

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

class MKConfigureBrowserComponent(val myProject: Project) : BorderLayoutPanel() {

    data class ScriptModel(val name: String, val language: String, val intro: String, val path: String) {}

    private val leftPanel = BorderLayoutPanel()
    private val rightPanel = BorderLayoutPanel()
    private val searchBar = PluginSearchTextField()
    private val listView = JBList<ScriptModel>()
    private val splitter = JBSplitter()
    private var scriptList = listOf<ScriptModel>()
    private val luaEnv = JsePlatform.standardGlobals()
    private var loadingDecorator: LoadingDecorator? = null
    private val editor: Editor? = null
    private var isLoad = false

    private val listViewModel = object : AbstractListModel<ScriptModel>() {
        override fun getSize(): Int {
            return scriptList.size
        }

        override fun getElementAt(index: Int): ScriptModel {
            return scriptList[index]
        }

        fun refresh() {
            this.fireContentsChanged(this, 0, scriptList.size)
        }

    }


    fun setupUI() {
        if (isLoad) {
            return
        }
        leftPanel.add(searchBar, BorderLayout.NORTH)
        setupListView()
        loadingDecorator = LoadingDecorator(listView, {}, 0)
        leftPanel.add(loadingDecorator!!.component, BorderLayout.CENTER)
        splitter.firstComponent = leftPanel
        splitter.secondComponent = rightPanel

        add(splitter, BorderLayout.CENTER)
        queryScripts()

        isLoad = true
    }

    private fun setupListView() {
        listView.model = listViewModel
        listView.selectionMode = ListSelectionModel.SINGLE_SELECTION
        listView.fixedCellHeight = fixedCellHeight
        listView.setCellRenderer { list, value, index, isSelected, cellHasFocus ->
            val label = JLabel(value.name)
            label.isOpaque = true
            label.background = Color.white
            if (isSelected) {
                label.background = Color(38, 117, 191)
            }
            return@setCellRenderer label
        }
        listView.addListSelectionListener {
            println("listView.addListSelectionListener $it")
        }

    }

    fun setupRightPanel() {

    }

    fun createEditor() {

    }


    private fun queryScripts() {
        loadingDecorator!!.startLoading(false)
        GlobalScope.launch {
            runCatching {
                val response =
                    URL("https://raw.githubusercontent.com/Mount-Huaguo/MonkeyKingScripts/main/index.lua").readText()
                println("response: $response")
                handleScriptsResponse(response)
                loadingDecorator!!.stopLoading()
            }
        }
    }

    private fun handleScriptsResponse(response: String) {
        val chuck = luaEnv.load(response)
        val table = chuck.call().checktable()
        table ?: return
        val list = mutableListOf<ScriptModel>()
        for (key in table.keys()) {
            val value = table[key]
            list.add(
                ScriptModel(
                    name = value["name"].checkstring().toString(),
                    language = value["language"].checkstring().toString(),
                    intro = value["intro"].checkstring().toString(),
                    path = value["path"].checkstring().toString(),
                )
            )
        }
        println("handleScriptsResponse: $list")
        scriptList = list
        listViewModel.refresh()
    }

    private fun queryScriptDesc() {

    }

    private fun handleScriptDescResponse() {

    }

    private fun queryScriptSource() {

    }


    private fun handleScriptSourceResponse() {

    }

}
