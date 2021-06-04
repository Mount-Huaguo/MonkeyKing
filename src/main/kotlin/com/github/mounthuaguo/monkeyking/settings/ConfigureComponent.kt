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
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.LoadingDecorator
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.ui.*
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.util.Alarm
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import org.luaj.vm2.lib.jse.JsePlatform
import java.awt.*
import java.net.URL
import java.util.*
import javax.swing.*


const val fixedCellHeight = 32

data class SampleScriptModel(val name: String, val language: String, val intro: String, val source: String) {
    fun invalid(): Boolean {
        return name == "" || language == "" || source == ""
    }
}

class ScriptConfigureComponent(myProject: Project) : BorderLayoutPanel() {

    private var scriptListView: CheckBoxList<ScriptModel> = ScriptCheckBoxList()
    private val state = ConfigureStateService.getInstance()
    private val editorPanel = ScriptEditorPanel(myProject) {
        (scriptListView.model as ScriptListModel).update(index = scriptListView.selectedIndex, it)
    }
    private val mySpliterator = JBSplitter(false, 0.35f, 0.3f, 0.5f)
    private var toolbarDecorator: ToolbarDecorator? = null

    init {
        // table
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
        val scriptListModel = ScriptListModel(state.cloneScripts().toMutableList())
        scriptListView.model = scriptListModel
        scriptListView.fixedCellHeight = fixedCellHeight
        scriptListView.selectionMode = ListSelectionModel.SINGLE_SELECTION
        scriptListView.setCheckBoxListListener { _, _ ->
            (scriptListView.model as ScriptListModel).toggleCheckBox(scriptListView.selectedIndex)
        }
        scriptListView.addListSelectionListener {
            if (it.valueIsAdjusting) {
                return@addListSelectionListener
            }
            val model = scriptListModel.getModel(scriptListView.selectedIndex)
            model?.let {
                editorPanel.setScriptModel(model)
            }
        }

    }

    private fun setupEditorView() {
        mySpliterator.secondComponent = editorPanel
    }


    private fun anActionGroup(): DefaultActionGroup {

        val newLuaAction: AnAction = object : DumbAwareAction("Add Lua Script") {
            override fun actionPerformed(e: AnActionEvent) {
                val script = ScriptModel("lua", luaScriptModelTemplate)
                (scriptListView.model as ScriptListModel).add(script)
            }
        }

        val newJsAction: AnAction = object : DumbAwareAction("Add Javascript Script") {
            override fun actionPerformed(e: AnActionEvent) {
                val script = ScriptModel("js", jsScriptModelTemplate)
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
                )
                popup.show(RelativePoint.getSouthWestOf(toolbarDecorator!!.actionsPanel))
            }
        }

        val copyAction: AnAction = object : DumbAwareAction(AllIcons.Actions.Copy) {
            override fun actionPerformed(e: AnActionEvent) {
                val index = scriptListView.selectedIndex
                if (index < 0) return
                val listModel = scriptListView.model as ScriptListModel
                val model = listModel.getModel(index)
                val scripts = listModel.scripts()
                val m = mutableMapOf<String, Unit>()
                for (s in scripts) {
                    m[s.name] = Unit
                }
                model?.let {
                    val raw = model.copyRaw(m.toMap())
                    val script = ScriptModel(model.language, raw)
                    listModel.add(script)
                }
            }
        }

        val removeAction: AnAction = object : DumbAwareAction(AllIcons.General.Remove) {
            override fun actionPerformed(e: AnActionEvent) {
                if (scriptListView.selectedIndex < 0) {
                    return
                }
                val model = scriptListView.model as ScriptListModel
                val currentIndex = scriptListView.selectedIndex
                model.remove(currentIndex)
                scriptListView.selectedIndex = if (model.size > 1 && currentIndex - 1 < 0) {
                    0
                } else {
                    currentIndex - 1
                }
            }
        }

        val actionGroup = DefaultActionGroup(addAction, copyAction, removeAction)
        actionGroup.isPopup = true

