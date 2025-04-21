package services

import io.lettuce.core.RedisClient
import io.lettuce.core.api.coroutines
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import models.Product
import repositories.ProductRepository
import services.ProductResponse

@Serializable
data class ProductListRequest(
    val requestId: Int,
    val type: String, // например "get_products_by_ids"
    val ids: List<Int>
)
@Serializable
data class ProductsResponse(
    val request_id: Int,
    val product_id: Int? = null,
    val name: String? = null,
    val calories: Double? = null,
    val B: Double? = null,
    val Z: Double? = null,
    val U: Double? = null,
    val mass: Double? = null,
    val products: List<ProductsResponse>? = null
)
class UserServiceQueueHandler(
    private val repository: ProductRepository,
    redisUri: String = "redis://redis:6379"
) {
    private val client = RedisClient.create(redisUri)
    private val connection = client.connect().coroutines()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun handleRequests() {
        while (true) {
            try {
                val message = connection.blpop(0, "user_service_product_requests")?.value
                if (message == null) continue

                println("Получен запрос от user-service: $message")

                val request = try {
                    json.decodeFromString<ProductListRequest>(message)
                } catch (e: Exception) {
                    println("Ошибка декодирования: ${e.message}")
                    continue
                }

                if (request.type != "get_products_by_ids") continue

                val products = request.ids.mapNotNull { repository.getById(it) }

                val response = ProductsResponse(
                    request_id = request.requestId,
                    products = products.map {
                        ProductsResponse(
                            request_id = request.requestId,
                            product_id = it.id!!,
                            name = it.name,
                            calories = it.calories,
                            B = it.proteins,
                            Z = it.fats,
                            U = it.carbohydrates,
                            mass = it.mass
                        )
                    }
                )

                println("Отправка ответа в user-service: $response")

                connection.rpush("user_service_product_responses", json.encodeToString(response))

            } catch (e: Exception) {
                println("Ошибка обработки запроса от user-service: ${e.message}")
            }
        }
    }
}
