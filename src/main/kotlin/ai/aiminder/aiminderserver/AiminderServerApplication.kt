package ai.aiminder.aiminderserver

import io.github.cdimascio.dotenv.Dotenv
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AiminderServerApplication

fun main(args: Array<String>) {
    val dotenv =
        Dotenv
            .configure()
            .directory("src/main/resources")
            .load()
    dotenv.entries().forEach { entry ->
        System.setProperty(entry.key, entry.value)
    }
    runApplication<AiminderServerApplication>(*args)
}
