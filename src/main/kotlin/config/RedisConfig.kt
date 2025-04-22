package config

import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands

object RedisConfig {
    init {
        println("Initializing Redis connection...")
    }

    val client = RedisClient.create("redis://redis:6379").also {
        println("Redis client created")
    }

    val connection: StatefulRedisConnection<String, String> = client.connect().also {
        println("Redis connection established")
    }

    val commands: RedisCommands<String, String> = connection.sync().also {
        println("Redis sync commands ready")
    }
}
