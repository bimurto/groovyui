package com.bimurto.groovyui

import groovy.lang.Binding
import groovy.lang.GroovyShell
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter
import java.io.PrintWriter
import java.io.StringWriter
import java.io.Writer
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

@RestController
class GroovyController(
    private val context: ApplicationContext,
    @Value("\${groovy-ui.remote.access.enabled:false}")
    private val remoteAccessEnabled: Boolean,
    @Value("\${groovy-ui.output.timeout:360000}")
    private val outputTimeout: Long,
) {

    private val executor = Executors.newSingleThreadExecutor()
    private val logger = LoggerFactory.getLogger(GroovyController::class.java)

    @GetMapping("/groovy-ui.html")
    fun getGroovyUi(request: HttpServletRequest): ResponseEntity<Any> {
        return if (allowAccess(request)) {
            try {
                val resource = ClassPathResource("static/groovy-ui.html")
                val content = resource.inputStream.readAllBytes().toString(StandardCharsets.UTF_8)
                ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(content)
            } catch (e: Exception) {
                logger.error("Error reading groovy-ui.html", e)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error reading groovy-ui.html")
            }
        } else {
            ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
    }

    @PostMapping("/execute-groovy")
    fun executeGroovy(@RequestBody script: String, request: HttpServletRequest): ResponseEntity<ResponseBodyEmitter> {
        if (!allowAccess(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        val emitter = ResponseBodyEmitter(outputTimeout)
        val writer: Writer = object : Writer() {
            override fun write(cbuf: CharArray, off: Int, len: Int) {
                try {
                    emitter.send(String(cbuf, off, len))
                } catch (ex: Exception) {
                    logger.error("Error sending data", ex)
                    emitter.completeWithError(ex)
                }
            }

            override fun flush() {}
            override fun close() {}
        }
        val printWriter = PrintWriter(writer)

        emitter.onCompletion { logger.info("Emitter completed") }
        emitter.onTimeout { logger.info("Emitter timed out") }

        executor.execute {
            try {
                val binding = Binding()
                binding.setVariable("ctx", context)
                binding.setProperty("out", printWriter)

                val shell = GroovyShell(binding)
                val result = shell.evaluate(script)

                if (result != null) {
                    emitter.send(result.toString())
                }
                emitter.complete()
            } catch (e: Exception) {
                logger.error("Error executing script", e)
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                emitter.send(sw.toString())
                emitter.complete()
            }
        }

        return ResponseEntity.ok(emitter)
    }

    private fun allowAccess(request: HttpServletRequest): Boolean {
        if(remoteAccessEnabled) return true
        val remoteAddr = request.remoteAddr
        return remoteAddr == "127.0.0.1" || remoteAddr == "0:0:0:0:0:0:0:1"
    }
}
