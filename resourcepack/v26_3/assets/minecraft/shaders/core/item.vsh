#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:light.glsl>
#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>
#include <minecraft:sample_lightmap.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
layout(location = 3) in ivec2 UV1;
layout(location = 4) in ivec2 UV2;
#ifdef GLINT_SPECIAL
layout(location = 5) in vec2 UV3;
#endif
layout(location = 6) in vec3 Normal;

#ifndef OIT_ALPHA_ONLY
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

layout(location = 0) out float sphericalVertexDistance;
layout(location = 1) out float cylindricalVertexDistance;
#endif
layout(location = 2) out vec4 vertexColor;
#ifndef OIT_ALPHA_ONLY
layout(location = 3) out vec4 lightMapColor;
layout(location = 4) out vec4 overlayColor;
#endif

layout(location = 5) out vec2 texCoord0;
#ifdef GLINT
layout(location = 6) out vec2 texCoordGlint;
#endif

#include <minecraft:globals.glsl> // DisplayHud
#include <minecraft:displayhud.glsl> // DisplayHud

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    #ifndef OIT_ALPHA_ONLY
    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
    #endif
    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color);
    #ifndef OIT_ALPHA_ONLY
    lightMapColor = sample_lightmap(Sampler2, UV2);
    overlayColor = texelFetch(Sampler1, UV1, 0);
    #endif

    texCoord0 = UV0;
    #ifdef GLINT
    #ifdef GLINT_SPECIAL
    texCoordGlint = (TextureMat * vec4(UV3, 0.0, 1.0)).xy;
    #else
    texCoordGlint = (TextureMat * vec4(UV0, 0.0, 1.0)).xy;
    #endif
    #endif

    // DisplayHud: 아주 아래(y < -1000)에 놓인 꼭짓점은 화면 고정 HUD 로 옮긴다
    if (displayhud_is_hud(Position)) {
        gl_Position = displayhud_clip(Position);
#if !defined(OIT)
        // DisplayHud 2.2: 손 위 표시 표식(밝기 block 1 / sky 2)이면 손보다 위로 올릴 전용 깊이 구간에 그린다
        gl_Position.z = displayhud_above_hand_depth(gl_Position.z, UV2);
#endif
        vertexColor = Color;
#ifndef OIT_ALPHA_ONLY
        lightMapColor = vec4(1.0);
        sphericalVertexDistance = 0.0;
        cylindricalVertexDistance = 0.0;
#endif
    }
}
