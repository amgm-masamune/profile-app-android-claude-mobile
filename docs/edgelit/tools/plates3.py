import sys, skia
sys.path.insert(0, sys.argv[1])
import glassrender as g
OUT, DEV = sys.argv[2], sys.argv[3]
INK = (0.17, 0.145, 0.125, 1)
MUTED = (0.42, 0.38, 0.35, 1)
ACCENT = (0.80, 0.42, 0.18, 1)
PAPER = skia.Color4f(0.965, 0.953, 0.94, 1)
D = 2.625
SAMP = skia.SamplingOptions(skia.CubicResampler.Mitchell())


def canvas(w, h, bg=PAPER):
    s = g.surface(w, h); c = s.getCanvas(); c.clear(bg); return s, c


def save(s, name):
    s.makeImageSnapshot().save(f"{OUT}/{name}", skia.kPNG); print(name, s.width(), s.height())


def load(name):
    return skia.Image.open(f"{DEV}/{name}")


def draw_img(c, img, x, y, w, h, src=None, r=0):
    dst = skia.Rect.MakeXYWH(x, y, w, h)
    c.save()
    if r:
        c.clipRRect(skia.RRect.MakeRectXY(dst, r, r), True)
    c.drawImageRect(img, src or skia.Rect.MakeWH(img.width(), img.height()), dst, SAMP)
    c.restore()


def badge(c, n, x, y, r=24):
    c.drawCircle(x, y, r + 3, skia.Paint(Color4f=skia.Color4f(1, 1, 1, 1), AntiAlias=True))
    c.drawCircle(x, y, r, skia.Paint(Color4f=skia.Color4f(*ACCENT), AntiAlias=True))
    g.draw_text(c, str(n), x, y + r * 0.42, r * 1.15, (1, 1, 1, 1), 700, align='c', shadow=False)


pad = 48

# ===== 02 部位の名前(Launch の拡大) =====
cat = load('catalog.png')
src = skia.Rect.MakeLTRB(0, 140, 640, 470)
scale = 1.9
iw, ih = src.width() * scale, src.height() * scale
items = [
    ((275, 201), '縁の線(リム)', '磨かれた細い縁。光に向いた左上ほど明るい'),
    ((60, 270), '縁の丸み(ベベル)', '厚い縁で背後が内側へ曲がって見え(屈折)、右下は暗い'),
    ((150, 214), '上面のつや', '天井の明るさが平らな面に映る'),
    ((430, 255), 'すりガラス', '後ろの壁と自分の影が、実際にぼけて透ける'),
    ((400, 322), 'ガラス内の散乱光', '光源の光がすりガラスの中で広がる'),
    ((275, 346), '光源の芯', '下端に沿った細く強い光(HDR画面では白より明るい)'),
    ((150, 356), 'グレア(にじみ)', '強い光が目に入ってにじむ。芯が鋭く裾が長い'),
    ((275, 378), '壁の光だまり', '光源が壁を照らす。距離の2乗で弱まる'),
    ((560, 330), '影', '左上のキーライトで右下へ。高さに比例してずれ、ぼける'),
]
H2 = int(pad + 80 + ih + 40 + len(items) * 76 + pad)
s, c = canvas(1290, H2)
g.draw_text(c, '部位の名前(主ボタンを拡大)', pad, pad + 34, 36, INK, 700, shadow=False)
ix = (1290 - iw) / 2; iy = pad + 80
draw_img(c, cat, ix, iy, iw, ih, src, r=20)
for i, ((px, py), _, _) in enumerate(items):
    badge(c, i + 1, ix + (px - src.left()) * scale, iy + (py - src.top()) * scale, 20)
y = iy + ih + 60
for i, (_, title, desc) in enumerate(items):
    badge(c, i + 1, pad + 22, y + 8, 20)
    g.draw_text(c, title, pad + 60, y + 18, 26, INK, 700, shadow=False)
    g.draw_text(c, desc, pad + 330, y + 18, 23, MUTED, 400, shadow=False)
    y += 76
save(s, '02-anatomy.png')

