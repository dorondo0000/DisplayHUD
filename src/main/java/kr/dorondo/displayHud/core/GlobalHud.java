package kr.dorondo.displayHud.core;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GlobalHud<T extends DisplayHud> {
    private final String id;
    private final T hud;
    private final Set<Player> players = ConcurrentHashMap.newKeySet();
    private boolean removed = false;

    public GlobalHud(String id, T hud) {
        this(id, hud, UUID.randomUUID());
    }

    public GlobalHud(T hud, String id) {
        this(id, hud);
    }

    public GlobalHud(String id, T hud, UUID uuid) {
        this.id = Objects.requireNonNull(id, "id");
        this.hud = Objects.requireNonNull(hud, "hud");
        Objects.requireNonNull(uuid, "uuid");
        if (!HudRegistry.registerGlobalHud(id, this)) {
            throw new IllegalArgumentException("GlobalHud id already exists: " + id);
        }
        if (!hud.attachGlobal(this, id, uuid)) {
            HudRegistry.unregisterGlobalHud(id, this);
            throw new IllegalStateException("Hud is already spawned or attached");
        }
    }

    public static GlobalHud<TextDisplayHud> text(String id) {
        return new GlobalHud<>(id, new TextDisplayHud());
    }

    public static GlobalHud<ItemDisplayHud> item(String id) {
        return new GlobalHud<>(id, new ItemDisplayHud());
    }

    public static GlobalHud<BlockDisplayHud> block(String id) {
        return new GlobalHud<>(id, new BlockDisplayHud());
    }

    public static <T extends DisplayHud> GlobalHud<T> create(String id, T hud) {
        return new GlobalHud<>(id, hud);
    }

    public static GlobalHud<? extends DisplayHud> getHud(String id) {
        return HudRegistry.getGlobalHud(id);
    }

    public static Map<String, GlobalHud<? extends DisplayHud>> getHuds() {
        return new LinkedHashMap<>(HudRegistry.getGlobalHuds());
    }

    public static void removeHud(String id) {
        GlobalHud<? extends DisplayHud> hud = getHud(id);
        if (hud != null) {
            hud.remove();
        }
    }

    public static void hideAll(Player player) {
        HudRegistry.hideGlobalHuds(player);
    }

    public boolean show(Player player) {
        Objects.requireNonNull(player, "player");
        if (removed) return false;
        if (!players.add(player)) {
            return false;
        }
        if (!hud.showTo(player)) {
            players.remove(player);
            return false;
        }
        return true;
    }

    public int show(Player... players) {
        Objects.requireNonNull(players, "players");
        int changed = 0;
        for (Player player : players) {
            if (show(player)) {
                changed++;
            }
        }
        return changed;
    }

    public int show(Collection<? extends Player> players) {
        return showAll(players);
    }

    public int show(Iterable<? extends Player> players) {
        return showAll(players);
    }

    public boolean hide(Player player) {
        Objects.requireNonNull(player, "player");
        if (!players.remove(player)) {
            return false;
        }
        hud.hideFrom(player);
        return true;
    }

    public int hide(Player... players) {
        Objects.requireNonNull(players, "players");
        int changed = 0;
        for (Player player : players) {
            if (hide(player)) {
                changed++;
            }
        }
        return changed;
    }

    public int hide(Collection<? extends Player> players) {
        return hideAll(players);
    }

    public int hide(Iterable<? extends Player> players) {
        return hideAll(players);
    }

    public void remove() {
        if (removed) return;
        removed = true;
        for (Player player : getPlayers()) {
            hide(player);
        }
        hud.removeGlobal();
        HudRegistry.unregisterGlobalHud(id, this);
    }

    public boolean isShown(Player player) {
        Objects.requireNonNull(player, "player");
        return players.contains(player);
    }

    public String getId() {
        return id;
    }

    public T getHud() {
        return hud;
    }

    public Collection<Player> getPlayers() {
        return Set.copyOf(players);
    }

    private int showAll(Iterable<? extends Player> players) {
        Objects.requireNonNull(players, "players");
        int changed = 0;
        for (Player player : players) {
            if (show(player)) {
                changed++;
            }
        }
        return changed;
    }

    private int hideAll(Iterable<? extends Player> players) {
        Objects.requireNonNull(players, "players");
        int changed = 0;
        for (Player player : players) {
            if (hide(player)) {
                changed++;
            }
        }
        return changed;
    }
}
