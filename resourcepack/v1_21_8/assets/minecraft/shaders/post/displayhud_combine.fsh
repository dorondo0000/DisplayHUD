#version 150

// DisplayHud 2.2 Beta - 손 위 표시 마지막 단계(이 체인의 마지막 패스, 출력 minecraft:entity_outline).
// 복사해 둔 HUD 픽셀이 있으면 그것(불투명)을, 없으면 발광 결과(Glow)를 남긴다. 손을 그린 뒤 이 버퍼가 화면에 덮인다.
// 다른 발광 셰이더와 합칠 때: 그쪽 마지막 결과를 Glow 로 넣으면 된다(README 참고).

uniform sampler2D GlowSampler;
uniform sampler2D HudSampler;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 hud = texture(HudSampler, texCoord);
    fragColor = hud.a > 0.5 ? hud : texture(GlowSampler, texCoord);
}
