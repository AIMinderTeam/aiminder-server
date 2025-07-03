package ai.aiminder.aiminderserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AiminderServerApplication

fun main(args: Array<String>) {
    runApplication<AiminderServerApplication>(*args)
}
