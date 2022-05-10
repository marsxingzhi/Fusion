package com.mars.infra.fusion.gradle.plugin

import com.android.build.api.transform.JarInput
import com.android.build.api.transform.TransformInvocation
import com.mars.infra.fusion.gradle.plugin.model.FusionNode
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile

/**
 * Created by Mars on 2022/5/10
 */
object FusionManager {

    var fusionNodeList = arrayListOf<FusionNode>()

    // TODO 这里修改class，然后写入新的文件中，能够覆盖原来的吗？ 或者说怎么写入到临时文件中？
    //  又或者能不能省略这一步，将修改super class和组合这两个步骤合成一步？
    fun remapSuperName(transformInvocation: TransformInvocation) {
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
                    remapSuperName(it.inputStream())
                }
            }
        }
    }

    private fun forEachJar(jarInput: JarInput) {
        ZipFile(jarInput.file).use { originJar ->
            originJar.entries().iterator().forEach { zipEntry ->
                if (!zipEntry.isDirectory && zipEntry.name.endsWith(".class")) {
                    remapSuperName(originJar.getInputStream(zipEntry))
                }
            }
        }
    }

    private fun remapSuperName(inputStream: InputStream) {

    }
}