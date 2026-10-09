#version 150

// DisplayHud 2.2 Beta - 바닐라 entity_sobel 과 같지만, 발광 표식(0x01FEFD) 자리는 비어 있는 것으로 본다
// (예전 발광 표식 HUD 와 트리거 둘레에 발광 테두리가 생기지 않게). 다른 색 발광은 바닐라와 똑같다.

uniform sampler2D InSampler;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

// 예전 방식(발광 표식): 발광색 0x01FEFD (R=1, G=254, B=253) 로 빛나는 아이템/블록 디스플레이 자리.
// 서버 DisplayHud.ABOVE_HAND_GLOW_COLOR 와 같아야 한다. 손 위 표시 트리거 디스플레이도 이 색으로 빛난다(투명이라 찍히지는 않는다).
bool displayhud_is_glow_mask(vec4 c) {
    vec3 rgb = c.rgb * 255.0;
    return c.a > 0.5 && abs(rgb.r - 1.0) < 0.6 && abs(rgb.g - 254.0) < 0.6 && abs(rgb.b - 253.0) < 0.6;
}

vec4 displayhud_glow_at(vec2 uv) {
    vec4 c = texture(InSampler, uv);
    return displayhud_is_glow_mask(c) ? vec4(0.0) : c;
}

void main(){
    vec4 center = displayhud_glow_at(texCoord);
    vec4 left = displayhud_glow_at(texCoord - vec2(oneTexel.x, 0.0));
    vec4 right = displayhud_glow_at(texCoord + vec2(oneTexel.x, 0.0));
    vec4 up = displayhud_glow_at(texCoord - vec2(0.0, oneTexel.y));
    vec4 down = displayhud_glow_at(texCoord + vec2(0.0, oneTexel.y));
    float leftDiff  = abs(center.a - left.a);
    float rightDiff = abs(center.a - right.a);
    float upDiff    = abs(center.a - up.a);
    float downDiff  = abs(center.a - down.a);
    float total = clamp(leftDiff + rightDiff + upDiff + downDiff, 0.0, 1.0);
    vec3 outColor = center.rgb * center.a + left.rgb * left.a + right.rgb * right.a + up.rgb * up.a + down.rgb * down.a;
    fragColor = vec4(outColor * 0.2, total);
}
