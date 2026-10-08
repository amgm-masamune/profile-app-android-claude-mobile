package com.example.businesscard.ui.glass

/**
 * すりガラスの描画に使うシェーダー。AGSL(Android Graphics Shading Language)で書き、
 * Android 13 以上の [android.graphics.RuntimeShader] で GPU 上で動かす。
 *
 * 光の計算はすべて「線形の光の量」(リニアsRGB)で行い、最後に画面の色に戻す。
 * こうすると、光を足したり影で減らしたりした結果が、実際の光の振る舞いに近くなる。
 *
 * 座標の単位はピクセル。`dp` は 1dp が何ピクセルかを表し、距離を dp で考えるときに使う。
 */
internal object GlassShaders {

    /** 全シェーダー共通の関数。 */
    private const val COMMON = """
// 角丸長方形の符号付き距離(SDF)。内側は負、外側は正、縁でちょうど 0
float sdRoundRect(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + float2(r);
    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;
}

// いちばん近い縁へ向かう外向きの向き(縁の法線)
float2 sdRoundRectNormal(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + float2(r);
    float2 s = float2(p.x < 0.0 ? -1.0 : 1.0, p.y < 0.0 ? -1.0 : 1.0);
    if (q.x > 0.0 && q.y > 0.0) {
        return s * normalize(q);
    }
    if (q.x > q.y) {
        return float2(s.x, 0.0);
    }
    return float2(0.0, s.y);
}

// 画素ごとの乱数(ざらつき・グラデーションの縞を消すディザに使う)
float hash12(float2 p) {
    float3 p3 = fract(float3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

// 明るすぎる部分を白に向けてなめらかに丸める(0.8 までは変えない)。
// フィルムや目と同じく、とても明るい光は色が抜けて白っぽく見える
float3 toneMap(float3 x) {
    float lum = dot(x, float3(0.2126, 0.7152, 0.0722));
    x = mix(x, float3(lum), smoothstep(0.6, 1.4, lum) * 0.65);
    float3 over = max(x - float3(0.8), float3(0.0));
    return min(x, float3(0.8)) + over / (float3(1.0) + over / 0.2);
}

// 長さを持つ線光源(x0..x1, 高さ0)を、にじみの形 1 / (1 + k d^2) で積分した値。
// 点を並べて足すと粒が見えるので、式で厳密に積分する(x, y, x0, x1 は dp)
float lineGlare(float x, float y, float x0, float x1, float k) {
    float a = sqrt(k);
    float b = sqrt(1.0 + k * y * y);
    return (atan(a * (x1 - x) / b) - atan(a * (x0 - x) / b)) / (a * b);
}

float3 toLin(half4 c) {
    return float3(toLinearSrgb(c.rgb));
}

float3 toDisplay(float3 c) {
    return float3(fromLinearSrgb(half3(c)));
}

// 部屋のキーライトの向き(lightDir)は uniform で受け取る。ふだんは左上・手前から差し(影は右下へ落ちる)、
// 端末を傾けると少し動く
"""

