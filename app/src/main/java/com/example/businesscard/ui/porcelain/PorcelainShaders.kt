package com.example.businesscard.ui.porcelain

/**
 * Porcelain の板(パネル)の面を描く AGSL シェーダー。
 *
 * 見本では、明るいグレーの板の中ほどを「波打つ薄い板」が横切っていて、その縁の向こうに隠れた光源が
 * 奥の面を白〜暖色に照らしている(縁から上へ光がにじむ)。手前の波の板は、光を背にしているので少し暗く、
 * 縁の近くは光が透けて暖色になる。
 *
 * - 波の縁: 見本の縁を正規化した曲線(u = 0..1 で左→右、0 = いちばん高い、1 = いちばん低い)を
 *   正弦級数で近似したもの。[waveTop] が最も高い所、[waveAmp] が高低差
 * - 奥の面: 縁からの距離のガウス(にじみ)と指数(芯)で、白〜暖色の光を混ぜる
 * - 手前の板: 縁から少し下がった所がいちばん暗く、下へ行くほど地の色に戻る。光源から遠い右側ほど陰が深い
 *
 * 色の計算は sRGB のまま行う(見本の写真の値に合わせて決めた係数なので)。
 * 同じソースを docs/porcelain/tools/ の Skia(CPU)でも実行して、見本と見比べている。
 */
internal object PorcelainShaders {

    const val PANEL = """
uniform float2 size;
uniform float dp;
uniform float waveTop;
uniform float waveAmp;
uniform float glow;
layout(color) uniform half4 panelTop;
layout(color) uniform half4 panelBottom;
layout(color) uniform half4 glowCore;
layout(color) uniform half4 glowWarm;
layout(color) uniform half4 sheetTint;

const float PI = 3.14159265;

// 見本の波の縁。u = 0..1(左→右)。0 = いちばん高い、1 = いちばん低い
float waveShape(float u) {
    return 1.0 - u
        - 0.0395 * sin(PI * u)
        - 0.1511 * sin(2.0 * PI * u)
        + 0.2231 * sin(3.0 * PI * u)
        - 0.0839 * sin(4.0 * PI * u)
        - 0.1239 * sin(5.0 * PI * u);
}

// waveShape の傾き(u で微分)
float waveSlope(float u) {
    return -1.0 + PI * (
        - 0.0395 * cos(PI * u)
        - 2.0 * 0.1511 * cos(2.0 * PI * u)
        + 3.0 * 0.2231 * cos(3.0 * PI * u)
        - 4.0 * 0.0839 * cos(4.0 * PI * u)
        - 5.0 * 0.1239 * cos(5.0 * PI * u));
}

half4 main(float2 p) {
    float u = clamp(p.x / size.x, 0.0, 1.0);
    float t = clamp(p.y / size.y, 0.0, 1.0);

    // 板の地: 上が明るく下が少し暗い。右へ行くほどわずかに暗い(光は左上から)
    half3 base = mix(panelTop.rgb, panelBottom.rgb, half(t)) * half(1.0 - 0.04 * u);

    // 縁までの距離(縁に垂直な向きに近似)。+ = 縁より下(手前の波の板)、- = 縁より上(奥の面)
    float edgeY = waveTop + waveAmp * waveShape(u);
    float slope = waveAmp * waveSlope(u) / size.x;
    float d = (p.y - edgeY) / sqrt(1.0 + slope * slope);

    // 光源は縁の向こうの左寄り(u = 0.3 あたり)に隠れている。そこに近いほど強く、広く、白く光る
    float near = exp(-((u - 0.3) * (u - 0.3)) / (0.22 * 0.22));

    // 奥の面: 縁の向こうの光源が照らす。芯は白く、にじみは暖色
    float a = max(-d, 0.0) / dp;
    float r = 30.0 + 16.0 * near;
    float halo = exp(-(a * a) / (r * r)) * (0.9 + 0.25 * near);
    float bloom = exp(-(a * a) / (85.0 * 85.0)) * 0.3 * near;
    float core = exp(-a / (7.0 + 6.0 * near)) * (0.3 + 0.7 * near);
    half3 lit = mix(glowWarm.rgb, glowCore.rgb, half(core));
    half3 back = mix(base, lit, half(clamp((halo + bloom) * glow, 0.0, 1.0)));

    // 手前の波の板: 光を背にしているので陰る。光源から遠い右側ほど陰が深い。縁の近くは光が透けて暖色
    float b = max(d, 0.0) / dp;
    float shadeMax = 0.08 + 0.10 * smoothstep(0.25, 0.6, u) + 0.16 * smoothstep(0.62, 0.85, u);
    float shade = shadeMax * (0.6 + 0.4 * smoothstep(0.0, 12.5, b)) * exp(-max(b - 19.0, 0.0) / 62.0);
    float warm = exp(-b / 24.0) * (0.45 + 0.55 * near) * (0.35 + 0.65 * glow);
    half3 front = base * half(1.0 - shade);
    front *= mix(half3(1.0), sheetTint.rgb, half(warm));
    // 左端は板が手前へ反り、裏側の暗い面が少し見える
    front *= half(1.0 - 0.28 * smoothstep(0.06, 0.0, u) * exp(-b / 7.0));

    // 縁は 1px ほどでなめらかにつなぐ
    half3 col = mix(back, front, half(smoothstep(-0.75, 0.75, d)));
    return half4(col, 1.0);
}
"""
}
