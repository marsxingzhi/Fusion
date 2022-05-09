package com.mars.infra.fusion.gradle.plugin

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created by Mars on 2022/5/9
 */
class FusionPlugin : Plugin<Project>{

    override fun apply(project: Project) {
        println("start FusionPlugin")
        val appExtension = project.extensions.getByType(AppExtension::class.java)
        appExtension.registerTransform(FusionTransform())
    }
}