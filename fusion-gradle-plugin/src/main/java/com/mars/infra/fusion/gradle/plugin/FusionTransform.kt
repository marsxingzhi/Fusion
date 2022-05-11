package com.mars.infra.fusion.gradle.plugin

import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import com.mars.infra.fusion.gradle.plugin.core.process
import com.mars.infra.fusion.gradle.plugin.visitor.RemapClassVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter

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
            onPostTransform()
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
//        FusionManager.remapSuperName(transformInvocation)
    }

    private fun onPostTransform() {

    }
}