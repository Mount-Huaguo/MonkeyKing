package com.github.mounthuaguo.monkeyking.settings

import com.github.mounthuaguo.monkeyking.MonkeyBundle
import com.intellij.application.options.colors.ColorAndFontOptions
import com.intellij.icons.AllIcons
import com.intellij.ide.plugins.newui.PluginSearchTextField
import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.LoadingDecorator
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.*
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.luaj.vm2.lib.jse.JsePlatform
import java.awt.BorderLayout
import java.awt.Color
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.net.URL
import javax.swing.*


const val fixedCellHeight = 32;

data class SampleScriptModel(val name: String, val language: String, val intro: String, val source: String) {
    fun invalid(): Boolean {
        return name == "" || language == "" || source == ""
    }
}

class MKConfigureComponent(private val myProject: Project) : BorderLayoutPanel() {
    private val tabbedPane = JBTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT)
    private val browserPanel = MKConfigureBrowserComponent(myProject) { model, source ->
        tabbedPane.selectedIndex = 0
        installedPanel.addScript(model, source)
    }
    private val installedPanel = ScriptConfigureComponent(myProject)

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

class ScriptConfigureComponent(private val myProject: Project) : BorderLayoutPanel() {

    private var scriptListView: CheckBoxList<ScriptModel> = ScriptCheckBoxList()
    private val scripts = mutableListOf<ScriptModel>()
    private val state = ConfigureStateService.getInstance()
    private val editorPanel = ScriptEditorPanel(myProject) {
        (scriptListView.model as ScriptListModel).update(index = scriptListView.selectedIndex, it)
    }
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
        val scriptListModel = ScriptListModel(scripts)
        scriptListView.model = scriptListModel
        scriptListView.fixedCellHeight = fixedCellHeight
        scriptListView.selectionMode = ListSelectionModel.SINGLE_SELECTION
        scriptListView.setCheckBoxListListener { index, value ->
            println("setCheckBoxListListener, $index, $value")
            (scriptListView.model as ScriptListModel).toggleCheckBox(scriptListView.selectedIndex)
        }
        scriptListView.addListSelectionListener {
            println("addListSelectionListener $it")
            if (it.valueIsAdjusting) {
                return@addListSelectionListener
            }
            editorPanel.setScriptModel(
                scriptListModel.getModel(scriptListView.selectedIndex)
            )
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
                (scriptListView.model as ScriptListModel).add(script)
            }
        }

        val newJsAction: AnAction = object : DumbAwareAction("Add js Script") {
            override fun actionPerformed(e: AnActionEvent) {
                println("action1 actionPerformed ${e}")
                val script = ScriptModel("js")
                (scriptListView.model as ScriptListModel).add(script)
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
                if (scriptListView.selectedIndex < 0) {
                    return
                }
                (scriptListView.model as ScriptListModel).remove(scriptListView.selectedIndex)
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

    fun addScript(model: SampleScriptModel, source: String) {
        val sm = ScriptModel(language = model.language, raw = source)
        (scriptListView.model as ScriptListModel).add(sm)
    }

    private class ScriptEditorPanel(
        val myProject: Project,
        val scriptHasChanged: (ScriptModel) -> Unit
    ) : JPanel(GridBagLayout()) {

        private var myScriptEditorPanel = BorderLayoutPanel()
        private var myScriptEditor: Editor? = null;
        private var myErrorPanel: JEditorPane? = null;
        private var myScriptModel: ScriptModel? = null;
        private val mySpliterator = JBSplitter(true, 1.0f, 0.5f, 1.0f)
        private var job: Job? = null;

        init {
            setupUI()
        }

        fun setupUI() {
            val panel = JEditorPane()
            panel.editorKit = UIUtil.getHTMLEditorKit()
            panel.isEditable = false
            panel.addHyperlinkListener(BrowserHyperlinkListener())
            panel.background = Color(0, 0, 0, 0)
            panel.isOpaque = false
            myErrorPanel = panel

            val errorComponent = ScrollPaneFactory.createScrollPane(panel)
            errorComponent.border = BorderFactory.createEmptyBorder()
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
            this.resetErrorMessage(scriptModel = script)
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
            val scheme = EditorColorsManager.getInstance().globalScheme
            val options = ColorAndFontOptions()
            options.reset()
            options.selectScheme(scheme.name)

            val editorFactory = EditorFactory.getInstance()
            val doc: Document = createDocument()
            val editor = editorFactory.createEditor(doc, myProject) as EditorEx
            editor.colorsScheme = scheme;
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
            job?.cancel()
            job = GlobalScope.launch {
                delay(1000L)
                val text = myScriptEditor?.document?.text
                val scriptModel = text?.let { ScriptModel(myScriptModel?.language ?: "lua", it) }
                resetErrorMessage(scriptModel!!)
                scriptHasChanged(scriptModel)
            }
        }

        private fun resetErrorMessage(scriptModel: ScriptModel) {
            val errorMessage = scriptModel.validate()
            errorMessage.let {
                myErrorPanel?.text = """
                   $errorMessage 
                """.trimIndent()
            }
        }

        private fun createDocument(): Document {
            println("createDocument myScriptModel?.raw $myScriptModel ")
            return EditorFactory.getInstance().createDocument(myScriptModel?.raw ?: "")
        }

        fun disposeUIResources() {
            myScriptEditor?.let {
                EditorFactory.getInstance().releaseEditor(myScriptEditor!!)
            }
        }

    }
}

class MKConfigureBrowserComponent(
    private val myProject: Project,
    private val useButtonOnClick: (modelSample: SampleScriptModel, source: String) -> Unit
) : BorderLayoutPanel() {

    private val leftPanel = BorderLayoutPanel()
    private val searchBar = PluginSearchTextField()
    private val listView = JBList<SampleScriptModel>()
    private val splitter = JBSplitter()
    private var scriptList = listOf<SampleScriptModel>()
    private var scriptRepo = listOf<SampleScriptModel>()
    private val luaEnv = JsePlatform.standardGlobals()
    private var scriptLoadingDecorator: LoadingDecorator? = null
    private var isLoad = false
    private var rightPanel = RightPanel(myProject) { model, source ->
        useButtonOnClick(model, source)
    }

    private val listViewModel = object : AbstractListModel<SampleScriptModel>() {
        override fun getSize(): Int {
            return scriptList.size
        }

        override fun getElementAt(index: Int): SampleScriptModel {
            return scriptList[index]
        }

        fun refresh() {
            this.fireContentsChanged(this, 0, scriptList.size)
        }

    }

    init {
        splitter.firstComponent = leftPanel
        splitter.secondComponent = rightPanel.component()
    }

    fun setupUI() {
        if (isLoad) {
            loadScripts()
            return
        }
        setupFirstPanel()
        isLoad = true
        loadScripts()
    }

    private fun setupFirstPanel() {
        leftPanel.add(searchBar, BorderLayout.NORTH)
        setupListView()
        scriptLoadingDecorator = LoadingDecorator(listView, {}, 0)
        leftPanel.add(scriptLoadingDecorator!!.component, BorderLayout.CENTER)
        add(splitter, BorderLayout.CENTER)
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
            if (it.valueIsAdjusting) {
                return@addListSelectionListener
            }
            reloadSecondPanel(listView.selectedIndex, scriptList[listView.selectedIndex])
        }
    }

    private fun reloadSecondPanel(index: Int, sampleScriptModel: SampleScriptModel) {
        rightPanel.startLoading()
        ApplicationManager.getApplication().executeOnPooledThread {
            try {

                var intro = ""
                if (sampleScriptModel.intro != "") {
                    val introUrl = MonkeyBundle.message("repositoryBaseUrl") + "/" + sampleScriptModel.intro
                    intro = URL(introUrl).readText()
                }
                println("intro: $intro")
                val sourceUrl = MonkeyBundle.message("repositoryBaseUrl") + "/" + sampleScriptModel.source
                val source = URL(sourceUrl).readText()
                println("source: $source")

                ApplicationManager.getApplication().invokeLater({
                    assert(SwingUtilities.isEventDispatchThread())
                    println(" ApplicationManager.getApplication, $source, $intro")
                    rightPanel.stopLoading()
                    rightPanel.reset(source, intro, sampleScriptModel)
                }, ModalityState.any())

            } catch (e: Exception) {
                e.printStackTrace()
                rightPanel.stopLoading()
                // todo show error message
            }
        }
    }

    private fun reset() {

    }

    private fun loadScripts() {
        if (scriptRepo.isNotEmpty()) {
            return
        }
        queryScripts()
    }

    private fun queryScripts() {
        scriptLoadingDecorator!!.startLoading(false)
        GlobalScope.launch {
            val result = runCatching {
                val url = MonkeyBundle.message("repositoryBaseUrl") + MonkeyBundle.message("repositoryPath")
                val response = URL(url).readText()
                println("response: $response")
                handleScriptsResponse(response)
            }
            if (result.isFailure) {
                println(result)
            }
            scriptLoadingDecorator!!.stopLoading()
        }
    }

    private fun handleScriptsResponse(response: String) {
        val chuck = luaEnv.load(response)
        val table = chuck.call().checktable()
        table ?: return
        val list = mutableListOf<SampleScriptModel>()
        for (key in table.keys()) {
            val value = table[key]
            val sm = SampleScriptModel(
                name = if (value["name"].isnil()) "" else value["name"].toString(),
                language = if (value["language"].isnil()) "" else value["language"].toString(),
                intro = if (value["intro"].isnil()) "" else value["intro"].toString(),
                source = if (value["source"].isnil()) "" else value["source"].toString(),
            )
            if (sm.invalid()) {
                continue
            }
            list.add(sm)
        }
        println("handleScriptsResponse: $list")
        scriptRepo = list.toList()
        scriptList = list.toList()
        listViewModel.refresh()
    }

    inner class RightPanel(
        private val myProject: Project,
        private val useButtonOnClick: (modelSample: SampleScriptModel, source: String) -> Unit
    ) {

        private val mainPanel = BorderLayoutPanel()
        private val spliterator = JBSplitter(true, 0.7f, 0.4f, 0.9f)
        private var editor: Editor? = null;
        private var editorPanel = BorderLayoutPanel()
        private var descriptionPanel = JEditorPane()
        private var loadingDecorator = LoadingDecorator(mainPanel, {}, 0)
        private var mySampleScriptModel: SampleScriptModel? = null;
        private var viewWasLoaded = false;

        private fun setupUI() {
            if (viewWasLoaded) {
                return
            }

            // desc panel
            val panel = JPanel(GridBagLayout())

            // description top panel
            val topPanel = BorderLayoutPanel()
            val label = JBLabel("Description")
            topPanel.add(label, BorderLayout.CENTER)
            val button = JButton("use")
            button.addActionListener {
                mySampleScriptModel ?: return@addActionListener
                editor?.let {
                    useButtonOnClick(mySampleScriptModel!!, editor!!.document.text)
                }
            }
            topPanel.add(button, BorderLayout.EAST)
            panel.add(
                topPanel,
                GridBagConstraints(
                    0, 0, 1, 1, 1.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH, JBUI.emptyInsets(), 0, 0
                )
            )

            // description detail panel
            descriptionPanel.editorKit = UIUtil.getHTMLEditorKit()
            descriptionPanel.isEditable = false
            descriptionPanel.addHyperlinkListener(BrowserHyperlinkListener())

            panel.add(
                ScrollPaneFactory.createScrollPane(descriptionPanel),
                GridBagConstraints(
                    0, 1, 1, 1, 1.0, 1.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH, JBUI.emptyInsets(), 0, 0
                )
            )

            spliterator.firstComponent = editorPanel
            spliterator.secondComponent = panel
            mainPanel.add(spliterator, BorderLayout.CENTER)

            resetEditor("unselected")
            viewWasLoaded = true
        }

        private fun resetEditor(source: String) {
            if (editor != null) {
                EditorFactory.getInstance().releaseEditor(editor!!)
            }
            editor = createEditor(source)
            editorPanel.removeAll()
            editorPanel.add(editor!!.component, BorderLayout.CENTER)
        }

        fun startLoading() {
            loadingDecorator.startLoading(false)
        }

        fun stopLoading() {
            loadingDecorator.stopLoading()
        }

        fun reset(source: String, description: String, modelSample: SampleScriptModel) {
            setupUI()
            println("reset: $source, $description")
            resetEditor(source)
            descriptionPanel.text = description
            mySampleScriptModel = modelSample
            repaint()
        }

        private fun createEditor(text: String): Editor {
            val editorFactory = EditorFactory.getInstance()
            val doc: Document = EditorFactory.getInstance().createDocument(text)
            doc.setReadOnly(true)
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
            return editor
        }

        fun component(): JComponent {
            return loadingDecorator.component
        }

        private fun repaint() {
            mainPanel.repaint()
            editorPanel.repaint()
            descriptionPanel.repaint()
        }
    }

}
