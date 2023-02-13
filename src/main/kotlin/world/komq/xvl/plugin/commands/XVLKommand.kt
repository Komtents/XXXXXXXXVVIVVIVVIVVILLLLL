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

package world.komq.xvl.plugin.commands

import world.komq.xvl.plugin.objects.XVLGameContentManager.plugin
import world.komq.xvl.plugin.objects.XVLGameContentManager.startGame
import world.komq.xvl.plugin.objects.XVLGameContentManager.stopGame
import io.github.monun.kommand.node.LiteralNode
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor

/**
 * @author ContentManager
 */

object XVLKommand {
    fun register(builder: LiteralNode) {
        builder.apply {
            then("start") {
                executes {
                    if (!plugin.config.getBoolean("game-running")) {
                        startGame()
                        sender.sendMessage(text("Game Started."))
                    }
                    else {
                        sender.sendMessage(text("The game has already started!", NamedTextColor.RED))
                    }
                }
            }
            then("stop") {
                executes {
                    if (plugin.config.getBoolean("game-running")) {
                        stopGame()
                        sender.sendMessage(text("Game Stopped."))
                    }
                    else {
                        sender.sendMessage(text("The game has already stopped!", NamedTextColor.RED))
                    }
                }
            }
        }
    }
}