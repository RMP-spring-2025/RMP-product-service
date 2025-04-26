package repositories

import models.Product
import models.Products
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class ProductRepository {

    fun getById(id: Int): Product? = transaction {
        Products.select { Products.id eq id }
            .map { rowToProduct(it) }
            .singleOrNull()
    }

    fun getByBarcode(bcode: Long): Product? = transaction {
        Products.select { Products.bcode eq bcode }
            .map { rowToProduct(it) }
            .singleOrNull()
    }

    fun searchByName(name: String): List<Product> = transaction {
        Products.select { Products.name like "%$name%" }
            .map { rowToProduct(it) }
    }

    fun addProduct(product: Product): Int = transaction {
        Products.insertAndGetId {
            it[name] = product.name
            it[bcode] = product.bcode
            it[calories] = product.calories
            it[proteins] = product.proteins
            it[fats] = product.fats
            it[carbohydrates] = product.carbohydrates
            it[mass] = product.mass
        }.value
    }

    private fun rowToProduct(row: ResultRow) = Product(
        id = row[Products.id].value,
        name = row[Products.name],
        bcode = row[Products.bcode],
        calories = row[Products.calories],
        proteins = row[Products.proteins],
        fats = row[Products.fats],
        carbohydrates = row[Products.carbohydrates],
        mass = row[Products.mass]
    )
}
