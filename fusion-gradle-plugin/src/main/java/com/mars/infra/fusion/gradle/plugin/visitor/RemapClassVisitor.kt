package com.mars.infra.fusion.gradle.plugin.visitor

import com.mars.infra.fusion.gradle.plugin.FusionManager
import com.mars.infra.fusion.gradle.plugin.model.FusionNode
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Created by Mars on 2022/5/10
 */
class RemapClassVisitor(classVisitor: ClassVisitor) : ClassVisitor(Opcodes.ASM9, classVisitor) {

    private lateinit var originSuperName: String
    private var needReMap = false
    private var fusionNode: FusionNode? = null

    override fun visit(
        version: Int,
        access: Int,
        name: String?,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        FusionManager.fusionNodeList.forEach {
            if (it.fusionData.target == superName) {
                needReMap = true
                fusionNode = it
            }
        }
        if (needReMap) {
            // com.mars.infra.generate.Fusion_androidx_appcompat_app_AppCompatActivity
            super.visit(version, access, name, signature, "com/mars/infra/generate/${fusionNode!!.remapClassName}", interfaces)
        } else {
            super.visit(version, access, name, signature, superName, interfaces)
        }
    }

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
        if (name == "<init>") {
            return RemapAdapter(mv)
        }
        return mv
    }
}

private class RemapAdapter(methodVisitor: MethodVisitor): MethodVisitor(Opcodes.ASM9, methodVisitor) {

    override fun visitMethodInsn(
        opcode: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
        isInterface: Boolean
    ) {
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }
}