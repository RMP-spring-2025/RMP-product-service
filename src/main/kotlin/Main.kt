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
import kotlinx.coroutines.*
import repositories.ProductRepository
import services.ProductQueueHandler
import services.UserServiceQueueHandler

fun main() {
    embeddedServer(Netty, port = 8084, module = Application::module).start(wait = true)
}

fun Application.module() {

    install(ContentNegotiation) {
        json()
    }

    DatabaseConfig.connect()
    println(" Подключение к PostgreSQL")

    RedisConfig.commands
    println("Подключение к Redis")

    val repository = ProductRepository()

    LiquibaseConfig.migrate()
    println("Миграции Liquibase выполнены")

    val productHandler = ProductQueueHandler(repository)
    val userHandler = UserServiceQueueHandler(repository)
    environment.monitor.subscribe(ApplicationStarted) {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        productHandler.launchConsumers(scope, threads = 8)
        userHandler.launchConsumers(scope, threads = 8)

        println("Запущены слушатели очередей Redis")
    }

    // HTTP-маршруты
    configureRouting(repository)
}

fun Application.configureRouting(repository: ProductRepository) {
    routing {
        productRoutes()
    }
}