# ===== 09 部品の一覧(カタログに番号) =====
src = skia.Rect.MakeLTRB(0, 90, 1080, 1360)
scale = 0.9
iw, ih = src.width() * scale, src.height() * scale
comps = [
    ((105, 104), 'GlassButton(Primary)', '主操作。1画面に1つ。太字・強い光'),
    ((307, 104), 'GlassButton(Secondary)', '副操作。細字・中くらいの光'),
    ((105, 228), 'GlassTextField + leadingIcon', 'ラベルは上、アイコンは左の明るいタイル'),
    ((307, 228), 'GlassDropdown', '押すと選択肢のメニュー。山形が回る'),
    ((105, 352), 'GlassSegmentedControl', '選択中のタイルが滑って移る'),
    ((254, 342), 'GlassSwitch', '光る玉がばねで動き、オンで枠も灯る'),
    ((105, 476), 'GlassTabs', '選択中のタブは上端が光る'),
    ((307, 476), 'GlassButton(Primary)', '見本の「Premium plan」'),
    ((60, 58), 'GlassLabel', '部品の上の小さな見出し'),
]
H9 = int(pad + 80 + ih + 40 + len(comps) * 76 + pad)
s, c = canvas(1290, H9)
g.draw_text(c, '部品(ComponentPreviews の SoftGlassCatalog)', pad, pad + 34, 36, INK, 700, shadow=False)
ix = (1290 - iw) / 2; iy = pad + 80
draw_img(c, cat, ix, iy, iw, ih, src, r=24)
for i, ((dx, dy), _, _) in enumerate(comps):
    px, py = dx * D, dy * D
    badge(c, i + 1, ix + (px - src.left()) * scale - 70 * scale, iy + (py - src.top()) * scale - 60 * scale, 22)
y = iy + ih + 60
for i, (_, title, desc) in enumerate(comps):
    badge(c, i + 1, pad + 22, y + 8, 20)
    g.draw_text(c, title, pad + 60, y + 18, 25, INK, 700, shadow=False)
    g.draw_text(c, desc, pad + 520, y + 18, 23, MUTED, 400, shadow=False)
    y += 76
save(s, '09-components.png')

# ===== 10 押す前と押した後 =====
pressed = load('catalog_pressed.png')
src = skia.Rect.MakeLTRB(0, 185, 560, 455)
sc2 = 1.05
w10, h10 = src.width() * sc2, src.height() * sc2
s, c = canvas(int(pad * 2 + w10 * 2 + 40), int(pad + 80 + h10 + 120))
g.draw_text(c, '押す前と押した後(エミュレータで撮影)', pad, pad + 34, 36, INK, 700, shadow=False)
for i, (img, lab, desc) in enumerate([(cat, '休んでいる', '浮く高さ 24dp'), (pressed, '押し込み中', '約8dp に沈み、影が寄り、指の所が光る')]):
    x = pad + i * (w10 + 40); y = pad + 80
    draw_img(c, img, x, y, w10, h10, src, r=18)
    g.draw_text(c, lab, x, y + h10 + 46, 28, INK, 700, shadow=False)
    g.draw_text(c, desc, x, y + h10 + 84, 22, MUTED, 400, shadow=False)
save(s, '10-press.png')

# ===== 01 ヒーロー =====
W1, H1 = 1290, 990
wall_sc = g.Scene(W1 / D, H1 / D)
bg = wall_sc.render(('wall',))
s, c = canvas(W1, H1)
c.drawImage(bg, 0, 0)
g.draw_text(c, 'Edgelit', 64, 130, 96, (1, 1, 1, 1), 700)
g.draw_text(c, '縁灯(ふちあかり) — 縁から灯り、触れると沈む、すりガラス', 68, 196, 32, (1, 1, 1, 0.92), 500)
phones = [('list.png', 0), ('catalog.png', 1), ('edit.png', 2)]
pw = 340; ph = pw * 2209 / 1080
for name, i in phones:
    x = 64 + i * (pw + 49); y = 270 + (0 if i == 1 else 40)
    sh = skia.Paint(Color4f=skia.Color4f(0.12, 0.08, 0.05, 0.38), AntiAlias=True, MaskFilter=skia.MaskFilter.MakeBlur(skia.kNormal_BlurStyle, 22))
    c.drawRRect(skia.RRect.MakeRectXY(skia.Rect.MakeXYWH(x + 26, y + 30, pw, ph * 0.86), 40, 40), sh)
    frame = skia.RRect.MakeRectXY(skia.Rect.MakeXYWH(x - 10, y - 10, pw + 20, ph * 0.86 + 20), 46, 46)
    c.drawRRect(frame, skia.Paint(Color4f=skia.Color4f(0.12, 0.11, 0.1, 1), AntiAlias=True))
    img = load(name)
    draw_img(c, img, x, y, pw, ph * 0.86, skia.Rect.MakeWH(img.width(), img.height() * 0.86), r=36)
save(s, '01-hero.png')

# ===== 11 横画面(表示画面) =====
det = load('detail.png')
w11 = 1290 - pad * 2; h11 = w11 * det.height() / det.width()
s, c = canvas(1290, int(pad + 80 + h11 + pad + 30))
g.draw_text(c, '表示画面(横画面固定)。戻るは左上、編集は右下で強く光る', pad, pad + 34, 32, INK, 700, shadow=False)
draw_img(c, det, pad, pad + 80, w11, h11, r=28)
save(s, '11-detail.png')
