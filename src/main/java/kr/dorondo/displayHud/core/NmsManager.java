package kr.dorondo.displayHud.core;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

public final class NmsManager {
    private static final String CHANNEL_HANDLER_NAME = "display_hud";
    private static final Set<ClientboundSetPassengersPacket> manualMountPackets =
            Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()));
    private static Constructor<ClientboundSetPassengersPacket> passengersBufferConstructor;
    private static Field passengersField;
    private static Field vehicleField;

    private NmsManager() {
    }

    public static void load(JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        preparePassengerPacketAccess();
        for (Player player : Bukkit.getOnlinePlayers()) {
            inject(player);
        }
    }

    public static void unload() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            uninject(player);
        }
        manualMountPackets.clear();
    }

    public static void inject(Player player) {
        Objects.requireNonNull(player, "player");
        Channel channel = getChannel(player);
        if (channel.pipeline().get(CHANNEL_HANDLER_NAME) != null) return;
        if (channel.pipeline().get("packet_handler") == null) return;
        channel.pipeline().addBefore("packet_handler", CHANNEL_HANDLER_NAME, new MountListener.Handler(player.getUniqueId()));
    }

    public static void uninject(Player player) {
        Objects.requireNonNull(player, "player");
        Channel channel = getChannel(player);
        if (channel.pipeline().get(CHANNEL_HANDLER_NAME) != null) {
            channel.pipeline().remove(CHANNEL_HANDLER_NAME);
        }
    }

    public static Channel getChannel(Player player) {
        Objects.requireNonNull(player, "player");
        return ((CraftPlayer) player).getHandle().connection.connection.channel;
    }

    public static void spawn(Player player, Display display, UUID uuid, Location location) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(display, "display");
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(location, "location");

        send(player, new ClientboundAddEntityPacket(
                display.getId(),
                uuid,
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getPitch(),
                location.getYaw(),
                display.getType(),
                0,
                Vec3.ZERO,
                location.getYaw()
        ));
    }

    public static void remove(Player player, int entityId) {
        Objects.requireNonNull(player, "player");
        send(player, new ClientboundRemoveEntitiesPacket(entityId));
    }

    public static void teleport(Player player, int entityId, Location location) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(location, "location");
        PositionMoveRotation change = new PositionMoveRotation(
                new Vec3(location.getX(), location.getY(), location.getZ()),
                Vec3.ZERO,
                location.getYaw(),
                location.getPitch()
        );
        send(player, ClientboundTeleportEntityPacket.teleport(entityId, change, Set.of(), false));
    }

    public static void update(Player player, int entityId, java.util.List<SynchedEntityData.DataValue<?>> metadata) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(metadata, "metadata");
        if (metadata.isEmpty()) return;
        send(player, new ClientboundSetEntityDataPacket(entityId, metadata));
    }

    public static void updateMount(Player player) {
        updateMount(player, new int[0]);
    }

    static void updateMount(Player player, int[] packetPassengers) {
        Objects.requireNonNull(player, "player");
        int[] passengers = mergePassengerIds(player, packetPassengers);
        ClientboundSetPassengersPacket packet = createSetPassengersPacket(player.getEntityId(), passengers);
        manualMountPackets.add(packet);
        send(player, packet);
    }

    static boolean handlePassengerPacket(Player player, ClientboundSetPassengersPacket packet) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(packet, "packet");
        if (manualMountPackets.remove(packet)) {
            return false;
        }
        if (packet.getVehicle() != player.getEntityId()) {
            return false;
        }
        if (DisplayHud.getVisibleHudIds(player).length == 0) {
            return false;
        }
        updateMount(player, packet.getPassengers());
        return true;
    }

    private static void send(Player player, Packet<?> packet) {
        if (!player.isOnline()) return;
        ((CraftPlayer) player).getHandle().connection.send(packet);
    }

    private static int[] mergePassengerIds(Player player, int[] packetPassengers) {
        int[] bukkitPassengers = player.getPassengers().stream()
                .mapToInt(Entity::getEntityId)
                .toArray();
        int[] hudPassengers = DisplayHud.getVisibleHudIds(player);
        return IntStream.concat(
                        Arrays.stream(packetPassengers),
                        IntStream.concat(Arrays.stream(bukkitPassengers), Arrays.stream(hudPassengers))
                )
                .distinct()
                .toArray();
    }

    private static ClientboundSetPassengersPacket createSetPassengersPacket(int vehicleEntityId, int[] passengers) {
        ClientboundSetPassengersPacket packet = createSetPassengersPacketFromBuffer(vehicleEntityId, passengers);
        if (packet != null) {
            return packet;
        }

        Player owner = Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.getEntityId() == vehicleEntityId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Cannot create passengers packet for entity " + vehicleEntityId));
        packet = new ClientboundSetPassengersPacket(((CraftPlayer) owner).getHandle());
        try {
            if (vehicleField != null) {
                vehicleField.setInt(packet, vehicleEntityId);
            }
            if (passengersField != null) {
                passengersField.set(packet, passengers);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot write passengers packet fields", e);
        }
        return packet;
    }

    private static ClientboundSetPassengersPacket createSetPassengersPacketFromBuffer(int vehicleEntityId, int[] passengers) {
        if (passengersBufferConstructor == null) {
            return null;
        }
        try {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            buffer.writeVarInt(vehicleEntityId);
            buffer.writeVarIntArray(passengers);
            return passengersBufferConstructor.newInstance(buffer);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static void preparePassengerPacketAccess() {
        try {
            passengersBufferConstructor = ClientboundSetPassengersPacket.class.getDeclaredConstructor(FriendlyByteBuf.class);
            passengersBufferConstructor.setAccessible(true);
        } catch (ReflectiveOperationException ignored) {
            passengersBufferConstructor = null;
        }

        for (Field field : ClientboundSetPassengersPacket.class.getDeclaredFields()) {
            if (field.getType() == int[].class) {
                field.setAccessible(true);
                passengersField = field;
            } else if (field.getType() == int.class) {
                field.setAccessible(true);
                vehicleField = field;
            }
        }
    }
}
