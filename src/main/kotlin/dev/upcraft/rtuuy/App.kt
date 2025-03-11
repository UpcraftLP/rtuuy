package dev.upcraft.rtuuy

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kordex.core.ExtensibleBot
import dev.kordex.core.utils.env
import dev.kordex.core.utils.envOfOrNull
import dev.kordex.modules.data.mongodb.mongoDB
import dev.upcraft.rtuuy.extensions.about.AboutExtension
import dev.upcraft.rtuuy.extensions.anti_reply_ping.AntiReplyPingExtension
import dev.upcraft.rtuuy.extensions.ban_sync.BanSyncExtension
import dev.upcraft.rtuuy.extensions.system_notifications.SystemNotificationExtension

// Get the test server ID from the env vars or a .env file
val TEST_SERVER_ID = envOfOrNull<Snowflake>("TEST_SERVER")

// Get the bot's token from the env vars or a .env file
private val TOKEN = env("TOKEN")

object App {
	const val REPOSITORY_URL = "https://github.com/upcraftlp/rtuuy"

	val VERSION: String = javaClass.`package`.specificationVersion ?: "UNKNOWN"
	val COMMIT_SHA: String? = javaClass.`package`.implementationVersion
	val COMMIT_URL: String = COMMIT_SHA?.let { "$REPOSITORY_URL/commit/$it" } ?: REPOSITORY_URL

	const val AUTHOR = "Up"
}

suspend fun main() {
	val bot = ExtensibleBot(TOKEN) {
		applicationCommands {
			defaultGuild(TEST_SERVER_ID)
		}

		extensions {
			add(::AboutExtension)
			add(::AntiReplyPingExtension)
			add(::BanSyncExtension)
			add(::SystemNotificationExtension)
		}

		mongoDB()
	}

	bot.start()
}

suspend fun getAppInfo(kord: Kord): MutableMap<String, Any?> {
	val map = HashMap<String, Any?>()
	map["version"] = App.VERSION
	map["author"] = App.AUTHOR
	map["repository_url"] = App.REPOSITORY_URL
	map["commit_url"] = App.COMMIT_URL
	map["commit_sha"] = App.COMMIT_SHA
	map["bot_username"] = kord.getSelf().username
	map["application_id"] = kord.getApplicationInfo().id
	return map
}
