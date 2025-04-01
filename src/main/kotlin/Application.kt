import config.DatabaseConfig
import config.RedisConfig
import controllers.productRoutes
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }

    DatabaseConfig.connect()
    println("✅ Подключение к PostgreSQL установлено!")

    RedisConfig
    println("✅ Подключение к Redis установлено!")

    configureRouting()
}

fun Application.configureRouting() {
    routing {
        productRoutes()
    }
}
