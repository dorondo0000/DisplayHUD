package kr.dorondo.displayHud.core;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 손 위 표시(above hand) 트리거. 플레이어마다 하나, 그 플레이어가 손 위 표시 HUD 를 하나라도 볼 때만 존재한다.
 * <p>
 * 손 위 표시는 리소스팩의 발광 후처리(post_effect/entity_outline.json)가 손을 그리기 전 화면을 복사해 두었다가
 * 손을 그린 뒤 다시 덮는 방식이다. 그런데 클라이언트는 이 후처리를 "발광하는 무언가가 있는 프레임"에만 돌린다
 * (1.21.8 ~ 26.1: 화면에 보이는 발광 엔티티가 있을 때, 26.2+: 발광 외곽선 제출이 있을 때).
 * 그래서 발광하는(색 0x01FEFD) 투명 아이템 디스플레이 HUD 를 하나 태워 둔다. 모델(displayhud:above_hand_trigger)은
 * 완전히 투명한 텍스처라 화면에도, 발광 버퍼에도 아무것도 찍히지 않고, 발광 "제출"만 생긴다.
 * <p>
 * 레지스트리(getHuds / getVisibleHuds)에는 나오지 않는다. 탑승 목록(getVisibleHudIds)에만 들어간다.
 */
final class AboveHandTrigger {
    /** 리소스팩 assets/displayhud/items/above_hand_trigger.json */
    static final String ITEM_MODEL = "displayhud:above_hand_trigger";
    private static final Map<UUID, ItemDisplayHud> triggers = new ConcurrentHashMap<>();

    private AboveHandTrigger() {
    }

    /** 이 플레이어가 지금 손 위 표시 HUD 를 보고 있으면 트리거를 붙이고, 아니면 뗀다. */
    static synchronized void refresh(Player viewer) {
        Objects.requireNonNull(viewer, "viewer");
        if (!viewer.isOnline()) {
            forget(viewer);
            return;
        }
        boolean needed = false;
        for (DisplayHud hud : HudRegistry.getVisibleHuds(viewer)) {
            if (hud.isAboveHand() && hud.isShownTo(viewer)) {
                needed = true;
                break;
            }
        }
        UUID key = viewer.getUniqueId();
        ItemDisplayHud current = triggers.get(key);
        if (needed && current == null) {
            ItemDisplayHud trigger = create();
            // showTo 의 탑승 갱신이 이 트리거 id 를 포함하도록 먼저 넣는다
            triggers.put(key, trigger);
            trigger.showTo(viewer);
        } else if (!needed && current != null) {
            triggers.remove(key);
            current.hideFrom(viewer);
        }
    }

    /** 월드 이동 등으로 클라이언트 엔티티가 사라졌을 때 다시 보낸다. */
    static void respawn(Player viewer) {
        ItemDisplayHud trigger = triggers.get(viewer.getUniqueId());
        if (trigger != null) {
            trigger.respawnTo(viewer);
        }
    }

    static void teleport(Player viewer) {
        ItemDisplayHud trigger = triggers.get(viewer.getUniqueId());
        if (trigger != null) {
            trigger.teleportTo(viewer);
        }
    }

    /** 접속 종료: 클라이언트 쪽 엔티티는 이미 없으므로 기록만 지운다. */
    static void forget(Player player) {
        triggers.remove(player.getUniqueId());
    }

    static Integer triggerId(Player viewer) {
        ItemDisplayHud trigger = triggers.get(viewer.getUniqueId());
        return trigger == null ? null : trigger.getNMSid();
    }

    private static ItemDisplayHud create() {
        ItemDisplayHud trigger = new ItemDisplayHud();
        trigger.aboveHandTrigger = true;
        trigger.updateWhenDataChanged(false); // 보이기 전에 다 정하고, showTo 가 전체 데이터를 한 번에 보낸다
        ItemStack stack = new ItemStack(Material.PAPER);
        ItemMeta meta = stack.getItemMeta();
        meta.setItemModel(NamespacedKey.fromString(ITEM_MODEL));
        stack.setItemMeta(meta);
        trigger.setItem(stack);
        // 화면 안(왼쪽 위 1px)에 1px 크기로 둔다. 텍스처가 완전히 투명해서 보이지 않는다.
        trigger.setScale(1f, 1f, 1f);
        trigger.setLocation(1f, 1f, 0f);
        trigger.setGlowColorOverride(DisplayHud.ABOVE_HAND_GLOW_COLOR);
        trigger.setGlowing(true);
        return trigger;
    }
}
