package kr.dorondo.displayHud.core;

import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Collection;

public class BukkitEventListener implements Listener{
    private final kr.dorondo.displayHud.DisplayHud plugin;

    public BukkitEventListener(kr.dorondo.displayHud.DisplayHud plugin) {
        this.plugin = plugin;
    }

    private void runAfter3Ticks(Runnable task) {
        Bukkit.getScheduler().runTaskLater(plugin, task, 3L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTeleport(PlayerTeleportEvent e) {
        Player p = e.getPlayer();
        runAfter3Ticks(() -> {
            Collection<DisplayHud> huds = DisplayHud.getVisibleHuds(p);
            for (DisplayHud hud : huds) {
                hud.teleportTo(p);
            }
            AboveHandTrigger.teleport(p);
            DisplayHud.mountVisibleHuds(p);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getPlayer();
        for (DisplayHud hud : DisplayHud.getVisibleHuds(p)) {
            if(hud.removeWhenPlayerDied){
                if (hud.isGlobalHud()) {
                    GlobalHud<?> globalHud = hud.getGlobalOwner();
                    if (globalHud != null) {
                        globalHud.hide(p);
                    }
                } else {
                    hud.remove();
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        runAfter3Ticks(() -> {
            Collection<DisplayHud> huds = DisplayHud.getVisibleHuds(p);
            for (DisplayHud hud : huds) {
                hud.teleportTo(p);
            }
            AboveHandTrigger.teleport(p);
            DisplayHud.mountVisibleHuds(p);

        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent e) {
        Player p = e.getPlayer();
        runAfter3Ticks(() -> {
            Collection<DisplayHud> huds = DisplayHud.getVisibleHuds(p);
            for (DisplayHud hud : huds) {
                hud.respawnTo(p);
            }
            AboveHandTrigger.respawn(p);
            DisplayHud.mountVisibleHuds(p);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        DisplayHud.clearHuds(e.getPlayer());
        GlobalHud.hideAll(e.getPlayer());
        AboveHandTrigger.forget(e.getPlayer());
    }




}
