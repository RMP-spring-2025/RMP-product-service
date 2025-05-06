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
    @SerialName("requestId")
    @Contextual val requestId: UUID,
    @SerialName("productId") val productId: Int? = null,
    val name: String? = null,
    val calories: Double? = null,
    val B: Double? = null,
    val Z: Double? = null,
    val U: Double? = null,
    val mass: Double? = null
)

@Serializable
data class ServiceResponse<T>(
    @SerialName("requestId") @Contextual val requestId: UUID,
    val status: String, // "success", "error", "not_found", "conflict"
    val data: T? = null,
    val errorMessage: String? = null
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

                        val response = if (product != null) {
                            ServiceResponse(
                                requestId = request.requestId,
                                status = "success",
                                data = ProductResponse(
                                    requestId = request.requestId,
                                    productId = product.id!!,
                                    name = product.name,
                                    calories = product.calories,
                                    B = product.proteins,
                                    Z = product.fats,
                                    U = product.carbohydrates,
                                    mass = product.mass
                                )
                            )
                        } else {
                            ServiceResponse<ProductResponse>(
                                requestId = request.requestId,
                                status = "not_found",
                                errorMessage = "Продукт с id=${request.id} не найден."
                            )
                        }

                        println("Отправка ответа: $response")
                        connection.rpush("product_service_response", json.encodeToString(response))
                    }

                    "get_product_by_bcode" -> {
                        val product = request.bcode?.let { repository.getByBarcode(it) }
                        val response = if (product != null) {
                            ServiceResponse(
                                requestId = request.requestId,
                                status = "success",
                                data = ProductResponse(
                                    requestId = request.requestId,
                                    productId = product.id!!,
                                    name = product.name,
                                    calories = product.calories,
                                    B = product.proteins,
                                    Z = product.fats,
                                    U = product.carbohydrates,
                                    mass = product.mass
                                )
                            )
                        } else {
                            ServiceResponse<ProductResponse>(
                                requestId = request.requestId,
                                status = "not_found",
                                errorMessage = "Продукт с баркодом ${request.bcode} не найден."
                            )
                        }

                        println("Отправка ответа: $response")
                        connection.rpush("product_service_response", json.encodeToString(response))
                    }

                    "add_product" -> {
                        val existingProduct = request.bcode?.let { repository.getByBarcode(it) }
                        println(existingProduct)
                        if (existingProduct != null) {
                            val errorResponse = ServiceResponse<ProductResponse>(
                                requestId = request.requestId,
                                status = "conflict",
                                errorMessage = "Продукт с таким баркодом уже существует."
                            )
                            println("Отправка ответа: $errorResponse")
                            connection.rpush("product_service_response", json.encodeToString(errorResponse))
                            continue
                        }

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

                        val successResponse = ServiceResponse(
                            requestId = request.requestId,
                            status = "success",
                            data = ProductResponse(
                                requestId = request.requestId,
                                productId = product?.id,
                                name = product?.name,
                                calories = product?.calories,
                                B = product?.proteins,
                                Z = product?.fats,
                                U = product?.carbohydrates,
                                mass = product?.mass
                            )
                        )

                        println("Отправка ответа: $successResponse")
                        connection.rpush("product_service_response", json.encodeToString(successResponse))
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
