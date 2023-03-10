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

package world.komq.xvl.plugin.events

import world.komq.xvl.plugin.enums.DecreaseReason
import world.komq.xvl.plugin.objects.XVLGameContentManager.currentDate
import world.komq.xvl.plugin.objects.XVLGameContentManager.ending
import world.komq.xvl.plugin.objects.XVLGameContentManager.gameTaskId
import world.komq.xvl.plugin.objects.XVLGameContentManager.injured
import world.komq.xvl.plugin.objects.XVLGameContentManager.manageFlags
import world.komq.xvl.plugin.objects.XVLGameContentManager.motd
import world.komq.xvl.plugin.objects.XVLGameContentManager.plugin
import world.komq.xvl.plugin.objects.XVLGameContentManager.respawnDelay
import world.komq.xvl.plugin.objects.XVLGameContentManager.respawnTaskId
import world.komq.xvl.plugin.objects.XVLGameContentManager.server
import world.komq.xvl.plugin.objects.XVLGameContentManager.thirstValue
import com.destroystokyo.paper.event.server.PaperServerListPingEvent
import io.github.monun.tap.effect.playFirework
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Color
import org.bukkit.FireworkEffect
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.entity.WitherSkeleton
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.*
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import kotlin.random.Random.Default.nextInt

/**
 * @author ContentManager
 */

class XVLGameEvent : Listener {
    private fun decreaseThirst(player: Player, decreaseReason: DecreaseReason) {
        if (player.thirstValue < 600) {
            player.removePotionEffect(PotionEffectType.SLOW)
        } else if (player.thirstValue < 3600) {
            player.removePotionEffect(PotionEffectType.SLOW)
            player.addPotionEffect(PotionEffect(PotionEffectType.SLOW, 1000000, 0, true, false))
        } else if (player.thirstValue < 7200) {
            player.removePotionEffect(PotionEffectType.CONFUSION)
            player.removePotionEffect(PotionEffectType.SLOW)
            player.addPotionEffect(PotionEffect(PotionEffectType.SLOW, 1000000, 2, true, false))
        }

        if (decreaseReason == DecreaseReason.POTION) {
            player.thirstValue = 0.coerceAtLeast(player.thirstValue - 300)
        }
        if (decreaseReason == DecreaseReason.MILK) {
            player.thirstValue = 0.coerceAtLeast(player.thirstValue - 150)
        }
    }

    private fun fallInjured(p: Player) {
        p.sendMessage(text("?????????????????? ????????? ??????????????????! ???.. ????????? ?????? ?????? ?????????.", NamedTextColor.GRAY).decorate(TextDecoration.ITALIC))
        p.addPotionEffect(PotionEffect(PotionEffectType.CONFUSION, 20 * 90, 0, true, false))
        injured[p.uniqueId] = true

        server.scheduler.runTaskLater(plugin, Runnable { injured[p.uniqueId] = false }, 20 * 90L)
    }

