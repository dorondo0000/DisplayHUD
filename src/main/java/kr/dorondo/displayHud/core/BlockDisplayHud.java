package kr.dorondo.displayHud.core;

import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.CraftWorld;
import org.joml.Vector3f;

public class BlockDisplayHud extends DisplayHud{
    protected Display.BlockDisplay NMSblockdisplay;

    public BlockDisplayHud() {
        setNMSdisplay(Bukkit.getWorlds().getFirst());
        this.NMSid = getNMSdisplay().getId();
    }

    public void setNMSdisplay(World world){
        NMSblockdisplay = new Display.BlockDisplay((net.minecraft.world.entity.EntityType<Display.BlockDisplay>) NmsCompat.entityType("BLOCK_DISPLAY"),((CraftWorld) world).getHandle());
    }

    public Display.BlockDisplay getNMSdisplay(){
        return NMSblockdisplay;
    }

    public EntityType getEntityType(){
        return NmsCompat.entityType("BLOCK_DISPLAY");
    }

    public void setBlock(BlockState blockstate){
        getNMSdisplay().setBlockState((net.minecraft.world.level.block.state.BlockState) blockstate);
        if(updateWhenDataChanged) update();
    }

    public BlockState getBlock(){
        return (BlockState) getNMSdisplay().getBlockState();
    }
    //get

    @Override
    public Vector3f getLocationVector(){
        Vector3f scale = new Vector3f(this.scale);
        double offsetX = 0;
        double offsetY = 0;
        double offsetZ = 0;
        offsetZ += scale.z/2; //block
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
