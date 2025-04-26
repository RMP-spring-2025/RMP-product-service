package models

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable

@Serializable
data class Product(
    @Contextual val id: Int? = null,
    val name: String,
    val bcode: Long?,
    val calories: Double,
    val proteins: Double?,
    val fats: Double?,
    val carbohydrates: Double?,
    val mass: Double?
)

object Products : IntIdTable("products") {
    val name = varchar("name", 255)
    val bcode = long("bcode").nullable()
    val calories = double("calories")
    val proteins = double("proteins").nullable()
    val fats = double("fats").nullable()
    val carbohydrates = double("carbohydrates").nullable()
    val mass = double("mass").nullable()
}

