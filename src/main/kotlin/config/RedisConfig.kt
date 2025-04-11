package config

import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands

object RedisConfig {
    val client = RedisClient.create("redis://redis:6379")
    val connection: StatefulRedisConnection<String, String> = client.connect()
    val commands: RedisCommands<String, String> = connection.sync()
}
