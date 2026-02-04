package kr.dorondo.displayHud.core;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public abstract class HudMover {

    public static void moveGroup(DisplayHud origin, float x,float y,float z, int time, DisplayHud... passengers){
        moveGroup(origin, Arrays.asList(passengers), new Vector3f(x,y,z), time);
    }

    public static void moveGroup(DisplayHud origin, Vector3f location, int time, DisplayHud... passengers){
        moveGroup(origin, Arrays.asList(passengers), location, time);
    }


    public static void moveGroup(DisplayHud origin, Collection<? extends DisplayHud> passengers,
                                 Vector3f location, int time){
        Vector3f offset = new Vector3f(location).sub(origin.location);
        offset.z = 0;

        moveByOffset(origin, offset, time);

        for (DisplayHud hud : passengers) {
            moveByOffset(hud, offset, time);
        }

    }

    public static void moveByOffset(DisplayHud hud, float x,float y,float z){
        moveByOffset(hud,new Vector3f(x,y,z),0);
    }
    public static void moveByOffset(DisplayHud hud, float x,float y,float z,int time){
        moveByOffset(hud,new Vector3f(x,y,z),time);
    }
    public static void moveByOffset(DisplayHud hud, Vector3f offset){
        moveByOffset(hud,offset,0);
    }

    public static void moveByOffset(DisplayHud hud, Vector3f offset,int time){
        Vector3f location = new Vector3f(hud.location).add(offset);
        hud.setLocation(location,time);
    }
}
