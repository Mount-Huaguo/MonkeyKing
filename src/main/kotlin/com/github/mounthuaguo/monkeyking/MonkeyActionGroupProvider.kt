package com.github.mounthuaguo.monkeyking

import com.intellij.ide.ui.customization.CustomizableActionGroupProvider

class MonkeyActionGroupProvider : CustomizableActionGroupProvider() {

  override fun registerGroups(registrar: CustomizableActionGroupRegistrar?) {
    registrar?.addCustomizableActionGroup(
      "MonkeyKing.ActionGroup.Main",
      "Monkey King Actions - Main popup"
    )
  }
}
