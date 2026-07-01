package kr.dorondo.displayHud.core;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public final class MountListener implements Listener {
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        NmsManager.inject(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        NmsManager.uninject(event.getPlayer());
    }

    static final class Handler extends ChannelDuplexHandler {
        private final UUID playerUuid;

        Handler(UUID playerUuid) {
            this.playerUuid = playerUuid;
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (msg instanceof ClientboundSetPassengersPacket packet) {
                Player player = Bukkit.getPlayer(playerUuid);
                if (player != null) {
                    super.write(ctx, msg, promise);
                    NmsManager.handlePassengerPacket(player, packet);
                    return;
                }
            }
            super.write(ctx, msg, promise);
        }
    }
}
