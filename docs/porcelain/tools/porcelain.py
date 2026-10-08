"""Porcelain の描画を Skia(CPU)で再現する小さな模型。アプリと同じ AGSL シェーダー(PorcelainShaders.kt)と、
アプリと同じ影の値(PorcelainTokens.kt と揃えている)で、見本との見比べや図の作成に使う。"""
import os
import re
import struct

import skia

HERE = os.path.dirname(os.path.abspath(__file__))
KT = os.path.join(HERE, '../../../app/src/main/java/com/example/businesscard/ui/porcelain/PorcelainShaders.kt')
CS = skia.ColorSpace.MakeSRGB()


def load(path=KT):
    s = open(path, encoding='utf-8').read()
    return {m.group(1): m.group(2) for m in re.finditer(r'const val (\w+) = """(.*?)"""', s, re.S)}


SRC = load()
EFFECTS = {}
for k, v in SRC.items():
    eff = skia.RuntimeEffect.MakeForShader(v)
    if eff is None:
        raise SystemExit(f'shader {k} did not compile')
    EFFECTS[k] = eff


def argb(c):
    return [((c >> 16) & 255) / 255, ((c >> 8) & 255) / 255, (c & 255) / 255, ((c >> 24) & 255) / 255]


def _decls(src):
    return re.findall(r'^\s*(?:layout\(color\)\s+)?uniform\s+(float|float2|float3|float4|int|half4)\s+(\w+)(?:\[(\d+)\])?\s*;', src, re.M)


def shader(name, values):
    data = b''
    for typ, uname, _ in _decls(SRC[name]):
        v = values[uname]
        if typ == 'int':
            data += struct.pack('<i', int(v))
        else:
            flat = v if isinstance(v, (list, tuple)) else [v]
            data += struct.pack('<%df' % len(flat), *flat)
    return EFFECTS[name].makeShader(skia.Data.MakeWithCopy(data))


def surface(w, h):
    return skia.Surface.MakeRaster(skia.ImageInfo.Make(int(w), int(h), skia.kRGBA_8888_ColorType, skia.kPremul_AlphaType, CS))


def sigma(radius_px):
    """Android の BlurMaskFilter(半径)と同じぼけ方になる Skia の sigma。"""
    return radius_px * 0.57735 + 0.5 if radius_px > 0 else 0.0


def color4(c, alpha=None):
    r, g, b, a = argb(c)
    return skia.Color4f(r, g, b, a if alpha is None else alpha)


# ---- トークン(PorcelainTokens.kt と同じ値) ----
WALL_TOP = 0xFFBBBBBB
WALL_BOTTOM = 0xFFA4A4A4
PANEL_TOP = 0xFFDADADA
PANEL_BOTTOM = 0xFFC6C6C6
GLOW_CORE = 0xFFFFFDF8
GLOW_WARM = 0xFFFFE6D0
SHEET_TINT = 0xFFF5CDA8


def panel_shader(w, h, dp, wave_top, wave_amp, glow=1.0):
    return shader('PANEL', dict(
        size=[w, h], dp=dp, waveTop=wave_top, waveAmp=wave_amp, glow=glow,
        panelTop=argb(PANEL_TOP), panelBottom=argb(PANEL_BOTTOM), glowCore=argb(GLOW_CORE),
        glowWarm=argb(GLOW_WARM), sheetTint=argb(SHEET_TINT),
    ))
