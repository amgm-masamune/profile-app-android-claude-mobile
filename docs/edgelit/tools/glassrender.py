"""Edgelit の描画を Skia(CPU)で再現する小さな模型。アプリと同じ AGSL シェーダーを使う(図の作成用)。"""
import re, struct
import skia
from shaders import load

D = 2.625
CS = skia.ColorSpace.MakeSRGB()
SRC = load()
EFFECTS = {k: skia.RuntimeEffect.MakeForShader(v) for k, v in SRC.items()}
LIGHT_DIR = [-0.55, -0.5, 0.6]
# すりガラスのぼかし(dp)。アプリの SoftGlassLight.frostBlur と同じ値にする
FROST = 10

GLASS = 0x40DCE1E6; PRESSED = 0x4DFFFFFF; SELECTED = 0x52FFFFFF; TILE = 0x5CFFFFFF
BORDER = 0x8CFFFFFF; FOCUS = 0xF2FFFFFF; GLOW = 0xFFFFFCF6
WALL_TOP = 0xFFAA9B8E; WALL_BOTTOM = 0xFF968677; THUMB_T = 0xFFF7F4F0; THUMB_B = 0xFFDAD5CF

FONT_JP = 'Noto Sans CJK JP'

# 影の濃さ(アプリの GlassRendering.kt と同じ式・値)
BALL_DENSITY = 0.85
BALL_LIFT = 2.0


def plate_density(fill):
    """すりガラスの板が光を遮る強さ。曇り(混ぜる白)が強い板ほど暗い影になる。"""
    return min(max(0.35 + 0.8 * argb(fill)[3], 0.35), 0.8)


