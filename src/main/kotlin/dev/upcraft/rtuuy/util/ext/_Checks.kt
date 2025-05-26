package dev.upcraft.rtuuy.util.ext

import dev.kord.common.entity.MessageType
import dev.kord.core.event.guild.BanAddEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kordex.core.checks.types.CheckContext
import dev.kordex.core.checks.userFor
import dev.upcraft.rtuuy.extensions.ban_sync.BanEntry
import dev.upcraft.rtuuy.i18n.Translations

suspend fun CheckContext<MessageCreateEvent>.isNotJoinMessage() {
	if (!passed) {
		return
	}

	failIf(Translations.Checks.IsNotJoin.failed) { event.message.type == MessageType.UserJoin }
}

suspend fun CheckContext<*>.hasUser() {
	if (!passed) {
		return
	}

	failIf(Translations.Checks.IsUser.failed) { userFor(event) == null }
}

suspend fun CheckContext<BanAddEvent>.isNotSyncedBan() {
	if (!passed) {
		return
	}

	failIf(Translations.Checks.IsNotSyncedBan.failed) { event.getBanOrNull()?.reason?.startsWith(BanEntry.SYNCED_PREFIX) ?: false }
}
