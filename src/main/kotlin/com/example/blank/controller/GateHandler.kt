package com.example.blank.controller

import com.example.blank.TgBotApplication.CUR_BOT_ID
import eu.vendeli.tgbot.generated.get
import eu.vendeli.tgbot.generated.set
import com.example.blank.types.ChatUser
import com.example.blank.utils.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.annotations.CommandHandler
import eu.vendeli.tgbot.annotations.UnprocessedHandler
import eu.vendeli.tgbot.annotations.UpdateHandler
import eu.vendeli.tgbot.api.answer.answerCallbackQuery
import eu.vendeli.tgbot.api.chat.banChatMember
import eu.vendeli.tgbot.api.chat.restrictChatMember
import eu.vendeli.tgbot.api.message.deleteMessage
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.User
import eu.vendeli.tgbot.types.chat.Chat
import eu.vendeli.tgbot.types.internal.*
import eu.vendeli.tgbot.types.msg.EntityType
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asDeferred
import kotlinx.datetime.Clock
import org.redisson.Redisson
import org.redisson.api.RLocalCachedMap
import org.redisson.codec.JsonJacksonCodec
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class GateHandler {
    private val redis = Redisson.create().apply {
        config.codec = JsonJacksonCodec(jacksonObjectMapper())
    }

    private val newcomer = redis.getSetCache<ChatUser>("gato_newcomer")
    private val gated: RLocalCachedMap<ChatUser, Int> = redis.localCachedMap<ChatUser, Int>("gated")


    private val keeperCoCtx = object : CoroutineScope {
        override val coroutineContext = Dispatchers.IO + CoroutineName("Keeper")
    }

    private fun getGateNum() = Random(seed = Instant.now().epochSecond).nextInt(1..3)

    @UpdateHandler([UpdateType.MESSAGE])
    suspend fun catchMessages(
        update: MessageUpdate,
        bot: TelegramBot,
    ) {
        val chatUser = ChatUser(userId = update.user.id, chatId = update.message.chat.id)
        when {
            update.message.leftChatMember != null -> {
                deleteMessage(update.message.messageId).send(update.message.chat.id, bot)
            } // remove user left service message

            update.message.newChatMembers != null -> {
                deleteMessage(update.message.messageId).send(update.message.chat.id, bot)
                // remove user join service message

                update.message.newChatMembers?.forEach { user ->
                    if (user.id == CUR_BOT_ID) { // if user's current bot
                        bot.greetAction(update.message.chat)
                        return
                    }
                    val gate = getGateNum()
                    val gateMsg = message {
                        "Please click $gate button to prove that you are not a bot" +
                                " and you will be able to write in chat :)"
                    }.silent().inlineKeyboardMarkup {
                        "1\uFE0F⃣" callback "checkGate?gate=1"
                        "2\uFE0F⃣" callback "checkGate?gate=2"
                        "3\uFE0F⃣" callback "checkGate?gate=3"
                    }.sendAsync(update.message.chat.id, bot)
                        .getOrNull()!!

                    val curChatUser = ChatUser(userId = user.id, chatId = update.message.chat.id)
                    gated[curChatUser] = gate
                    newcomer.add(curChatUser, 1, TimeUnit.DAYS)
                    user["gateMsgId"] = gateMsg.messageId.toString()

                    keeperCoCtx.launch g@{
                        delay(120.seconds)
                        deleteMessage(gateMsg.messageId).send(update.message.chat.id, bot)
                        if (!gated.containsKeyAsync(curChatUser).asDeferred().await()) return@g
                        // if user solved gate quest > break
                        // otherwise ban
                        bot.gatedAction(user, update.message.chat)
                    }
                }
            }

            else -> {
                if (update.userOrNull == null) return
                if (gated.containsKey(chatUser)) {
                    deleteMessage(update.message.messageId).send(update.message.chat.id, bot)
                }

                if (newcomer.contains(chatUser)) update.message.entities?.find {
                    it.type == EntityType.Url || it.type == EntityType.TextLink
                }?.let {
                    deleteMessage(update.message.messageId).send(update.message.chat.id, bot)
                    message {
                        "Sorry for the inconvenience newcomers can't post links on the first day, have a good day sir :)"
                    }.silent().sendAnd(update.message.chat.id, bot) {
                        keeperCoCtx.launch g@{
                            delay(10.seconds)
                            deleteMessage(messageId).send(update.message.chat.id, bot)
                        }
                    }
                }

            }

        }
    }

    @UpdateHandler([UpdateType.MESSAGE_REACTION]) // ban reaction spammers
    suspend fun reactionsHandler(
        update: MessageReactionUpdate,
        bot: TelegramBot,
    ) {
        val anchor = update.user?.let { ChatUser(userId = it.id, chatId = update.messageReaction.chat.id) } ?: return
        if (gated[anchor] != null) bot.ban(anchor.userId, anchor.chatId)
    }

    @CommandHandler.CallbackQuery(["checkGate"])
    suspend fun check(
        gate: Int,
        upd: CallbackQueryUpdate,
        user: User,
        bot: TelegramBot,
    ) {
        val chatUser = ChatUser(
            userId = user.id,
            chatId = upd.callbackQuery.message!!
                .chat.id,
        )
        val gateNumber = gated[chatUser] ?: return
        if (gate != gateNumber) {
            answerCallbackQuery(upd.callbackQuery.id)
                .options {
                    text = "Nop :p"
                    showAlert = true
                }.send(user, bot)

            return
        }

        answerCallbackQuery(upd.callbackQuery.id)
            .options {
                text = "Welcome!"
                showAlert = true
            }.send(user, bot)

        user["gateMsgId"]?.toLongOrNull()?.let {
            deleteMessage(it).send(upd.callbackQuery.message!!.chat.id, bot)
        }
        gated.remove(chatUser)
    }

    @UnprocessedHandler
    fun noop() {
    }

    suspend fun TelegramBot.greetAction(chat: Chat) {
        message {
            "Hi everyone, I will now be on the guard for this chat :)"
        }.silent().send(chat, this)
    }

    suspend fun TelegramBot.gatedAction(user: User, chat: Chat) {
        message(
            "\uD83D\uDEA8 User #${user.id} restricted for an hour!"
        ).silent().send(chat.id, this)

        restrictChatMember(
            user,
            RESTRICTED_PERMISSIONS,
            Clock.System.now().plus(1.hours)
        ).send(chat, this)
    }

    suspend fun TelegramBot.ban(userId: Long, chatId: Long) {
        banChatMember(userId).send(chatId, this)
        message(
            "\uD83D\uDEA8 User #${userId} BANNED because " +
                    "reactions before gate. \uD83D\uDEA8"
        ).silent().send(chatId, this)
    }
}