def tile_density(fill):
    """板の上の白いタイルが、板の影の中でさらに光を遮る強さ。"""
    return 0.8 * argb(fill)[3]


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
        # 板の上に載ったもの(白いタイル・つまみの玉)。板の影の中に、さらに濃い影を落とす
        self.overlays = []

    def px(self, v):
        return v * self.d

    def add(self, x, y, w, h, rad=11, emit=1.0, top=0, fill=GLASS, rim=BORDER, rw=1, elev=24, text=None, bold=False, size=19,
            touch=None, touch_amount=0.0):
        self.floating.append(dict(r=(x, y, w, h), rad=rad, emit=emit, top=top, fill=fill, rim=rim, rw=rw, elev=elev,
                                  touch=touch, touch_amount=touch_amount))
        if text:
            self.texts.append((text, x + w / 2, y + h / 2 + size * 0.36, size, 700 if bold else 400, 1.0, 'c'))

    def tile(self, x, y, w, h, rad=9, fill=TILE, rim=BORDER, rw=1):
        """板の上に貼った白いタイル(アプリの GlassLayer.Inset)。"""
        self.overlays.append(dict(kind='tile', r=(x, y, w, h), rad=rad, fill=fill, rim=rim, rw=rw,
                                  density=tile_density(fill), lift=0.0))

    def ball(self, x, y, size=26):
        """スイッチのつまみの玉。"""
        self.overlays.append(dict(kind='ball', r=(x, y, size, size), rad=size / 2, density=BALL_DENSITY, lift=BALL_LIFT))

    def label(self, s, x, y, size=15, weight=500, alpha=1.0, align='l'):
        self.texts.append((s, x, y, size, weight, alpha, align))

    def render(self, stages=('wall', 'shadow', 'pool', 'frost', 'surface', 'emission', 'text'), light_dir=LIGHT_DIR):
        d = self.d
        wm = int(round(64 * d))
        els = self.floating if 'shadow' in stages else []
        rects, props, extra = [0.0] * 64, [0.0] * 64, [0.0] * 64
        n = 0
        for e in els:
            x, y, w, h = e['r']
            rects[n * 4:n * 4 + 4] = [x * d, y * d, (x + w) * d, (y + h) * d]
            emit = e['emit'] * (1 if 'pool' in stages else 0)
            # アプリと同じ: 壁に近づいたぶんだけ照り返しを一部打ち消す(遠ざかるときは打ち消さない)
            near = min(e['elev'] / 24.0, 1.0)
            props[n * 4:n * 4 + 4] = [e['rad'] * d, e['elev'], emit * near * near ** 0.5, e['top']]
            extra[n * 4:n * 4 + 3] = [1.0, plate_density(e['fill']), 0.0]
            n += 1
        for o in (self.overlays if 'shadow' in stages else []):
            x, y, w, h = o['r']
            cx, cy = x + w / 2, y + h / 2
            # アプリと同じ: 載っている板(中心を含む板)の高さを使う
            parent = next((e for e in els if e['r'][0] <= cx <= e['r'][0] + e['r'][2] and e['r'][1] <= cy <= e['r'][1] + e['r'][3]), None)
            if parent is None:
                continue
            rects[n * 4:n * 4 + 4] = [x * d, y * d, (x + w) * d, (y + h) * d]
            props[n * 4:n * 4 + 4] = [o['rad'] * d, parent['elev'] + o['lift'], 0.0, 0.0]
            extra[n * 4:n * 4 + 3] = [1.0, o['density'], 1.0]
            n += 1
        wall = shader('WALL', dict(size=[self.w, self.h], dp=d, count=n, rects=rects, props=props, extra=extra,
                                   albedoTop=argb(WALL_TOP), albedoBottom=argb(WALL_BOTTOM), lightColor=argb(GLOW), lightDir=light_dir))
        ws = surface(self.w + 2 * wm, self.h + 2 * wm)
        wc = ws.getCanvas(); wc.translate(wm, wm)
        wc.drawRect(skia.Rect.MakeXYWH(-wm, -wm, self.w + 2 * wm, self.h + 2 * wm), skia.Paint(Shader=wall))
        wall_img = ws.makeImageSnapshot()

        out = surface(self.w, self.h)
        oc = out.getCanvas()
        oc.drawImage(wall_img, -wm, -wm)
        gm = int(round(40 * d))
        sigma = 0.57735 * FROST * d + 0.5
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
                                         lightColor=argb(GLOW), touch=[(e['touch'] or (0, 0))[0] * d, (e['touch'] or (0, 0))[1] * d],
                                         touchAmount=e['touch_amount'], touchRadius=(10 + 16 * e['touch_amount']) * d))
                oc.save(); oc.translate(x, y)
                oc.drawRect(skia.Rect.MakeXYWH(-gl, -gl, w + 2 * gl, h + 2 * gl), skia.Paint(Shader=l, BlendMode=skia.BlendMode.kPlus))
                oc.restore()
        if 'surface' in stages:
            for o in self.overlays:
                x, y, w, h = [v * d for v in o['r']]
                if o['kind'] == 'tile':
                    t = shader('TILE', dict(size=[w, h], radius=o['rad'] * d, dp=d, rimWidth=o['rw'] * d, fill=argb(o['fill']),
                                            rimColor=argb(o['rim']), lightDir=light_dir))
                    oc.save(); oc.translate(x, y); oc.drawRect(skia.Rect.MakeWH(w, h), skia.Paint(Shader=t)); oc.restore()
                else:
                    for mode in (0.0, 1.0):
                        b = shader('BALL', dict(size=[w, h], dp=d, mode=mode, hdr=1.0, topColor=argb(THUMB_T), bottomColor=argb(THUMB_B),
                                                lightColor=argb(GLOW), lightDir=light_dir))
                        g = 24 * d * mode
                        oc.save(); oc.translate(x, y)
                        oc.drawRect(skia.Rect.MakeXYWH(-g, -g, w + 2 * g, h + 2 * g),
                                    skia.Paint(Shader=b, BlendMode=skia.BlendMode.kPlus if mode else skia.BlendMode.kSrcOver))
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
