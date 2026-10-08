"""アプリの GlassShaders.kt から AGSL のソース(COMMON + 各シェーダー)を取り出す。"""
import os
import re

KT = os.path.join(os.path.dirname(__file__), '../../../app/src/main/java/com/example/businesscard/ui/glass/GlassShaders.kt')


def load(path=KT):
    s = open(path, encoding='utf-8').read()
    common = re.search(r'private const val COMMON = """(.*?)"""', s, re.S).group(1)
    out = {}
    for m in re.finditer(r'const val (\w+) = COMMON \+ """(.*?)"""', s, re.S):
        out[m.group(1)] = common + m.group(2)
    return out
