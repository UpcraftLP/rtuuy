package dev.upcraft.rtuuy

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.rest.builder.message.embed
import dev.kordex.core.ExtensibleBot
import dev.kordex.core.utils.env
import dev.kordex.core.utils.envOfOrNull
import dev.kordex.modules.data.mongodb.mongoDB
import dev.upcraft.rtuuy.extensions.anti_reply_ping.AntiReplyPingExtension
import dev.upcraft.rtuuy.extensions.ban_sync.BanSyncExtension
import dev.upcraft.rtuuy.extensions.system_notifications.SystemNotificationExtension
import dev.upcraft.rtuuy.i18n.Translations

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
			add(::AntiReplyPingExtension)
			add(::BanSyncExtension)
			add(::SystemNotificationExtension)
		}

		mongoDB()

		about {
			ephemeral = false

			section(
				Translations.About.ApplicationInfo.commandName,
				Translations.About.ApplicationInfo.commandDescription
			) {
				message {
					val kord = getKoin().get<Kord>()
					val info = getAppInfo(kord)

					embed {
						title = Translations.About.ApplicationInfo.title
							.translateNamed(info)
						description = Translations.About.ApplicationInfo.text
							.translateNamed(info)

						field {
							name = Translations.About.ApplicationInfo.IdField.title.translateNamed(info)
							value = Translations.About.ApplicationInfo.IdField.text.translateNamed(info)
							inline = true
						}
						field {
							name = Translations.About.ApplicationInfo.GitField.title.translateNamed(info)
							value = Translations.About.ApplicationInfo.GitField.text.translateNamed(info)
							inline = true
						}

						footer {
							text = Translations.About.ApplicationInfo.footer.translateNamed(info)
						}
					}
				}
			}
		}
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
