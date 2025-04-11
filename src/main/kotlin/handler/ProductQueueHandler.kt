package services

import io.lettuce.core.RedisClient
import io.lettuce.core.api.coroutines
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import models.Product
import repositories.ProductRepository

@Serializable
data class ProductRequest(
    val requestId: Int,
    val type: String,
    val id: Int? = null,
    val bcode: String? = null,
    val name: String? = null,
    val calories: Double? = null,
    val B: Double? = null,
    val Z: Double? = null,
    val U: Double? = null,
    val mass: Double? = null
)

@Serializable
data class ProductResponse(
    val request_id: Int,
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
    private val json = Json { ignoreUnknownKeys = true }
    suspend fun handleRequests() {
        while (true) {
            try {
                val message = connection.blpop(0, "product_service_requests")?.value
                if (message == null) {
                    continue
                }

                println("Получен запрос: $message")

                val request = try {
                    json.decodeFromString<ProductRequest>(message)
                } catch (e: Exception) {
                    println("Ошибка декодирования запроса: ${e.message}")
                    continue
                }

                val response = when (request.type) {
                    "get_product_by_id" -> {
                        val product = request.id?.let { repository.getById(it) }
                        product?.let {
                            ProductResponse(
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
                    }

                    "get_product_by_bcode" -> {
                        val product = request.bcode?.let { repository.getByBarcode(it) }
                        product?.let {
                            ProductResponse(
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
                    }

                    "get_products_by_name" -> {
                        val products = request.name?.let { repository.searchByName(it) } ?: emptyList()
                        ProductResponse(
                            request_id = request.requestId,
                            products = products.map {
                                ProductResponse(
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
                    }

                    "add_product" -> {
                        val id = repository.addProduct(
                            Product(
                                name = request.name ?: "unknown",
                                barcode = request.bcode,
                                calories = request.calories ?: 0.0,
                                proteins = request.B,
                                fats = request.Z,
                                carbohydrates = request.U,
                                mass = request.mass
                            )
                        )
                        ProductResponse(
                            request_id = request.requestId,
                            product_id = id
                        )
                    }

                    else -> null
                }

                println("Отправка ответа: $response")

                response?.let {
                    connection.rpush("product_service_response", json.encodeToString(it))
                } ?: println("Ответ для запроса ${request.requestId} не был сгенерирован.")

            } catch (e: Exception) {
                println("Ошибка в процессе обработки запроса: ${e.message}")
            }
        }
    }
}