        return actionGroup
    }

    fun saveScripts() {
        val scripts = (scriptListView.model as ScriptListModel).scripts()
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
        println("saveScripts $scripts")
    }

    fun addScript(model: SampleScriptModel, source: String) {
        val sm = ScriptModel(language = model.language, raw = source)
        (scriptListView.model as ScriptListModel).add(sm)
    }

    fun isModified(): Boolean {
        val scripts = (scriptListView.model as ScriptListModel).scripts()
        if (state.getScripts().size != scripts.size) {
            return true
        }
        val m = mutableMapOf<String, Boolean>()
        val ss = state.getScripts()
        for (s in ss) {
            m[s.raw] = s.enabled
        }
        for (s in scripts) {
            if (!m.containsKey(s.raw)) {
                return true
            }
            if (m[s.raw]!! != s.enabled) {
                return true
            }
        }

        return false
    }

    private inner class ScriptEditorPanel(
        val myProject: Project,
        val scriptHasChanged: (ScriptModel) -> Unit
    ) : JPanel(GridBagLayout()) {

        private var myScriptEditorPanel = BorderLayoutPanel()
        private var myScriptEditor: Editor? = null
        private var myErrorPanel: JEditorPane? = null
        private var myScriptModel: ScriptModel? = null
        private val mySpliterator = JBSplitter(true, 1.0f, 0.5f, 1.0f)

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
                EditorFactory.getInstance().releaseEditor(myScriptEditor!!)
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
            val fileExtension: String = FileUtilRt.getExtension("script.${myScriptModel?.language}")

            val editorFactory = EditorFactory.getInstance()
            val doc: Document = createDocument()
            val editor = editorFactory.createEditor(
                doc,
                myProject,
                FileTypeManager.getInstance().getFileTypeByExtension(fileExtension),
                false,
            ) as EditorEx
            editor.colorsScheme = scheme
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
            text?.let {
                val scriptModel = ScriptModel(myScriptModel?.language ?: "lua", it)
                resetErrorMessage(scriptModel)
                scriptHasChanged(scriptModel)
                return
            }
            myErrorPanel?.text = "Script can't be empty"
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
            return EditorFactory.getInstance().createDocument(myScriptModel?.raw ?: "")
        }

    }
}

