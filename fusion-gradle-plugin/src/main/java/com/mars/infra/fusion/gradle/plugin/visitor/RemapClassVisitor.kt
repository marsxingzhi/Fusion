package com.mars.infra.fusion.gradle.plugin.visitor

import com.mars.infra.fusion.gradle.plugin.Fusion
import com.mars.infra.fusion.gradle.plugin.model.FusionNode
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Created by Mars on 2022/5/10
 */
class RemapClassVisitor(classVisitor: ClassVisitor) : ClassVisitor(Opcodes.ASM9, classVisitor) {

    private lateinit var originSuperName: String
    private lateinit var remapSuperName: String
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
        // TODO 这里需要屏蔽@Fusion类
        Fusion.fusionNodeList.forEach {
            if (it.fusionData.target == superName
                && name != it.remapClassName.internalName()) {  // cannot extend itself
                needReMap = true
                fusionNode = it
            }
        }
        if (needReMap) {
            originSuperName = superName!!
            // com.mars.infra.generate.Fusion_androidx_appcompat_app_AppCompatActivity
            remapSuperName = fusionNode!!.remapClassName.internalName()
            super.visit(version, access, name, signature, remapSuperName, interfaces)
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
        if (needReMap && name == "<init>") {
            return RemapAdapter(originSuperName, remapSuperName, mv)
        }
        return mv
    }
}

private class RemapAdapter(
    private val originSuperName: String,
    private val remapSuperName: String,
    methodVisitor: MethodVisitor
) : MethodVisitor(Opcodes.ASM9, methodVisitor) {

    override fun visitMethodInsn(
        opcode: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
        isInterface: Boolean
    ) {
        if (opcode == Opcodes.INVOKESPECIAL
            && owner == originSuperName
            && name == "<init>"
            && descriptor == "()V"
        ) {
            super.visitMethodInsn(opcode, remapSuperName, name, descriptor, isInterface)
        } else {
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
        }
    }
}

private fun String.internalName(): String {
    return "com/mars/infra/generate/$this"
}