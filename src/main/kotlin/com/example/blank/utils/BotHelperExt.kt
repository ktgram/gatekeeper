package com.example.blank.utils

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.interfaces.action.Action
import eu.vendeli.tgbot.interfaces.action.TgAction
import eu.vendeli.tgbot.interfaces.features.OptionsFeature
import eu.vendeli.tgbot.types.chat.ChatPermissions
import eu.vendeli.tgbot.types.internal.getOrNull
import eu.vendeli.tgbot.types.internal.options.Options
import eu.vendeli.tgbot.types.internal.options.OptionsCommon

val RESTRICTED_PERMISSIONS = ChatPermissions(
    canSendMessages = false,
    canSendAudios = false,
    canSendDocuments = false,
    canSendPhotos = false,
    canSendVideos = false,
    canSendVideoNotes = false,
    canSendVoiceNotes = false,
    canSendPolls = false,
    canSendOtherMessages = false,
    canAddWebPagePreviews = false,
    canChangeInfo = false,
    canInviteUsers = false,
    canPinMessages = false,
    canManageTopics = false,
)


suspend fun <R> Action<R>.sendAnd(to: Long, bot: TelegramBot, block: suspend R.() -> Unit) {
    val result = sendReturning(to, bot).getOrNull() ?: error("Failed to process response")
    block(result)
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T, R, O> T.silent(crossinline block: O.() -> Unit = {}): T
        where               T : TgAction<R>,
                            T : OptionsFeature<T, O>,
                            O : Options,
                            O : OptionsCommon =
    options {
        disableNotification = true
        block()
    }
