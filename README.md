# DisplayHud
DisplayHud is a project that allows display entities to be fixed to the player’s screen and used like a HUD by utilizing the resource pack’s core shaders.
Through shader-based positioning, displays are rendered at a specific Y-offset below the player, making them appear anchored to the screen rather than the world.

Versions: 1.21.8 ~ 26.3 (one jar, one resource pack)<br>
No dependencies (packetevents is no longer required since 2.0)<br>
Resource pack: `DisplayHud-resourcepack.zip` from the releases (one zip for 1.21.8 ~ 26.3, per-version shaders in pack overlays; source in `resourcepack/`)<br>
Latest: **2.2 Beta** (above hand HUD, see below)<br>



original : https://github.com/Yesang642/DisplayHud

wiki(update soon) : https://displayhud.gitbook.io/displayhud-docs/


## ShowCase
![2026-01-12+05-23-25](https://github.com/user-attachments/assets/3545fff5-8a65-4b7d-9079-33f1885856c5)


## Above hand - 2.2 Beta
Vanilla clears the depth buffer right before it draws the first-person hand, so a HUD made of display entities is always covered by the hand / held item.
With `setAboveHand(true)` a HUD element (item, text or block display) is drawn **on top of the hand**.

> **Beta.** Implemented for every pack overlay (1.21.8 ~ 26.3). All shaders are validated offline against each version's vanilla shaders, but in-game testing is still in progress. Please report problems (version, graphics mode, GPU).<br>
> Needs the **2.2 resource pack**. With an older pack the HUD simply stays under the hand.<br>
> **OpenGL fix** (2.2 Beta pack re-uploaded): the core shaders no longer include `globals.glsl`. Vanilla entity / item / text / block / outline shaders don't use the `Globals` block, and adding it broke entity rendering with Graphics API = OpenGL on 26.3 (mangled first-person arm, black player skins). The screen aspect ratio for aligned HUDs now comes from the projection matrix. If you downloaded the 2.2 Beta pack before, download it again.

### Java
```java
TextDisplayHud text = new TextDisplayHud();
text.setText("&e&lAbove hand");
text.setAboveHand(true);          // draw over the first-person hand
text.spawn(player, "my_text");

ItemDisplayHud item = new ItemDisplayHud();
item.setItem(new ItemStack(Material.DIAMOND));
item.setAboveHand(true);
item.setGlowColorOverride(0xFF5555); // normal (vanilla) glow still works, any colour
item.setGlowing(true);
item.spawn(player, "my_item");

item.isAboveHand();               // true
item.setAboveHand(false);         // back to normal (under the hand)
```
Works for personal HUDs and `GlobalHud`s (`globalHud.getHud().setAboveHand(true)`), before or after `spawn`.

### Skript (skript-reflect, like `displayhudexample.sk`)
```
set {_t} to new TextDisplayHud()
{_t}.setText("&e&lAbove hand")
{_t}.setAboveHand(true)
{_t}.spawn(player,"my_text")
```

### Markers
The resource pack has to know which pixels go over the hand. Two markers are accepted:

| marker | how | glow | display types |
|---|---|---|---|
| **light marker** (default, `setAboveHand`) | brightness override block **1** / sky **2** | free: normal glow with any colour still works | item, text, block |
| legacy glow marker | `setGlowColorOverride(DisplayHud.ABOVE_HAND_GLOW_COLOR)` (`0x01FEFD`) + `setGlowing(true)` | that display can't have a visible glow | item, block (text displays never draw an outline) |

- HUD shaders ignore brightness, so the light marker is invisible. `setAboveHand(true)` sets it for you. `setBrightness` called while above hand is on is remembered and applied when you turn it off (`getBrightnessBlock/Sky` return your values).
- Reserved values: brightness **block 1 / sky 2** and glow colour **0x01FEFD**. Don't use them for normal HUD elements.

### How it works
1. Core shaders: a HUD vertex with the light marker is moved into a reserved depth band right in front of the camera (no world geometry can be that close). The order between above-hand elements is kept for `location.z` -900 ~ 900.
2. `post_effect/entity_outline.json` (the glow post effect) runs inside level rendering **before** the hand is drawn, and its result is blended onto the screen **after** the hand. Its first pass (`displayhud_capture`) copies the screen pixels in that depth band (or under the legacy glow colour) into a private target, and its last pass (`displayhud_combine`) writes them, opaque, into `minecraft:entity_outline`. Everything else keeps the vanilla glow result, so normal glowing entities and glowing HUD elements look exactly like vanilla.
3. The client only runs this post effect on frames where something glows (1.21.8 ~ 26.1: a visible glowing entity, 26.2+: a glow outline submission). So while a player sees at least one above-hand HUD, the plugin mounts one hidden **trigger** on that player: a glowing (`0x01FEFD`) item display using the fully transparent model `displayhud:above_hand_trigger` (shipped in the pack), 1px at the top-left corner. It submits a glow outline but draws no pixel. It is removed automatically when the player has no above-hand HUD, is not listed in `getHuds` / `getVisibleHuds`, and follows teleport / respawn / world change like the other HUDs (`respawn()` of an above-hand HUD also re-sends it).

### Limitations
- Semi-transparent above-hand pixels (text opacity, translucent textures) are lifted as opaque copies of the screen *before* the hand: where they overlap the hand you see the world behind them, not the hand.
- Above-hand elements always draw above non-above-hand HUD elements.
- Fabulous graphics (1.21.8 ~ 26.2): vanilla draws translucent item entities into a separate buffer, so item display images may not be lifted there (text is fine).
- Improved Transparency (26.3): semi-transparent elements drawn through OIT are not lifted (they stay under the hand). Opaque ones are fine.
- Another resource pack that also replaces `post_effect/entity_outline.json` and is placed above DisplayHud's pack turns above hand off. Merge them (next section).


## Merging with an existing glow (entity_outline) shader
If your server pack already has its own `assets/minecraft/post_effect/entity_outline.json`, only one of the two files can win. Put DisplayHud's two passes around **your** passes:

1. Copy into your pack, from the overlay folder that matches your Minecraft version (`resourcepack/v1_21_8`, `v1_21_9` (1.21.9 ~ 1.21.11), `v26_1`, `v26_2`, `v26_3`; the GLSL syntax differs per version):
   - `assets/minecraft/shaders/post/displayhud_capture.fsh`
   - `assets/minecraft/shaders/post/displayhud_combine.fsh`
   - optional `assets/minecraft/shaders/post/displayhud_sobel.fsh` (vanilla sobel that ignores the `0x01FEFD` colour, only needed for the legacy glow marker)
   - DisplayHud's core shaders, `include/displayhud.glsl` and `assets/displayhud/...` must still be loaded (keep the DisplayHud pack below yours, or copy them too).
2. Declare the private target `"displayhud_above_hand": {}`.
3. Add `displayhud_capture` as the **first** pass. It must read `minecraft:entity_outline` before anything overwrites it, plus `minecraft:main` colour and depth.
4. Keep your passes, but let your last pass write to one of your own targets (e.g. `my_result`) instead of `minecraft:entity_outline`.
5. Add `displayhud_combine` as the **last** pass: `Glow` = your result, `Hud` = `displayhud_above_hand`, output `minecraft:entity_outline`. (Or do the same in your own last shader: `fragColor = hud.a > 0.5 ? hud : yourResult;`)

Skeleton (1.21.9 ~ 26.x; on 1.21.8 use `"vertex_shader": "minecraft:post/screenquad"` for the two DisplayHud passes):
```json
{
    "targets": {
        "displayhud_above_hand": {},
        "my_result": {}
    },
    "passes": [
        {
            "vertex_shader": "minecraft:core/screenquad",
            "fragment_shader": "minecraft:post/displayhud_capture",
            "inputs": [
                { "sampler_name": "In", "target": "minecraft:entity_outline" },
                { "sampler_name": "Main", "target": "minecraft:main" },
                { "sampler_name": "MainDepth", "target": "minecraft:main", "use_depth_buffer": true }
            ],
            "output": "displayhud_above_hand"
        },
        { "comment": "... your passes here, the last one writing to my_result ..." },
        {
            "vertex_shader": "minecraft:core/screenquad",
            "fragment_shader": "minecraft:post/displayhud_combine",
            "inputs": [
                { "sampler_name": "Glow", "target": "my_result" },
                { "sampler_name": "Hud", "target": "displayhud_above_hand" }
            ],
            "output": "minecraft:entity_outline"
        }
    ]
}
```
The `"comment"` entry is a placeholder: replace it with your passes (it is not a valid pass). Every sampler a shader declares must be given by an input of its pass (26.3 / Vulkan is strict about this). If your pack covers several versions with overlays, do this once per overlay with that overlay's shader files.