class ConfigureBrowserComponent(
    private val myProject: Project,
    private val useButtonOnClick: (modelSample: SampleScriptModel, source: String) -> Unit
) : BorderLayoutPanel() {

    private val splitter = JBSplitter(false, 0.35f, 0.3f, 0.5f)

    private val leftPanel = BorderLayoutPanel()
    private val searchBar = PluginSearchTextField()
    private val listView = JBList<SampleScriptModel>()

    private var scriptList = listOf<SampleScriptModel>()
    private var scriptRepo = listOf<SampleScriptModel>()

    private var scriptLoadingDecorator: LoadingDecorator? = null
    private var isLoad = false
    private val urlCache = URLCache()
    private var rightPanel = ConfigureBrowserSecondPanel(myProject, urlCache) { model, source ->
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

    private val alarm = Alarm()

    init {
        splitter.firstComponent = leftPanel
        splitter.secondComponent = rightPanel.component()

        searchBar.textEditor.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: javax.swing.event.DocumentEvent) {
                val text = searchBar.text
                alarm.cancelAllRequests()
                alarm.addRequest({
                    handleSearchTextChanged(text)
                }, 300)
            }
        })
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
        val disposable = Disposer.newDisposable()
        Disposer.register(myProject, disposable)
        scriptLoadingDecorator = LoadingDecorator(listView, disposable, 0)
        leftPanel.add(scriptLoadingDecorator!!.component, BorderLayout.CENTER)
        add(splitter, BorderLayout.CENTER)
    }

    private fun setupListView() {
        listView.model = listViewModel
        listView.selectionMode = ListSelectionModel.SINGLE_SELECTION
        listView.fixedCellHeight = fixedCellHeight
        listView.setCellRenderer { _, value, _, isSelected, _ ->
            val label = JLabel(value.name)
            label.isOpaque = true
            label.background = Color.white
            if (isSelected) {
                label.background = Color(38, 117, 191)
            } else {
                label.background = null
                label.isOpaque = false
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
        rightPanel.resetWithModel(sampleScriptModel)
    }

    private fun loadScripts() {
        if (scriptRepo.isNotEmpty()) {
            return
        }
        queryScripts()
    }

    private fun handleSearchTextChanged(text: String) {
        val list = mutableListOf<SampleScriptModel>()
        for (s in scriptRepo) {
            if (s.name.contains(text, true)) {
                list.add(s)
            }
        }
        scriptList = list
        listViewModel.refresh()
    }

    private fun queryScripts() {
        scriptLoadingDecorator!!.startLoading(false)
        urlCache.load {
            scriptRepo = it
            scriptList = it
            listViewModel.refresh()
            scriptLoadingDecorator!!.stopLoading()
        }
    }

    inner class ConfigureBrowserSecondPanel(
        myProject: Project,
        private val urlCache: URLCache,
        private val useButtonOnClick: (modelSample: SampleScriptModel, source: String) -> Unit
    ) {

        private val mainPanel = BorderLayoutPanel()
        private val spliterator = JBSplitter(true, 0.7f, 0.4f, 0.9f)
        private var editorPanel: ConfigureBrowserEditorPanel? = null
        private var descriptionPanel = JEditorPane()
        private var loadingDecorator: LoadingDecorator
        private var mySampleScriptModel: SampleScriptModel? = null
        private var viewWasLoaded = false

        init {
            mainPanel.border = BorderFactory.createEmptyBorder(1, 1, 1, 1)
            val disposable = Disposer.newDisposable()
            Disposer.register(myProject, disposable)
            loadingDecorator = LoadingDecorator(mainPanel, disposable, 0)
        }

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
                editorPanel?.let {
                    val source = editorPanel!!.getSource()
                    if (source != "") {
                        useButtonOnClick(mySampleScriptModel!!, source)
                    }
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
            editorPanel = ConfigureBrowserEditorPanel()
            spliterator.firstComponent = editorPanel!!.getComponent()
            spliterator.secondComponent = panel
            mainPanel.add(spliterator, BorderLayout.CENTER)

            viewWasLoaded = true
        }

        private fun startLoading() {
            loadingDecorator.startLoading(false)
        }

        private fun stopLoading() {
            loadingDecorator.stopLoading()
        }

        fun resetWithModel(model: SampleScriptModel) {
            setupUI()
            startLoading()
            mySampleScriptModel = model
            urlCache.loadIntroAndSource(model.intro, model.source) { intro, source ->
                intro?.let {
                    descriptionPanel.text = intro.response
                }
                source?.let {
                    editorPanel!!.showSource(model.language, source.response)
                }
                stopLoading()
            }
        }

        fun component(): JComponent {
            return loadingDecorator.component
        }

    }

    class URLCache {

        data class Response(val url: String, val response: String, val timestamp: Long)

        private val repository = mutableListOf<SampleScriptModel>()
        private var timestamp = Date().time

        private val cache = mutableMapOf<String, Response>()

        private fun getScripts(): List<SampleScriptModel> = synchronized(repository) {
            if (Date().time - timestamp > 5 * 60 * 1000) {
                repository.clear()
            }
            return repository.toList()
        }

        fun load(callBack: (List<SampleScriptModel>) -> Unit) {
            val scripts = this.getScripts()
            if (scripts.isNotEmpty()) {
                return callBack(scripts)
            }

            val app = ApplicationManager.getApplication()
            app.executeOnPooledThread() {
                try {
                    val url = MonkeyBundle.message("repositoryBaseUrl") + MonkeyBundle.message("repositoryPath")
                    val response = URL(url).readText()
                    this.processSampleScriptRaw(response)
                    println("response: $response")
                    app.invokeLater(
                        {
                            callBack(this.getScripts())
                        },
                        ModalityState.any()
                    )
                } catch (e: Exception) {
                    callBack(this.getScripts())
                }
            }
        }

        private fun processSampleScriptRaw(source: String) {
            synchronized(repository) {
                println("source $source")
                val luaEnv = JsePlatform.standardGlobals()
                val chuck = luaEnv.load(source)
                val table = chuck.call().checktable()
                table ?: return
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
                    repository.add(sm)
                }
                timestamp = Date().time
            }
        }

        private fun getSource(uri: String): Response? {
            synchronized(cache) {
                if (cache.containsKey(uri)) {
                    val source = cache[uri]!!
                    if (Date().time - source.timestamp > 5 * 60 * 1000) {
                        return null
                    }
                    return source
                }
                return null
            }
        }


        private fun url(pth: String): URL {
            return URL(MonkeyBundle.message("repositoryBaseUrl") + "/" + pth)
        }

        fun loadIntroAndSource(introUrl: String, sourceUrl: String, callback: (Response?, Response?) -> Unit) {
            var intro: Response? = null
            var source: Response? = getSource(sourceUrl)
            if (introUrl != "") {
                intro = getSource(introUrl)
            }
            if (intro != null && source != null) {
                callback(intro, source)
                return
            }
            if (introUrl == "" && source != null) {
                callback(null, source)
                return
            }
            val app = ApplicationManager.getApplication()
            app.executeOnPooledThread {
                try {
                    if (introUrl != "" && intro == null) {
                        intro = Response(introUrl, url(introUrl).readText(), Date().time)
                        synchronized(cache) { cache[introUrl] = intro!! }
                    }
                    if (source == null) {
                        source = Response(sourceUrl, url(sourceUrl).readText(), Date().time)
                        synchronized(cache) { cache[sourceUrl] = source!! }
                    }
                    app.invokeLater(
                        {
                            callback(intro, source)
                        },
                        ModalityState.any()
                    )

                } catch (e: Exception) {
                    e.printStackTrace()
                    callback(null, null)
                }
            }
        }

    }

    inner class ConfigureBrowserEditorPanel() {

        private val jsPanel = BorderLayoutPanel()
        private val luaPanel = BorderLayoutPanel()
        private var luaDocument: Document? = null
        private var jsDocument: Document? = null
        private val mainPanel = JPanel(CardLayout())
        private var language: String = ""

        init {
            setupUI()
        }

        fun getComponent(): JComponent {
            return mainPanel
        }

        private fun setupUI() {
            val jsEditor = createJsEditor()
            jsPanel.addToCenter(jsEditor.component)
            val luaEditor = createLuaEditor()
            luaPanel.addToCenter(luaEditor.component)
            mainPanel.add("js", jsPanel)
            mainPanel.add("lua", luaPanel)
        }

        fun getSource(): String {
            when (language) {
                "lua" -> {
                    luaDocument?.let {
                        return luaDocument!!.text
                    }
                }
                "js" -> {
                    jsDocument?.let {
                        return jsDocument!!.text
                    }
                }
            }
            return ""
        }

        fun showSource(language: String, source: String) {
            ApplicationManager.getApplication().invokeLater({
                runWriteAction {
                    when (language) {
                        "js" -> {
                            jsDocument?.let {
                                jsDocument!!.setReadOnly(false)
                                jsDocument!!.setText(source)
                                jsDocument!!.setReadOnly(true)
                            }
                            this.language = "js"
                            (mainPanel.layout as CardLayout).first(mainPanel)
                        }
                        "lua" -> {
                            luaDocument?.let {
                                luaDocument!!.setReadOnly(false)
                                luaDocument!!.setText(source)
                                luaDocument!!.setReadOnly(true)
                            }
                            this.language = "lua"
                            (mainPanel.layout as CardLayout).last(mainPanel)
                        }
                        else -> {
                            // do nothing
                        }
                    }
                }
            }, ModalityState.any())
        }

        private fun createJsEditor(): Editor {
            val editorFactory = EditorFactory.getInstance()
            val fileExtension: String = FileUtilRt.getExtension("script.js")
            jsDocument = editorFactory.createDocument("")
            jsDocument!!.setReadOnly(true)
            val editor = editorFactory.createEditor(
                jsDocument!!,
                myProject,
                FileTypeManager.getInstance().getFileTypeByExtension(fileExtension),
                false,
            )
            editorSettings(editor)
            return editor
        }

        private fun createLuaEditor(): Editor {
            val editorFactory = EditorFactory.getInstance()
            val fileExtension: String = FileUtilRt.getExtension("script.lua")
            luaDocument = editorFactory.createDocument("")
            luaDocument!!.setReadOnly(true)
            val editor = editorFactory.createEditor(
                luaDocument!!,
                myProject,
                FileTypeManager.getInstance().getFileTypeByExtension(fileExtension),
                false,
            )
            editorSettings(editor)
            return editor
        }

        private fun editorSettings(editor: Editor) {
            val editorSettings = editor.settings
            editorSettings.isVirtualSpace = false
            editorSettings.isLineMarkerAreaShown = false
            editorSettings.isIndentGuidesShown = false
            editorSettings.isLineNumbersShown = false
            editorSettings.isFoldingOutlineShown = false
            editorSettings.additionalColumnsCount = 3
            editorSettings.additionalLinesCount = 3
            editorSettings.isCaretRowShown = false
        }

    }

}
