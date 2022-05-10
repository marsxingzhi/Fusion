package com.mars.infra.fusion.gradle.plugin

import com.mars.infra.fusion.gradle.plugin.model.FusionNode

/**
 * Created by Mars on 2022/5/10
 */

fun FusionNode.print() {
    println(">>> start print fusionNode <<<")
    println("fusionData:     $fusionData")
    println("originClass:    $originClass")
    println("remapClassName: $remapClassName")
    originField.forEach {
        println("originField key:  ${it.key}, value: ${it.value}")
    }
    remapField.forEach {
        println("remapField key:   ${it.key}, value: ${it.value}")
    }
    originMethod.forEach { (key, methodNode) ->
        println("originMethod key: $key, value: $methodNode")
    }
    remapMethod.forEach { (key, methodNode) ->
        println("remapMethod key:  $key, value: $methodNode")
    }
    println("")
}