package com.mars.infra.fusion.gradle.plugin

/**
 * Created by Mars on 2022/5/10
 */

fun String.convertFieldName(prefix: String): String {
    return "${prefix.replace("/", "_")}_$this"
}

fun String.remapClassName(): String {
    val prefix = "Fusion_"
    return "${prefix}${replace("/", "_")}"
}