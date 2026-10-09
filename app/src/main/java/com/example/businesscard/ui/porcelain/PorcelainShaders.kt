package com.example.businesscard.ui.porcelain

/**
 * Porcelain の板(パネル)の面を描く AGSL シェーダー(Android 13 以上の RuntimeShader で GPU 上で動かす)。
 * 光の量を計算して描く。
 *
 * ## 見立て(照明の模型)
 *
 * ```
 *   横から見た図(左が奥の壁、右が見ている人)
 *
 *      奥の面 ─┐
 *              │   ↖ 光は奥の面を照らし、縁の上へ広がる
 *              │  ◉ 帯状の光源(縁の裏に隠れている)
 *              │  ┃
 *              │  ┃ 手前の波の板(薄い陶器。裏からの光が透ける)
 *              │  ┃
 * ```
 *
 * - 部屋の光: 左上・手前から差す光(キーライト)と、まわりからの柔らかい光(環境光)
 * - 奥の面: 部屋の光で照らされた明るいグレーの面(panelTop → panelBottom)
 * - 手前の波の板: 奥の面の手前に立つ薄い板。上の縁が波打っている。縁の近くは手前へ少し反っていて、
 *   左上から来る部屋の光が当たりにくい(縁の下が陰る)
 * - 帯状の光源: 波の板の裏、縁のすぐ下に、縁に沿って取り付けた光の帯(間接照明)。見ている人からは見えない
 *
 * ## 計算すること(すべて線形の光の量で足し合わせ、最後に画面の色に戻す)
 * 1. 奥の面: 帯を短い直線に分け、各直線が奥の面の点に届ける光を厳密に積分して足す
 *    (点光源の照度 h / r^3 を直線に沿って積分した式。点を並べて足すと粒が見えるので式で積分する)。
 *    帯の明るさは場所でなめらかに変わる(左寄りがいちばん明るい。見本の光の位置)。
 *    跳ね返った光がまわりをうっすら明るくする分も同じ式で足す
 * 2. 手前の板: 反りの法線による部屋の光の陰影 + 裏からの透過光(暖かい色に色づいて縁から下へ薄れる)
 * 3. グレア: とても明るい所は、目やカメラの中で光がにじむ。手前の板の輪郭をぼかさないよう、奥の面にだけ足す
 * 4. 明るすぎる所は白へ向かってなめらかに丸める(芯は白く飛び、まわりは暖色に見える)。
 *    HDR 対応の画面では、hdr 倍まで白より明るく光らせる
 * 5. ゆるやかなグラデーションに段差(縞)が出ないよう、画素ごとにごく小さな揺らぎ(ディザ)を足す
 *
 * 手前の板の輪郭は 1px でくっきり切り替える(光が板の側へはみ出すと、輪郭がぼやけて粗く見える)。
 *
 * 色の計算は線形 sRGB で行う。座標の単位はピクセル、`dp` は 1dp のピクセル数。
 * 同じソースを docs/porcelain/tools/ の Skia(CPU)でも実行して、見本と見比べている。
 */
internal object PorcelainShaders {

