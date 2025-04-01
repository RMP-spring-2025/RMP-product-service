package config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database

object DatabaseConfig {
    fun connect() {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = "jdbc:postgresql://localhost:5432/product_db"
            driverClassName = "org.postgresql.Driver"
            username = "postgres"
            password = "password"
            maximumPoolSize = 10
        }
        val dataSource = HikariDataSource(hikariConfig)
        Database.connect(dataSource)
    }
}

