package dev.upcraft.rtuuy.extensions.about

import dev.kord.rest.builder.message.embed
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ephemeralSlashCommand
import dev.upcraft.rtuuy.getAppInfo
import dev.upcraft.rtuuy.i18n.Translations

class AboutExtension: Extension() {
	override val name = "about"

	override suspend fun setup() {
		ephemeralSlashCommand {
			name = Translations.Commands.About.name
			description = Translations.Commands.About.description

			action {

				val info = getAppInfo(this@ephemeralSlashCommand.kord)

				respond {
					embed {
						title = Translations.Commands.About.Response.title
							.translateNamed(info)
						description = Translations.Commands.About.Response.text
							.translateNamed(info)
					}
				}
			}
		}
	}
}
