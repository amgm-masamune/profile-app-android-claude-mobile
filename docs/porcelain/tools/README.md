# Porcelain の模型

Porcelain の板の面(波と光)は、アプリと同じ AGSL シェーダー(`PorcelainShaders.kt`)を Skia(skia-python)で CPU 実行して描ける。
部品の影も、アプリ(`PorcelainSurface.kt`)と同じ手順・同じ値(`PorcelainTokens.kt`)で描く。
見本の画像と同じ大きさで描いて並べ、色や影の濃さを合わせるのに使った。

```
pip install skia-python pillow
python3 -c "import sys; sys.path.insert(0, 'docs/porcelain/tools'); import porcelain"  # シェーダーがコンパイルできるかの確認
```

- `porcelain.py`: シェーダーの読み込みと、板の面のシェーダー(`panel_shader`)
- `surfaces.py`: 盛り上がった板・押し込まれた板・光る縁の描き方

値を変えたときは、`PorcelainTokens.kt` とこの2つのファイルを揃えること。
