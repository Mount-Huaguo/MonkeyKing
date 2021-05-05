package com.github.mounthuaguo.monkeyking.services

import com.github.mounthuaguo.monkeyking.Bundle
import com.intellij.openapi.project.Project

class ProjectService(project: Project) {

    init {
        println(Bundle.message("projectService", project.name))
    }
}