    /**
     * 壁。部屋の光で壁を照らし、各部品の影と、各部品の光源が壁を照らす量を計算する。
     *
     * - 環境光: 部屋全体からの柔らかい光。部品の近くでは少し遮られる(アンビエントオクルージョン)
     * - キーライト: 左上からの光。部品(すりガラスなので3割は透ける)が右下に柔らかい影を落とす
     * - 日差し: 斜めに差し込む帯状の光。影は少しくっきりする
     * - 部品の光源: 下端(または上端)に沿った線状の光源。距離の2乗で弱まり、壁に斜めに当たるほど弱まる
     */
    const val WALL = COMMON + """
uniform float2 size;
uniform float dp;
uniform int count;
uniform float4 rects[16];
uniform float4 props[16];
uniform float4 extra[16];
layout(color) uniform half4 albedoTop;
layout(color) uniform half4 albedoBottom;
layout(color) uniform half4 lightColor;
uniform float3 lightDir;

const float GLASS_T = 0.08;
const float EMIT_GAIN = 9.0;

// 線光源(部品の光る辺)が壁の点 xy を照らす量。光源の上を13点で数値積分する
// (光源は壁から elev 離れているので、点の間隔より遠く、粒にならない)
float emittedLight(float2 xy, float4 r, float elev, float top) {
    float halfW = (r.z - r.x) * 0.5;
    float cx = (r.x + r.z) * 0.5;
    float edgeY = mix(r.w, r.y, top);
    float faceY = mix(1.0, -1.0, top);
    // 光源は壁側を向き、少し下向き(上端なら上向き)
    float3 facing = normalize(float3(0.0, faceY * 0.7, -0.7));
    float lh = min(halfW * 0.74, 130.0 * dp);
    float e = 0.0;
    for (int k = 0; k < 13; k++) {
        float s = float(k) / 6.0 - 1.0;
        float w = 1.0 - smoothstep(0.45, 1.0, abs(s));
        float3 v = float3(cx + s * lh - xy.x, edgeY - xy.y, elev) / dp;
        float d2 = dot(v, v);
        float d = sqrt(d2);
        float cosWall = v.z / d;
        float toward = max(dot(-v / d, facing), 0.0);
        e += w * cosWall * (0.15 + 0.85 * toward) / d2;
    }
    return e * (lh / dp) * EMIT_GAIN * (7.0 / 13.0);
}

half4 main(float2 xy) {
    float2 uv = clamp(xy / size, 0.0, 1.0);
    float3 albedo = mix(toLin(albedoTop), toLin(albedoBottom), uv.y);

    // 部屋の左下の隅は暗い
    float corner = exp(-length((xy - float2(0.0, size.y)) / dp) / 260.0);

    // 左上から右下へ斜めに差し込む日差しの帯
    float2 beamDir = normalize(float2(1.0, 1.36));
    float2 beamNormal = float2(beamDir.y, -beamDir.x);
    float across = dot(xy - size * float2(0.3, 0.4), beamNormal) / dp;
    float beam = 1.0 - smoothstep(25.0, 95.0, abs(across));

    // 影は光と逆向きに、浮いている高さに比例してずれる
    float3 l = normalize(lightDir);
    float2 shadowDir = -l.xy / l.z;

    float keyVis = 1.0;
    float sunVis = 1.0;
    float ao = 1.0;
    float emitted = 0.0;
    for (int i = 0; i < 16; i++) {
        if (i >= count) {
            break;
        }
        float4 r = rects[i];
        float4 pr = props[i];
        // 部品の見えている度合い(登場の途中は薄い)。見えていない板は影も落とさない
        float opacity = extra[i].x;
        float2 halfS = (r.zw - r.xy) * 0.5;
        float2 ctr = (r.xy + r.zw) * 0.5;
        float rad = min(pr.x, min(halfS.x, halfS.y));
        float elev = max(pr.y, 1.0) * dp;
        // 遠く離れた部品は計算しない(境目で段差が出ないよう、手前でなめらかに0へ落とす)
        float2 box = abs(xy - ctr) - halfS - float2(elev * 2.5 + 96.0 * dp);
        float reach = max(box.x, box.y);
        if (reach > 0.0) {
            continue;
        }
        float fade = 1.0 - smoothstep(-32.0 * dp, 0.0, reach);
        // 影: 部品の形を光と逆向きにずらし、壁からの距離に比例してぼかす
        float sdShadow = sdRoundRect(xy - ctr - shadowDir * elev, halfS, rad);
        float penKey = 0.15 * elev + dp;
        keyVis *= 1.0 - (1.0 - smoothstep(-penKey, penKey, sdShadow)) * (1.0 - GLASS_T) * opacity;
        float penSun = 0.08 * elev + dp;
        sunVis *= 1.0 - (1.0 - smoothstep(-penSun, penSun, sdShadow)) * (1.0 - GLASS_T) * opacity;
        // 部品のすぐ近くは環境光が届きにくい
        float sd = sdRoundRect(xy - ctr, halfS, rad);
        ao *= 1.0 - 0.28 * fade * opacity * exp(-max(sd, 0.0) / (0.8 * elev + dp));
        if (pr.z > 0.0) {
            emitted += fade * pr.z * emittedLight(xy, r, elev, pr.w);
        }
    }

    float ambient = 0.38 * (1.0 - 0.55 * corner) * ao;
    float key = 0.62 * (1.0 - 0.45 * corner) * keyVis;
    float sun = 0.45 * beam * sunVis;
    float3 col = albedo * (float3(ambient + key + sun) + toLin(lightColor) * emitted);
    float3 outc = toDisplay(toneMap(col)) + (hash12(xy) - 0.5) / 170.0;
    return half4(half3(outc), 1.0);
}
"""

