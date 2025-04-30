package services

import handler.UUIDSerializer
import io.lettuce.core.RedisClient
import io.lettuce.core.api.coroutines
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule
import repositories.ProductRepository
import java.util.UUID

@Serializable
data class ProductListRequest(
    @Contextual val requestId: UUID,
    val type: String,
    val ids: List<Int>
)

@Serializable
data class ProductsResponse(
    @SerialName("request_id")
    @Contextual val requestId: UUID,
    val products: List<ProductsDTO>
)

@Serializable
data class ProductsDTO(
    @SerialName("product_id") val productId: Int,
    val name: String? = null,
    val calories: Double? = null,
    val B: Double? = null,
    val Z: Double? = null,
    val U: Double? = null,
    val mass: Double? = null
)

class UserServiceQueueHandler(
    private val repository: ProductRepository,
    redisUri: String = System.getenv("KEYDB_URL") ?: "redis://redis:6379"
) {
    private val client = RedisClient.create(redisUri)
    private val connection = client.connect().coroutines()

    private val json = Json {
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            contextual(UUID::class, UUIDSerializer)
        }
    }

    suspend fun handleRequests() {
        while (true) {
            try {
                val result = connection.blpop(60,  "user_service_product_requests")
                val message = result?.value ?: continue
                println("Получен запрос от user-service: $message")

                val request = try {
                    json.decodeFromString(ProductListRequest.serializer(), message)
                } catch (e: Exception) {
                    println("Ошибка декодирования: ${e.message}")
                    continue
                }

                if (request.type != "get_products_by_ids") continue

                val products = request.ids.mapNotNull { repository.getById(it) }

                val response = ProductsResponse(
                    requestId = request.requestId,
                    products = products.map {
                        ProductsDTO(
                            productId = it.id!!,
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

                connection.rpush("user_service_product_responses", json.encodeToString(ProductsResponse.serializer(), response))

            } catch (e: Exception) {
                println("Ошибка обработки запроса от user-service: ${e.message}")
            }
        }
    }
}
