#version 330

#moj_import <minecraft:light.glsl>
#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:sample_lightmap.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColor;
out vec4 lightMapColor;
out vec4 overlayColor;

out vec2 texCoord0;

#moj_import <minecraft:displayhud.glsl> // DisplayHud

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);

    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color);
    lightMapColor = sample_lightmap(Sampler2, UV2);
    overlayColor = texelFetch(Sampler1, UV1, 0);

    texCoord0 = UV0;

    // DisplayHud: 아주 아래(y < -1000)에 놓인 꼭짓점은 화면 고정 HUD 로 옮긴다
    if (displayhud_is_hud(Position)) {
        gl_Position = displayhud_clip(Position);
        // DisplayHud 2.2: 손 위 표시 표식(밝기 block 1 / sky 2)이면 손보다 위로 올릴 전용 깊이 구간에 그린다
        gl_Position.z = displayhud_above_hand_depth(gl_Position.z, UV2);
        vertexColor = Color;
        lightMapColor = vec4(1.0);
        sphericalVertexDistance = 0.0;
        cylindricalVertexDistance = 0.0;
    }
}
