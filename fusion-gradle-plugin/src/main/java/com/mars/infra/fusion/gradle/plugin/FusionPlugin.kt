package com.mars.infra.fusion.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created by Mars on 2022/5/9
 */
class FusionPlugin : Plugin<Project>{

    override fun apply(target: Project) {
        println("start FusionPlugin")
    }
}