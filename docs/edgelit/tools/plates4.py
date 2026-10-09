"""13: ガラスの影のしくみ(板を外して影だけを見た図と、エミュレータで撮ったスイッチの動き)。

python3 plates4.py <tools> <out> <DEV>   # DEV は screenshots ブランチの画像を置いたフォルダ(interactions.gif を使う)
"""
import io, sys, skia
sys.path.insert(0, sys.argv[1])
import glassrender as g
from PIL import Image
OUT, DEV = sys.argv[2], sys.argv[3]
INK = (0.17, 0.145, 0.125, 1)
MUTED = (0.42, 0.38, 0.35, 1)
ACCENT = (0.80, 0.42, 0.18, 1)
PAPER = skia.Color4f(0.965, 0.953, 0.94, 1)
SAMP = skia.SamplingOptions(skia.CubicResampler.Mitchell())
D = g.D


def badge(c, n, x, y, r=20):
    c.drawCircle(x, y, r + 3, skia.Paint(Color4f=skia.Color4f(1, 1, 1, 1), AntiAlias=True))
    c.drawCircle(x, y, r, skia.Paint(Color4f=skia.Color4f(*ACCENT), AntiAlias=True))
    g.draw_text(c, str(n), x, y + r * 0.42, r * 1.15, (1, 1, 1, 1), 700, align='c', shadow=False)


def scene():
    sc = g.Scene(400, 150)
    sc.add(24, 24, 168, 56, emit=1.0, text='Launch', bold=True)
    sc.add(232, 34, 64, 36, rad=18, emit=0.0, fill=0x14DCE1E6, rim=0xD9FFFFFF, rw=2)
    sc.ball(237, 39, 26)
    sc.add(316, 34, 64, 36, rad=18, emit=0.7, fill=g.SELECTED, rim=0xD9FFFFFF, rw=2)
    sc.ball(349, 39, 26)
    return sc


pad = 48
full = scene().render()
shadow_only = scene().render(('wall', 'shadow'))
W = full.width(); H = full.height()

# スイッチのコマ(エミュレータ): オフ → 動いている途中 → オン
gif = Image.open(f"{DEV}/interactions.gif")
frames = []
for i in (17, 21, 25):
    gif.seek(i)
    f = gif.convert('RGB').crop((180, 255, 320, 345)).resize((420, 270), Image.LANCZOS)
    buf = io.BytesIO(); f.save(buf, 'PNG')
    frames.append(skia.Image.MakeFromEncoded(skia.Data.MakeWithCopy(buf.getvalue())))

CW = W + pad * 2
fw = (CW - pad * 2 - 2 * 24) / 3; fh = fw * 270 / 420
CH = int(pad + 70 + H + 70 + H + 80 + 4 * 43 + 33 + 40 + 24 + fh + 70)
s = g.surface(CW, CH); c = s.getCanvas(); c.clear(PAPER)
g.draw_text(c, 'ガラスの影(板は光を通し、縁は光を曲げる)', pad, pad + 34, 36, INK, 700, shadow=False)
y = pad + 70
c.drawImage(full, pad, y)
g.draw_text(c, 'ふだんの見え方', pad, y + H + 40, 24, MUTED, 500, shadow=False)
y += H + 70
c.drawImage(shadow_only, pad, y)
g.draw_text(c, '板を外して、壁に落ちた影だけを見たところ', pad, y + H + 40, 24, MUTED, 500, shadow=False)
# 印(dp → px)
for n, (xd, yd) in enumerate([(96, 100), (124, 74), (207, 58), (290, 94)], 1):
    badge(c, n, pad + xd * D, y + yd * D)
ly = y + H + 80
notes = [
    '濃い輪郭: 板の厚い縁が光を曲げて外へ逃がすので、影の縁ほど暗い',
    '明るい面: すりガラスを通った光が届く。曇りの強い板ほど暗く、ほぼ透明なスイッチの枠は輪郭だけが濃い',
    '淡い明るい線: 縁がレンズのように集めた光(コースティクス)',
    '玉の影: 光を通さないつまみの玉は、枠の影の中に丸い濃い影を落とし、玉と一緒に動く',
]
for n, t in enumerate(notes, 1):
    badge(c, n, pad + 20, ly + 2, 16)
    g.draw_wrapped(c, t, pad + 50, ly + 10, CW - pad * 2 - 50, 22, INK)
    ly += 33 * len(g.wrap(t, 22, CW - pad * 2 - 50)) + 10
ly += 40
g.draw_text(c, 'スイッチを入れると、玉の影が左から右へ動く(エミュレータで撮影)', pad, ly, 24, MUTED, 500, shadow=False)
ly += 24
for i, (img, lab) in enumerate(zip(frames, ['オフ', '動いている途中', 'オン'])):
    x = pad + i * (fw + 24)
    c.save(); c.clipRRect(skia.RRect.MakeRectXY(skia.Rect.MakeXYWH(x, ly, fw, fh), 14, 14), True)
    c.drawImageRect(img, skia.Rect.MakeWH(img.width(), img.height()), skia.Rect.MakeXYWH(x, ly, fw, fh), SAMP)
    c.restore()
    g.draw_text(c, lab, x, ly + fh + 34, 22, INK, 700, shadow=False)
s.makeImageSnapshot().save(f"{OUT}/13-glass-shadow.png", skia.kPNG)
print('13-glass-shadow.png', CW, CH)
