package com.github.mounthuaguo.monkeyking.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.actions.BigPopupUI
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereActions
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.ActionMenu
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.ui.*
import com.intellij.ui.components.JBList
import com.intellij.ui.components.fields.ExtendableTextComponent
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.Alarm
import com.intellij.util.Consumer
import com.intellij.util.ui.EmptyIcon
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import java.util.function.Supplier
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.ListSelectionEvent

class MonkeySearchUI(
    project: Project,
    private val searchFun: (String, Boolean) -> Any,
    private val hasSelectedIndex: (Int) -> Unit,
) : BigPopupUI(project) {

    private val rebuildListAlarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)

    private val myListModel: MonkeySearchListModel = MonkeySearchListModel()

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

            val model = list.model as MonkeySearchListModel

            @Suppress("UNCHECKED_CAST")
            val data = model.getElementAt(index) as Map<String, String>

            data["name"]?.let {
                append(
                    it,
                    SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, list.foreground)
                )
            }
            data["desc"]?.let {
                append("    ")
                append(
                    it,
                    SimpleTextAttributes(
                        SimpleTextAttributes.STYLE_PLAIN,
                        JBColor.GRAY
                    )
                )
            }
            background = UIUtil.getListBackground(selected, hasFocus)
        }
    }

    init {
        init()
        initSearchActions()
        rebuildList(false)
    }

    override fun dispose() {
        this.stopSearch()
    }

    override fun createList(): JBList<Any> {
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

    private fun stopSearch() {
        rebuildListAlarm.cancelAllRequests()
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
        res.putClientProperty("mk-search-everywhere-textfield", true)
        res.layout = BorderLayout()
        return res
    }

    private fun scheduleRebuildList() {
        rebuildListAlarm.cancelAllRequests()
        rebuildListAlarm.addRequest(
            {
                rebuildList(false)
            },
            300
        )
    }

    private fun rebuildList(loadMore: Boolean) {
        ApplicationManager.getApplication().assertIsDispatchThread()
        myResultsList.setEmptyText("Searching...")
        val text = mySearchField.text

        val app = ApplicationManager.getApplication()
        app.executeOnPooledThread {
            val data = searchFun(text, loadMore)
            app.invokeLater(
                {
                    val newText = mySearchField.text
                    if (text != newText) {
                        return@invokeLater
                    }
                    if (data is String) {
                        myListModel.clear()
                        myResultsList.setEmptyText(data)
                        return@invokeLater
                    }
                    try {
                        @Suppress("UNCHECKED_CAST")
                        val results = data as List<Map<String, String>>
                        myListModel.resetItems(results)
                    } catch (e: Exception) {
                        myListModel.clear()
                        myResultsList.setEmptyText(e.toString())
                    }
                },
                ModalityState.any()
            )
        }
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
        val selectedIndex = myResultsList.selectedIndex
        if (selectedIndex > -1) {
            val item = myListModel.item(selectedIndex)
            if (item.containsKey("hasMore")) {
                rebuildList(true)
                return
            }
            hasSelectedIndex(selectedIndex)
            closePopup()
        }
    }

    private fun closePopup() {
        ActionMenu.showDescriptionInStatusBar(true, myResultsList, null)
        searchFinishedHandler.run()
    }

    private fun registerAction(actionID: String, action: Consumer<in AnActionEvent?>) {
        registerAction(actionID, Supplier<AnAction> { DumbAwareAction.create(action) })
    }

    private fun registerAction(actionID: String, actionSupplier: Supplier<out AnAction>) {
        Optional.ofNullable(ActionManager.getInstance().getAction(actionID))
            .map { a: AnAction -> a.shortcutSet }
            .ifPresent { shortcuts: ShortcutSet? ->
                actionSupplier.get().registerCustomShortcutSet(
                    shortcuts!!, this, this
                )
            }
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

        val selectedItemAction = Consumer { e: AnActionEvent? ->
            val index = myResultsList.selectedIndex
            if (index >= 0) {
                val item = myListModel.item(index)
                if (item.containsKey("hasMore")) {
                    rebuildList(true)
                    return@Consumer
                }
                hasSelectedIndex(index)
                closePopup()
            }
        }
        registerAction(SearchEverywhereActions.SELECT_ITEM, selectedItemAction)

        val escape = ActionManager.getInstance().getAction("EditorEscape")
        DumbAwareAction.create { closePopup() }
            .registerCustomShortcutSet(escape?.shortcutSet ?: CommonShortcuts.ESCAPE, this)

        mySearchField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                scheduleRebuildList()
            }
        })
        myResultsList.addListSelectionListener { e: ListSelectionEvent? ->
            e?.let {
                val item = myListModel.item(e.firstIndex)
                return@addListSelectionListener
            }
        }
        mySearchField.addFocusListener(object : FocusAdapter() {
            override fun focusLost(e: FocusEvent) {
                // todo I don't know what to do
            }
        })
    }

    private class MonkeySearchListModel : AbstractListModel<Any>() {

        private val elements = mutableListOf<Map<String, String>>()

        override fun getSize(): Int {
            return elements.size
        }

        override fun getElementAt(index: Int): Any {
            return elements[index]
        }

        fun resetItems(items: List<Map<String, String>>) {
            clear()
            addItems(items)
        }

        fun addItems(items: List<Map<String, String>>) {
            val start = elements.size
            elements.addAll(items)
            val end = elements.size
            fireIntervalAdded(this, start, end)
        }

        fun item(index: Int): Map<String, String> {
            if (index < 0 || index >= elements.size) {
                return mapOf()
            }
            return elements[index]
        }

        fun clear() {
            val index = elements.size - 1
            elements.clear()
            fireContentsChanged(this, 0, index)
        }
    }
}
