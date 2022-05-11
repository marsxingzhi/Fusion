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




sealed class BooleanExt<out T>

object Otherwise : BooleanExt<Nothing>()
class Data<T>(val data: T) : BooleanExt<T>()

inline fun <T> Boolean.yes(block: () -> T) =
    when {
        this -> {
            Data(block())
        }
        else -> {
            Otherwise
        }
    }

inline fun <T> Boolean.no(block: () -> T) = when {
    this -> Otherwise
    else -> {
        Data(block())
    }
}

inline fun <T> BooleanExt<T>.otherwise(block: () -> T): T =
    when (this) {
        is Otherwise -> block()
        is Data -> this.data
    }
