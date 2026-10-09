package kr.dorondo.displayHud.core;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import com.mojang.math.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

public abstract class DisplayHud {
    /**
     * 손 위 표시(above hand) 표식 밝기. {@link #setAboveHand(boolean)} 가 켜진 HUD 는 밝기를 block 1 / sky 2 로 보낸다.
     * HUD 셰이더는 밝기를 쓰지 않으므로 화면에는 영향이 없고, 리소스팩이 이 값을 보고 손 위로 올릴 HUD 를 알아본다.
     * 리소스팩(include/displayhud.glsl 의 DISPLAYHUD_ABOVE_HAND_LIGHT = (16, 32))과 같아야 한다.
     */
    public static final int ABOVE_HAND_BLOCK_LIGHT = 1;
    public static final int ABOVE_HAND_SKY_LIGHT = 2;
    /**
     * 예전 방식(발광 표식) 손 위 표시 색 0x01FEFD. 이 색으로 빛나는 아이템/블록 디스플레이 HUD 도 손 위로 올라간다.
     * 손 위 표시 트리거도 이 색으로 빛난다(리소스팩이 이 색은 발광 테두리를 그리지 않는다).
     */
    public static final int ABOVE_HAND_GLOW_COLOR = 0x01FEFD;

    public enum HudAlignment {
        CENTER,
        LEFT,
        RIGHT,
        UNALIGNED
    }

    public static DisplayHud getHud(Player player, String id) {
        return HudRegistry.getPersonalHud(player, id);
    }

    public static Map<String, DisplayHud> getHuds(Player player) {
        return HudRegistry.getPersonalHuds(player);
    }

    public static Collection<DisplayHud> getVisibleHuds(Player player) {
        return HudRegistry.getVisibleHuds(player);
    }

    public static int[] getVisibleHudIds(Player player) {
        return HudRegistry.getVisibleHudIds(player);
    }

    public static void mountVisibleHuds(Player player) {
        Objects.requireNonNull(player, "player");
        NmsManager.updateMount(player);
    }

    public static void removeHud(Player player,String id){
        DisplayHud hud = getHud(player,id);
        if (hud!=null) hud.remove();
    }

    public static void clearHuds(Player player) {
        HudRegistry.clearPersonalHuds(player);
    }

    private enum HudScope {
        UNSPAWNED,
        PERSONAL,
        GLOBAL,
        REMOVED
    }

    protected Player player;
    protected String id;
    protected UUID uuid;

    protected Display NMSdisplay;
    protected Integer NMSid;

    private final Set<Player> viewers = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private HudScope hudScope = HudScope.UNSPAWNED;
    private GlobalHud<?> globalOwner;
    private boolean defaultsInitialized = false;
    /** 손 위 표시 트리거(내부용 투명 디스플레이)인지. */
    boolean aboveHandTrigger = false;

    protected boolean aboveHand = false;
    protected int brightnessBlock = 15;
    protected int brightnessSky = 15;

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
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(uuid, "uuid");
        if(hudScope != HudScope.UNSPAWNED || this.player != null){
            return false;
        }
        if (!HudRegistry.registerPersonalHud(player, id, this)) {
            return false;
        }

        this.player = player;
        this.id = id;
        this.uuid = uuid;
        this.hudScope = HudScope.PERSONAL;

        initializeDefaults();
        showTo(player);


