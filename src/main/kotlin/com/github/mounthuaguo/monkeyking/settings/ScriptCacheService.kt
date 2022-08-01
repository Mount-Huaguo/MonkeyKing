package com.github.mounthuaguo.monkeyking.settings

import com.intellij.ide.fileTemplates.impl.UrlUtil
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import java.net.URL

@State(name = "com.github.mounthuaguo.monkeyking.cache", storages = [Storage("monkeyking-cache.xml")])
class ScriptCacheService : PersistentStateComponent<ScriptCacheService> {

  var repo = mutableMapOf<String, String>()

  companion object {
    fun getInstance(): ScriptCacheService {
      return ServiceManager.getService(ScriptCacheService::class.java)
    }
  }

  override fun getState(): ScriptCacheService {
    return this
  }

  override fun loadState(state: ScriptCacheService) {
    XmlSerializerUtil.copyBean(state, this)
  }

  fun loadRepo(uri: String): String {
    synchronized(this) {
      if (repo.containsKey(uri)) {
        return repo[uri]!!
      }
      return try {
        val source = UrlUtil.loadText(URL(uri))
        repo[uri] = source
        source
      } catch (e: Exception) {
        ""
      }
    }
  }
}
