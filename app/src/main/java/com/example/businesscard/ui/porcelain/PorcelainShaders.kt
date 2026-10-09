package com.example.businesscard.ui.porcelain

/**
 * Porcelain の光と陰影を計算する AGSL シェーダー(Android 13 以上の RuntimeShader で GPU 上で動かす)。
 *
 * ## 見立て(照明の模型)
 * 見本の板は、次のものでできていると考えて、光の量を計算して描く。
 *
 * ```
 *   横から見た図(左が奥の壁、右が見ている人)
 *
 *      奥の面 ─┐
 *              │      ボタン・名刺(奥の面から浮いている)
 *              │   ↖
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
 * - ボタン・名刺: 奥の面や波の板から浮いた、縁の丸い陶器の板
 *
 * ## 計算すること(すべて線形の光の量で足し合わせ、最後に画面の色に戻す)
 * 1. 帯の光: 帯を短い直線に分け、各直線が受け手の点に届ける光を厳密に積分して足す
 *    (点光源の照度 (n·r) / |r|^3 を直線に沿って積分した式。点を並べて足すと粒が見えるので式で積分する)。
 *    受け手の面の向き(法線)も入るので、奥の面にも、ボタンの縁の丸みにも同じ式で光が当たる
 * 2. 奥の面: 帯の光 + 奥の面で跳ね返った光(まわりをうっすら明るくする)
 * 3. 手前の板: 部屋の光の陰影(反りの法線)+ 裏からの透過光(暖かい色に色づいて縁から下へ薄れる)
 *    + 縁の照り(板の厚みが光源のすぐそばで細く光る)。手前の板より下にある部品には、帯の光は届かない
 * 4. ボタン・名刺: 縁の丸みを法線にした部屋の光の陰影 + 帯の光(光源のほうを向いた縁が暖かく光る)
 * 5. グレア: とても明るい所は、目やカメラの中で光がにじむ
 * 6. 明るすぎる所は白へ向かってなめらかに丸める(芯は白く飛び、まわりは暖色に見える)。
 *    HDR 対応の画面では、hdr 倍まで白より明るく光らせる
 *
 * 色の計算は線形 sRGB で行う。座標の単位はピクセル、`dp` は 1dp のピクセル数。
 * 同じソースを docs/porcelain/tools/ の Skia(CPU)でも実行して、見本と見比べている。
 */
internal object PorcelainShaders {

    /** 照明の模型(全シェーダー共通)。波の縁・帯の光源・部屋の光・明るさの丸め方。 */
    private const val LIGHT_RIG = """
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

const float PI = 3.14159265;

// 帯を何本の直線に分けて積分するか(各直線の中は厳密に積分するので、少なくても粒は出ない)
const int SEGMENTS = 18;

// ---- 照明の寸法(dp) ----
// 帯は見えている縁より少し下(板の裏)にある
const float STRIP_DROP = 3.0;
// 帯の、奥の面からの高さ。小さいほど縁の近くに光が集まる
const float WALL_GAP = 8.0;

// ---- 部屋の光(左上・手前から) ----
const float3 KEY_DIR = float3(-0.42, -0.72, 0.55);

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

// 帯の光が、点 P(パネルの座標)・奥の面からの高さ zP・向き n の面に届く量。
// 帯の各直線について、点光源の照度 (n·r)/|r|^3 を直線に沿って厳密に積分して足す。
// まっすぐ無限に続く帯のすぐ横の奥の面が 1 になるようにそろえてある
float stripLight(float2 P, float zP, float3 n, float gap) {
    float dz = gap - zP;
    float sum = 0.0;
    float stride = panelSize.x / float(SEGMENTS);
    float drop = STRIP_DROP * dp;
    float2 a = float2(0.0, edgeAt(0.0) + drop);
    for (int i = 0; i < SEGMENTS; i++) {
        float x1 = stride * float(i + 1);
        float2 b = float2(x1, edgeAt(x1) + drop);
        float2 ab = b - a;
        float len = length(ab);
        float2 dir = ab / len;
        float2 w = a - P;
        // 直線上の位置 s(P から下ろした垂線の足が 0)で、s = t0 .. t1 の範囲を積分する
        float t0 = dot(w, dir);
        float t1 = t0 + len;
        float D2 = max(dot(w, w) - t0 * t0, 0.0) + dz * dz;
        float c1 = dot(n.xy, dir);
        float c0 = dot(n.xy, w) + n.z * dz - t0 * c1;
        float r0 = sqrt(D2 + t0 * t0);
        float r1 = sqrt(D2 + t1 * t1);
        float e = c0 * (t1 / r1 - t0 / r0) / D2 - c1 * (1.0 / r1 - 1.0 / r0);
        // 面の裏から当たる光は届かない
        sum += stripPower((a.x + 0.5 * ab.x) / panelSize.x) * max(e, 0.0);
        a = b;
    }
    return sum * 0.5 * gap;
}

// 平らな奥の面(向きが正面、高さ 0)が、帯の1本の直線(高さ h)から受ける光。stripLight の特別な場合で、
// 板の面のように1画素で何種類もの光を計算するときに、直線の位置関係(perp2, t0, t1)を使い回すためのもの
float flatLight(float perp2, float h, float t0, float t1) {
    float D2 = perp2 + h * h;
    return 0.5 * h * h / D2 * (t1 / sqrt(D2 + t1 * t1) - t0 / sqrt(D2 + t0 * t0));
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
"""

