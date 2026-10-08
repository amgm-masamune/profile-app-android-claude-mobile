import sys, skia
sys.path.insert(0, sys.argv[1])
import glassrender as g
OUT = sys.argv[2]
INK = (0.17, 0.145, 0.125, 1)
MUTED = (0.42, 0.38, 0.35, 1)
PAPER = skia.Color4f(0.965, 0.953, 0.94, 1)


def canvas(w, h):
    s = g.surface(w, h)
    c = s.getCanvas()
    c.clear(PAPER)
    return s, c


def save(s, name):
    s.makeImageSnapshot().save(f"{OUT}/{name}", skia.kPNG)
    print(name, s.width(), s.height())


def rounded_image(c, img, x, y, r=18):
    c.save()
    c.clipRRect(skia.RRect.MakeRectXY(skia.Rect.MakeXYWH(x, y, img.width(), img.height()), r, r), True)
    c.drawImage(img, x, y)
    c.restore()


# ---- 描画の工程 ----
def stage_scene():
    sc = g.Scene(220, 128)
    sc.add(25, 32, 170, 56, text='Launch', bold=True)
    return sc


stages = [
    (('wall',), '1. 壁', '部屋の照明だけ。環境光・左上のキーライト・斜めの日差し'),
    (('wall', 'shadow', 'pool'), '2. 壁が受ける影と光', '板の影(右下)と、板の光源が壁を照らす光だまり'),
    (('wall', 'shadow', 'pool', 'frost', 'text'), '3. すりガラス', '後ろの壁を実際にぼかし(RenderEffect)、白を少し混ぜる'),
    (('wall', 'shadow', 'pool', 'frost', 'surface', 'text'), '4. 縁と表面', '縁の屈折・陰影、つや、磨かれた縁の線'),
    (('wall', 'shadow', 'pool', 'frost', 'surface', 'emission', 'text'), '5. 発光', 'ガラス内の散乱・縁の導光・光源の芯・グレア'),
]
imgs = [stage_scene().render(st) for st, _, _ in stages]
pw, ph = imgs[0].width(), imgs[0].height()
pad, gap, cap = 48, 40, 150
W = pad * 2 + pw * 2 + gap
H = pad * 2 + (ph + cap) * 3 + gap * 2 + 70
s, c = canvas(W, H)
g.draw_text(c, 'Edgelit の描き方: 5つの工程を重ねて1枚のガラスになる', pad, pad + 34, 36, INK, 700, shadow=False)
for i, (img, (_, title, desc)) in enumerate(zip(imgs, stages)):
    col, row = i % 2, i // 2
    x = pad + col * (pw + gap)
    y = pad + 70 + row * (ph + cap + gap)
    rounded_image(c, img, x, y)
    g.draw_text(c, title, x + 4, y + ph + 46, 32, INK, 700, shadow=False)
    g.draw_wrapped(c, desc, x + 4, y + ph + 88, pw - 8, 24, MUTED)
# 6th cell: arrows legend
x = pad + (pw + gap); y = pad + 70 + 2 * (ph + cap + gap)
g.draw_text(c, '影と光は手で描かない。', x + 10, y + 80, 30, INK, 700, shadow=False)
g.draw_text(c, '部品の位置・大きさ・浮いている', x + 10, y + 140, 26, MUTED, 400, shadow=False)
g.draw_text(c, '高さから、GPU のシェーダーが', x + 10, y + 180, 26, MUTED, 400, shadow=False)
g.draw_text(c, '毎フレーム計算する。', x + 10, y + 220, 26, MUTED, 400, shadow=False)
save(s, '04-render-stages.png')

# ---- 光の強さ ----
sc = g.Scene(412, 196)
levels = [('None', 0.0), ('Soft  0.4', 0.4), ('Medium  0.7', 0.7), ('Strong  1.0', 1.0)]
for i, (name, e) in enumerate(levels):
    col, row = i % 2, i // 2
    x = 20 + col * (170 + 32); y = 36 + row * 92
    sc.label(['光なし', '弱 Soft', '中 Medium', '強 Strong'][i], x, y - 8)
    sc.add(x, y, 170, 56, emit=e, text=name.split()[0], bold=(e >= 1.0))
img = sc.render()
s, c = canvas(img.width() + 96, img.height() + 230)
g.draw_text(c, '光の強さ(GlassGlow)', 48, 82, 36, INK, 700, shadow=False)
rounded_image(c, img, 48, 120)
g.draw_text(c, '主操作だけ Strong。入力欄・副ボタンは Medium、外枠やドロップダウンは Soft。', 48, img.height() + 180, 24, MUTED, 400, shadow=False)
save(s, '05-glow-levels.png')

# ---- 浮いている高さ ----
sc = g.Scene(412, 128)
for i, (lab, el) in enumerate([('押し込み 8dp', 8), ('通常 24dp', 24), ('フォーカス 30dp', 30)]):
    x = 16 + i * 132
    sc.label(lab, x, 30, size=14)
    sc.add(x, 44, 112, 56, emit=0.7, elev=el, text='Aa')
img = sc.render()
s, c = canvas(img.width() + 96, img.height() + 250)
g.draw_text(c, '浮いている高さ(elevation)と影', 48, 82, 36, INK, 700, shadow=False)
rounded_image(c, img, 48, 120)
g.draw_wrapped(c, '影は高さに比例して右下へずれ、壁から離れるほどぼける。押すと沈んで影が寄り、光だまりが小さく明るくなる。', 48, img.height() + 172, img.width(), 24, MUTED)
save(s, '06-elevation.png')
