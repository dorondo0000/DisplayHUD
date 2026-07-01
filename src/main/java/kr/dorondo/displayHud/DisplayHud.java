package kr.dorondo.displayHud;

import kr.dorondo.displayHud.core.BukkitEventListener;
import kr.dorondo.displayHud.core.DisplayHudManager;
import kr.dorondo.displayHud.core.MountListener;
import kr.dorondo.displayHud.core.NmsManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class DisplayHud extends JavaPlugin {

    private static DisplayHud INSTANCE;

    @Override
    public void onEnable() {
        INSTANCE = this;
        DisplayHudManager.load(this);
        NmsManager.load(this);
        Bukkit.getPluginManager().registerEvents(new MountListener(), this);

        if(DisplayHudManager.BukkitEventListener){
            Bukkit.getPluginManager().registerEvents(new BukkitEventListener(this), this);
        }

        //getLogger().info("displayhud");
    }

    @Override
    public void onDisable() {
        NmsManager.unload();
    }

    public static DisplayHud getInstance() {
        return INSTANCE;
    }
}
