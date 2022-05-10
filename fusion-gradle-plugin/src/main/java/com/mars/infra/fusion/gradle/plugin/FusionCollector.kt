package com.mars.infra.fusion.gradle.plugin

import com.android.build.api.transform.JarInput
import com.android.build.api.transform.TransformInvocation
import com.mars.infra.fusion.gradle.plugin.model.FusionData
import com.mars.infra.fusion.gradle.plugin.model.FusionNode
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile
import kotlin.reflect.KClass

/**
 * Created by Mars on 2022/5/9
 */
object FusionCollector {

    fun collectFusionAnnotationInfo(transformInvocation: TransformInvocation) {
        transformInvocation.inputs.forEach {
            it.directoryInputs.forEach { directoryInput ->
                forEachDir(directoryInput.file)
            }
            it.jarInputs.forEach { jarInput ->
                forEachJar(jarInput)
            }
        }
    }

    private fun forEachDir(input: File) {
        input.listFiles()?.forEach {
            if (it.isDirectory) {
                forEachDir(it)
            } else if (it.isFile) {
                if (it.absolutePath.endsWith(".class")) {
                    collectInternal(it.inputStream())
                }
            }
        }
    }

    private fun forEachJar(jarInput: JarInput) {
        ZipFile(jarInput.file).use { originJar ->
            originJar.entries().iterator().forEach { zipEntry ->
                if (!zipEntry.isDirectory && zipEntry.name.endsWith(".class")) {
                    collectInternal(originJar.getInputStream(zipEntry))
                }
            }
        }
    }

    private fun collectInternal(inputStream: InputStream) {
        inputStream.use {
            val classReader = ClassReader(it.readBytes())
            val classNode = ClassNode()
            classReader.accept(classNode, ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)

            classNode.invisibleAnnotations?.forEach { annotationNode ->
                println("annotationNode = ${annotationNode.desc}")
                if (annotationNode.desc == ANNOTATION_FUSION) {
                    // 解析注解，封装成FusionNode
                    val fusionData = annotationNode.parseNode()
                    val fusionNode = FusionNode(
                        fusionData,
                        classNode.name,
                        fusionData.target.remapClassName(),
                        mutableMapOf(),
                        mutableMapOf(),
                        mutableMapOf(),
                        mutableMapOf()
                    )
                    classNode.parseNode(fusionNode)
                    fusionNode.print()
                    FusionManager.fusionNodeList.add(fusionNode)
                }
            }
        }
    }
}

fun ClassNode.parseNode(fusionNode: FusionNode) {
    fields?.forEach { fieldNode ->
        val key = "${fieldNode.name}#${fieldNode.desc}"
        println("parseClassNode field key = $key")
        val isPublic = fieldNode.access and Opcodes.ACC_PUBLIC != 0
        val isProtected = fieldNode.access and Opcodes.ACC_PROTECTED != 0
        if (!isPublic && !isProtected) {
            fieldNode.name = fieldNode.name.convertFieldName(fusionNode.originClass)
            (fusionNode.remapField as MutableMap)[key] = fieldNode
        } else {
            (fusionNode.originField as MutableMap)[key] = fieldNode
        }
    }
    methods?.forEach { methodNode ->
        val key = "${methodNode.name}#${methodNode.desc}"
        println("parseClassNode method key = $key")

        val isPrivate = methodNode.access and Opcodes.ACC_PRIVATE != 0
        val isAbstract = methodNode.access and Opcodes.ACC_ABSTRACT != 0
        val isNative = methodNode.access and Opcodes.ACC_NATIVE != 0
        if (isPrivate
            && !isAbstract
            && !isNative
            && methodNode.name != "<clinit>"
            && methodNode.name != "<init>"
        ) {
            methodNode.name = methodNode.name.convertFieldName(fusionNode.originClass)
            (fusionNode.remapMethod as MutableMap)[key] = methodNode
        } else {
            (fusionNode.originMethod as MutableMap)[key] = methodNode
        }
    }
}

private fun AnnotationNode.parseNode(): FusionData {
    var index = 0
    index++
    val target = values[index]
    val targetName = with(target as Type) {
        if (this.sort != Type.OBJECT) {
            throw Exception("target of Fusion Annotation only support object type")
        }
        this.internalName
    }
    index++
    val interfaces =
        if (index < values.size) {
            values[index] as List<String>
        } else {
            emptyList()
        }
    return FusionData(targetName, interfaces)
}

