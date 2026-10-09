package kr.dorondo.displayHud.core;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

final class HudRegistry {
    private static final Map<Player, Map<String, DisplayHud>> personalHuds = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<String, GlobalHud<? extends DisplayHud>> globalHuds = new ConcurrentHashMap<>();

    private HudRegistry() {
    }

    static DisplayHud getPersonalHud(Player player, String id) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(id, "id");
        synchronized (personalHuds) {
            Map<String, DisplayHud> huds = personalHuds.get(player);
            if (huds == null) {
                return null;
            }
            return huds.get(id);
        }
    }

    static Map<String, DisplayHud> getPersonalHuds(Player player) {
        Objects.requireNonNull(player, "player");
        synchronized (personalHuds) {
            Map<String, DisplayHud> huds = personalHuds.get(player);
            if (huds == null || huds.isEmpty()) {
                return Collections.emptyMap();
            }
            return new LinkedHashMap<>(huds);
        }
    }

    static boolean registerPersonalHud(Player player, String id, DisplayHud hud) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(hud, "hud");
        synchronized (personalHuds) {
            Map<String, DisplayHud> huds = personalHuds.computeIfAbsent(player, v -> new LinkedHashMap<>());
            if (huds.containsKey(id)) {
                return false;
            }
            huds.put(id, hud);
            return true;
        }
    }

    static void unregisterPersonalHud(Player player, String id, DisplayHud hud) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(id, "id");
        synchronized (personalHuds) {
            Map<String, DisplayHud> huds = personalHuds.get(player);
            if (huds == null) return;
            if (huds.get(id) == hud) {
                huds.remove(id);
            }
            if (huds.isEmpty()) {
                personalHuds.remove(player);
            }
        }
    }

    static void clearPersonalHuds(Player player) {
        Objects.requireNonNull(player, "player");
        List<DisplayHud> huds;
        synchronized (personalHuds) {
            Map<String, DisplayHud> removed = personalHuds.remove(player);
            if (removed == null || removed.isEmpty()) {
                return;
            }
            huds = new ArrayList<>(removed.values());
        }
        for (DisplayHud hud : huds) {
            hud.remove();
        }
    }

    static boolean registerGlobalHud(String id, GlobalHud<? extends DisplayHud> hud) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(hud, "hud");
        return globalHuds.putIfAbsent(id, hud) == null;
    }

    static void unregisterGlobalHud(String id, GlobalHud<? extends DisplayHud> hud) {
        Objects.requireNonNull(id, "id");
        globalHuds.remove(id, hud);
    }

    static GlobalHud<? extends DisplayHud> getGlobalHud(String id) {
        Objects.requireNonNull(id, "id");
        return globalHuds.get(id);
    }

    static Map<String, GlobalHud<? extends DisplayHud>> getGlobalHuds() {
        return new LinkedHashMap<>(globalHuds);
    }

    static Collection<DisplayHud> getVisibleHuds(Player player) {
        Objects.requireNonNull(player, "player");
        List<DisplayHud> huds = new ArrayList<>(getPersonalHuds(player).values());
        for (GlobalHud<? extends DisplayHud> globalHud : globalHuds.values()) {
            if (globalHud.isShown(player)) {
                huds.add(globalHud.getHud());
            }
        }
        return huds;
    }

    static Collection<GlobalHud<? extends DisplayHud>> getVisibleGlobalHuds(Player player) {
        Objects.requireNonNull(player, "player");
        List<GlobalHud<? extends DisplayHud>> huds = new ArrayList<>();
        for (GlobalHud<? extends DisplayHud> globalHud : globalHuds.values()) {
            if (globalHud.isShown(player)) {
                huds.add(globalHud);
            }
        }
        return huds;
    }

    static int[] getVisibleHudIds(Player player) {
        List<Integer> ids = new ArrayList<>();
        for (DisplayHud hud : getVisibleHuds(player)) {
            if (hud.getNMSid() != null) ids.add(hud.getNMSid());
        }
        // 손 위 표시 트리거(레지스트리 밖)도 플레이어에게 태운다
        Integer trigger = AboveHandTrigger.triggerId(player);
        if (trigger != null) ids.add(trigger);
        return ids.stream().mapToInt(Integer::intValue).toArray();
    }

    static void hideGlobalHuds(Player player) {
        for (GlobalHud<? extends DisplayHud> hud : getVisibleGlobalHuds(player)) {
            hud.hide(player);
        }
    }
}
