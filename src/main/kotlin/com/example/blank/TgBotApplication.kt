package com.example.blank

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.api.botactions.getMe
import eu.vendeli.tgbot.types.internal.getOrNull
import kotlinx.coroutines.runBlocking

object TgBotApplication {
    private val bot = TelegramBot("BOT_TOKEN")

    val CUR_BOT_ID: Long by lazy {
        runBlocking {
            getMe().sendAsync(bot).getOrNull()!!.id
        }
    }

    @JvmStatic
    suspend fun main(args: Array<String>) {
        println("Current bot id: $CUR_BOT_ID")

        bot.handleUpdates()
    }
}
