package kr.dorondo.displayHud.core;

import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Display.ItemDisplay;
import net.minecraft.world.entity.Display.TextDisplay;
import net.minecraft.world.entity.Display.BlockDisplay;
import net.minecraft.world.item.ItemDisplayContext;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.CraftWorld;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.ItemDisplay.ItemDisplayTransform;
import org.bukkit.entity.TextDisplay.TextAlignment;
import com.mojang.math.Transformation;
import org.bukkit.inventory.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Method;
import java.util.*;

public abstract class DisplayHud {
    public enum HudAlignment {
        CENTER,
        LEFT,
        RIGHT,
        UNALIGNED
    }

    private static WeakHashMap<Player, Map<String, DisplayHud>> HudRegistry = new WeakHashMap<>();

    public static DisplayHud getHud(Player player, String id) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(id, "id");

        Map<String, DisplayHud> huds = HudRegistry.get(player);
        if (huds == null) {
            return null;
        }
        return huds.get(id);
    }

    public static Map<String, DisplayHud> getHuds(Player player) {
        Objects.requireNonNull(player, "player");

        Map<String, DisplayHud> huds = HudRegistry.get(player);
        if (huds == null || huds.isEmpty()) {
            return Collections.emptyMap();
        }
        return new HashMap<>(huds);
    }

    public static void removeHud(Player player,String id){
        DisplayHud hud = getHud(player,id);
        if (hud!=null) hud.remove();
    }

    public static void clearHuds(Player player) {
        Objects.requireNonNull(player, "player");
        Map<String, DisplayHud> huds = HudRegistry.remove(player);
        if(huds == null)return;
        huds.values().forEach(DisplayHud::remove);
    }

    protected Player player;
    protected String id;
    protected UUID uuid;

    protected Display NMSdisplay;
    protected Integer NMSid;

    protected Vector3f location = new Vector3f(940f,540f,0f);
    protected Vector3f scale = new Vector3f(100f, 100f, 1f);
    protected HudAlignment alignment = HudAlignment.CENTER;

    protected boolean removeWhenPlayerDied = false;
    protected boolean updateWhenDataChanged = true;

    protected Map<String,Object> ExtraData = new java.util.concurrent.ConcurrentHashMap<>();

    public DisplayHud() {
        setNMSdisplay(Bukkit.getWorlds().getFirst());
        this.NMSid = getNMSdisplay().getId();
    }

    public boolean spawn(Player player, String id){
        return spawn(player,id,UUID.randomUUID());
    }

    public boolean spawn(Player player, String id, UUID uuid){
        if(this.player != null){
            return false;
        }
        Map<String, DisplayHud> map = HudRegistry.computeIfAbsent(player, v -> new HashMap<>());

        if (map.containsKey(id)) {
            return false;
        }

        map.put(id, this);

        this.player = Objects.requireNonNull(player, "player");
        this.id = Objects.requireNonNull(id, "id");
        this.uuid = uuid;

        Location blocation = player.getLocation().clone();//.add(0,-4000,0); // x,z 청크 유지한채 y만 -4000으로 내려서 탑승 보강 삭제
        blocation.setPitch(0);
        blocation.setYaw(0);

        PacketSender.spawn(player,NMSid,uuid,getEntityType(),blocation);


        setLeftRotation(getLeftRotationVector());
        setBrightness(15,15);
        setScale(scale);
        setLocation(location);
        update();
        mount();


        return true;




    }

    public void respawn() {
        PacketSender.remove(player,NMSid);

        Location blocation = player.getLocation().clone();//.add(0,-4000,0); // x,z 청크 유지한채 y만 -4000으로 내려서 탑승 보강 삭제
        blocation.setPitch(0);
        blocation.setYaw(0);

        PacketSender.spawn(player,NMSid,uuid,getEntityType(),blocation);
        PacketSender.update(player,NMSid,SpigotConversionUtil.getEntityMetadata(getNMSdisplay().getBukkitEntity()));
        //mount();


    }

    public void remove() {
        if(player == null) return;
        PacketSender.remove(player,NMSid);
        Map<String, DisplayHud> huds = HudRegistry.get(player);
        if (huds == null) {
            return;
        }
        huds.remove(id);
        if (huds.isEmpty()) {
            HudRegistry.remove(player);
        }
    }

    public void update(){
        if(id == null) return;
        List<SynchedEntityData.DataValue<?>> pack = getNMSdisplay().getEntityData().packDirty();
        if (pack == null) return;
        if (pack.isEmpty()) return;
        Set<Integer> dirtyIds = new HashSet<>();
        for (SynchedEntityData.DataValue<?> dv : pack) {
            dirtyIds.add(dv.id());
        }
        List<EntityData<?>> metadata = new ArrayList<>(); //좀 애매한방식이긴한데 일단쓰기
        for (EntityData<?> ed: SpigotConversionUtil.getEntityMetadata(getNMSdisplay().getBukkitEntity())){
            if (dirtyIds.contains(ed.getIndex())) {
                metadata.add(ed);
            }
        }
        PacketSender.update(player,NMSid,metadata);
    }


    public void teleport(){
        if(player == null) return;
        Location location = player.getLocation().clone();
        location.setYaw(0);
        location.setPitch(0);
        PacketSender.teleport(player,NMSid,location);
    }


    public void mount() {
        if(player == null) return;
        PacketSender.mount(player,player.getEntityId(),NMSid);
    }

    public void removeWhenPlayerDied(){
        removeWhenPlayerDied = true;
    }
    public void removeWhenPlayerDied(boolean toggle){
        removeWhenPlayerDied = toggle;
    }

    public void updateWhenDataChanged(){
        updateWhenDataChanged = true;
    }
    public void updateWhenDataChanged(boolean toggle){
        updateWhenDataChanged = toggle;
    }

    public Player getPlayer(){
        return this.player;
    }

    public String getId(){
        return this.id;
    }

    public UUID getUuid(){
        return this.uuid;
    }

    public void setNMSdisplay(World world){
        NMSdisplay = new ItemDisplay(net.minecraft.world.entity.EntityType.ITEM_DISPLAY,((CraftWorld) world).getHandle());
    }

    public Display getNMSdisplay(){
        return NMSdisplay;
    }

    public Integer getNMSid() {return NMSid;}

    public EntityType getEntityType(){
        return EntityTypes.DISPLAY;
    }

    public void setExtraData(String key, Object value) {
        Objects.requireNonNull(key, "key");
        if (value == null) {
            ExtraData.remove(key);
        } else {
            ExtraData.put(key, value);
        }
    }
    public Object getExtraData(String key) {
        Objects.requireNonNull(key, "key");
        return ExtraData.get(key);
    }

    public boolean hasExtraData(String key) {
        Objects.requireNonNull(key, "key");
        return ExtraData.containsKey(key);
    }

    public Object removeExtraData(String key) {
        Objects.requireNonNull(key, "key");
        return ExtraData.remove(key);
    }

    public void setLocation(float x,float y,float z){
        setLocation(new Vector3f(x,y,z),0);
    }

    public void setLocation(float x,float y,float z,int time){
        setLocation(new Vector3f(x,y,z),time);
    }

    public void setLocation(Vector3f location){
        setLocation(location,0);
    }
    public void setLocation(Vector3f location,Integer time) {
        this.location = location;
        Vector3f lv = getLocationVector();
        lv.y -= getAlignmentInt();

        Transformation tf = Display.createTransformation(getNMSdisplay().getEntityData());
        tf = new Transformation(lv,tf.getLeftRotation(),tf.getScale(),tf.getRightRotation());
        getNMSdisplay().setTransformation(tf);
        getNMSdisplay().setTransformationInterpolationDelay(0);
        getNMSdisplay().setTransformationInterpolationDuration(time);
        if(updateWhenDataChanged) update();
    }

    public Vector3f getLocation() {
        return location;
    }

    public void setScale(float x,float y,float z){
        setScale(new Vector3f(x,y,z),0);
    }

    public void setScale(float x,float y,float z,int time){
        setScale(new Vector3f(x,y,z),time);
    }

    public void setScale(Vector3f scale){
        setScale(scale,0);
    }
    public void setScale(Vector3f scale,Integer time) {
        this.scale = scale;
        Vector3f sv = getScaleVector();

        Transformation tf = Display.createTransformation(getNMSdisplay().getEntityData());
        tf = new Transformation(tf.getTranslation(),tf.getLeftRotation(),sv,tf.getRightRotation());
        getNMSdisplay().setTransformation(tf);
        getNMSdisplay().setTransformationInterpolationDelay(0);
        getNMSdisplay().setTransformationInterpolationDuration(time);
        if(updateWhenDataChanged) update();
    }

    public Vector3f getScale() {
        return scale;
    }

    public void setLeftRotation(float x,float y,float z){
        setLeftRotation(new Vector3f(x,y,z),0);
    }

    public void setLeftRotation(float x,float y,float z,int time){
        setLeftRotation(new Vector3f(x,y,z),time);
    }

    public void setLeftRotation(Vector3f vector){
        setLeftRotation(vector,0);
    }
    public void setLeftRotation(Vector3f vector,Integer time) {
        Quaternionf quat = vecToQuat(vector);
        Transformation tf = Display.createTransformation(getNMSdisplay().getEntityData());
        tf = new Transformation(tf.getTranslation(),quat,tf.getScale(),tf.getRightRotation());
        getNMSdisplay().setTransformation(tf);
        getNMSdisplay().setTransformationInterpolationDelay(0);
        getNMSdisplay().setTransformationInterpolationDuration(time);
        if(updateWhenDataChanged) update();
    }
    public Quaternionf getLeftRotation(){
        return new Quaternionf(Display.createTransformation(getNMSdisplay().getEntityData()).getLeftRotation());
    }

    public Vector3f getLeftRotationVector(){
        return quatToVec(getLeftRotation());
    }

    public void setRightRotation(float x,float y,float z){
        setRightRotation(new Vector3f(x,y,z),0);
    }

    public void setRightRotation(float x,float y,float z,int time){
        setRightRotation(new Vector3f(x,y,z),time);
    }

    public void setRightRotation(Vector3f vector){
        setRightRotation(vector,0);
    }
    public void setRightRotation(Vector3f vector,Integer time) {
        Quaternionf quat = vecToQuat(vector);
        Transformation tf = Display.createTransformation(getNMSdisplay().getEntityData());
        tf = new Transformation(tf.getTranslation(),tf.getLeftRotation(),tf.getScale(),quat);
        getNMSdisplay().setTransformation(tf);
        getNMSdisplay().setTransformationInterpolationDelay(0);
        getNMSdisplay().setTransformationInterpolationDuration(time);
        if(updateWhenDataChanged) update();
    }
    public Quaternionf getRightRotation(){
        return new Quaternionf(Display.createTransformation(getNMSdisplay().getEntityData()).getRightRotation());
    }

    public Vector3f getRightRotationVector(){
        return quatToVec(getRightRotation());
    }

    public void setAlignment(HudAlignment alignment){
        this.alignment = alignment;
        setLocation(location,0);
    }

    public HudAlignment getAlignment(){
        return alignment;
    }

    public Integer getAlignmentInt(){
        //get
        Integer gap = DisplayHudManager.alignmentGap;
        if(alignment == HudAlignment.LEFT){
            return gap;
        }
        else if(alignment == HudAlignment.CENTER){
            return gap*2;
        }
        else if(alignment == HudAlignment.RIGHT) {
            return gap * 3;
        }
        return 0;
    }

    public void setInterpolationDuration(Integer n){
        getNMSdisplay().setTransformationInterpolationDuration(n);
        if(updateWhenDataChanged) update();
    }

    public Integer getInterpolationDuration(){
        return getNMSdisplay().getTransformationInterpolationDuration();
    }

    public void setInterpolationDelay(Integer n){
        getNMSdisplay().setTransformationInterpolationDelay(n);
        if(updateWhenDataChanged) update();
    }

    public Integer getInterpolationDelay(){
        return getNMSdisplay().getTransformationInterpolationDelay();
    }

    public void setBrightness(int block, int sky){
        getNMSdisplay().setBrightnessOverride(new Brightness(block,sky));
        if(updateWhenDataChanged) update();
    }

    public Integer getBrightnessBlock(){
        return getNMSdisplay().getBrightnessOverride().block();
    }

    public Integer getBrightnessSky(){
        return getNMSdisplay().getBrightnessOverride().sky();
    }

    public void setHeight(float height){
        getNMSdisplay().setHeight(height);
        if(updateWhenDataChanged) update();
    }

    public float getHeight(){
        return getNMSdisplay().getHeight();
    }

    public void setWidth(float width){
        getNMSdisplay().setWidth(width);
        if(updateWhenDataChanged) update();
    }

    public float getWidth(){
        return getNMSdisplay().getWidth();
    }

    public void setGlowColorOverride(int color){
        getNMSdisplay().setGlowColorOverride(color);
        if(updateWhenDataChanged) update();
    }

    public int getGlowColorOverride(){
        return getNMSdisplay().getGlowColorOverride();
    }

    public void setViewRange(float viewRange){
        getNMSdisplay().setViewRange(viewRange);
        if(updateWhenDataChanged) update();
    }

    public float getViewRange(){
        return getNMSdisplay().getViewRange();
    }



    public Vector3f getLocationVector(){
        return new Vector3f(location); //in child
    }

    public Vector3f getScaleVector(){
        float unitX = DisplayHudManager.unitX;
        float unitY = DisplayHudManager.unitY;
        return new Vector3f(scale.x*unitX,scale.y*unitY,scale.z*unitX);
    }

    public static Quaternionf vecToQuat(Vector3f vector){
        return new Quaternionf().rotateZYX(
                (float) Math.toRadians(vector.x),
                (float) Math.toRadians(vector.y),
                (float) Math.toRadians(vector.z)
        );
    }

    public static Vector3f quatToVec(Quaternionf quat) {
        Vector3f eulerRad = new Vector3f();
        quat.getEulerAnglesZYX(eulerRad);

        return new Vector3f(
                (float) Math.toDegrees(eulerRad.x),
                (float) Math.toDegrees(eulerRad.y),
                (float) Math.toDegrees(eulerRad.z)
        );
    }

}
