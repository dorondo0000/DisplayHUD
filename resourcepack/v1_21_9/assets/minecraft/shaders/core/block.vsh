#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler2;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;

vec4 minecraft_sample_lightmap(sampler2D lightMap, ivec2 uv) {
    return texture(lightMap, clamp((uv / 256.0) + 0.5 / 16.0, vec2(0.5 / 16.0), vec2(15.5 / 16.0)));
}

#moj_import <minecraft:displayhud.glsl> // DisplayHud

void main() {
    vec3 pos = Position + ModelOffset;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    sphericalVertexDistance = fog_spherical_distance(pos);
    cylindricalVertexDistance = fog_cylindrical_distance(pos);
    vertexColor = Color * minecraft_sample_lightmap(Sampler2, UV2);
    texCoord0 = UV0;

    // DisplayHud: 아주 아래(y < -1000)에 놓인 꼭짓점은 화면 고정 HUD 로 옮긴다
    if (displayhud_is_hud(pos)) {
        gl_Position = displayhud_clip(pos);
        // DisplayHud 2.2: 손 위 표시 표식(밝기 block 1 / sky 2)이면 손보다 위로 올릴 전용 깊이 구간에 그린다
        gl_Position.z = displayhud_above_hand_depth(gl_Position.z, UV2);
        vertexColor = Color;
        sphericalVertexDistance = 0.0;
        cylindricalVertexDistance = 0.0;
    }
}
