package com.mars.infra.fusion.annotations

import kotlin.reflect.KClass

/**
 * Created by Mars on 2022/5/9
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Fusion(
    val target: KClass<*>,
    val interfaces: Array<KClass<*>> = []
)
