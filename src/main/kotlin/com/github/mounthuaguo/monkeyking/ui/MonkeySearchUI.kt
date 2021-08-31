package com.github.mounthuaguo.monkeyking.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.SearchTopHitProvider
import com.intellij.ide.actions.searcheverywhere.*
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.actionSystem.impl.ActionMenu
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.util.ProgressWindow
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.DumbService.DumbModeListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.*
import com.intellij.ui.components.JBList
import com.intellij.ui.components.fields.ExtendableTextComponent
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.Alarm
import com.intellij.util.ui.EmptyIcon
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.ListSelectionEvent


class MonkeySearchListModel : AbstractListModel<Any>() {
    override fun getSize(): Int {
        return 10
    }

    override fun getElementAt(index: Int): Any {
        return "hello"
    }
}

class MonkeySearchUI(project: Project) : SearchEverywhereUIBase(project) {

    private var mySearcher: ProgressIndicator? = null
    private var mySearchProgressIndicator: ProgressIndicator? = null

    private val REBUILD_LIST_DELAY: Long = 100
    private val rebuildListAlarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)

    private val myCommandRenderer: ListCellRenderer<Any> = object : ColoredListCellRenderer<Any>() {
        override fun customizeCellRenderer(
            list: JList<*>,
            value: Any,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean
        ) {
            setPaintFocusBorder(false)
            icon = EmptyIcon.ICON_16
            font = list.font
            append(
                "commandWithPrefix" + " ",
                SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, list.foreground)
            )
            append("command.definition", SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.GRAY))
            background = UIUtil.getListBackground(selected, hasFocus)
        }
    }


    init {
        init()
        initSearchActions()
    }


    override fun dispose() {

    }

    override fun createList(): JBList<Any> {
        val myListModel = MonkeySearchListModel()
        return JBList(myListModel)
    }

    override fun createCellRenderer(): ListCellRenderer<Any> {
        return myCommandRenderer
    }

    override fun createTopLeftPanel(): JPanel {
        val contributorsPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        contributorsPanel.isOpaque = false
        return contributorsPanel
    }

    override fun createSettingsPanel(): JPanel {
        return JPanel()
    }

    override fun getInitialHint(): String {
        return "Enjoy it"
    }

    override fun getAccessibleName(): String {
        return "Monkey Search"
    }

    override fun toggleEverywhereFilter() {
    }

    override fun switchToContributor(contributorID: String) {
    }

    override fun getSelectedContributorID(): String {
        return "aaa"
    }

    override fun getSelectionIdentity(): Any? {
        return null
    }

    override fun findElementsForPattern(pattern: String?): Future<MutableList<Any>> {
        val future = CompletableFuture<MutableList<Any>>()
        mySearchField.text = pattern
        return future
    }

    override fun clearResults() {
    }

    override fun createSearchField(): ExtendableTextField {
        val res: SearchField = object : SearchField() {
            override fun getLeftExtension(): ExtendableTextComponent.Extension {
                return object : ExtendableTextComponent.Extension {
                    override fun getIcon(hovered: Boolean): Icon {
                        return AllIcons.Actions.Search
                    }

                    override fun isIconBeforeText(): Boolean {
                        return true
                    }

                    override fun getIconGap(): Int {
                        return JBUIScale.scale(10)
                    }
                }
            }
        }
        res.putClientProperty(SEARCH_EVERYWHERE_SEARCH_FILED_KEY, true)
        res.layout = BorderLayout()
        return res
    }

    private fun stopSearching() {
        if (mySearchProgressIndicator != null && !mySearchProgressIndicator!!.isCanceled()) {
            mySearchProgressIndicator!!.cancel()
        }
//        if (myBufferedListener != null) {
//            myBufferedListener.clearBuffer()
//        }
    }

    private fun scheduleRebuildList() {
        if (rebuildListAlarm.activeRequestCount == 0) rebuildListAlarm.addRequest({ rebuildList() }, REBUILD_LIST_DELAY)
    }

    private fun rebuildList() {
        ApplicationManager.getApplication().assertIsDispatchThread()
        stopSearching()
        myResultsList.setEmptyText("Search")
//        val rawPattern = searchPattern
//        updateViewType(if (rawPattern.isEmpty()) ViewType.SHORT else ViewType.FULL)
//        val namePattern: String = mySelectedTab.getContributor()
//            .map<String>(Function { contributor: SearchEverywhereContributor<*> ->
//                contributor.filterControlSymbols(
//                    rawPattern
//                )
//            })
//            .orElse(rawPattern)
//        val matcher = NameUtil.buildMatcherWithFallback(
//            "*$rawPattern",
//            "*$namePattern", NameUtil.MatchingCaseSensitivity.NONE
//        )
//        MatcherHolder.associateMatcher(myResultsList, matcher)
//        val contributorsMap: MutableMap<SearchEverywhereContributor<*>, Int> = HashMap()
//        val selectedContributor: Optional<SearchEverywhereContributor<*>> = mySelectedTab.getContributor()
//        if (selectedContributor.isPresent) {
//            contributorsMap[selectedContributor.get()] = SearchEverywhereUI.SINGLE_CONTRIBUTOR_ELEMENTS_LIMIT
//        } else {
//            contributorsMap.putAll(getAllTabContributors().stream().collect(Collectors.toMap(
//                { c: SearchEverywhereContributor<*>? -> c }
//            ) { c: SearchEverywhereContributor<*>? -> SearchEverywhereUI.MULTIPLE_CONTRIBUTORS_ELEMENTS_LIMIT })
//            )
//        }
//        val contributors: List<SearchEverywhereContributor<*>>
//        if (myProject != null) {
//            contributors = DumbService.getInstance(myProject).filterByDumbAwareness(contributorsMap.keys)
//            if (contributors.isEmpty() && DumbService.isDumb(myProject)) {
//                myResultsList.setEmptyText(
//                    IdeBundle.message(
//                        "searcheverywhere.indexing.mode.not.supported",
//                        mySelectedTab.getText(),
//                        ApplicationNamesInfo.getInstance().fullProductName
//                    )
//                )
//                myListModel.clear()
//                return
//            }
//            if (contributors.size != contributorsMap.size) {
//                myResultsList.setEmptyText(
//                    IdeBundle.message(
//                        "searcheverywhere.indexing.incomplete.results",
//                        mySelectedTab.getText(),
//                        ApplicationNamesInfo.getInstance().fullProductName
//                    )
//                )
//            }
//        } else {
//            contributors = ArrayList(contributorsMap.keys)
//        }
//        myListModel.expireResults()
//        contributors.forEach(Consumer { contributor: SearchEverywhereContributor<*>? ->
//            myListModel.setHasMore(
//                contributor,
//                false
//            )
//        })
//        val commandPrefix = SearchTopHitProvider.getTopHitAccelerator()
//        if (rawPattern.startsWith(commandPrefix)) {
//            val typedCommand = rawPattern.split(" ").toTypedArray()[0].substring(commandPrefix.length)
//            val commands = SearchEverywhereUI.getCommandsForCompletion(contributors, typedCommand)
//            if (!commands.isEmpty()) {
//                if (rawPattern.contains(" ")) {
//                    contributorsMap.keys.retainAll(commands.stream()
//                        .map { obj: SearchEverywhereCommandInfo -> obj.contributor }
//                        .collect(Collectors.toSet()))
//                } else {
//                    myListModel.clear()
//                    val lst = ContainerUtil.map(
//                        commands
//                    ) { command: SearchEverywhereCommandInfo? ->
//                        SearchEverywhereFoundElementInfo(
//                            command,
//                            0,
//                            myStubCommandContributor
//                        )
//                    }
//                    myListModel.addElements(lst)
//                    ScrollingUtil.ensureSelectionExists(myResultsList)
//                }
//            }
//        }
//        mySearchProgressIndicator = mySearcher.search(contributorsMap, rawPattern)
    }

    private fun onMouseClicked(e: MouseEvent) {
        val multiSelectMode = e.isShiftDown || UIUtil.isControlKeyDown(e)
        if (e.button == MouseEvent.BUTTON1 && !multiSelectMode) {
            e.consume()
            val i = myResultsList.locationToIndex(e.point)
            if (i > -1) {
                myResultsList.selectedIndex = i
                elementsSelected(intArrayOf(i), e.modifiers)
            }
        }
    }

    private fun elementsSelected(indexes: IntArray, modifiers: Int) {
        var indexes = indexes
//        if (indexes.size == 1 && myListModel.isMoreElement(indexes[0])) {
//            val contributor: SearchEverywhereContributor<*> = myListModel.getContributorForIndex<Any>(indexes[0])
//            showMoreElements(contributor)
//            return
//        }
//        indexes = Arrays.stream(indexes)
//            .filter { i: Int -> !myListModel.isMoreElement(i) }
//            .toArray()
        val searchText = searchPattern
        if (searchText.startsWith(SearchTopHitProvider.getTopHitAccelerator()) && searchText.contains(" ")) {
//            featureTriggered(SearchEverywhereUsageTriggerCollector.COMMAND_USED, null)
        }
        var closePopup = false
        for (i in indexes) {
//            val contributor: SearchEverywhereContributor<Any> = myListModel.getContributorForIndex<Any>(i)
//            val value: Any = myListModel.getElementAt(i)
//            val selectedTabContributorID: String = mySelectedTab.getContributor()
//                .map<String>(Function { contributor: SearchEverywhereContributor<*>? ->
//                    SearchEverywhereUsageTriggerCollector.getReportableContributorID(
//                        contributor!!
//                    )
//                })
//                .orElse(SearchEverywhereManagerImpl.ALL_CONTRIBUTORS_GROUP_ID)
//            val reportableContributorID = SearchEverywhereUsageTriggerCollector.getReportableContributorID(contributor)
//            val data =
//                SearchEverywhereUsageTriggerCollector.createData(reportableContributorID, selectedTabContributorID, i)
//            if (value is PsiElement) {
//                data.addLanguage(value.language)
//            }
//            featureTriggered(SearchEverywhereUsageTriggerCollector.CONTRIBUTOR_ITEM_SELECTED, data)
//            closePopup = closePopup or contributor.processSelectedItem(value, modifiers, searchText)
        }
        if (closePopup) {
            closePopup()
        } else {
            ApplicationManager.getApplication().invokeLater { myResultsList.repaint() }
        }
    }

    private fun showMoreElements(contributor: SearchEverywhereContributor<*>) {
//        featureTriggered(SearchEverywhereUsageTriggerCollector.MORE_ITEM_SELECTED, null)
//        val found: Map<SearchEverywhereContributor<*>, Collection<SearchEverywhereFoundElementInfo>> =
//            myListModel.getFoundElementsMap()
//        val limit: Int = (myListModel.getItemsForContributor(contributor)
//                + if (mySelectedTab.getContributor()
//                .isPresent()
//        ) SearchEverywhereUI.SINGLE_CONTRIBUTOR_ELEMENTS_LIMIT else SearchEverywhereUI.MULTIPLE_CONTRIBUTORS_ELEMENTS_LIMIT)
//        mySearchProgressIndicator = mySearcher.findMoreItems(found, searchPattern, contributor, limit)
    }

    private fun closePopup() {
        ActionMenu.showDescriptionInStatusBar(true, myResultsList, null)
        stopSearching()
        searchFinishedHandler.run()
    }

    private fun initSearchActions() {

        val listMouseListener: MouseAdapter = object : MouseAdapter() {
            private var currentDescriptionIndex = -1
            override fun mouseClicked(e: MouseEvent) {
                onMouseClicked(e)
            }

            override fun mouseMoved(e: MouseEvent) {
                val index = myResultsList.locationToIndex(e.point)
                indexChanged(index)
            }

            override fun mouseExited(e: MouseEvent) {
                val index = myResultsList.selectedIndex
                indexChanged(index)
            }

            private fun indexChanged(index: Int) {
                if (index != currentDescriptionIndex) {
                    currentDescriptionIndex = index
                }
            }
        }
        myResultsList.addMouseMotionListener(listMouseListener)
        myResultsList.addMouseListener(listMouseListener)
        ScrollingUtil.redirectExpandSelection(myResultsList, mySearchField)

        val escape = ActionManager.getInstance().getAction("EditorEscape")
        DumbAwareAction.create { _: AnActionEvent? -> closePopup() }
            .registerCustomShortcutSet(escape?.shortcutSet ?: CommonShortcuts.ESCAPE, this)
        mySearchField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                scheduleRebuildList()
            }
        })
        myResultsList.addListSelectionListener { e: ListSelectionEvent? ->
            val selectedValue = myResultsList.selectedValue
            if (selectedValue != null && myHint != null && myHint.isVisible) {
//                updateHint(selectedValue)
            }
//            showDescriptionForIndex(myResultsList.selectedIndex)
        }

        val busConnection =
            myProject?.messageBus?.connect(this) ?: ApplicationManager.getApplication().messageBus.connect(this)
        busConnection.subscribe(DumbService.DUMB_MODE, object : DumbModeListener {
            override fun exitDumbMode() {
                ApplicationManager.getApplication().invokeLater {
                    updateSearchFieldAdvertisement()
                    scheduleRebuildList()
                }
            }
        })
        busConnection.subscribe(AnActionListener.TOPIC, object : AnActionListener {
            override fun afterActionPerformed(action: AnAction, dataContext: DataContext, event: AnActionEvent) {
//                if (action === mySelectedTab.everywhereAction && event.inputEvent != null) {
////                    myEverywhereAutoSet = false
//                }
            }
        })
        busConnection.subscribe(ProgressWindow.TOPIC,
            ProgressWindow.Listener { pw: ProgressWindow? ->
                Disposer.register(
                    pw!!
                ) { myResultsList.repaint() }
            })

        mySearchField.addFocusListener(object : FocusAdapter() {
            override fun focusLost(e: FocusEvent) {
//                val oppositeComponent = e.oppositeComponent
//                if (!isHintComponent(oppositeComponent) && !UIUtil.haveCommonOwner(
//                        this@SearchEverywhereUI,
//                        oppositeComponent
//                    )
//                ) {
//                    closePopup()
//                }
            }
        })
    }

    private fun showDescriptionForIndex(index: Int) {
//        if (index >= 0 && !myListModel.isMoreElement(index)) {
//            val contributor: SearchEverywhereContributor<Any> = myListModel.getContributorForIndex<Any>(index)
//            val data = contributor.getDataForItem(
//                myListModel.getElementAt(index), SearchEverywhereDataKeys.ITEM_STRING_DESCRIPTION.name
//            )
//            if (data is String) {
//                ActionMenu.showDescriptionInStatusBar(true, myResultsList, data as String?)
//            }
//        }
    }

    private fun updateSearchFieldAdvertisement() {
        if (mySearchField == null) return
//        val commandsSupported: Boolean = mySelectedTab.getContributor()
//            .map<Boolean>(Function { contributor: SearchEverywhereContributor<*> ->
//                !contributor.supportedCommands.isEmpty()
//            })
//            .orElse(true)
//        val advertisementText: String?
//        advertisementText = if (commandsSupported) {
//            IdeBundle.message("searcheverywhere.textfield.hint", SearchTopHitProvider.getTopHitAccelerator())
//        } else {
//            mySelectedTab.getContributor()
//                .map<String>(Function { c: SearchEverywhereContributor<*> -> c.advertisement }).orElse(null)
//        }
//        mySearchField.remove(myAdvertisementLabel)
//        if (advertisementText != null) {
//            myAdvertisementLabel.setText(advertisementText)
//            mySearchField.add(myAdvertisementLabel, BorderLayout.EAST)
//        }
    }

}