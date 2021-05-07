package com.github.mounthuaguo.monkeyking.ui

import com.github.mounthuaguo.monkeyking.MKBundle
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.EditorTextField
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTextField
import org.luaj.vm2.lib.jse.JsePlatform
import java.awt.BorderLayout
import java.awt.GridLayout
import java.net.URL
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.ListSelectionModel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener


class BrowserDialog() : DialogWrapper(false) {

    init {
        init()

        title = "Browser Scripts"
    }

    override fun createCenterPanel(): JComponent? {
        return BrowserDialogJComponent().getPanel()
    }


}

class BrowserDialogJComponent() {

    private data class Data(
        val name: String,
        val description: String,
        val author: String,
        val language: String,
        val path: String,
        val capture: List<String>,
    ) {}

    // datasource
    private var repo = listOf<Data>()
    private var scripts = listOf<Data>()
    private val scriptList = JBList<Data>()

    private var filter = ""

    // ui
    private val panel = JBPanel<JBPanel<*>>(GridLayout(1, 2, 10, 10))
    private val displayPanel = DisplayPanel()


    init {

        // left
        val leftPanel = JBPanel<JBPanel<*>>(BorderLayout())
        val searchBar = JBTextField()
        searchBar.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                filter = searchBar.text
                refreshList()
            }

            override fun removeUpdate(e: DocumentEvent?) {
                filter = searchBar.text
                refreshList()
            }

            override fun changedUpdate(e: DocumentEvent?) {
                filter = searchBar.text
                refreshList()
            }
        })

        leftPanel.add(searchBar, BorderLayout.NORTH)
        scriptList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        scriptList.setCellRenderer { list, value, index, selected, _ ->
            JBLabel(value.name)
        }
        scriptList.addListSelectionListener { event ->
            if (!event.valueIsAdjusting) {
                displayPanel.setData(scripts[event.firstIndex])
            }
        }
        leftPanel.add(scriptList, BorderLayout.CENTER)

        panel.add(leftPanel)
        panel.add(displayPanel)

        queryScripts()
    }

    fun getPanel(): JComponent {
        return panel
    }


    // todo async
    // query script
    private fun queryScripts() {
        val txt = URL(MKBundle.message("scriptsRepository") + MKBundle.message("scriptsRepoFile")).readText()
        repo = ScriptRepoParser(txt).getScripts()
        refreshList()

    }

    private fun refreshList() {
        if (filter == "") {
            scripts = repo
            scriptList.setListData(scripts.toTypedArray())
            return
        }
        val prefix = mutableListOf<Data>()
        val match = mutableListOf<Data>()
        val ds = mutableListOf<Data>()
        for (item in repo) {
            if (item.name.startsWith(filter, true)) {
                prefix.add(item)
                continue
            }
            if (item.name.contains(filter, true)) {
                match.add(item)
                continue
            }
            if (item.description.contains(filter, true)) {
                ds.add(item)
                continue
            }
        }
        prefix.addAll(match)
        prefix.addAll(ds)
        scripts = prefix

        scriptList.setListData(scripts.toTypedArray())
    }


    private class ScriptRepoParser(val txt: String) {

        private val scripts = mutableListOf<Data>()

        init {
            try {
                val jse = JsePlatform.standardGlobals()
                val chuck = jse.load(txt)
                val table = chuck.call().checktable()!!

                for (key in table.keys()) {
                    val obj = table[key]
                    scripts.add(
                        Data(
                            name = obj["name"].checkstring().toString(),
                            description = obj["description"].checkstring().toString(),
                            author = obj["author"].checkstring().toString(),
                            language = obj["language"].checkstring().toString(),
                            path = obj["path"].checkstring().toString(),
                            capture = obj["capture"].checkstring().toString().split(";")
                        )
                    )
                }
            } catch (e: Exception) {
                // todo
                println("error parse script data: $e")
            }
        }

        fun getScripts(): List<Data> {
            return scripts
        }
    }


    private class DisplayPanel() : JBPanel<JBPanel<*>>(GridLayout()) {

        private var d = Data("", "", "", "", "", listOf())

        private val nameLabel = JBLabel()
        private val descLabel = JBLabel()
        private val useButton = JButton("use")

        // todo display images
        // private val imageList = JBList()

        private val editor: EditorTextField = EditorTextField()

        init {
            editor.setOneLineMode(false)
            editor.document.setReadOnly(true)
        }

        fun querySource() {
            val txt = URL(MKBundle.message("scriptsRepository") + d.path).readText()
            editor.document.setReadOnly(false)
            editor.document.setText(txt)
            editor.document.setReadOnly(true)

            useButton.addActionListener { e ->
                // todo
            }

            add(nameLabel)
            add(descLabel)
            add(useButton)
        }

        fun setData(s: Data) {
            d = s
            refreshData()
        }

        private fun refreshData() {
            nameLabel.text = d.name
            descLabel.text = d.description
            querySource()
        }

    }


}
