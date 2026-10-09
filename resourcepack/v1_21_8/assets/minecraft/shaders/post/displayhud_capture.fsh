#version 150

// DisplayHud 2.2 Beta - 손 위 표시 1단계(이 체인의 첫 패스).
// 이 체인(entity_outline)은 레벨 렌더 안에서 1인칭 손보다 먼저 돌고, 결과(minecraft:entity_outline)는 손을 그린 뒤 화면에 알파 합성된다.
// 손 위로 올릴 HUD 픽셀(밝기 표식 → 전용 깊이 구간, 또는 예전 발광 표식)만 골라 손 그리기 전 메인 화면을 복사해 둔다.

uniform sampler2D InSampler;
uniform sampler2D MainSampler;
uniform sampler2D MainDepthSampler;

in vec2 texCoord;

out vec4 fragColor;

// 예전 방식(발광 표식): 발광색 0x01FEFD (R=1, G=254, B=253) 로 빛나는 아이템/블록 디스플레이 자리.
// 서버 DisplayHud.ABOVE_HAND_GLOW_COLOR 와 같아야 한다. 손 위 표시 트리거 디스플레이도 이 색으로 빛난다(투명이라 찍히지는 않는다).
bool displayhud_is_glow_mask(vec4 c) {
    vec3 rgb = c.rgb * 255.0;
    return c.a > 0.5 && abs(rgb.r - 1.0) < 0.6 && abs(rgb.g - 254.0) < 0.6 && abs(rgb.b - 253.0) < 0.6;
}

// 손 위 표시 깊이 구간(core 셰이더 displayhud_above_hand_depth): NDC -0.99 ~ -0.90 → 깊이 버퍼 0.005 ~ 0.05.
// 보통 HUD 는 0.5 근처, 월드는 카메라에서 0.055 블록 안쪽이어야 0.075 아래로 내려온다.
bool displayhud_is_above_hand_depth(float depth) {
    return depth < 0.075;
}

void main() {
    if (displayhud_is_above_hand_depth(texture(MainDepthSampler, texCoord).r) || displayhud_is_glow_mask(texture(InSampler, texCoord))) {
        fragColor = vec4(texture(MainSampler, texCoord).rgb, 1.0);
    } else {
        fragColor = vec4(0.0);
    }
}
