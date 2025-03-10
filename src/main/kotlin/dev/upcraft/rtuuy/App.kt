package dev.upcraft.rtuuy

import dev.kord.common.entity.Snowflake
import dev.kordex.core.ExtensibleBot
import dev.kordex.core.utils.env
import dev.kordex.core.utils.envOfOrNull
import dev.kordex.modules.data.mongodb.mongoDB
import dev.upcraft.rtuuy.extensions.anti_reply_ping.AntiReplyPingExtension
import dev.upcraft.rtuuy.extensions.ban_sync.BanSyncExtension
import dev.upcraft.rtuuy.extensions.system_notifications.SystemNotificationExtension

// Get the test server ID from the env vars or a .env file
val TEST_SERVER_ID = envOfOrNull<Snowflake>("TEST_SERVER")

// Get the bot's token from the env vars or a .env file
private val TOKEN = env("TOKEN")

object App {
	val VERSION: String = javaClass.`package`.implementationVersion ?: "UNKNOWN"
}

suspend fun main() {
	val bot = ExtensibleBot(TOKEN) {
		applicationCommands {
			defaultGuild(TEST_SERVER_ID)
		}

		extensions {
			add(::SystemNotificationExtension)
			add(::AntiReplyPingExtension)
			add(::BanSyncExtension)
		}

		mongoDB()
	}

	bot.start()
}
