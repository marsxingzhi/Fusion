package com.mars.infra.fusion.gradle.plugin.core

import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.TransformInvocation
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Created by Mars on 2022/4/14
 */
fun TransformInvocation.process(onPreTransform: (() -> Unit)? = null,
                                onPostTransform: (() -> Unit)? = null,
                                block: (ByteArray) -> ByteArray?) {

    val outputProvider = this.outputProvider
    outputProvider.deleteAll()
    onPreTransform?.invoke()
    this.inputs.forEach {
        it.directoryInputs.forEach { directoryInput ->
            val output = outputProvider!!.getContentLocation(
                directoryInput.name,
                directoryInput.contentTypes,
                directoryInput.scopes,
                Format.DIRECTORY
            )
            directoryInput.file.processDir(output, block)
        }
        it.jarInputs.forEach { jarInput ->
            var destName = jarInput.file.name
            // jar包重命名，因为可能存在同名文件，同名文件会覆盖
            val hash = DigestUtils.md5Hex(jarInput.file.absolutePath).substring(0, 8)
            if (destName.endsWith(".jar")) {
                destName = destName.substring(0, destName.length - 4)
            }
            val output = outputProvider!!.getContentLocation(
                "${destName}_${hash}",
                jarInput.contentTypes,
                jarInput.scopes,
                Format.JAR
            )
            jarInput.processJar(output, block)
        }
    }
    onPostTransform?.invoke()
}

fun File.processDir(output: File, block: (ByteArray) -> ByteArray?) {
    if (output.exists()) {
        FileUtils.forceDelete(output)
    }
    com.android.utils.FileUtils.mkdirs(output)
    val srcDirPath = this.absolutePath
    val destDirPath = output.absolutePath
    this.listFiles()?.forEach {
        val destFilePath = it.absolutePath.replace(srcDirPath, destDirPath)
        val destFile = File(destFilePath)
        if (it.isDirectory) {
            it.processDir(destFile, block)
        } else if (it.isFile) {
            if (it.absolutePath.endsWith(".class")) {
                FileInputStream(it.absolutePath).weave(destFile, block)
            } else {
                FileUtils.copyFile(it, destFile)
            }
        }
    }
}

fun JarInput.processJar(output: File, block: (ByteArray) -> ByteArray?) {
    ZipOutputStream(output.outputStream()).use { zos ->
        zos.setMethod(ZipOutputStream.STORED)  // 设置压缩方法，这里仅打包归档存储
        ZipFile(this.file).use { originJar ->
            originJar.entries().iterator().forEach { zipEntry ->
                if (!zipEntry.isDirectory && zipEntry.name.endsWith(".class")) {
                    val inputStream = originJar.getInputStream(zipEntry).readBytes()
                    block(inputStream)?.let {
                        zos.writeEntry(zipEntry.name, it)
                    }
                }
            }
        }
    }
}

fun FileInputStream.weave(destFile: File, block: (ByteArray) -> ByteArray?) {
    this.use {
        block(it.readBytes())?.let { bytes ->
            val fileOutputStream = FileOutputStream(destFile)
            bytes.write(fileOutputStream)
        }
    }
}


fun ByteArray.write(outputStream: FileOutputStream) {
    outputStream.use {
        it.write(this)
    }
}

fun ZipOutputStream.writeEntry(entryName: String, byteArray: ByteArray) {
    val crc32 = CRC32()
    crc32.apply {
        reset()
        update(byteArray)
    }
    val zipEntry = ZipEntry(entryName)
    zipEntry.also {
        it.size = byteArray.size.toLong()
        it.crc = crc32.value
    }
    putNextEntry(zipEntry)
    write(byteArray)
}


abstract class ClassTransformer(private val ct: ClassTransformer?) {

    open fun transform(cn: ClassNode?) {
        ct?.transform(cn)
    }
}

abstract class MethodTransformer(private val mt: MethodTransformer?) {

    open fun transform(node: MethodNode?) {
        mt?.transform(node)
    }
}