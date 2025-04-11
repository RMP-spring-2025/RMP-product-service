package config

import liquibase.Contexts
import liquibase.Liquibase
import liquibase.database.core.PostgresDatabase
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import java.sql.DriverManager

object LiquibaseConfig {
    fun migrate() {
        val databaseUrl = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/product_db"
        val username = System.getenv("DATABASE_USER") ?: "postgres"
        val password = System.getenv("DATABASE_PASSWORD") ?: "password"

        val connection = DriverManager.getConnection(databaseUrl, username, password)

        val jdbcConnection = JdbcConnection(connection)

        val liquibase = Liquibase(
            "db/changelog/db.changelog-master.xml",
            ClassLoaderResourceAccessor(),
            PostgresDatabase().apply {
                setConnection(jdbcConnection)
            }
        )

        liquibase.update(Contexts())
    }
}
