package com.mars.infra.fusion.gradle.plugin

import com.android.build.api.transform.JarInput
import com.android.build.api.transform.TransformInvocation
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile

/**
 * Created by Mars on 2022/5/9
 */
object FusionCollector {

    fun collectFusionAnnotationInfo(transformInvocation: TransformInvocation) {
        transformInvocation.inputs.forEach {
            it.directoryInputs.forEach {  directoryInput ->
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
            }
        }
    }
}