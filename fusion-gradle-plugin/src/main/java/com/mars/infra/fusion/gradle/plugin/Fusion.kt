package com.mars.infra.fusion.gradle.plugin

import com.mars.infra.fusion.gradle.plugin.model.FusionNode
import com.mars.infra.fusion.gradle.plugin.visitor.FusionClassVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes

/**
 * Created by Mars on 2022/5/10
 */
object Fusion {

    var fusionNodeList = arrayListOf<FusionNode>()

    fun filter(className: String): Boolean {
        fusionNodeList.forEach { node ->
            if (node.originClass == className) {
                return true
            }
        }
        return false
    }

    fun generateFusionClass(): ByteArray? {
        val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES)
        val fusionClassVisitor = FusionClassVisitor(cw)

        fusionClassVisitor.visit(
            Opcodes.V1_8,
            Opcodes.ACC_PUBLIC,
            "$GENERATE_PACKAGE_NAME/Fusion_androidx_appcompat_app_AppCompatActivity",
            null,
            APP_COMPACT_ACTIVITY_SUPER_NAME,
            null)

        val methodVisitor =
            fusionClassVisitor.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null)
        methodVisitor.apply {
            visitVarInsn(Opcodes.ALOAD, 0)
            visitMethodInsn(Opcodes.INVOKESPECIAL, APP_COMPACT_ACTIVITY_SUPER_NAME, "<init>", "()V", false)

            // 语句打印，验证是否构造函数创建成功
            visitLdcInsn("mars")
            visitLdcInsn("正在执行<init>方法")
            visitMethodInsn(Opcodes.INVOKESTATIC, "android/util/Log", "e", "(Ljava/lang/String;Ljava/lang/String;)I", false)

            visitInsn(Opcodes.RETURN)
            visitMaxs(1, 1)
            visitEnd()
        }
        fusionClassVisitor.visitEnd()
        return cw.toByteArray()
    }

    /**
     * 1. 写入私有方法
     * 2. 非私有方法的写入，不会修改非私有方法的方法名和描述符，而是新建一个方法，在非私有方法中调用该新建的方法
     * 3. 字段的写入
     */
    fun visitEnd(classVisitor: ClassVisitor) {
        val fusionNode = fusionNodeList[0]
        fusionNode.remapMethod.values.forEach {
            it.accept(classVisitor)
        }
    }
}