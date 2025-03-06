package dev.upcraft.rtuuy

import dev.kord.common.entity.Snowflake
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.kordex.core.ExtensibleBot
import dev.kordex.core.utils.env
import dev.kordex.modules.data.mongodb.mongoDB
import dev.upcraft.rtuuy.extensions.anti_reply_ping.AntiReplyPingExtension
import dev.upcraft.rtuuy.extensions.ban_sync.BanSyncExtension

val TEST_SERVER_ID = Snowflake(
	// Get the test server ID from the env vars or a .env file
	env("TEST_SERVER").toLong()
)

// Get the bot's token from the env vars or a .env file
private val TOKEN = env("TOKEN")

suspend fun main() {
	val bot = ExtensibleBot(TOKEN) {
		applicationCommands {
			defaultGuild(TEST_SERVER_ID)
		}

		extensions {
			add(::AntiReplyPingExtension)
			add(::BanSyncExtension)
		}

		@OptIn(PrivilegedIntent::class)
		intents {
			+Intent.GuildMessages
		}

		mongoDB()
	}

	bot.start()
}
