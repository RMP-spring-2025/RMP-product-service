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
    @SerialName("requestType")
    val type: String,
    val id: Int? = null,
    val bcode: Long? = null,
    val name: String? = null,
    val calories: Double? = null,
    @SerialName("b") val B: Double? = null,
    @SerialName("z") val Z: Double? = null,
    @SerialName("u") val U: Double? = null,
    val mass: Double? = null
)

@Serializable
data class ProductResponse(
    @SerialName("request_id")
    @Contextual val requestId: UUID,
    @SerialName("product_id") val productId: Int? = null,
    val name: String? = null,
    val calories: Double? = null,
    val B: Double? = null,
    val Z: Double? = null,
    val U: Double? = null,
    val mass: Double? = null,
)

class ProductQueueHandler(
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
                val result = connection.blpop(60, "product_service_requests")
                val message = result?.value ?: continue

                println("Получен запрос: $message")

                val request = try {
                    json.decodeFromString<ProductRequest>(message)
                } catch (e: Exception) {
                    println("Ошибка декодирования запроса: ${e.message}")
                    continue
                }

                when (request.type) {
                    "get_product_by_id" -> {
                        val product = request.id?.let { repository.getById(it) }
                        val response = product?.let {
                            ProductResponse(
                                requestId = request.requestId,
                                productId = it.id!!,
                                name = it.name,
                                calories = it.calories,
                                B = it.proteins,
                                Z = it.fats,
                                U = it.carbohydrates,
                                mass = it.mass
                            )
                        }

                        println("Отправка ответа: $response")
                        response?.let {
                            connection.rpush("product_service_response", json.encodeToString(ProductResponse.serializer(), it))
                        }
                    }

                    "get_product_by_bcode" -> {
                        val product = request.bcode?.let { repository.getByBarcode(it) }
                        val response = product?.let {
                            ProductResponse(
                                requestId = request.requestId,
                                productId = it.id!!,
                                name = it.name,
                                calories = it.calories,
                                B = it.proteins,
                                Z = it.fats,
                                U = it.carbohydrates,
                                mass = it.mass
                            )
                        }

                        println("Отправка ответа: $response")
                        response?.let {
                            connection.rpush("product_service_response", json.encodeToString(ProductResponse.serializer(), it))
                        }
                    }

                    "get_products_by_name" -> {
                        val products = request.name?.let { repository.searchByName(it) } ?: emptyList()
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

                        println("Отправка списка продуктов: $response")
                        connection.rpush("product_service_response", json.encodeToString(ProductsResponse.serializer(), response))
                    }

                    "add_product" -> {
                        val id = repository.addProduct(
                            Product(
                                name = request.name ?: "unknown",
                                bcode = request.bcode,
                                calories = request.calories ?: 0.0,
                                proteins = request.B,
                                fats = request.Z,
                                carbohydrates = request.U,
                                mass = request.mass
                            )
                        )

                        val product = repository.getById(id!!)

                        val response = product?.let {
                            ProductResponse(
                                requestId = request.requestId,
                                productId = it.id,
                                name = it.name,
                                calories = it.calories,
                                B = it.proteins,
                                Z = it.fats,
                                U = it.carbohydrates,
                                mass = it.mass
                            )
                        }

                        println("Отправка ответа: $response")
                        response?.let {
                            connection.rpush("product_service_response", json.encodeToString(ProductResponse.serializer(), it))
                        }
                    }

                    else -> {
                        println("Неизвестный тип запроса: ${request.type}")
                    }
                }

            } catch (e: Exception) {
                println("Ошибка обработки запроса: ${e.message}")
            }
        }
    }
}
