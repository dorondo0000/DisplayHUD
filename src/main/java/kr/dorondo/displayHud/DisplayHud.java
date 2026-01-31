package kr.dorondo.displayHud;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import kr.dorondo.displayHud.core.BukkitEventListener;
import kr.dorondo.displayHud.core.DisplayHudManager;
import kr.dorondo.displayHud.core.MountListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class DisplayHud extends JavaPlugin {

    private static DisplayHud INSTANCE;

    @Override
    public void onEnable() {
        INSTANCE = this;
        PacketEvents.getAPI().getEventManager()
                .registerListener(new MountListener(), PacketListenerPriority.NORMAL);
        DisplayHudManager.load(this);

        if(DisplayHudManager.BukkitEventListener){
            Bukkit.getPluginManager().registerEvents(new BukkitEventListener(this), this);
        }

        //getLogger().info("displayhud");
    }

    public static DisplayHud getInstance() {
        return INSTANCE;
    }
}
