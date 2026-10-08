import sys, skia
sys.path.insert(0, sys.argv[1])
import glassrender as g
OUT, DEV = sys.argv[2], sys.argv[3]
INK = (0.17, 0.145, 0.125, 1)
MUTED = (0.42, 0.38, 0.35, 1)
PAPER = skia.Color4f(0.965, 0.953, 0.94, 1)
D = 2.625
SAMP = skia.SamplingOptions(skia.CubicResampler.Mitchell())


def canvas(w, h):
    s = g.surface(w, h); c = s.getCanvas(); c.clear(PAPER); return s, c


def save(s, name):
    s.makeImageSnapshot().save(f"{OUT}/{name}", skia.kPNG); print(name, s.width(), s.height())


def c4(argb):
    r, gg, b, a = g.argb(argb); return skia.Color4f(r, gg, b, a)


def wall_strip(w, h):
    sc = g.Scene(w / D, h / D)
    return sc.render(('wall',))


def draw_img(c, img, x, y, w=None, h=None, r=0, src=None):
    w = w or img.width(); h = h or img.height()
    dst = skia.Rect.MakeXYWH(x, y, w, h)
    c.save()
    if r:
        c.clipRRect(skia.RRect.MakeRectXY(dst, r, r), True)
    if src is None:
        src = skia.Rect.MakeWH(img.width(), img.height())
    c.drawImageRect(img, src, dst, SAMP)
    c.restore()