    /**
     * 板(パネル)の面: 奥の面・手前の波の板・帯の光。
     * 描く範囲はパネル全体(左上が 0,0)。
     */
    const val PANEL = LIGHT_RIG + """
layout(color) uniform half4 panelTop;
layout(color) uniform half4 panelBottom;
layout(color) uniform half4 sheetFilter;

// 奥の面で跳ね返った光の広がり(dp)
const float BOUNCE_GAP = 45.0;
// 帯から手前の板の裏までの距離(透過光の広がり、dp)
const float SHEET_GAP = 22.0;

// 光の強さ(1 = 帯のすぐ横の奥の面を、地の明るさと同じだけ照らす)
const float WALL_POWER = 12.0;
const float BOUNCE_POWER = 0.45;
const float SHEET_POWER = 0.8;
const float LIP_POWER = 2.2;
const float GLARE_POWER = 0.10;
// グレアの裾の広がり(小さいほど広い)
const float GLARE_K = 0.012;

// 手前の板が部屋の光から受ける明るさ
const float AMBIENT = 0.22;
// 手前の板は奥の面より部屋の光を受けにくい(上からの光が浅く当たる)
const float SHEET_ALBEDO = 0.9;

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
        for (int i = 0; i < SEGMENTS; i++) {
            float x1 = stride * float(i + 1);
            float2 b = float2(x1, edgeAt(x1) + drop);
            float2 ab = b - a;
            float len = length(ab);
            float2 dir = ab / len;
            float2 w = a - p;
            float t0 = dot(w, dir);
            float t1 = t0 + len;
            float perp2 = max(dot(w, w) - t0 * t0, 0.0);
            float power = stripPower((a.x + 0.5 * ab.x) / panelSize.x);
            if (onWall) {
                wall += power * flatLight(perp2, hWall, t0, t1);
                // 跳ね返りの光は、強い所(左寄り)ほど目立つ
                bounce += power * power * flatLight(perp2, hBounce, t0, t1);
            }
            if (onSheet) {
                // 板を透けて見えるのは強い光だけ(弱い所は板の陰に負ける)
                sheet += power * power * flatLight(perp2, hSheet, t0, t1);
            }
            // グレア: にじみの形 1 / (1 + k r^2)(r は dp)を直線に沿って積分。裾が長い
            float q = sqrt(1.0 + GLARE_K * perp2 / (dp * dp));
            glare += power * (atan(sk * t1 / (dp * q)) - atan(sk * t0 / (dp * q))) / (sk * q) * 0.05;
            a = b;
        }
    }

    // ---- 奥の面: 部屋の光 + 帯の光 + 跳ね返りの光 ----
    float3 back = base + base * light * (wall * WALL_POWER + bounce * BOUNCE_POWER) * glow;

    // ---- 手前の波の板 ----
    float3 front = base;
    if (d > -1.0) {
        float b = max(d, 0.0) / dp;
        // 縁の近くは手前へ反っている(法線が下を向き、左上の部屋の光が当たりにくい)。
        // 縁が高い所(右)ほど板が高く、反りも大きい
        float height = 1.0 - waveShape(u);
        float reach = 16.0 + 30.0 * height;
        float tilt = (1.05 + 0.3 * height) * (1.0 - exp(-b / 1.5)) * exp(-b / reach);
        // 板の内側へ向かう向き(縁に垂直)
        float2 inward = float2(-slope, 1.0) / norm;
        float3 n = normalize(float3(inward * sin(tilt), cos(tilt)));
        float3 key = normalize(KEY_DIR);
        float lit = (AMBIENT + (1.0 - AMBIENT) * max(dot(n, key), 0.0)) / (AMBIENT + (1.0 - AMBIENT) * key.z);
        front = base * SHEET_ALBEDO * lit;
        if (glow > 0.0) {
            // 裏からの光が透ける。板の中で散って、暖かい色に色づく
            front += base * light * toLin(sheetFilter) * sheet * SHEET_POWER * glow;
            // 縁の厚みが光源に照らされて、細く光る
            front += mix(light, float3(1.0), 0.6) * stripPower(u) * glow * LIP_POWER * exp(-b / 1.1);
        }
        // 左端は板が手前へ反り、裏側の暗い面が少し見える
        front *= 1.0 - 0.28 * smoothstep(0.06, 0.0, u) * exp(-b / 7.0);
    }

    // 縁は 1px ほどでなめらかにつなぐ
    float3 col = mix(back, front, smoothstep(-0.75, 0.75, d));
    col += light * glare * GLARE_POWER * glow;
    return half4(half3(toDisplay(toneMap(col, max(hdr, 1.0)))), 1.0);
}
"""

