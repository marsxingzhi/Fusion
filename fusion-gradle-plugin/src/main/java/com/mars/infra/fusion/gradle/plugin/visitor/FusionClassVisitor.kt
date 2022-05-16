package com.mars.infra.fusion.gradle.plugin.visitor

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Created by Mars on 2022/5/17
 */
class FusionClassVisitor(classVisitor: ClassVisitor): ClassVisitor(Opcodes.ASM9, classVisitor) {

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        return super.visitMethod(access, name, descriptor, signature, exceptions)
    }
}