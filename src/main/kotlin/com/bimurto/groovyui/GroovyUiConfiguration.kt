package com.bimurto.groovyui

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan(basePackageClasses = [GroovyController::class])
open class GroovyUiConfiguration