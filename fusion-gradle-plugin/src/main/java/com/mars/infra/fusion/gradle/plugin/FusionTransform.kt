package com.mars.infra.fusion.gradle.plugin

import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import com.mars.infra.fusion.gradle.plugin.core.process
import com.mars.infra.fusion.gradle.plugin.visitor.RemapClassVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import java.io.File

/**
 * Created by Mars on 2022/5/9
 */
class FusionTransform : Transform() {

    override fun getName(): String = "FusionTransform"

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> =
        TransformManager.CONTENT_CLASS

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> =
        TransformManager.SCOPE_FULL_PROJECT

    override fun isIncremental(): Boolean = false

    override fun transform(transformInvocation: TransformInvocation) {
        super.transform(transformInvocation)
        transformInvocation.process({
            onPreTransform(transformInvocation)
        }, {
            onPostTransform(transformInvocation)
        }) { bytes: ByteArray ->
            val cr = ClassReader(bytes)
            FusionManager.filter(cr.className).no {
                val cw = ClassWriter(cr, 0)
                val rootClassVisitor = RemapClassVisitor(cw)
                cr.accept(rootClassVisitor, ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
                cw.toByteArray()
            }.otherwise {
                null
            }
        }
    }

    // 收集Fusion注解信息
    private fun onPreTransform(transformInvocation: TransformInvocation) {
        FusionCollector.collectFusionAnnotationInfo(transformInvocation)
    }

    /**
     * 1. 生成新类添加到transforms目录里
     * 2. 将Fusion类内容写入新类
     *
     * TODO 只针对一个类
     */
    private fun onPostTransform(transformInvocation: TransformInvocation) {
        val types = setOf(QualifiedContent.DefaultContentType.CLASSES)
        val scopes = mutableSetOf(QualifiedContent.Scope.PROJECT)
        val out = transformInvocation.outputProvider.getContentLocation(
            "fusion",
            types,
            scopes,
            Format.DIRECTORY
        )
        println("out = ${out.absolutePath}")

        // 创建class，写入out目录中
        val file = File(out, "$GENERATE_PACKAGE_NAME/Fusion_androidx_appcompat_app_AppCompatActivity.class")
        file.parentFile.mkdirs()
        if (!file.exists()) {
            file.createNewFile()
        }

        val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES)

        val cn = ClassNode()
        cn.version = Opcodes.V1_8
        cn.access = Opcodes.ACC_PUBLIC
        cn.name = "$GENERATE_PACKAGE_NAME/Fusion_androidx_appcompat_app_AppCompatActivity"
        cn.superName = APP_COMPACT_ACTIVITY_SUPER_NAME
        cn.signature = null


//        val methodNode = MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null)
//        cn.methods.add(methodNode)
//        val il = methodNode.instructions
//        il.add(VarInsnNode(Opcodes.ALOAD, 0))
//        il.add(
//            MethodInsnNode(
//                Opcodes.INVOKESPECIAL,
//                APP_COMPACT_ACTIVITY_SUPER_NAME,
//                "<init>",
//                "()V",
//                false
//            )
//        )
//        il.add(InsnNode(Opcodes.RETURN))
//        methodNode.maxStack = 1
//        methodNode.maxLocals = 1

        // 只考虑一个的情况
        val fusionNode = FusionManager.fusionNodeList[0]
        fusionNode.originField.values.forEach {
            cn.fields.add(it)
        }
        fusionNode.remapField.values.forEach {
            cn.fields.add(it)
        }
        fusionNode.originMethod.values.forEach {
            cn.methods.add(it)
        }
        fusionNode.remapMethod.values.forEach {
            cn.methods.add(it)
        }

        cn.accept(cw)
        file.writeBytes(cw.toByteArray())
    }
}