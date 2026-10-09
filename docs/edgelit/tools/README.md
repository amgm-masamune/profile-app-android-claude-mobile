# 図の作り方

`docs/edgelit/` の図は、アプリと同じ AGSL シェーダー(`GlassShaders.kt`)を Skia(skia-python)で CPU 実行して描いている。
押した状態・画面の写真は、CI の Android エミュレータで撮った `screenshots` ブランチの画像を使う。

```
pip install skia-python
# 01〜12 の図(<DEV> は screenshots ブランチの画像を置いたフォルダ)
python3 docs/edgelit/tools/plates.py  docs/edgelit/tools docs/edgelit
python3 docs/edgelit/tools/plates2.py docs/edgelit/tools docs/edgelit <DEV>
python3 docs/edgelit/tools/plates3.py docs/edgelit/tools docs/edgelit <DEV>
# 13 ガラスの影のしくみ(DEV の interactions.gif からスイッチのコマを使う)
python3 docs/edgelit/tools/plates4.py docs/edgelit/tools docs/edgelit <DEV>
```

文字は Noto Sans CJK JP を使う。
