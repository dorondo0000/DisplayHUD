package kr.dorondo.displayHud.core;

import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemDisplayContext;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.joml.Vector3f;

public class ItemDisplayHud extends DisplayHud{
    private Display.ItemDisplay NMSitemdisplay;

    public ItemDisplayHud() {
        setNMSdisplay(Bukkit.getWorlds().getFirst());
        this.NMSid = getNMSdisplay().getId();
    }

    public void setNMSdisplay(World world){
        NMSitemdisplay = new Display.ItemDisplay(net.minecraft.world.entity.EntityType.ITEM_DISPLAY,((CraftWorld) world).getHandle());
    }

    public Display.ItemDisplay getNMSdisplay(){
        return NMSitemdisplay;
    }

    public EntityType getEntityType(){
        return EntityType.ITEM_DISPLAY;
    }

    public void setItem(ItemStack itemstack){
        getNMSdisplay().setItemStack(net.minecraft.world.item.ItemStack.fromBukkitCopy(itemstack));
        setLocation(location);
        if(updateWhenDataChanged) update();
    }

    public ItemStack getItem(){
        return getNMSdisplay().getItemStack().asBukkitCopy();
    }

    public void setItemTransform(ItemDisplay.ItemDisplayTransform transform){
        setItemTransform(transform.name());
    }

    public void setItemTransform(String string){
        getNMSdisplay().setItemTransform(ItemDisplayContext.valueOf(string));
        if(updateWhenDataChanged) update();
    }

    //get

    @Override
    public Vector3f getLocationVector(){
        Vector3f scale = new Vector3f(this.scale);
        double offsetX = 0;
        double offsetY = 0;
        double offsetZ = 0;
        if(getItem().getType().isBlock()){
            offsetZ += scale.z/2;
        }
        else{
            offsetZ += scale.z/32;
        }
        double UnitX = DisplayHudManager.unitX;
        double UnitY = DisplayHudManager.unitY;
        double ScreenX = DisplayHudManager.screenX;
        double ScreenY = DisplayHudManager.screenY;
        Integer AlignmentGap = DisplayHudManager.alignmentGap;
        double newX = ( (ScreenX/2) - location.x - offsetX) * UnitX;
        double newY = -1.5*AlignmentGap -0.178+(((ScreenY/2)-(location.y)-offsetY)*UnitY);
        double newZ = (UnitX*offsetZ)-((location.z)*100);
        return new Vector3f((float) newX,(float) newY,(float) newZ);
    }
}
