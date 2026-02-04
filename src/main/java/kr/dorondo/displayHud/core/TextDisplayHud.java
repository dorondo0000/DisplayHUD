package kr.dorondo.displayHud.core;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.mojang.math.Transformation;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.minecraft.world.entity.Display;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.TextDisplay;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Method;

public final class TextDisplayHud extends DisplayHud{
    protected Display.TextDisplay NMStextdisplay;
    protected TextDisplay.TextAlignment textAlignment = TextDisplay.TextAlignment.CENTER;

    public TextDisplayHud() {
        setNMSdisplay(Bukkit.getWorlds().getFirst());
        this.NMSid = getNMSdisplay().getId();
    }

    public void setNMSdisplay(World world){
        NMStextdisplay = new Display.TextDisplay(net.minecraft.world.entity.EntityType.TEXT_DISPLAY,((CraftWorld) world).getHandle());
    }

    public Display.TextDisplay getNMSdisplay(){
        return NMStextdisplay;
    }

    public EntityType getEntityType(){
        return EntityTypes.TEXT_DISPLAY;
    }

    public void setText(String text){
        String string = " ".repeat(getLineWidth()) + "\n";//첫번째줄 폰트 고정해야할느낌? 항상 균일하게보이려면
        string += text;
        getNMSdisplay().setText(net.minecraft.network.chat.Component.literal(string));
        setLocation(location);
        if(updateWhenDataChanged) update();
    }

    public void setText(Component component){
        Component res = Component.text(" ".repeat(getLineWidth()) + "\n");
        res = res.append(component);
        getNMSdisplay().setText(PaperAdventure.asVanilla(res));
        setLocation(location);
        if(updateWhenDataChanged) update();
    }

    public String getText(){
        String text = getNMSdisplay().getText().getString();
        int width = getNMSdisplay().getLineWidth() + 1;
        if (text.length() > width) return text.substring(width);
        return text;
    }

    public Component getTextComponent(){
        Component res = Component.empty();
        for (Component child : PaperAdventure.asAdventure(getNMSdisplay().getText()).children()) {
            res = res.append(child);
        }
        return res;
    }



    public void setShadowToggle(boolean toggle){
        byte FLAG = 1;
        byte flag = getNMSdisplay().getFlags();
        getNMSdisplay().setFlags((byte) (toggle? (flag|FLAG) : (flag&~FLAG)));
        if(updateWhenDataChanged) update();
    }
        /*
        public void setSeeThroughToggle(boolean toggle){
            byte FLAG = 2;
            byte flag = getNMSdisplay().getFlags();
            getNMSdisplay().setFlags((byte) (toggle? (flag|FLAG) : (flag&~FLAG)));
        }*/

    public void setTextAlignment(TextDisplay.TextAlignment textAlignment){
        byte FLAG_ALIGN_LEFT = 8;
        byte FLAG_ALIGN_RIGHT = 16;
        byte flag = getNMSdisplay().getFlags();
        flag &= ~(FLAG_ALIGN_LEFT | FLAG_ALIGN_RIGHT);
        switch (textAlignment) {
            case LEFT  -> flag |= FLAG_ALIGN_LEFT;
            case RIGHT -> flag |= FLAG_ALIGN_RIGHT;
        }
        getNMSdisplay().setFlags(flag);
        this.textAlignment = textAlignment;
        if(updateWhenDataChanged) update();
    }

        /*
        //linewidth 정렬때문에 background설정이 애매함 - 리소스팩에도 안넣음
        public void setBackGroundColor(int color){
            byte FLAG = 4;
            byte flag = getNMSdisplay().getFlags();
            getNMSdisplay().setFlags((byte) (flag&~FLAG));
            try {
                Method method = getNMSdisplay().getClass().getDeclaredMethod("setBackgroundColor",int.class);
                method.setAccessible(true);
                method.invoke(getNMSdisplay(),color);

            } catch (NoSuchMethodException e) {
                throw new RuntimeException("nosuchmethod", e);
            } catch (Exception e) {
                throw new RuntimeException("exception", e);
            }
            update();
        }

        public void setBackGroundColor(){
            byte FLAG = 4;
            byte flag = getNMSdisplay().getFlags();
            getNMSdisplay().setFlags((byte) (flag|FLAG));
            update();
        }*/

    public void setLineWidth(int width){
        try {
            Method method = getNMSdisplay().getClass().getDeclaredMethod("setLineWidth", int.class);
            method.setAccessible(true);
            method.invoke(getNMSdisplay(),width);

        } catch (NoSuchMethodException e) {
            throw new RuntimeException("nosuchmethod", e);
        } catch (Exception e) {
            throw new RuntimeException("exception", e);
        }
        if(updateWhenDataChanged) update();
    }

    public int getLineWidth(){
        return getNMSdisplay().getLineWidth();
    }

    public void setOpacity(int n){
        setOpacity(n,0);
    }

    public void setOpacity(int n,Integer time){
        n = Math.clamp(n, -128, 255);
        if(n>127) n-=256;
        getNMSdisplay().setTextOpacity((byte) n);
        getNMSdisplay().setTransformationInterpolationDelay(0);
        getNMSdisplay().setTransformationInterpolationDuration(time);
        if(updateWhenDataChanged) update();
    }

    @Override
    public void setLeftRotation(Vector3f vector, Integer time) {
        vector.add(0,180,0);
        Quaternionf quat = vecToQuat(vector);
        Transformation tf = Display.createTransformation(getNMSdisplay().getEntityData());
        tf = new Transformation(tf.getTranslation(),quat,tf.getScale(),tf.getRightRotation());
        getNMSdisplay().setTransformation(tf);
        getNMSdisplay().setTransformationInterpolationDelay(0);
        getNMSdisplay().setTransformationInterpolationDuration(time);
        if(updateWhenDataChanged) update();
    }

    public Vector3f getLocationVector(){
        Vector3f scale = new Vector3f(this.scale);
        double offsetX = 0;
        double offsetY = 0;
        double offsetZ = 0;

        String text = getText();
        offsetY -= scale.y/2;
        int lines = countLinesNl(text);
        offsetY += ((scale.y / 4.0) * (lines - 1.5));
        int lineWidth = getNMSdisplay().getLineWidth();
        switch(textAlignment){
            case LEFT -> offsetX = (scale.x)*(((lineWidth)*0.0125) - 0.5);
            case RIGHT -> offsetX = (scale.x)*(((lineWidth)*-0.0125) - 0.55);
            case CENTER -> scale.x = 0;
        }
        offsetX += scale.x/2;
        offsetY += scale.y/2;

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

    private static int countLinesNl(String s) {
        return (s == null || s.isEmpty()) ? 1 : s.split("\n").length;
    }
}