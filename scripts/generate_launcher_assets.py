#!/usr/bin/env python3
"""Create project-authored network-node launcher vectors and PNG derivatives.

Requires Pillow only when regenerating bitmap assets; no third-party artwork.
"""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'app/src/main/res'
BACKGROUND, EDGE, NODE, HUB = '#102330', '#2DD4BF', '#CBF7ED', '#FFCA70'
NODES = [(35, 35), (73, 35), (35, 73), (73, 73)]

def circle(x, y, radius):
    return f'M{x-radius},{y} A{radius},{radius} 0 1,0 {x+radius},{y} A{radius},{radius} 0 1,0 {x-radius},{y} Z'

def vector(monochrome=False):
    color = '#000000' if monochrome else EDGE
    paths = [f'    <path android:fillColor="@android:color/transparent" android:strokeColor="{color}" android:strokeWidth="3" android:strokeLineCap="round" android:pathData="M{x},{y} L54,54" />' for x, y in NODES]
    for x, y in NODES:
        paths.append(f'    <path android:fillColor="{color if monochrome else NODE}" android:pathData="{circle(x,y,5)}" />')
    paths.append(f'    <path android:fillColor="{color if monochrome else EDGE}" android:pathData="{circle(54,54,9)}" />')
    if not monochrome:
        paths.append(f'    <path android:fillColor="{HUB}" android:pathData="{circle(54,54,4)}" />')
    return '<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">\n' + '\n'.join(paths) + '\n</vector>\n'

def render(size, round_mask=False):
    # Legacy icons use the visible 72dp viewport of the 108dp adaptive layer.
    scale = size * 4 / 72
    image = Image.new('RGBA', (size*4, size*4), BACKGROUND)
    draw = ImageDraw.Draw(image)
    def point(x,y): return ((x-18)*scale, (y-18)*scale)
    for x,y in NODES:
        draw.line([point(x,y),point(54,54)], fill=EDGE, width=round(3*scale))
    for x,y,r,color in [(x,y,5,NODE) for x,y in NODES]+[(54,54,9,EDGE),(54,54,4,HUB)]:
        draw.ellipse([point(x-r,y-r),point(x+r,y+r)],fill=color)
    if round_mask:
        mask=Image.new('L',image.size); ImageDraw.Draw(mask).ellipse((0,0,size*4-1,size*4-1),fill=255)
        image.putalpha(mask)
    return image.resize((size,size),Image.Resampling.LANCZOS)

if __name__ == '__main__':
    for name,mono in [('ic_launcher_foreground',False),('ic_launcher_monochrome',True)]:
        (RES/'drawable'/f'{name}.xml').write_text(vector(mono),encoding='utf8',newline='\n')
    glyph = vector(True).replace('108', '48').replace('>\n    <path', '>\n    <group android:translateX="-30" android:translateY="-30">\n    <path', 1).replace('</vector>', '    </group>\n</vector>')
    (RES/'drawable/ic_client_network.xml').write_text(glyph,encoding='utf8',newline='\n')
    background = f'<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">\n    <path android:fillColor="{BACKGROUND}" android:pathData="M0,0 h108 v108 h-108 z" />\n</vector>\n'
    (RES/'drawable/ic_launcher_background.xml').write_text(background,encoding='utf8',newline='\n')
    for density,size in [('mdpi',48),('hdpi',72),('xhdpi',96),('xxhdpi',144),('xxxhdpi',192)]:
        for name,rounded in [('ic_launcher',False),('ic_launcher_round',True)]:
            render(size,rounded).save(RES/f'mipmap-{density}'/f'{name}.png')
    assets = ROOT/'docs/brand'; assets.mkdir(parents=True,exist_ok=True)
    render(512).save(assets/'store-icon-512.png')
    svg=f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="18 18 72 72"><path fill="{BACKGROUND}" d="M18 18h72v72H18z"/>'
    for x,y in NODES: svg+=f'<path d="M{x} {y}L54 54" stroke="{EDGE}" stroke-width="3" stroke-linecap="round"/>'
    for x,y,r,color in [(x,y,5,NODE) for x,y in NODES]+[(54,54,9,EDGE),(54,54,4,HUB)]: svg+=f'<circle cx="{x}" cy="{y}" r="{r}" fill="{color}"/>'
    (assets/'network-nodes.svg').write_text(svg+'</svg>\n',encoding='utf8',newline='\n')
    print('Generated adaptive/monochrome vectors, ten legacy PNGs and the 512px store icon.')
