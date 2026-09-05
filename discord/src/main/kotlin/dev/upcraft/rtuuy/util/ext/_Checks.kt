package dev.upcraft.rtuuy.util.ext

import dev.kord.common.entity.MessageType
import dev.kord.core.event.guild.BanAddEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kordex.core.checks.guildFor
import dev.kordex.core.checks.memberFor
import dev.kordex.core.checks.types.CheckContext
import dev.kordex.core.checks.userFor
import dev.kordex.core.utils.isNullOrBot
import dev.kordex.core.utils.repliedMessageOrNull
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

	failIf(Translations.Checks.IsNotSyncedBan.failed) {
		event.getBanOrNull()?.reason?.startsWith(BanEntry.SYNCED_PREFIX) ?: false
	}
}

suspend fun CheckContext<MessageCreateEvent>.hasReplyPing() {
	if (!passed) {
		return
	}

	val parentMessage = event.message.repliedMessageOrNull()
	if (parentMessage == null) {
		return fail(Translations.Checks.HasReplyPing.notReply)
	}

	val parentAuthor = parentMessage.author
	if (parentAuthor.isNullOrBot()) {
		return fail(Translations.Checks.HasReplyPing.notPingable)
	}

	if (!event.message.mentionedUserIds.contains(parentAuthor.id)) {
		return fail(Translations.Checks.HasReplyPing.noPing)
	}

	// FIXME figure out a better way to see if the message would actually ping the user
	failIf(Translations.Checks.HasReplyPing.regularPing) { event.message.content.contains(parentAuthor.mention) }
}

suspend fun CheckContext<*>.isNotGuildOwner() {
	if (!passed) {
		return
	}

	memberFor(event)?.let { member ->
		failIf(Translations.Checks.IsNotGuildOwner.failed) { guildFor(event)?.asGuild()?.owner == member }
	}
}
