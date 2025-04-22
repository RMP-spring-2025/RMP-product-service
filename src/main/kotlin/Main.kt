import config.DatabaseConfig
import config.LiquibaseConfig
import config.RedisConfig
import controllers.productRoutes
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import repositories.ProductRepository
import services.ProductQueueHandler
import services.UserServiceQueueHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

fun Application.module() {

    install(ContentNegotiation) {
        json()
    }

    DatabaseConfig.connect()
    println("Подключение к PostgreSQL")

    RedisConfig.commands
    println("Подключение к Redis")

    val repository = ProductRepository()
    val handler = ProductQueueHandler(repository)

    LiquibaseConfig.migrate()
    println("Миграции Liquibase выполнены")

    environment.monitor.subscribe(ApplicationStarted) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            handler.handleRequests()
        }
        scope.launch {
            UserServiceQueueHandler(repository).handleRequests()
        }
    }
    configureRouting(repository)
}

fun Application.configureRouting(repository: ProductRepository) {
    routing {
        productRoutes()
    }
}
