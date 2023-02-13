/*
 * Copyright (c) 2021 Komtents Dev Team
 *
 *  Licensed under the General Public License, Version 3.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/gpl-3.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package world.komq.xvl.plugin

import world.komq.xvl.plugin.commands.XVLKommand.register
import world.komq.xvl.plugin.config.XVLConfig.load
import world.komq.xvl.plugin.events.XVLGameEvent
import world.komq.xvl.plugin.events.XVLMotdEvent
import world.komq.xvl.plugin.objects.XVLGameContentManager.gameEvent
import world.komq.xvl.plugin.objects.XVLGameContentManager.motdEvent
import world.komq.xvl.plugin.objects.XVLGameContentManager.startGame
import world.komq.xvl.plugin.objects.XVLGameContentManager.thirstValue
import world.komq.xvl.plugin.tasks.XVLConfigReloadTask
import io.github.monun.kommand.kommand
import net.kyori.adventure.text.Component.text
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

/**
 * @author ContentManager
 */

class XVLPluginMain : JavaPlugin() {

    companion object {
        lateinit var instance: XVLPluginMain
            private set
    }

    private val configFile = File(dataFolder, "config.yml")

    override fun onEnable() {
        instance = this
        gameEvent = XVLGameEvent()
        motdEvent = XVLMotdEvent()

        server.pluginManager.registerEvents(motdEvent, this)
        server.scheduler.runTaskTimer(this, XVLConfigReloadTask(), 0L, 20L)

        load(configFile)

        kommand {
            register("xvl") {
                requires { isOp }
                executes {
                    sender.sendMessage(text("XVL 0.0.2\nA Rechallenge for broken world."))
                }
                register(this)
            }
        }

        if (this.config.getBoolean("game-running")) {
            startGame()
        }
    }

    override fun onDisable() {
        server.onlinePlayers.forEach {
            config.set("${it.name}.death", it.scoreboard.getObjective("Death")?.getScore(it.name)?.score)
            config.set("${it.name}.thirstvalue", it.thirstValue)
            saveConfig()
        }
    }
}