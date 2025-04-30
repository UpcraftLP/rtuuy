package dev.upcraft.rtuuy

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.effectiveName
import dev.kord.rest.builder.message.embed
import dev.kordex.core.ExtensibleBot
import dev.kordex.core.utils.env
import dev.kordex.core.utils.envOfOrNull
import dev.kordex.core.utils.envOrNull
import dev.kordex.modules.data.mongodb.mongoDB
import dev.kordex.modules.web.core.backend.utils.web
import dev.upcraft.rtuuy.extensions.analytics.AnalyticsExtension
import dev.upcraft.rtuuy.extensions.anti_reply_ping.AntiReplyPingExtension
import dev.upcraft.rtuuy.extensions.ban_sync.BanSyncExtension
import dev.upcraft.rtuuy.extensions.system_notifications.SystemNotificationExtension
import dev.upcraft.rtuuy.i18n.Translations
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

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
	Security.addProvider(BouncyCastleProvider())

	val bot = ExtensibleBot(TOKEN) {
		botVersion = App.COMMIT_SHA ?: App.VERSION

		applicationCommands {
			defaultGuild(TEST_SERVER_ID)
		}

		extensions {
			web {
				hostname = envOrNull("FRONTEND_URL")
				port = envOfOrNull<Int>("PORT") ?: 3000
			}

			add(::AnalyticsExtension)
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
						field {
							name = Translations.About.ApplicationInfo.TermsOfUse.title.translateNamed(info)
							value = Translations.About.ApplicationInfo.TermsOfUse.text.translateNamed(info)
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
	return mutableMapOf(
		"version" to App.VERSION,
		"author" to App.AUTHOR,
		"repository_url" to App.REPOSITORY_URL,
		"commit_url" to App.COMMIT_URL,
		"commit_sha" to App.COMMIT_SHA,
		"bot_id" to kord.getSelf().id,
		"bot_username" to kord.getSelf().effectiveName,
		"application_id" to kord.getApplicationInfo().id,
		"privacy_policy_url" to (kord.getApplicationInfo().privacyPolicyUrl ?: App.REPOSITORY_URL),
		"terms_of_service_url" to (kord.getApplicationInfo().termsOfServiceUrl ?: App.REPOSITORY_URL),
	)
}
