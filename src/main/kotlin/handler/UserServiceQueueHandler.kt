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
data class ProductsDTO(
    @SerialName("productId") val productId: Int,
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
        encodeDefaults = false
        serializersModule = SerializersModule {
            contextual(UUID::class, UUIDSerializer)
        }
    }

    suspend fun handleRequests() {
        while (true) {
            try {
                val result = connection.blpop(60, "user_service_product_requests")
                val message = result?.value ?: continue
                println("Получен запрос от user-service: $message")

                val request = try {
                    json.decodeFromString(ProductListRequest.serializer(), message)
                } catch (e: Exception) {
                    println("Ошибка декодирования: ${e.message}")
                    continue
                }

                if (request.type != "get_products_by_ids") {
                    println("Неизвестный тип запроса: ${request.type}")
                    continue
                }

                val products = request.ids.mapNotNull { repository.getById(it) }

                val response = if (products.isEmpty()) {
                    ServiceResponse<List<ProductsDTO>>(
                        requestId = request.requestId,
                        status = "not_found",
                        errorMessage = "Продукты не найдены по заданным id: ${request.ids}"
                    )
                } else {
                    ServiceResponse(
                        requestId = request.requestId,
                        status = "success",
                        data = products.map {
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
                }

                println("Отправка ответа в user-service: $response")
                connection.rpush("user_service_product_responses", json.encodeToString(response))

            } catch (e: Exception) {
                println("Ошибка обработки запроса от user-service: ${e.message}")
                val fallbackId = UUID.randomUUID()
                val errorResponse = ServiceResponse<List<ProductsDTO>>(
                    requestId = fallbackId,
                    status = "error",
                    errorMessage = "Ошибка обработки запроса: ${e.message}"
                )
                connection.rpush("user_service_product_responses", json.encodeToString(errorResponse))
            }
        }
    }
}
