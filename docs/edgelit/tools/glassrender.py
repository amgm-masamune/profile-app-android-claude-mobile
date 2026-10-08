"""Edgelit の描画を Skia(CPU)で再現する小さな模型。アプリと同じ AGSL シェーダーを使う(図の作成用)。"""
import re, struct
import skia
from shaders import load

D = 2.625
CS = skia.ColorSpace.MakeSRGB()
SRC = load()
EFFECTS = {k: skia.RuntimeEffect.MakeForShader(v) for k, v in SRC.items()}
LIGHT_DIR = [-0.55, -0.5, 0.6]

GLASS = 0x40DCE1E6; PRESSED = 0x4DFFFFFF; SELECTED = 0x52FFFFFF; TILE = 0x5CFFFFFF
BORDER = 0x8CFFFFFF; FOCUS = 0xF2FFFFFF; GLOW = 0xFFFFFCF6
WALL_TOP = 0xFFAA9B8E; WALL_BOTTOM = 0xFF968677; THUMB_T = 0xFFF7F4F0; THUMB_B = 0xFFDAD5CF

FONT_JP = 'Noto Sans CJK JP'


def argb(c):
    return [((c >> 16) & 255) / 255, ((c >> 8) & 255) / 255, (c & 255) / 255, ((c >> 24) & 255) / 255]


def _decls(src):
    return re.findall(r'^\s*(?:layout\(color\)\s+)?uniform\s+(float|float2|float3|float4|int|half4)\s+(\w+)(?:\[(\d+)\])?\s*;', src, re.M)


def shader(name, values, child=None):
    data = b''
    for typ, uname, count in _decls(SRC[name]):
        v = values[uname]
        if typ == 'int':
            data += struct.pack('<i', int(v))
        else:
            flat = v if isinstance(v, (list, tuple)) else [v]
            data += struct.pack('<%df' % len(flat), *flat)
    d = skia.Data.MakeWithCopy(data)
    return EFFECTS[name].makeShader(d) if child is None else EFFECTS[name].makeShader(d, child, 1)


def surface(w, h):
    return skia.Surface.MakeRaster(skia.ImageInfo.Make(int(w), int(h), skia.kRGBA_8888_ColorType, skia.kPremul_AlphaType, CS))


def font(size_px, weight=400):
    style = skia.FontStyle(weight, skia.FontStyle.kNormal_Width, skia.FontStyle.kUpright_Slant)
    return skia.Font(skia.Typeface(FONT_JP, style), size_px)


def draw_text(c, s, x, y, size_px, color=(1, 1, 1, 1), weight=400, align='l', shadow=True):
    f = font(size_px, weight)
    w = f.measureText(s)
    xx = x - (w / 2 if align == 'c' else (w if align == 'r' else 0))
    if shadow:
        sp = skia.Paint(Color4f=skia.Color4f(0, 0, 0, 0.22), AntiAlias=True,
                        MaskFilter=skia.MaskFilter.MakeBlur(skia.kNormal_BlurStyle, size_px * 0.08))
        c.drawString(s, xx, y + size_px * 0.06, f, sp)
    c.drawString(s, xx, y, f, skia.Paint(Color4f=skia.Color4f(*color), AntiAlias=True))
    return w


