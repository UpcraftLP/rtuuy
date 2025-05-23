package dev.upcraft.rtuuy.util

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.kordex.core.utils.env
import dev.kordex.core.utils.loadModule
import dev.upcraft.rtuuy.model.DiscordUserRepository
import org.jetbrains.exposed.sql.Database
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf

object DatabaseFactory {
	fun create(): Database {
		val config = HikariConfig().apply {
			jdbcUrl = env("DATABASE_URL")
			username = env("DATABASE_USERNAME")
			password = env("DATABASE_PASSWORD")
			maximumPoolSize = 6
			transactionIsolation
		}
		val dataSource = HikariDataSource(config)
		val db = Database.connect(dataSource)

		// TODO flyway for DB migrations

		return db
	}
}

fun registerDataRepositories() {
	loadModule {
		singleOf(::DiscordUserRepository) { bind() }
	}
}