        return true;




    }

    public void respawn() {
        for (Player viewer : getViewers()) {
            respawnTo(viewer);
            if (aboveHand) {
                AboveHandTrigger.respawn(viewer);
            }
        }
    }

    public void remove() {
        if (hudScope == HudScope.GLOBAL && globalOwner != null) {
            globalOwner.remove();
            return;
        }
        removePacketsFromViewers();
        if (hudScope == HudScope.PERSONAL && player != null && id != null) {
            HudRegistry.unregisterPersonalHud(player, id, this);
        }
        hudScope = HudScope.REMOVED;
    }

    public void update(){
        if(id == null) return;
        List<SynchedEntityData.DataValue<?>> metadata = getNMSdisplay().getEntityData().packDirty();
        if (metadata == null || metadata.isEmpty()) return;
        for (Player viewer : getViewers()) {
            NmsManager.update(viewer,NMSid,metadata);
        }
    }


    public void teleport(){
        for (Player viewer : getViewers()) {
            teleportTo(viewer);
        }
    }


    public void mount() {
        for (Player viewer : getViewers()) {
            mountTo(viewer);
        }
    }

    boolean attachGlobal(GlobalHud<?> owner, String id, UUID uuid) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(uuid, "uuid");
        if (hudScope != HudScope.UNSPAWNED || this.player != null) {
            return false;
        }
        this.id = id;
        this.uuid = uuid;
        this.globalOwner = owner;
        this.hudScope = HudScope.GLOBAL;
        initializeDefaults();
        return true;
    }

    void removeGlobal() {
        removePacketsFromViewers();
        hudScope = HudScope.REMOVED;
    }

    boolean showTo(Player viewer) {
        Objects.requireNonNull(viewer, "viewer");
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        initializeDefaults();
        if (!viewers.add(viewer)) {
            return false;
        }
        Location location = viewer.getLocation().clone();
        location.setPitch(0);
        location.setYaw(0);
        NmsManager.spawn(viewer,getNMSdisplay(),uuid,location);
        sendFullUpdate(viewer);
        mountTo(viewer);
        if (!aboveHandTrigger) {
            AboveHandTrigger.refresh(viewer);
        }
        return true;
    }

    boolean hideFrom(Player viewer) {
        Objects.requireNonNull(viewer, "viewer");
        if (!viewers.remove(viewer)) {
            return false;
        }
        NmsManager.remove(viewer,NMSid);
        NmsManager.updateMount(viewer);
        if (!aboveHandTrigger) {
            AboveHandTrigger.refresh(viewer);
        }
        return true;
    }

    boolean isShownTo(Player viewer) {
        return viewers.contains(viewer);
    }

    void respawnTo(Player viewer) {
        Objects.requireNonNull(viewer, "viewer");
        if (!viewers.contains(viewer)) return;
        NmsManager.remove(viewer,NMSid);
        Location location = viewer.getLocation().clone();
        location.setPitch(0);
        location.setYaw(0);
        NmsManager.spawn(viewer,getNMSdisplay(),uuid,location);
        sendFullUpdate(viewer);
        mountTo(viewer);
    }

    void teleportTo(Player viewer) {
        Objects.requireNonNull(viewer, "viewer");
        if (!viewers.contains(viewer)) return;
        Location location = viewer.getLocation().clone();
        location.setYaw(0);
        location.setPitch(0);
        NmsManager.teleport(viewer,NMSid,location);
    }

    void mountTo(Player viewer) {
        Objects.requireNonNull(viewer, "viewer");
        if (!viewers.contains(viewer)) return;
        NmsManager.updateMount(viewer);
    }

    boolean isGlobalHud() {
        return hudScope == HudScope.GLOBAL;
    }

    GlobalHud<?> getGlobalOwner() {
        return globalOwner;
    }

    Set<Player> getViewers() {
        return new LinkedHashSet<>(viewers);
    }

    private void removePacketsFromViewers() {
        for (Player viewer : getViewers()) {
            hideFrom(viewer);
        }
    }

    private void initializeDefaults() {
        if (defaultsInitialized) return;
        boolean update = updateWhenDataChanged;
        updateWhenDataChanged = false;
        setLeftRotation(getLeftRotationVector());
        applyBrightness(); // 손 위 표시가 켜져 있으면 표식 밝기(1/2), 아니면 setBrightness 값(기본 15/15)
        setScale(scale);
        setLocation(location);
        updateWhenDataChanged = update;
        defaultsInitialized = true;
    }

    private void sendFullUpdate(Player viewer) {
        List<SynchedEntityData.DataValue<?>> metadata = getNMSdisplay().getEntityData().packAll();
        if (metadata == null || metadata.isEmpty()) return;
        NmsManager.update(viewer,NMSid,metadata);
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

    protected abstract void setNMSdisplay(World world);

    public Display getNMSdisplay(){
        return NMSdisplay;
    }

    public Integer getNMSid() {return NMSid;}

    public EntityType getEntityType(){
        return getNMSdisplay().getType();
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
        tf = NmsCompat.transformation(lv,NmsCompat.leftRotation(tf),NmsCompat.scale(tf),NmsCompat.rightRotation(tf));
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
        tf = NmsCompat.transformation(NmsCompat.translation(tf),NmsCompat.leftRotation(tf),sv,NmsCompat.rightRotation(tf));
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
        tf = NmsCompat.transformation(NmsCompat.translation(tf),quat,NmsCompat.scale(tf),NmsCompat.rightRotation(tf));
        getNMSdisplay().setTransformation(tf);
        getNMSdisplay().setTransformationInterpolationDelay(0);
        getNMSdisplay().setTransformationInterpolationDuration(time);
        if(updateWhenDataChanged) update();
    }
    public Quaternionf getLeftRotation(){
        return NmsCompat.leftRotation(Display.createTransformation(getNMSdisplay().getEntityData()));
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
        tf = NmsCompat.transformation(NmsCompat.translation(tf),NmsCompat.leftRotation(tf),NmsCompat.scale(tf),quat);
        getNMSdisplay().setTransformation(tf);
        getNMSdisplay().setTransformationInterpolationDelay(0);
        getNMSdisplay().setTransformationInterpolationDuration(time);
        if(updateWhenDataChanged) update();
    }
    public Quaternionf getRightRotation(){
        return NmsCompat.rightRotation(Display.createTransformation(getNMSdisplay().getEntityData()));
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

    /**
     * 밝기 고정값. HUD 셰이더는 밝기를 쓰지 않으므로 보통은 화면에 영향이 없다(기본 15/15).
     * 손 위 표시({@link #setAboveHand(boolean)})가 켜져 있는 동안에는 표식 밝기(block 1 / sky 2)가 유지되고,
     * 여기서 넣은 값은 기억해 두었다가 손 위 표시를 끌 때 적용된다. block 1 / sky 2 는 손 위 표시 표식이라 쓰지 않는 게 좋다.
     */
    public void setBrightness(int block, int sky){
        brightnessBlock = block;
        brightnessSky = sky;
        applyBrightness();
        if(updateWhenDataChanged) update();
    }

    /** setBrightness 로 넣은 값(손 위 표시 표식이 아니라). */
    public Integer getBrightnessBlock(){
        return brightnessBlock;
    }

    /** setBrightness 로 넣은 값(손 위 표시 표식이 아니라). */
    public Integer getBrightnessSky(){
        return brightnessSky;
    }

    private void applyBrightness() {
        getNMSdisplay().setBrightnessOverride(aboveHand
                ? new Brightness(ABOVE_HAND_BLOCK_LIGHT, ABOVE_HAND_SKY_LIGHT)
                : new Brightness(brightnessBlock, brightnessSky));
    }

    /**
     * 손 위 표시 (2.2 Beta). true 면 이 HUD 가 1인칭 손/든 아이템에 가려지지 않고 그 위에 그려진다.
     * 아이템/텍스트/블록 디스플레이 모두 된다. 밝기 표식(block 1 / sky 2)을 쓰므로 발광은 그대로 자유롭게 쓸 수 있다.
     * 이 HUD 를 보는 플레이어에게는 손 위 표시 처리를 켜기 위한 투명 트리거 디스플레이가 하나 붙는다(자동 관리).
     * DisplayHud 리소스팩(2.2 이상)이 필요하다.
     */
    public void setAboveHand(boolean aboveHand){
        if (aboveHandTrigger || this.aboveHand == aboveHand) return;
        this.aboveHand = aboveHand;
        applyBrightness();
        if(updateWhenDataChanged) update();
        for (Player viewer : getViewers()) {
            AboveHandTrigger.refresh(viewer);
        }
    }

    public boolean isAboveHand(){
        return aboveHand;
    }

    /** 바닐라 발광(외곽선). 색은 {@link #setGlowColorOverride(int)} (기본: 흰색). 손 위 표시와 같이 써도 된다. */
    public void setGlowing(boolean glowing){
        getNMSdisplay().setGlowingTag(glowing);
        if(updateWhenDataChanged) update();
    }

    public boolean isGlowing(){
        return getNMSdisplay().hasGlowingTag();
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
