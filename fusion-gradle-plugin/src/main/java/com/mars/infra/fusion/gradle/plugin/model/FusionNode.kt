package com.mars.infra.fusion.gradle.plugin.model

import com.mars.infra.fusion.gradle.plugin.GENERATE_PACKAGE_NAME
import com.mars.infra.fusion.gradle.plugin.PACKAGE_NAME
import org.objectweb.asm.commons.Remapper
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
    val originClass: String,  // internal name
    val remapClassName: String,  // 类名，非internal name
    val originField: Map<String, FieldNode>,
    val originMethod: Map<String, MethodNode>,
    val remapField: Map<String, FieldNode>,
    val remapMethod: Map<String, MethodNode>
) {
    val mRemapper by lazy {
        object : Remapper() {
            override fun mapMethodName(owner: String, name: String, descriptor: String): String {
                return if (owner == originClass) {
                    val key = "$name#$descriptor"
                    remapMethod[key]?.name ?: name
                } else {
                    name
                }
            }

            // TODO 需要考虑内部类和lambda
            override fun map(internalName: String): String {
                return when {
                    internalName == originClass -> "$GENERATE_PACKAGE_NAME/$remapClassName"
                    else -> internalName
                }
            }
        }
    }
}