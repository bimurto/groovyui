package com.bimurto.groovyui

import org.springframework.context.annotation.Import

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(GroovyUiConfiguration::class)
annotation class EnableGroovyUi
