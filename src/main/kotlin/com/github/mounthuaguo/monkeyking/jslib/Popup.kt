package com.github.mounthuaguo.monkeyking.jslib

import com.github.mounthuaguo.monkeyking.ui.MonkeySearchUI
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.BrowserHyperlinkListener
import com.intellij.ui.JBColor
import com.intellij.ui.ScrollPaneFactory
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.Color
import java.awt.Dimension
import javax.swing.JEditorPane

class Popup(
    private val event: AnActionEvent,
) {

    fun builder(): PopupBuilder {
        return PopupBuilder(event)
    }

    class PopupBuilder(
        private val event: AnActionEvent,
    ) {
        private var backgroundColor = Color(240, 240, 240)
        private var foregroundColor = Color(60, 60, 60)

        private var width = 0
        private var height = 0

        private var minWidth = 100
        private var minHeight = 100

        fun showBalloon(content: String) {
            val editor: Editor = event.getData(PlatformDataKeys.EDITOR) as Editor
            val factory = JBPopupFactory.getInstance()
            val balloon = factory.createHtmlTextBalloonBuilder(
                content,
                null,
                JBColor(this.backgroundColor, this.foregroundColor),
                null
            )
                .setFadeoutTime(5000)
                .createBalloon()
            balloon.show(factory.guessBestPopupLocation(editor), Balloon.Position.below)
        }

        fun backgroundColor(r: Int, g: Int, b: Int): PopupBuilder {
            this.backgroundColor = Color(r, g, b)
            return this
        }

        fun foregroundColor(r: Int, g: Int, b: Int): PopupBuilder {
            this.foregroundColor = Color(r, g, b)
            return this
        }

        fun size(width: Int, height: Int): PopupBuilder {
            this.width = width
            this.height = height
            return this
        }

        fun minSize(width: Int, height: Int): PopupBuilder {
            this.minWidth = width
            this.minHeight = height
            return this
        }

        fun show(content: String) {
            val factory = JBPopupFactory.getInstance()
            val component = BorderLayoutPanel()
            val descriptionPanel = JEditorPane()
            val scroll = ScrollPaneFactory.createScrollPane(descriptionPanel)
            descriptionPanel.editorKit = UIUtil.getHTMLEditorKit()
            descriptionPanel.text = content
            descriptionPanel.isEditable = false
            descriptionPanel.addHyperlinkListener(BrowserHyperlinkListener())
            component.addToCenter(scroll)
            val builder = factory.createComponentPopupBuilder(component, component)
            builder.setResizable(true)
            builder.setMinSize(Dimension(this.minWidth, this.minHeight))
            val popup = builder.createPopup()
            if (this.width > 0 && this.height > 0) {
                popup.size = Dimension(width, height)
            }
            popup.setRequestFocus(true)
            popup.showInBestPositionFor(event.dataContext)
        }

        fun showList(list: List<String>) {
            // not implement
        }

        fun showActions(list: List<String>) {
            // not implement
        }

        private fun dealWithSearchAction(searchKey: String, searchFunc: (_: String) -> Any): Any {
            return try {
                val ret = mutableListOf<Map<String, String>>()
                val objs = searchFunc(searchKey) as Collection<*>
                for (obj in objs) {
                    @Suppress("UNCHECKED_CAST")
                    ret.add(obj as Map<String, String>)
                }
                ret
            } catch (e: Exception) {
                ""
            }
        }

        // search everywhere popup
        fun showSearchEverywhere(
            searchFunc: (_: String) -> Any,
            moreFunc: (_: String) -> Any,
            hasSelectedIndex: (_: Int) -> Unit
        ) {
            event.project ?: return;
            val project = event.project!!
            val ui = MonkeySearchUI(project, {
                return@MonkeySearchUI dealWithSearchAction(it, searchFunc)
            }, {
                return@MonkeySearchUI dealWithSearchAction(it, moreFunc)
            }, {
                hasSelectedIndex(it)
            })

            val myBalloon = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(ui, ui.searchField)
                .setProject(project)
                .setModalContext(false)
                .setCancelOnClickOutside(true)
                .setRequestFocus(true)
                .setCancelKeyEnabled(false)
                .setCancelCallback {
                    true
                }
                .addUserData("SIMPLE_WINDOW")
                .setResizable(true)
                .setMovable(true)
                .setDimensionServiceKey(project, "search.monkeyking.everywhere.popup", true)
                .setLocateWithinScreenBounds(false)
                .createPopup()

            ui.setSearchFinishedHandler {
                myBalloon.cancel()
            }
            val size: Dimension = ui.minimumSize
            JBInsets.addTo(size, myBalloon.content.insets)
            myBalloon.setMinimumSize(size)
            myBalloon.showInFocusCenter()
        }
    }
}