    @EventHandler
    fun onEntityDamage(e: EntityDamageEvent) {
        val entity = e.entity

        if (entity is Player) {
            if (e.cause == EntityDamageEvent.DamageCause.FALL) {
                if (respawnDelay[entity.uniqueId] == false) {
                    if (entity.fallDistance >= 23) {
                        when (nextInt(10)) {
                            0, 1 -> fallInjured(entity)
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    fun onPlayerRespawn(e: PlayerRespawnEvent) {
        val p = e.player

        if (respawnTaskId != 0) {
            server.scheduler.cancelTask(respawnTaskId)
        }
        respawnDelay[p.uniqueId] = true
        p.sendMessage(text("10????????? ???????????? ??? ????????????!", NamedTextColor.RED))
    }

    @EventHandler
    fun onPlayerMove(e: PlayerMoveEvent) {
        val p = e.player

        if (injured[p.uniqueId] == true) {
            e.isCancelled = true
            p.sendMessage(text("???????????? ??????????????? ?????? ???????????? ??? ????????????!", NamedTextColor.RED).decorate(TextDecoration.ITALIC))
        }

        if (respawnDelay[p.uniqueId] == true) {
            e.isCancelled = true
            val respawnTask = server.scheduler.runTaskLater(plugin, Runnable {
                respawnDelay[p.uniqueId] = false
            }, 20 * 10L)

            respawnTaskId = respawnTask.taskId
        }
    }

    // No Damage Ticks to 0
    @EventHandler
    fun onPlayerJoin(e: PlayerJoinEvent) {
        val p = e.player

        p.thirstValue = plugin.config.getInt("${p.name}.thirstvalue")

        e.joinMessage(null)
        p.noDamageTicks = 0
    }

    @EventHandler
    fun onPlayerQuit(e: PlayerQuitEvent) {
        val p = e.player
        e.quitMessage(null)
        plugin.config.set("${p.name}.thirstvalue", p.thirstValue)
        plugin.saveConfig()

        manageFlags(FreezingFlag = false, ThirstyFlag = false, WarmBiomeFlag = false, NetherBiomeFlag = false)
    }

    // Nerfed from original LVX; Returning damage is set to 1.5x, which is lower than original.
    @EventHandler
    fun onEntityDamageByEntityEvent(e: EntityDamageByEntityEvent) {
        val dmgr = e.damager
//
//        if (dmgr is Projectile) {
//            if (dmgr.shooter is Player) {
//                (dmgr.shooter as Player).damage((dmg * 1.5))
//            }
//            else if (dmgr.shooter is Monster) {
//                e.damage = e.damage * 1.5
//            }
//        }
//
//        if (dmgr is Player) {
//            dmgr.damage(dmg * 1.5)
//        }
//
//        if (dmgr is Monster) {
//            e.damage = e.damage * 1.5
//        }
        if (dmgr is WitherSkeleton) {
            e.damage = e.damage * 1.5
        }
    }
    // Suggestion accepted; no JagangDucheon.

    // Bed Event
    @EventHandler
    fun onPlayerBedLeave(e: PlayerBedLeaveEvent) {
        val p = e.player

        if (p.world.time == 0L) {
            when (nextInt(7)) {
                0 -> {
                    p.sendMessage(text("?????? ?????? ??? ?????? ???????????? ?????? ???????????????! ?????? ??? ??? ??? ???????????????...? (?????? II 3???, ??? II 30???)", NamedTextColor.GRAY).decorate(TextDecoration.ITALIC))
                    p.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 20 * 180, 1, true, false))
                    p.addPotionEffect(PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 30, 1, true, false))
                }
                1 -> {
                    p.sendMessage(text("????????? ?????? ????????? ???????????? ?????? ????????????... (?????? 2???)", NamedTextColor.GRAY).decorate(TextDecoration.ITALIC))
                    p.addPotionEffect(PotionEffect(PotionEffectType.SLOW, 20 * 120, 0, true, false))
                }
                2 -> {
                    p.sendMessage(text("????????? ???????????????. ????????? ?????????????????? ?????? ?????? ????????? ?????????????????????. (?????? II 3???, ???????????? 1???)", NamedTextColor.GRAY).decorate(TextDecoration.ITALIC))
                    p.addPotionEffect(PotionEffect(PotionEffectType.SLOW, 20 * 180, 1, true, false))
                    p.addPotionEffect(PotionEffect(PotionEffectType.SLOW_DIGGING, 20 * 60, 0, true, false))
                }
                3 -> {
                    p.sendMessage(text("???????????? ???????????????. ???????????? ?????? ??? ??? ????????? ?????????. (?????? 1??? 30???)", NamedTextColor.GRAY).decorate(TextDecoration.ITALIC))
                    p.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 20 * 90, 0, true, false))
                }
                4 -> {
                    p.sendMessage(text("????????????(????????????)??? ??????????????????. ????????? ???????????? ??? ???????????? ???????????????. (?????? II 1???, ???????????? 40???)", NamedTextColor.GRAY).decorate(TextDecoration.ITALIC))
                    p.addPotionEffect(PotionEffect(PotionEffectType.SLOW, 20 * 60, 1, true, false))
                    p.addPotionEffect(PotionEffect(PotionEffectType.SLOW_DIGGING, 20 * 40, 0, true, false))
                }
                5 -> {
                    p.sendMessage(text("?????? ????????? ?????? ??????????????????...? ???????????? ???????????????????????????.. (?????? 1???)", NamedTextColor.GRAY).decorate(TextDecoration.ITALIC))
                    p.addPotionEffect(PotionEffect(PotionEffectType.CONFUSION, 20 * 60, 0, true, false))
                }
                6 -> {
                    p.sendMessage(text("????????? ?????? ????????? ?????? ????????????! ?????? ????????? ????????? ????????????????????????? (?????? II 3???, ??? II 15???, ????????? 2???)", NamedTextColor.GRAY).decorate(TextDecoration.ITALIC))
                    p.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 20 * 180, 1, true, false))
                    p.addPotionEffect(PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 15, 1, true, false))
                    p.addPotionEffect(PotionEffect(PotionEffectType.FAST_DIGGING, 20 * 120, 0, true, false))
                }
            }
        }
    }

    // Milk Event, Decrease Thirst
    @EventHandler
    fun onPlayerItemConsume(e: PlayerItemConsumeEvent) {
        val p = e.player
        val type = e.item.type

        if (type == Material.MILK_BUCKET) {
            when (nextInt(3)) {
                0 -> {
                    p.sendMessage(text("????????? ????????? ?????? ??? ????????????. (?????? ?????? ??????)", NamedTextColor.GRAY).decorate(TextDecoration.ITALIC))
                }
                1 -> {
                    p.sendMessage(text("????????? ?????? ??????, ????????? ?????? ????????????, ??????????????? ????????? ?????? ?????? ????????? ????????? ????????????... (??? 10???)", NamedTextColor.GRAY).decorate(TextDecoration.ITALIC))
                    server.scheduler.runTaskLater(plugin, Runnable {
                        p.addPotionEffect(PotionEffect(PotionEffectType.POISON, 200, 0, true, false))
                    }, 10L)
                }
                2 -> {
                    p.sendMessage(text("???????????? ????????? ?????? ???????????? ?????? ??? ?????????... (?????? 15???)", NamedTextColor.GRAY).decorate(TextDecoration.ITALIC))
                    server.scheduler.runTaskLater(plugin, Runnable {
                        p.addPotionEffect(PotionEffect(PotionEffectType.CONFUSION, 20 * 15, 0, true, false))
                    }, 10L)
                }
            }
            decreaseThirst(p, DecreaseReason.MILK)
        }
        if (e.item.type == Material.POTION) {
            decreaseThirst(p, DecreaseReason.POTION)
        }
    }

    // Check ending conditions
    @EventHandler
    fun onPlayerAdvancementDone(e: PlayerAdvancementDoneEvent) {
        val firework = FireworkEffect.builder().with(FireworkEffect.Type.STAR).withColor(Color.AQUA).build()
        val advc = e.advancement

        if (advc.key.toString() == "minecraft:end/kill_dragon") {
            plugin.config.set("kill-dragon", true)
            plugin.saveConfig()
        }
        if (advc.key.toString() == "minecraft:nether/create_beacon") {
            if (plugin.config.getBoolean("kill-dragon")) {
                server.onlinePlayers.forEach {
                    val loc = it.location.add(0.0, 0.9, 0.0)

                    loc.world.playFirework(loc, firework)
                }
                server.scheduler.cancelTask(gameTaskId)
                ending = true
            }
        }
    }

    @EventHandler
    fun onPaperServerListPing(e: PaperServerListPingEvent) {
        // Project start date; it has been planned earlier, but I forgot to set up the Wakatime & in this date I actually started writing in-game managing codes.

        e.numPlayers = 20211122
        e.maxPlayers = currentDate()
        e.playerSample.clear()
        e.motd(motd())
    }
}