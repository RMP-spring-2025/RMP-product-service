package config

import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection

object RedisConfig {
    private val redisClient = RedisClient.create("redis://localhost:6379")
    val connection: StatefulRedisConnection<String, String> = redisClient.connect()

    fun close() {
        connection.close()
        redisClient.shutdown()
    }
}