class Scene:
    """dp 単位で部品を置き、段階(stages)を指定して描く。"""

    def __init__(self, w_dp, h_dp, d=D):
        self.d = d
        self.w, self.h = int(w_dp * d), int(h_dp * d)
        self.floating, self.insets, self.texts = [], [], []

    def px(self, v):
        return v * self.d

    def add(self, x, y, w, h, rad=11, emit=1.0, top=0, fill=GLASS, rim=BORDER, rw=1, elev=24, text=None, bold=False, size=19):
        self.floating.append(dict(r=(x, y, w, h), rad=rad, emit=emit, top=top, fill=fill, rim=rim, rw=rw, elev=elev))
        if text:
            self.texts.append((text, x + w / 2, y + h / 2 + size * 0.36, size, 700 if bold else 400, 1.0, 'c'))

    def label(self, s, x, y, size=15, weight=500, alpha=1.0, align='l'):
        self.texts.append((s, x, y, size, weight, alpha, align))

    def render(self, stages=('wall', 'shadow', 'pool', 'frost', 'surface', 'emission', 'text'), light_dir=LIGHT_DIR):
        d = self.d
        wm = int(round(64 * d))
        els = self.floating if 'shadow' in stages else []
        rects, props, extra = [0.0] * 64, [0.0] * 64, [0.0] * 64
        for i, e in enumerate(els):
            x, y, w, h = e['r']
            rects[i * 4:i * 4 + 4] = [x * d, y * d, (x + w) * d, (y + h) * d]
            emit = e['emit'] * (1 if 'pool' in stages else 0)
            near = e['elev'] / 24.0
            props[i * 4:i * 4 + 4] = [e['rad'] * d, e['elev'], emit * near * near ** 0.5, e['top']]
            extra[i * 4] = 1.0
        wall = shader('WALL', dict(size=[self.w, self.h], dp=d, count=len(els), rects=rects, props=props, extra=extra,
                                   albedoTop=argb(WALL_TOP), albedoBottom=argb(WALL_BOTTOM), lightColor=argb(GLOW), lightDir=light_dir))
        ws = surface(self.w + 2 * wm, self.h + 2 * wm)
        wc = ws.getCanvas(); wc.translate(wm, wm)
        wc.drawRect(skia.Rect.MakeXYWH(-wm, -wm, self.w + 2 * wm, self.h + 2 * wm), skia.Paint(Shader=wall))
        wall_img = ws.makeImageSnapshot()

        out = surface(self.w, self.h)
        oc = out.getCanvas()
        oc.drawImage(wall_img, -wm, -wm)
        gm = int(round(40 * d))
        sigma = 0.57735 * 18 * d + 0.5
        for e in self.floating:
            if 'frost' not in stages:
                break
            x, y, w, h = [v * d for v in e['r']]
            pw, ph = int(round(w)) + 2 * gm, int(round(h)) + 2 * gm
            ps = surface(pw, ph)
            ps.getCanvas().drawImage(wall_img, gm - x - wm, gm - y - wm)
            bs = surface(pw, ph)
            bs.getCanvas().drawImage(ps.makeImageSnapshot(), 0, 0, skia.SamplingOptions(),
                                     skia.Paint(ImageFilter=skia.ImageFilters.Blur(sigma, sigma, skia.TileMode.kClamp)))
            blurred = bs.makeImageSnapshot()
            if 'surface' in stages:
                g = shader('GLASS', dict(size=[w, h], margin=float(gm), radius=e['rad'] * d, dp=d, rimWidth=e['rw'] * d,
                                         tint=argb(e['fill']), rimColor=argb(e['rim']), lightDir=light_dir), child=blurred.makeShader())
                oc.save(); oc.translate(x - gm, y - gm)
                oc.drawRect(skia.Rect.MakeWH(pw, ph), skia.Paint(Shader=g))
                oc.restore()
            else:
                rr = skia.RRect.MakeRectXY(skia.Rect.MakeXYWH(x, y, w, h), e['rad'] * d, e['rad'] * d)
                oc.save(); oc.clipRRect(rr, True)
                oc.drawImage(blurred, x - gm, y - gm)
                a = argb(e['fill'])
                oc.drawRRect(rr, skia.Paint(Color4f=skia.Color4f(a[0], a[1], a[2], a[3])))
                oc.restore()
            if 'emission' in stages and e['emit'] > 0:
                gl = 48 * d
                l = shader('LIGHT', dict(size=[w, h], radius=e['rad'] * d, dp=d, emit=e['emit'], emitTop=e['top'], hdr=1.0,
                                         lightColor=argb(GLOW), touch=[w / 2, h / 2], touchAmount=0.0, touchRadius=10 * d))
                oc.save(); oc.translate(x, y)
                oc.drawRect(skia.Rect.MakeXYWH(-gl, -gl, w + 2 * gl, h + 2 * gl), skia.Paint(Shader=l, BlendMode=skia.BlendMode.kPlus))
                oc.restore()
        if 'text' in stages:
            for s, x, y, size, weight, alpha, align in self.texts:
                draw_text(oc, s, x * d, y * d, size * d * 0.95, (1, 1, 1, alpha), weight, align)
        return out.makeImageSnapshot()


def wrap(s, size_px, max_w, weight=400):
    """日本語向けの1文字ずつの折り返し(行頭に句読点を置かない)。"""
    f = font(size_px, weight)
    lines, cur = [], ''
    for ch in s:
        if f.measureText(cur + ch) > max_w and cur:
            if ch in '、。・)」』':
                cur += ch
                lines.append(cur); cur = ''
                continue
            lines.append(cur); cur = ch
        else:
            cur += ch
    if cur:
        lines.append(cur)
    return lines


def draw_wrapped(c, s, x, y, max_w, size_px, color=(1, 1, 1, 1), weight=400, line_h=1.5, shadow=False):
    lines = wrap(s, size_px, max_w, weight)
    for i, ln in enumerate(lines):
        draw_text(c, ln, x, y + i * size_px * line_h, size_px, color, weight, shadow=shadow)
    return len(lines)
