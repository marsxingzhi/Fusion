package com.mars.infra.fusion.gradle.plugin

import com.android.build.api.transform.JarInput
import com.android.build.api.transform.TransformInvocation
import com.mars.infra.fusion.gradle.plugin.model.FusionData
import com.mars.infra.fusion.gradle.plugin.model.FusionNode
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.MethodRemapper
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile

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
                    Fusion.fusionNodeList.add(fusionNode)
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

            // 注意：不能在这里进行MethodRemapper，fusionNode.mRemapper中的mapMethodName是从remapMethod中取值的
            // 而此时remapMethod为空，因为当前未将元素添加到remapMethod中。所以remapper的逻辑需要放到后面
            // 问题描述：在execute方法体中，调用的是super.checkLoginStatus()方法，首先checkLoginStatus方法是当前类的实例方法，
            // 父类中没有，其次在当前类中，这个方法名已经修改了，这里调用的方法名也不对，因此定位到remapper的时机有问题
//            val newMethodNode = MethodNode(
//                methodNode.access,
//                methodNode.name,
//                methodNode.desc,
//                methodNode.signature,
//                methodNode.exceptions.toTypedArray()
//            )
//            val methodRemapper = MethodRemapper(newMethodNode, fusionNode.mRemapper)
//            methodNode.accept(methodRemapper)

            (fusionNode.remapMethod as MutableMap)[key] = methodNode
        } else {

//            val newMethodNode = MethodNode(
//                methodNode.access,
//                methodNode.name,
//                methodNode.desc,
//                methodNode.signature,
//                methodNode.exceptions.toTypedArray()
//            )
//            val methodRemapper = MethodRemapper(newMethodNode, fusionNode.mRemapper)
//            methodNode.accept(methodRemapper)

            (fusionNode.originMethod as MutableMap)[key] = methodNode
        }
    }

    val newRemapperMethodMap = mutableMapOf<String, MethodNode>()
    fusionNode.remapMethod.forEach { key, node ->
        val newNode = MethodNode(
            node.access,
            node.name,
            node.desc,
            node.signature,
            node.exceptions.toTypedArray()
        )
        val methodRemapper = MethodRemapper(newNode, fusionNode.mRemapper)
        node.accept(methodRemapper)
        newRemapperMethodMap[key] = newNode
    }
    val newOriginMethodMap = mutableMapOf<String, MethodNode>()
    fusionNode.originMethod.forEach { key, node ->
        val newNode = MethodNode(
            node.access,
            node.name,
            node.desc,
            node.signature,
            node.exceptions.toTypedArray()
        )
        val methodRemapper = MethodRemapper(newNode, fusionNode.mRemapper)
        node.accept(methodRemapper)
        newOriginMethodMap[key] = newNode
    }

    (fusionNode.remapMethod as MutableMap).apply {
        clear()
        putAll(newRemapperMethodMap)
    }
    (fusionNode.originMethod as MutableMap).apply {
        clear()
        putAll(newOriginMethodMap)
    }


    fusionNode.originMethod.values.forEach { methodNode ->
        methodNode.instructions.asIterable().filter {
            it is MethodInsnNode && it.opcode == Opcodes.INVOKESPECIAL && it.owner == this.superName
        }.map {
            it as MethodInsnNode
        }.forEach {
            // 需要删除Fusion-Class的构造函数中调用父类构造函数的指令，
            // 调用父类构造函数需要先加载ALOAD_0，而Fusion-Class是不会被打到apk中的
            if (it.name == "<init>") {
                methodNode.instructions.remove(it.previous)
                methodNode.instructions.remove(it)
            }
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

