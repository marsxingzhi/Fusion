package com.mars.infra.fusion.gradle.plugin.model

import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import kotlin.reflect.KClass


/**
 * Created by Mars on 2022/5/9
 */
// @Fusion注解内容
data class FusionData(
    val target: String,
    val interfaces: List<String>
)

// @Fusion类内容
data class FusionNode(
    val fusionData: FusionData,
    val originClass: String,
    val remapClassName: String,
    val originField: Map<String, FieldNode>,
    val originMethod: Map<String, MethodNode>,
    val remapField: Map<String, FieldNode>,
    val remapMethod: Map<String, MethodNode>
)