    /**
     * すりガラスの板(壁から浮いている部品)。RenderEffect で実際にぼかした壁(backdrop)を受け取り、
     * 厚みのある縁の屈折・陰影・つや・縁の輝きを付ける。光源の光は [LIGHT] で別に足す。
     */
    const val GLASS = COMMON + """
uniform shader backdrop;
uniform float2 size;
uniform float margin;
uniform float radius;
uniform float dp;
uniform float rimWidth;
layout(color) uniform half4 tint;
layout(color) uniform half4 rimColor;
uniform float3 lightDir;

half4 main(float2 fragCoord) {
    float2 p = fragCoord - float2(margin);
    float2 halfS = size * 0.5;
    float2 c = p - halfS;
    float rad = min(radius, min(halfS.x, halfS.y));
    float sd = sdRoundRect(c, halfS, rad);
    float coverage = clamp(0.5 - sd, 0.0, 1.0);
    if (coverage <= 0.0) {
        return half4(0.0);
    }

    // 縁の丸み(ベベル)。縁ほど面が傾き、内側は平ら
    float depth = -sd;
    float bevel = min(3.5 * dp, min(halfS.x, halfS.y));
    float t = clamp(depth / bevel, 0.0, 1.0);
    float edge = (1.0 - t) * (1.0 - t);
    float2 g = sdRoundRectNormal(c, halfS, rad);
    float3 n = normalize(float3(g * edge * 1.6, 1.0));

    // 屈折: 厚い縁はレンズのように背後を内側へ曲げて見せる
    float2 refr = -g * edge * 5.0 * dp;
    float4 bd = float4(backdrop.eval(fragCoord + refr));
    float3 col = float3(toLinearSrgb(half3(bd.rgb / max(bd.a, 0.0001))));

    // すりガラスの拡散: 背後の色を少しくすませ、部屋の光を散らして少し明るくする
    col = mix(col, toLin(tint), float(tint.a));
    col = col * 1.02 + 0.03;

    // 平らな上面に映る天井の明るさ(上ほど強い)
    float yN = clamp(p.y / size.y, 0.0, 1.0);
    col += (1.0 - smoothstep(0.0, 0.5, yN)) * 0.035;

    // 縁の陰影: 光に向いた左上の縁は明るく、右下の縁は暗い
    float3 l = normalize(lightDir);
    col *= 1.0 + (dot(n, l) - l.z) * 0.7;
    // 鏡面反射(つや)とフレネル反射(浅い角度ほどよく映り込む)
    float3 h = normalize(l + float3(0.0, 0.0, 1.0));
    col += pow(max(dot(n, h), 0.0), 48.0) * edge * 0.9;
    col += pow(1.0 - n.z, 2.5) * 0.25;

    // 磨かれた縁の細い線
    float rimLine = 1.0 - smoothstep(rimWidth - 0.6, rimWidth + 0.6, depth);
    float rimLit = 0.6 + 0.4 * dot(g, normalize(l.xy));
    col = mix(col, max(col, toLin(rimColor) * rimLit), rimLine * float(rimColor.a));

    // すりガラスの細かなざらつき
    col += (hash12(fragCoord) - 0.5) * 0.012;

    float3 o = toDisplay(toneMap(max(col, float3(0.0)))) * coverage;
    return half4(half3(o), half(coverage));
}
"""

    /**
     * ほかのガラスの上に重ねるタイル(アイコンの四角・選択中のタブ)。
     * 背後はすでにガラスなので、白い膜・縁の陰影・つやを半透明で重ねる。
     */
    const val TILE = COMMON + """
uniform float2 size;
uniform float radius;
uniform float dp;
uniform float rimWidth;
layout(color) uniform half4 fill;
layout(color) uniform half4 rimColor;
uniform float3 lightDir;

half4 main(float2 p) {
    float2 halfS = size * 0.5;
    float2 c = p - halfS;
    float rad = min(radius, min(halfS.x, halfS.y));
    float sd = sdRoundRect(c, halfS, rad);
    float coverage = clamp(0.5 - sd, 0.0, 1.0);
    if (coverage <= 0.0) {
        return half4(0.0);
    }
    float depth = -sd;
    float bevel = min(3.5 * dp, min(halfS.x, halfS.y));
    float t = clamp(depth / bevel, 0.0, 1.0);
    float edge = (1.0 - t) * (1.0 - t);
    float2 g = sdRoundRectNormal(c, halfS, rad);
    float3 n = normalize(float3(g * edge * 1.6, 1.0));
    float3 l = normalize(lightDir);
    float3 h = normalize(l + float3(0.0, 0.0, 1.0));

    float yN = clamp(p.y / size.y, 0.0, 1.0);
    float3 body = toLin(fill) * (1.0 + (dot(n, l) - l.z) * 0.7) + (1.0 - smoothstep(0.0, 0.5, yN)) * 0.05;
    float3 bodyColor = toDisplay(clamp(body, 0.0, 1.0));
    float a = float(fill.a);

    float rimLine = 1.0 - smoothstep(rimWidth - 0.6, rimWidth + 0.6, depth);
    float rimLit = 0.6 + 0.4 * dot(g, normalize(l.xy));
    float highlight = clamp(
        pow(max(dot(n, h), 0.0), 48.0) * edge * 0.9
            + pow(1.0 - n.z, 2.5) * 0.25
            + rimLine * float(rimColor.a) * rimLit,
        0.0, 1.0);

    // 白い光沢を、半透明の膜の上に重ねる(乗算済みアルファ)
    float3 premul = float3(highlight) + bodyColor * a * (1.0 - highlight);
    float alpha = highlight + a * (1.0 - highlight);
    return half4(half3(premul * coverage), half(alpha * coverage));
}
"""

