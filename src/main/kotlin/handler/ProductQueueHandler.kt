package services

import handler.UUIDSerializer
import io.lettuce.core.RedisClient
import io.lettuce.core.api.coroutines
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule
import models.Product
import repositories.ProductRepository
import java.util.UUID

@Serializable
data class ProductRequest(
    @Contextual val requestId: UUID,
    val type: String,
    val id: Int? = null,
    val barcode: Long? = null,
    val name: String? = null,
    val calories: Double? = null,
    val B: Double? = null,
    val Z: Double? = null,
    val U: Double? = null,
    val mass: Double? = null
)

@Serializable
data class ProductResponse(
    @SerialName("request_id")
    @Contextual val requestId: UUID,
    val product_id: Int? = null,
    val name: String? = null,
    val calories: Double? = null,
    val B: Double? = null,
    val Z: Double? = null,
    val U: Double? = null,
    val mass: Double? = null,
    val products: List<ProductResponse>? = null
)

class ProductQueueHandler(
    private val repository: ProductRepository,
    redisUri: String = "redis://redis:6379"
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
                val message = connection.blpop(0, "product_service_requests")?.value ?: continue

                println("Получен запрос: $message")

                val request = try {
                    json.decodeFromString<ProductRequest>(message)
                } catch (e: Exception) {
                    println("Ошибка декодирования запроса: ${e.message}")
                    continue
                }

                val response = when (request.type) {
                    "get_product_by_id" -> request.id?.let { repository.getById(it) }?.let {
                        ProductResponse(
                            requestId = request.requestId,
                            product_id = it.id!!,
                            name = it.name,
                            calories = it.calories,
                            B = it.proteins,
                            Z = it.fats,
                            U = it.carbohydrates,
                            mass = it.mass
                        )
                    }

                    "get_product_by_bcode" -> request.barcode?.let { repository.getByBarcode(it) }?.let {
                        ProductResponse(
                            requestId = request.requestId,
                            product_id = it.id!!,
                            name = it.name,
                            calories = it.calories,
                            B = it.proteins,
                            Z = it.fats,
                            U = it.carbohydrates,
                            mass = it.mass
                        )
                    }

                    "get_products_by_name" -> {
                        val products = request.name?.let { repository.searchByName(it) } ?: emptyList()
                        ProductResponse(
                            requestId = request.requestId,
                            products = products.map {
                                ProductResponse(
                                    requestId = request.requestId,
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
                    }

                    "add_product" -> {
                        val id = repository.addProduct(
                            Product(
                                name = request.name ?: "unknown",
                                barcode = request.barcode,
                                calories = request.calories ?: 0.0,
                                proteins = request.B,
                                fats = request.Z,
                                carbohydrates = request.U,
                                mass = request.mass
                            )
                        )
                        ProductResponse(
                            requestId = request.requestId,
                            product_id = id
                        )
                    }

                    else -> null
                }

                println("Отправка ответа: $response")

                response?.let {
                    connection.rpush("product_service_response", json.encodeToString(ProductResponse.serializer(), it))
                } ?: println("Ответ не был сгенерирован для запроса ${request.requestId}")

            } catch (e: Exception) {
                println("Ошибка обработки запроса: ${e.message}")
            }
        }
    }
}
