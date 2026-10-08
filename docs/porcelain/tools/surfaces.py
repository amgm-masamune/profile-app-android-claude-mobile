"""Porcelain の部品(盛り上がった丸い板・押し込まれた板・光る縁)を Skia で描く。
アプリの Modifier.porcelainSurface(PorcelainSurface.kt)と同じ手順・同じ値で描く。"""
import skia

from porcelain import argb, sigma

# PorcelainTokens.kt の PorcelainElevation と同じ値(dp)
RAISED = dict(
    light_dx=-2.0, light_dy=-2.5, light_blur=6.0, light_alpha=0.55,
    drop_dx=2.0, drop_dy=5.5, drop_blur=10.0, drop_alpha=0.36,
    contact_dy=1.5, contact_blur=2.5, contact_alpha=0.2,
)
INSET = dict(dx=1.0, dy=3.0, blur=6.0, alpha=0.26, light_alpha=0.45)

LIGHT_TOP = 0xFFE4E4E4
LIGHT_BOTTOM = 0xFFDAD9D8
DARK_TOP = 0xFF727171
DARK_BOTTOM = 0xFF656464
MUTED_TOP = 0xFFA3A3A3
MUTED_BOTTOM = 0xFF999999


def rrect(x, y, w, h, r):
    return skia.RRect.MakeRectXY(skia.Rect.MakeXYWH(x, y, w, h), r, r)


def blur_paint(color, alpha, blur_px):
    r, g, b, _ = argb(color)
    p = skia.Paint(AntiAlias=True, Color4f=skia.Color4f(r, g, b, alpha))
    if blur_px > 0:
        p.setMaskFilter(skia.MaskFilter.MakeBlur(skia.kNormal_BlurStyle, sigma(blur_px)))
    return p


def surface(c, x, y, w, h, radius, dp, top, bottom, pressed=0.0, glow=0.0, dark=False):
    """盛り上がった板。pressed = 0..1(1 = 押し込まれて、外の影が消え、内側に影ができる)、glow = 0..1(縁が暖色に光る)。"""
    rr = rrect(x, y, w, h, radius)
    lift = 1.0 - pressed
    R = RAISED
    if glow > 0:
        for blur, a in ((18.0, 0.55), (7.0, 0.65)):
            gp = blur_paint(0xFFFFE3C4, a * glow, blur * dp)
            c.drawRRect(rrect(x - 1 * dp, y - 1 * dp, w + 2 * dp, h + 2 * dp, radius + dp), gp)
    if lift > 0:
        c.save(); c.translate(R['light_dx'] * dp * lift, R['light_dy'] * dp * lift)
        c.drawRRect(rr, blur_paint(0xFFFFFFFF, R['light_alpha'] * lift * (0.45 if dark else 1.0), R['light_blur'] * dp))
        c.restore()
        c.save(); c.translate(R['drop_dx'] * dp * lift, R['drop_dy'] * dp * lift)
        c.drawRRect(rr, blur_paint(0xFF000000, R['drop_alpha'] * lift, R['drop_blur'] * dp))
        c.restore()
        c.save(); c.translate(0, R['contact_dy'] * dp * lift)
        c.drawRRect(rr, blur_paint(0xFF000000, R['contact_alpha'] * lift, R['contact_blur'] * dp))
        c.restore()
    # 面: 上が明るいグラデーション
    fill = skia.Paint(AntiAlias=True)
    fill.setShader(skia.GradientShader.MakeLinear(
        [skia.Point(0, y), skia.Point(0, y + h)], [skia.Color4f(*argb(top)).toColor(), skia.Color4f(*argb(bottom)).toColor()]))
    c.drawRRect(rr, fill)
    # 内側の影(押し込まれた分だけ)
    if pressed > 0:
        I = INSET
        c.save(); c.clipRRect(rr, skia.ClipOp.kIntersect, True)
        hole = skia.Path(); hole.addRect(skia.Rect.MakeXYWH(x - w, y - h, w * 3, h * 3)); hole.addRRect(rr, skia.PathDirection.kCCW)
        hole.setFillType(skia.PathFillType.kEvenOdd)
        c.save(); c.translate(I['dx'] * dp, I['dy'] * dp)
        c.drawPath(hole, blur_paint(0xFF000000, I['alpha'] * pressed, I['blur'] * dp)); c.restore()
        c.save(); c.translate(-I['dx'] * dp, -I['dy'] * dp)
        c.drawPath(hole, blur_paint(0xFFFFFFFF, I['light_alpha'] * pressed * (0.3 if dark else 1.0), I['blur'] * dp)); c.restore()
        c.restore()
    # 上の縁のつや(1dp)
    rim = skia.Paint(AntiAlias=True, Style=skia.Paint.kStroke_Style, StrokeWidth=1.0 * dp)
    a_top = (0.18 if dark else 0.7) * lift
    rim.setShader(skia.GradientShader.MakeLinear(
        [skia.Point(0, y), skia.Point(0, y + h * 0.55)], [skia.Color4f(1, 1, 1, a_top).toColor(), skia.Color4f(1, 1, 1, 0).toColor()]))
    c.drawRRect(rrect(x + 0.5 * dp, y + 0.5 * dp, w - dp, h - dp, radius - 0.5 * dp), rim)
