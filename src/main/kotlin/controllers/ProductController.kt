package controllers

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import models.Product
import repositories.ProductRepository

fun Route.productRoutes() {
    val repository = ProductRepository()

    route("/products") {
        get("/id/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            val product = id?.let { repository.getById(it) }
            if (product != null) {
                call.respond(product)
            } else {
                call.respond(HttpStatusCode.NotFound, "Продукт не найден")
            }
        }

        get("/bcode/{bcode}") {
            val barcode = call.parameters["bcode"]
            val product = barcode?.let { repository.getByBarcode(it) }
            if (product != null) {
                call.respond(product)
            } else {
                call.respond(HttpStatusCode.NotFound, "Продукт не найден")
            }
        }

        get("/name/{name}") {
            val name = call.parameters["name"]
            val products = name?.let { repository.searchByName(it) } ?: emptyList()
            call.respond(products)
        }

        post {
            val product = call.receive<Product>()
            val id = repository.addProduct(product)
            call.respond(HttpStatusCode.Created, mapOf("id" to id))
        }
    }
}
