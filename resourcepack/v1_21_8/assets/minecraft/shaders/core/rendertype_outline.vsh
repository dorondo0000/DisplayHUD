#version 150

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;

out vec4 vertexColor;
out vec2 texCoord0;

#moj_import <minecraft:displayhud.glsl> // DisplayHud

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    vertexColor = Color;
    texCoord0 = UV0;

    // DisplayHud: 아주 아래(y < -1000)에 놓인 꼭짓점은 화면 고정 HUD 로 옮긴다
    if (displayhud_is_hud(Position)) {
        gl_Position = displayhud_clip(Position);
    }
}