    /**
     * ボタン・名刺などの陶器の板の面。縁は丸く(ベベル)、その向きで部屋の光の陰影が付き、
     * 帯の光源のほうを向いた縁は暖かく照らされる。
     * 描く範囲は部品の大きさ(左上が 0,0)。形の外は透明。
     */
    const val SURFACE = LIGHT_RIG + """
// 部品の大きさと角丸の半径(ピクセル)
uniform float2 size;
uniform float radius;
// 部品の左上の、パネルの座標での位置
uniform float2 origin;
// 縁の丸みの幅(ピクセル)。0 = 平ら(押し込まれた・彫り込まれた面)
uniform float bevel;
// 帯の光を受けるか(1 = 受ける。パネルの外、ダイアログなどでは 0)
uniform float receive;
// 面のつや(白い光沢の強さ)
uniform float sheen;
layout(color) uniform half4 topColor;
layout(color) uniform half4 bottomColor;

// 部品の面の、奥の面からの高さ(dp)。帯(WALL_GAP)より手前なので、正面には帯の光は当たらず、
// 光源のほうを向いた縁の丸みにだけ当たる
const float FACE_HEIGHT = 13.0;
// 部品の陰影の柔らかさ(環境光の割合)。陶器の釉薬は光をよく散らすので、陰は浅い
const float AMBIENT = 0.5;
const float RIM_POWER = 1.6;

float sdRoundRect(float2 q, float2 b, float r) {
    float2 v = abs(q) - b + float2(r);
    return length(max(v, float2(0.0))) + min(max(v.x, v.y), 0.0) - r;
}

// いちばん近い縁へ向かう外向きの向き
float2 outwardOf(float2 q, float2 b, float r) {
    float2 v = abs(q) - b + float2(r);
    float2 s = float2(q.x < 0.0 ? -1.0 : 1.0, q.y < 0.0 ? -1.0 : 1.0);
    if (v.x > 0.0 && v.y > 0.0) {
        return s * normalize(v);
    }
    if (v.x > v.y) {
        return float2(s.x, 0.0);
    }
    return float2(0.0, s.y);
}

half4 main(float2 p) {
    float2 halfS = size * 0.5;
    float r = min(radius, min(halfS.x, halfS.y));
    float2 q = p - halfS;
    float sd = sdRoundRect(q, halfS, r);
    float coverage = clamp(0.5 - sd, 0.0, 1.0);
    if (coverage <= 0.0) {
        return half4(0.0);
    }

    // 縁の丸み: 縁で最も外を向き、内側へ bevel 進むと正面を向く
    float k = bevel > 0.0 ? clamp(1.0 + sd / bevel, 0.0, 1.0) : 0.0;
    float tilt = 1.2 * k * sqrt(k);
    float3 n = float3(outwardOf(q, halfS, r) * sin(tilt), cos(tilt));

    float3 albedo = mix(toLin(topColor), toLin(bottomColor), clamp(p.y / size.y, 0.0, 1.0));
    float3 key = normalize(KEY_DIR);
    float lit = (AMBIENT + (1.0 - AMBIENT) * max(dot(n, key), 0.0)) / (AMBIENT + (1.0 - AMBIENT) * key.z);
    float3 col = albedo * lit;
    // 釉薬のつや: 光の向きと視線の中間を向いた所が白く光る
    float3 h = normalize(key + float3(0.0, 0.0, 1.0));
    col += float3(pow(max(dot(n, h), 0.0), 28.0) * sheen);

    // 帯の光。手前の波の板より下にある部品には、板に遮られて届かない
    if (receive > 0.0 && glow > 0.0) {
        float2 P = origin + p;
        float open = smoothstep(6.0 * dp, -6.0 * dp, P.y - edgeAt(P.x));
        if (open > 0.0) {
            float e = stripLight(P, FACE_HEIGHT * dp, n, WALL_GAP * dp);
            col += albedo * toLin(lightColor) * e * RIM_POWER * glow * open;
        }
    }
    float3 shown = toDisplay(toneMap(col, max(hdr, 1.0)));
    return half4(half3(shown) * coverage, coverage);
}
"""
}