# ===== 07 色 =====
groups = [
    ('壁(地の色。実際の明るさは照明で決まる)', [
        ('wallTop', 0xFFAA9B8E, '#AA9B8E', '壁の上'), ('wallBottom', 0xFF968677, '#968677', '壁の下')]),
    ('ガラス(壁を透かした上に混ぜる色)', [
        ('glass', 0x40DCE1E6, '#DCE1E6 · 25%', 'ガラスの膜'), ('glassPressed', 0x4DFFFFFF, '白 · 30%', '押下・フォーカス'),
        ('glassSelected', 0x52FFFFFF, '白 · 32%', '選択中のタブ'), ('glassTile', 0x5CFFFFFF, '白 · 36%', 'アイコンのタイル'),
        ('glassBorder', 0x8CFFFFFF, '白 · 55%', '縁の線'), ('focusBorder', 0xF2FFFFFF, '白 · 95%', 'フォーカス中の縁'),
        ('dialog', 0xB8B0A59B, '#B0A59B · 72%', 'ダイアログの板')]),
    ('光', [
        ('glow', 0xFFFFFCF6, '#FFFCF6', '光源の色'), ('thumbTop', 0xFFF7F4F0, '#F7F4F0', 'つまみ(上)'),
        ('thumbBottom', 0xFFDAD5CF, '#DAD5CF', 'つまみ(下)')]),
    ('文字', [
        ('ink', 0xFFFFFFFF, '白', '文字'), ('inkMuted', 0xC7FFFFFF, '白 · 78%', '補助の文字'),
        ('inkFaint', 0x99FFFFFF, '白 · 60%', 'プレースホルダ・非選択'), ('danger', 0xFFFFB8AB, '#FFB8AB', '削除・エラー')]),
]
W = 1290; pad = 48; colw = (W - pad * 2 - 40) // 2; rowh = 118
rows_total = sum((len(it) + 1) // 2 for _, it in groups)
H = pad + 80 + len(groups) * 70 + rows_total * rowh + pad
s, c = canvas(W, H)
g.draw_text(c, '色のトークン(SoftGlassColors → Edgelit の色)', pad, pad + 34, 36, INK, 700, shadow=False)
wall = wall_strip(200, 200)
y = pad + 80
for title, items in groups:
    g.draw_text(c, title, pad, y + 40, 26, INK, 700, shadow=False)
    y += 70
    for i, (name, col, val, use) in enumerate(items):
        cx = pad + (i % 2) * (colw + 40)
        cy = y + (i // 2) * rowh
        draw_img(c, wall, cx, cy, 96, 96, r=16)
        c.drawRRect(skia.RRect.MakeRectXY(skia.Rect.MakeXYWH(cx + 14, cy + 14, 68, 68), 12, 12), skia.Paint(Color4f=c4(col), AntiAlias=True))
        c.drawRRect(skia.RRect.MakeRectXY(skia.Rect.MakeXYWH(cx + 14, cy + 14, 68, 68), 12, 12),
                    skia.Paint(Color4f=skia.Color4f(1, 1, 1, 0.5), AntiAlias=True, Style=skia.Paint.kStroke_Style, StrokeWidth=1.5))
        g.draw_text(c, name, cx + 120, cy + 36, 26, INK, 700, shadow=False)
        g.draw_text(c, val, cx + 120, cy + 70, 22, MUTED, 400, shadow=False)
        g.draw_text(c, use, cx + 120, cy + 100, 22, MUTED, 400, shadow=False)
    y += ((len(items) + 1) // 2) * rowh
save(s, '07-colors.png')

# ===== 08 文字 =====
styles = [
    ('title', 28, 34, 700, '名刺一覧', '画面タイトル'),
    ('cardName', 30, 36, 700, '山田 太郎', '名刺の氏名'),
    ('buttonStrong', 19, 24, 700, '名刺を追加', '主ボタン'),
    ('button', 19, 24, 400, 'キャンセル', '副ボタン・タブ'),
    ('field', 18, 24, 400, 'taro@example.com', '入力値・選択肢'),
    ('cardCompany', 17, 22, 600, '株式会社サンプル', '名刺の会社名・ダイアログ見出し'),
    ('label', 15, 20, 500, 'メールアドレス', '部品の上のラベル'),
    ('body', 15, 22, 400, '090-1234-5678', '本文・補助'),
    ('caption', 13, 18, 500, '氏名を入力してください', 'エラーなどの注記'),
]
rowh = 116
H = pad + 90 + len(styles) * rowh + pad
s, c = canvas(W, H)
g.draw_text(c, '文字のトークン(すべて白 + ごく弱い影)', pad, pad + 34, 36, INK, 700, shadow=False)
band = wall_strip((W - pad * 2) * 0.62, rowh - 14)
y = pad + 90
for name, sp, lh, wt, sample, use in styles:
    draw_img(c, band, pad, y, band.width(), band.height(), r=14)
    g.draw_text(c, sample, pad + 28, y + band.height() / 2 + sp * D * 0.36, sp * D * 0.95, (1, 1, 1, 1), wt)
    x2 = pad + band.width() + 32
    g.draw_text(c, name, x2, y + 40, 26, INK, 700, shadow=False)
    g.draw_text(c, f'{sp}sp / 行 {lh}sp / {wt}', x2, y + 72, 22, MUTED, 400, shadow=False)
    g.draw_text(c, use, x2, y + 100, 22, MUTED, 400, shadow=False)
    y += rowh
save(s, '08-type.png')

# ===== 03 横から見た図 =====
W3, H3 = 1290, 1000
s, c = canvas(W3, H3)
g.draw_text(c, '横から見た Edgelit(壁・板・光の関係)', pad, pad + 34, 36, INK, 700, shadow=False)
P = lambda col, a=1.0, sw=None, stroke=False: skia.Paint(Color4f=skia.Color4f(col[0], col[1], col[2], a), AntiAlias=True,
                                                        Style=skia.Paint.kStroke_Style if stroke else skia.Paint.kFill_Style, StrokeWidth=sw or 1)
greige = (0.63, 0.58, 0.53)
wallx = 260
# 壁
wall_grad = skia.GradientShader.MakeLinear([skia.Point(0, 140), skia.Point(0, 900)], [skia.Color4f(0.69, 0.63, 0.58, 1).toColor(), skia.Color4f(0.55, 0.5, 0.45, 1).toColor()])
c.drawRect(skia.Rect.MakeLTRB(150, 140, wallx, 840), skia.Paint(Shader=wall_grad))
g.draw_text(c, '壁', 172, 180, 30, (1, 1, 1, 1), 700)
# 光源(キーライト)
lx, ly = 1130, 170
for r_, a_ in [(70, 0.10), (48, 0.18), (30, 1.0)]:
    c.drawCircle(lx, ly, r_, P((1.0, 0.86, 0.55), a_))
g.draw_text(c, 'キーライト', lx - 70, ly + 76, 24, INK, 700, shadow=False)
g.draw_text(c, '(左上・手前)', lx - 70, ly + 106, 22, MUTED, 400, shadow=False)
# 板
px0, px1, py0, py1 = 480, 500, 300, 640
# 光線と影
top_t = (wallx - lx) / (px0 - lx); sy0 = ly + (py0 - ly) * top_t
bot_t = (wallx - lx) / (px0 - lx); sy1 = ly + (py1 - ly) * bot_t
c.drawRect(skia.Rect.MakeLTRB(wallx - 18, sy0, wallx, sy1), P((0.22, 0.18, 0.15), 0.55))
dash = skia.Paint(Color4f=skia.Color4f(0.85, 0.62, 0.25, 0.9), AntiAlias=True, Style=skia.Paint.kStroke_Style, StrokeWidth=2.5,
                  PathEffect=skia.DashPathEffect.Make([12, 10], 0))
for py in (py0, py1):
    t = (wallx - lx) / (px0 - lx)
    c.drawLine(lx, ly, wallx, ly + (py - ly) * t, dash)
g.draw_text(c, '影', wallx + 14, sy1 - 20, 26, INK, 700, shadow=False)
g.draw_text(c, '板より下(画面では右下)へずれる', wallx + 14, sy1 + 14, 22, MUTED, 400, shadow=False)
# 光源の光(下端から壁へ)
ex, ey = px0, py1 - 6
for i in range(9):
    tx = wallx; tyy = ey + 30 + i * 22
    c.drawLine(ex, ey, tx, tyy, skia.Paint(Color4f=skia.Color4f(1, 0.95, 0.8, 0.35), AntiAlias=True, StrokeWidth=3))
pool = skia.GradientShader.MakeRadial(skia.Point(wallx, ey + 110), 120, [skia.Color4f(1, 0.95, 0.82, 0.85).toColor(), skia.Color4f(1, 0.95, 0.82, 0).toColor()])
c.drawRect(skia.Rect.MakeLTRB(wallx - 18, ey - 10, wallx + 4, ey + 230), skia.Paint(Shader=pool))
# 板本体
c.drawRRect(skia.RRect.MakeRectXY(skia.Rect.MakeLTRB(px0, py0, px1, py1), 8, 8), P((0.93, 0.94, 0.95), 0.75))
c.drawRRect(skia.RRect.MakeRectXY(skia.Rect.MakeLTRB(px0, py0, px1, py1), 8, 8), P((1, 1, 1), 1, 2, True))
glowp = skia.GradientShader.MakeRadial(skia.Point(px0 + 10, py1), 60, [skia.Color4f(1, 0.98, 0.9, 1).toColor(), skia.Color4f(1, 0.98, 0.9, 0).toColor()])
c.drawCircle(px0 + 10, py1, 60, skia.Paint(Shader=glowp))
c.drawCircle(px0 + 10, py1 - 4, 7, P((1, 1, 1), 1))
# 押したときの板(指へ吸い寄せられて手前へ浮く: 24 → 約31dp)
lift_px = (px0 - wallx) / 24 * 7
ghost = skia.Paint(Color4f=skia.Color4f(*INK[:3], 0.55), AntiAlias=True, Style=skia.Paint.kStroke_Style, StrokeWidth=2,
                   PathEffect=skia.DashPathEffect.Make([7, 6], 0))
c.drawRRect(skia.RRect.MakeRectXY(skia.Rect.MakeLTRB(px0 + lift_px, py0 - 4, px1 + lift_px, py1 + 4), 8, 8), ghost)
ay = (py0 + py1) / 2
c.drawLine(px1 + 6, ay, px1 + lift_px - 10, ay, arrow_p := P(INK, 1, 2.5, True))
c.drawLine(px1 + lift_px - 10, ay, px1 + lift_px - 22, ay - 8, arrow_p); c.drawLine(px1 + lift_px - 10, ay, px1 + lift_px - 22, ay + 8, arrow_p)
tx0 = px1 + lift_px + 28
g.draw_text(c, 'すりガラスの板', tx0, py0 + 30, 28, INK, 700, shadow=False)
g.draw_wrapped(c, '背後の壁(と自分の影)を実際にぼかして透かす。縁は丸く磨かれ、光を受けて光る', tx0, py0 + 70, 360, 22, MUTED)
g.draw_text(c, '押すと指へ寄る(点線)', tx0, ay + 20, 24, INK, 700, shadow=False)
g.draw_text(c, '24 → 約31dp、影が伸びる', tx0, ay + 54, 22, MUTED, 400, shadow=False)
g.draw_text(c, '縁の光源(下端)', tx0, py1 + 10, 26, INK, 700, shadow=False)
g.draw_wrapped(c, 'ガラスの中で光が散り、縁を伝い、壁を照らす(光だまり)', tx0, py1 + 46, 360, 22, MUTED)
# 高さの寸法
arrow = P(INK, 1, 2.5, True)
c.drawLine(wallx, 260, px0, 260, arrow)
for x_, d_ in [(wallx, 1), (px0, -1)]:
    c.drawLine(x_, 260, x_ + 14 * d_, 252, arrow); c.drawLine(x_, 260, x_ + 14 * d_, 268, arrow)
g.draw_text(c, '浮いている高さ 24dp', wallx + 30, 245, 24, INK, 700, shadow=False)
# 見る人
vx, vy = 1150, 560
c.drawCircle(vx, vy, 34, P(INK, 1, 3, True)); c.drawCircle(vx - 12, vy, 9, P(INK, 1))
c.drawLine(vx - 40, vy, px1 + 30, vy - 20, skia.Paint(Color4f=skia.Color4f(*INK), AntiAlias=True, StrokeWidth=2, Style=skia.Paint.kStroke_Style,
                                                       PathEffect=skia.DashPathEffect.Make([4, 8], 0)))
g.draw_text(c, '見る人(画面)', vx - 70, vy + 70, 24, INK, 700, shadow=False)
g.draw_wrapped(c, '押すと板は指に吸い寄せられて手前へ浮き(24 → 約31dp)、影が伸びてぼけ、光だまりは広く淡くなる。離すと壁側へ戻る。', pad, 920, W3 - pad * 2, 24, MUTED)
save(s, '03-side-view.png')
