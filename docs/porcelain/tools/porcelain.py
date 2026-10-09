"""Porcelain の描画を Skia(CPU)で再現する小さな模型。アプリと同じ AGSL シェーダー(PorcelainShaders.kt)と、
アプリと同じ値(PorcelainTokens.kt と揃えている)で、見本との見比べや図の作成に使う。"""
import os
import re
import struct

import skia

HERE = os.path.dirname(os.path.abspath(__file__))
KT = os.path.join(HERE, '../../../app/src/main/java/com/example/businesscard/ui/porcelain/PorcelainShaders.kt')
CS = skia.ColorSpace.MakeSRGB()


def load(path=KT):
    """Kotlin の文字列定数(const val X = 生文字列、または const val X = A + 生文字列)を読んで、シェーダーのソースを組み立てる。"""
    s = open(path, encoding='utf-8').read()
    out = {}
    for m in re.finditer(r'const val (\w+) = (?:(\w+) \+ )?"""(.*?)"""', s, re.S):
        name, prefix, body = m.groups()
        out[name] = (out[prefix] if prefix else '') + body
    return out


SRC = load()
EFFECTS = {}
for k in ('PANEL', 'SURFACE'):
    eff = skia.RuntimeEffect.MakeForShader(SRC[k])
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


def surface(w, h, f16=False):
    color = skia.kRGBA_F16_ColorType if f16 else skia.kRGBA_8888_ColorType
    return skia.Surface.MakeRaster(skia.ImageInfo.Make(int(w), int(h), color, skia.kPremul_AlphaType, CS))


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
LIGHT_COLOR = 0xFFFFCC92
SHEET_FILTER = 0xFFFFD4AC
WAVE_AMPLITUDE = 0.233


class Rig:
    """照明の模型(LIGHT_RIG)の値。板と、板の上の部品で共通。"""

    def __init__(self, w, h, dp, wave_top, wave_amp, glow=1.0, hdr=1.0):
        self.w, self.h, self.dp = w, h, dp
        self.wave_top, self.wave_amp = wave_top, wave_amp
        self.glow, self.hdr = glow, hdr

    def values(self):
        return dict(panelSize=[self.w, self.h], dp=self.dp, waveTop=self.wave_top, waveAmp=self.wave_amp,
                    glow=self.glow, hdr=self.hdr, lightColor=argb(LIGHT_COLOR))


def panel_shader(rig):
    v = rig.values()
    v.update(panelTop=argb(PANEL_TOP), panelBottom=argb(PANEL_BOTTOM), sheetFilter=argb(SHEET_FILTER))
    return shader('PANEL', v)


def surface_shader(rig, w, h, radius, origin, bevel, top, bottom, sheen, receive=True):
    """部品の面。origin は板の座標での部品の左上。"""
    v = rig.values()
    v.update(size=[w, h], radius=radius, origin=list(origin), bevel=bevel, receive=1.0 if receive else 0.0,
             sheen=sheen, topColor=argb(top), bottomColor=argb(bottom))
    return shader('SURFACE', v)
