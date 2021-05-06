package com.github.mounthuaguo.monkeyking.services

import com.github.mounthuaguo.monkeyking.MKBundle
import com.intellij.openapi.project.Project

class ProjectService(project: Project) {

    init {
        println(MKBundle.message("projectService", project.name))
    }

//    override fun toString(): String {
//        return "ProjectService: project ${project.name}"
//    }
}
