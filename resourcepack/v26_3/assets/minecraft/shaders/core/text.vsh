#version 330
#extension GL_ARB_separate_shader_objects : require

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
#include <minecraft:fog.glsl>
#include <minecraft:sample_lightmap.glsl>
#endif

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
layout(location = 3) in ivec2 UV2;
#endif

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
uniform sampler2D Sampler2;
layout(location = 0) out float sphericalVertexDistance;
layout(location = 1) out float cylindricalVertexDistance;
#endif

layout(location = 2) out vec4 vertexColor;
layout(location = 3) out vec2 texCoord0;

#include <minecraft:globals.glsl> // DisplayHud
#include <minecraft:displayhud.glsl> // DisplayHud

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
    vertexColor = Color * sample_lightmap(Sampler2, UV2);
#else
    vertexColor = Color;
#endif
    texCoord0 = UV0;

    // DisplayHud: 아주 아래(y < -1000)에 놓인 꼭짓점은 화면 고정 HUD 로 옮긴다
#ifndef IS_GUI
    if (displayhud_is_hud(Position)) {
        gl_Position = displayhud_clip(Position);
#if !defined(IS_SEE_THROUGH) && !defined(OIT)
        // DisplayHud 2.2: 손 위 표시 표식(밝기 block 1 / sky 2)이면 손보다 위로 올릴 전용 깊이 구간에 그린다
        gl_Position.z = displayhud_above_hand_depth(gl_Position.z, UV2);
#endif
        vertexColor = Color;
#ifndef IS_SEE_THROUGH
        sphericalVertexDistance = 0.0;
        cylindricalVertexDistance = 0.0;
#endif
    }
#endif
}