    /**
     * 光源の光と、指で押した所の光。加算合成(BlendMode.Plus)で重ねる。
     * 1. ガラス内部の散乱: すりガラスの中で光が散って、光源の近くがぼうっと明るい
     * 2. 縁の導光: ガラスの縁は光を閉じ込めて運ぶので、縁が細く光る
     * 3. 光源そのもの: 縁に沿ったごく細く強い光。HDR対応の画面では白より明るい
     * 4. グレア: 強い光は目やカメラの中でにじむ。中心が鋭く裾が長い形(PSF)を光源に沿って積分する
     */
    const val LIGHT = COMMON + """
uniform float2 size;
uniform float radius;
uniform float dp;
uniform float emit;
uniform float emitTop;
uniform float hdr;
layout(color) uniform half4 lightColor;
uniform float2 touch;
uniform float touchAmount;
uniform float touchRadius;

const float GLARE = 0.1;

half4 main(float2 p) {
    float2 halfS = size * 0.5;
    float rad = min(radius, min(halfS.x, halfS.y));
    float sd = sdRoundRect(p - halfS, halfS, rad);
    float inside = clamp(0.5 - sd, 0.0, 1.0);
    float depth = max(-sd, 0.0);
    float edgeY = mix(size.y, 0.0, emitTop);
    float dy = abs(p.y - edgeY);
    float lh = min(halfS.x * 0.74, 130.0 * dp);
    float profile = 1.0 - smoothstep(lh * 0.45, lh, abs(p.x - halfS.x));

    float scatter = profile * (0.4 * exp(-dy / (6.0 * dp)) + 0.08 * exp(-dy / (22.0 * dp)));
    float rim = exp(-depth / (1.2 * dp)) * exp(-dy / max(size.y * 0.8, dp)) * 0.45;
    float core = profile * exp(-dy / (0.9 * dp)) * 1.8 * hdr;

    // にじみは、鋭い芯(幅 約2dp)と裾(幅 約6dp)の2つの形を足したもの
    float gx = (p.x - halfS.x) / dp;
    float gy = (p.y - edgeY) / dp;
    float gl = lh * 0.72 / dp;
    float glare = 0.6 * lineGlare(gx, gy, -gl, gl, 0.35) * 0.59
        + 0.4 * lineGlare(gx, gy, -gl, gl, 0.03) * 0.17;

    float light = ((scatter + rim + core) * inside + glare * GLARE) * emit;

    // 指で押した所: すりガラスの中で光がふわっと広がり、縁まで届いた光が少しにじむ
    float td = length(p - touch) / max(touchRadius, 1.0);
    float touchLight = touchAmount * (0.16 * exp(-td * td * 1.6) * inside + 0.03 / (1.0 + td * td * 3.0));
    light += touchLight;
    float3 col = toLin(lightColor) * light;
    float a = clamp(max(col.r, max(col.g, col.b)), 0.0, 1.0);
    return half4(half3(col), half(a));
}
"""

    /**
     * トグルの白い玉。mode = 0 で玉そのもの(球として陰影を付ける)、mode = 1 でまわりのにじみ(加算合成)。
     */
    const val BALL = COMMON + """
uniform float2 size;
uniform float dp;
uniform float mode;
uniform float hdr;
layout(color) uniform half4 topColor;
layout(color) uniform half4 bottomColor;
layout(color) uniform half4 lightColor;
uniform float3 lightDir;

half4 main(float2 p) {
    float2 c = p - size * 0.5;
    float r = min(size.x, size.y) * 0.5;
    float d = length(c);
    if (mode < 0.5) {
        float coverage = clamp(r - d + 0.5, 0.0, 1.0);
        if (coverage <= 0.0) {
            return half4(0.0);
        }
        float2 q = c / r;
        float3 n = float3(q, sqrt(max(1.0 - dot(q, q), 0.0)));
        float3 l = normalize(lightDir);
        float3 h = normalize(l + float3(0.0, 0.0, 1.0));
        float3 base = mix(toLin(topColor), toLin(bottomColor), q.y * 0.5 + 0.5);
        float3 col = base * (0.62 + 0.38 * max(dot(n, l), 0.0));
        col += pow(max(dot(n, h), 0.0), 40.0) * 0.6;
        col += toLin(lightColor) * 0.12;
        float3 o = toDisplay(toneMap(col)) * coverage;
        return half4(half3(o), half(coverage));
    }
    float dd = max(d - r, 0.0) / dp;
    float g = (0.55 / (1.0 + dd * dd * 0.5) + 0.45 / (1.0 + dd * dd * 0.03)) * 0.45 * hdr;
    float3 col = toLin(lightColor) * g;
    return half4(half3(col), half(clamp(max(col.r, max(col.g, col.b)), 0.0, 1.0)));
}
"""
}