    const val PANEL = """
// 板(パネル)の大きさと、波の縁の位置(パネルの座標、ピクセル)
uniform float2 panelSize;
uniform float dp;
uniform float waveTop;
uniform float waveAmp;
// 帯の光の灯り具合 0..1(画面を開くとゆっくり灯る)
uniform float glow;
// HDR表示の余裕(白の何倍まで出せるか)。1 = 通常の画面
uniform float hdr;
layout(color) uniform half4 lightColor;
layout(color) uniform half4 panelTop;
layout(color) uniform half4 panelBottom;
layout(color) uniform half4 sheetFilter;

const float PI = 3.14159265;

// 帯を何本の直線に分けて積分するか(各直線の中は厳密に積分するので、少なくても粒は出ない)
const int SEGMENTS = 18;

// ---- 照明の寸法(dp) ----
// 帯は見えている縁より少し下(板の裏)にある
const float STRIP_DROP = 3.0;
// 帯の、奥の面からの高さ。小さいほど縁の近くに光が集まる
const float WALL_GAP = 8.0;
// 奥の面で跳ね返った光の広がり
const float BOUNCE_GAP = 45.0;
// 帯から手前の板の裏までの距離(透過光の広がり)
const float SHEET_GAP = 22.0;

// ---- 光の強さ(1 = 帯のすぐ横の奥の面を、地の明るさと同じだけ照らす) ----
const float WALL_POWER = 12.0;
const float BOUNCE_POWER = 0.45;
const float SHEET_POWER = 0.8;
const float GLARE_POWER = 0.10;
// グレアの裾の広がり(小さいほど広い)
const float GLARE_K = 0.012;

// ---- 部屋の光(左上・手前から) ----
const float3 KEY_DIR = float3(-0.42, -0.72, 0.55);
// 手前の板が部屋の光から受ける明るさ(環境光の割合)
const float AMBIENT = 0.22;
// 手前の板は奥の面より部屋の光を受けにくい(上からの光が浅く当たる)
const float SHEET_ALBEDO = 0.9;

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

// パネルの x(ピクセル)での縁の高さ
float edgeAt(float x) {
    return waveTop + waveAmp * waveShape(clamp(x / panelSize.x, 0.0, 1.0));
}

// 帯の明るさ。見本では左寄り(u = 0.33 あたり)がいちばん明るく、右へ行くほど控えめ
float stripPower(float u) {
    float d = (u - 0.33) / 0.21;
    return 0.45 + 0.55 * exp(-d * d);
}

// 平らな面が、帯の1本の直線(面からの高さ h)から受ける光。点光源の照度 h / r^3 を直線に沿って厳密に積分した式。
// perp2 は点から直線までの距離の2乗、t0..t1 は点から下ろした垂線の足を 0 とした直線の範囲。
// 帯の明るさは直線の両端 pa → pb のあいだで一様に変わる(直線ごとに段差が出ないように)。
// まっすぐ無限に続く、明るさ 1 の帯のすぐ横の面が 1 になるようにそろえてある
float flatLight(float perp2, float h, float t0, float t1, float pa, float pb) {
    float D2 = perp2 + h * h;
    float r0 = sqrt(D2 + t0 * t0);
    float r1 = sqrt(D2 + t1 * t1);
    // ∫ ds / r^3 と ∫ s ds / r^3
    float i0 = (t1 / r1 - t0 / r0) / D2;
    float i1 = 1.0 / r0 - 1.0 / r1;
    float k = (pb - pa) / (t1 - t0);
    return 0.5 * h * h * (pa * i0 + k * (i1 - t0 * i0));
}

// 線形の光の量を画面に出せる明るさへ丸める。peak は出せる最大(通常の画面は 1、HDR は白の何倍まで出せるか)。
// とても明るい光は色が抜けて白く見える(フィルムや目と同じ)。0.8 までは変えない
float3 toneMap(float3 x, float peak) {
    float lum = dot(x, float3(0.2126, 0.7152, 0.0722));
    x = mix(x, float3(lum), smoothstep(0.75, 2.4, lum) * 0.8);
    float3 over = max(x - float3(0.8), float3(0.0));
    return min(x, float3(0.8)) + over / (float3(1.0) + over / (peak - 0.8));
}

float3 toLin(half4 c) {
    return float3(toLinearSrgb(c.rgb));
}

float3 toDisplay(float3 c) {
    return float3(fromLinearSrgb(half3(c)));
}

// 画素ごとの乱数 0..1(ディザ用)
float hash12(float2 p) {
    float3 p3 = fract(float3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

half4 main(float2 p) {
    float u = clamp(p.x / panelSize.x, 0.0, 1.0);
    float t = clamp(p.y / panelSize.y, 0.0, 1.0);

    // 部屋の光だけで照らした地の色(上が明るく下が少し暗い。光は左上から)
    float3 base = mix(toLin(panelTop), toLin(panelBottom), t) * (1.0 - 0.05 * u);
    float3 light = toLin(lightColor);

    // 縁までの距離(縁に垂直な向きに近似)。+ = 縁より下(手前の波の板)、- = 縁より上(奥の面)
    float edgeY = edgeAt(p.x);
    float slope = waveAmp * waveSlope(u) / panelSize.x;
    float norm = sqrt(1.0 + slope * slope);
    float d = (p.y - edgeY) / norm;

    // ---- 帯の光: 帯を1回たどって、奥の面・跳ね返り・透過・グレアをまとめて積分する ----
    float wall = 0.0;
    float bounce = 0.0;
    float sheet = 0.0;
    float glare = 0.0;
    if (glow > 0.0) {
        bool onWall = d < 1.0;
        bool onSheet = d > -1.0;
        float hWall = WALL_GAP * dp;
        float hBounce = BOUNCE_GAP * dp;
        float hSheet = SHEET_GAP * dp;
        float sk = sqrt(GLARE_K);
        float stride = panelSize.x / float(SEGMENTS);
        float drop = STRIP_DROP * dp;
        float2 a = float2(0.0, edgeAt(0.0) + drop);
        float pa = stripPower(0.0);
        for (int i = 0; i < SEGMENTS; i++) {
            float x1 = stride * float(i + 1);
            float2 b = float2(x1, edgeAt(x1) + drop);
            float pb = stripPower(x1 / panelSize.x);
            float2 ab = b - a;
            float len = length(ab);
            float2 dir = ab / len;
            float2 w = a - p;
            float t0 = dot(w, dir);
            float t1 = t0 + len;
            float perp2 = max(dot(w, w) - t0 * t0, 0.0);
            if (onWall) {
                wall += flatLight(perp2, hWall, t0, t1, pa, pb);
                // 跳ね返りの光は、強い所(左寄り)ほど目立つ
                bounce += flatLight(perp2, hBounce, t0, t1, pa * pa, pb * pb);
                // グレア: にじみの形 1 / (1 + k r^2)(r は dp)を直線に沿って積分。裾が長い
                float q = sqrt(1.0 + GLARE_K * perp2 / (dp * dp));
                glare += 0.5 * (pa + pb) * (atan(sk * t1 / (dp * q)) - atan(sk * t0 / (dp * q))) / (sk * q) * 0.05;
            }
            if (onSheet) {
                // 板を透けて見えるのは強い光だけ(弱い所は板の陰に負ける)
                sheet += flatLight(perp2, hSheet, t0, t1, pa * pa, pb * pb);
            }
            a = b;
            pa = pb;
        }
    }

    // ---- 奥の面: 部屋の光 + 帯の光 + 跳ね返りの光 + グレア ----
    float3 back = base + base * light * (wall * WALL_POWER + bounce * BOUNCE_POWER) * glow
        + light * glare * GLARE_POWER * glow;

    // ---- 手前の波の板 ----
    float3 front = base;
    if (d > -1.0) {
        float b = max(d, 0.0) / dp;
        // 縁の近くは手前へ反っている(法線が下を向き、左上の部屋の光が当たりにくい)。
        // 縁が高い所(右)ほど板が高く、反りも大きい。縁のすぐ下から陰る(輪郭がぼやけないように)
        float height = 1.0 - waveShape(u);
        float reach = 16.0 + 30.0 * height;
        float tilt = (1.05 + 0.3 * height) * (1.0 - exp(-b / 0.4)) * exp(-b / reach);
        // 板の内側へ向かう向き(縁に垂直)
        float2 inward = float2(-slope, 1.0) / norm;
        float3 n = normalize(float3(inward * sin(tilt), cos(tilt)));
        float3 key = normalize(KEY_DIR);
        float lit = (AMBIENT + (1.0 - AMBIENT) * max(dot(n, key), 0.0)) / (AMBIENT + (1.0 - AMBIENT) * key.z);
        front = base * SHEET_ALBEDO * lit;
        // 裏からの光が透ける。板の中で散って、暖かい色に色づく
        front += base * light * toLin(sheetFilter) * sheet * SHEET_POWER * glow;
        // 左端は板が手前へ反り、裏側の暗い面が少し見える
        front *= 1.0 - 0.28 * smoothstep(0.06, 0.0, u) * exp(-b / 7.0);
    }

    // 輪郭は 1px でくっきり切り替える(なめらかにするのは 1px だけ)
    float3 col = mix(back, front, smoothstep(-0.5, 0.5, d));
    float3 shown = toDisplay(toneMap(col, max(hdr, 1.0)));
    // ディザ: ±0.5 段ぶんの三角分布の揺らぎ(8bit の段差を消す)
    shown += (hash12(p) + hash12(p + float2(17.3, 41.7)) - 1.0) / 255.0;
    return half4(half3(shown), 1.0);
}
"""
}
