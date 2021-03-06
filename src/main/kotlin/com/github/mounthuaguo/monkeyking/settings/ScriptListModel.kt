package com.github.mounthuaguo.monkeyking.settings

import javax.swing.AbstractListModel
import javax.swing.JCheckBox

class ScriptListModel(scripts: List<ScriptModel>) : AbstractListModel<JCheckBox>() {
  private val myScriptModels = scripts.toMutableList()

  override fun getSize(): Int {
    return myScriptModels.size
  }

  override fun getElementAt(index: Int): JCheckBox {
    val box = JCheckBox()
    box.putClientProperty("model", myScriptModels[index])
    return box
  }

  fun getModel(index: Int): ScriptModel? {
    if (index < 0) {
      return null
    }
    return myScriptModels[index]
  }

  fun add(script: ScriptModel) {
    myScriptModels.add(script)
    fireContentsChanged(this, 0, myScriptModels.size - 1)
  }

  fun remove(index: Int) {
    myScriptModels.removeAt(index)
    fireIntervalRemoved(this, index, index)
  }

  fun update(index: Int, script: ScriptModel) {
    myScriptModels[index] = script
    fireContentsChanged(this, index, index)
  }

  fun toggleCheckBox(index: Int) {
    myScriptModels[index].enabled = !myScriptModels[index].enabled
    fireContentsChanged(this, index, index)
  }

  fun scripts(): List<ScriptModel> {
    return myScriptModels.toList()
  }

  fun up(index: Int): Boolean {
    if (index <= 0) {
      return false
    }
    val tmp = myScriptModels[index]
    myScriptModels[index] = myScriptModels[index - 1]
    myScriptModels[index - 1] = tmp
    fireContentsChanged(this, index - 1, index)
    return true
  }

  fun down(index: Int): Boolean {
    if (index < 0) {
      return false
    }
    if (index >= myScriptModels.size - 1) {
      return false
    }
    val tmp = myScriptModels[index]
    myScriptModels[index] = myScriptModels[index + 1]
    myScriptModels[index + 1] = tmp
    fireContentsChanged(this, index + 1, index)
    return true
  }
}
