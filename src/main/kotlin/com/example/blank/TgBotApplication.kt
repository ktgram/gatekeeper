package com.example.blank

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.api.botactions.getMe
import eu.vendeli.tgbot.types.component.getOrNull
import kotlinx.coroutines.runBlocking

object TgBotApplication {
    private val bot = TelegramBot("BOT_TOKEN")

    val CUR_BOT_ID: Long by lazy {
        runBlocking {
            getMe().sendReturning(bot).getOrNull()!!.id
        }
    }

    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        println("Current bot id: $CUR_BOT_ID")

        bot.handleUpdates()
    }
}
