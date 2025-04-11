package config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database

object DatabaseConfig {
    fun connect() {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = System.getenv("DATABASE_URL") ?: "jdbc:postgresql:/postgres_db:5432/product_db"
            driverClassName = "org.postgresql.Driver"
            username = System.getenv("DATABASE_USER") ?: "postgres"
            password = System.getenv("DATABASE_PASSWORD") ?: "password"
            maximumPoolSize = 10
        }
        val dataSource = HikariDataSource(hikariConfig)
        Database.connect(dataSource)
    }
}