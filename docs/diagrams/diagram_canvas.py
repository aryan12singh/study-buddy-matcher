"""Shared drawing surface for the design panels: palette, text, UML shapes and markers.

Only the Python 3 standard library is used. Panels are built by the modules that import
Canvas; render_diagrams.py is the entry point that writes and checks the exports.
"""

from html import escape
from pathlib import Path

from brand_icons import BRAND_ICONS


HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
INK = "#172d3b"
MUTED = "#506474"
LINE = "#647b88"
BLUE = "#254e73"
TEAL = "#176c63"
AMBER = "#8a5a1a"
BG = "#f6f8f9"
PALE = "#eaf1f7"
GREEN = "#eaf5f1"
WARM = "#fff5e5"


class Canvas:
    def __init__(self, slug, title, subtitle, width=1600, height=1100):
        self.slug, self.title = slug, title
        self.width, self.height = width, height
        self.back, self.edges, self.nodes, self.labels = [], [], [], []
        self.geo = {}
        self.rect(0, 0, width, height, fill=BG, stroke="none", layer=self.back)
        self.rect(0, 0, width, 128, fill=INK, stroke="none", layer=self.back)
        self.text(46, 45, "STUDY BUDDY MATCHER  /  DESIGN REFERENCE", 15, "#a8d4ca", weight=700)
        self.text(46, 85, title, 31, "#ffffff", weight=700)
        self.text(46, 110, subtitle, 17, "#d9e5ea")
        for n, glyph in enumerate(("_", "□", "×")):
            # Decorative window controls, echoing the application's retro desktop title bars.
            left = width-46-(3-n)*34
            self.rect(left, 28, 26, 26, "#d1d2d0", "#f4f5f4", 0, self.back)
            self.text(left+13, 47 if glyph != "_" else 43, glyph, 17, INK, 700, "middle", self.back)
        self.line([(44, height-54), (width-44, height-54)], color="#c5d2da")
        self.text(width-46, height-27, f"docs/diagrams/exports/{slug}.svg", 14, MUTED, anchor="end")

    def rect(self, x, y, w, h, fill="white", stroke="#c5d2da", radius=0, layer=None, extra=""):
        (self.nodes if layer is None else layer).append(
            f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{radius}" '
            f'fill="{fill}" stroke="{stroke}" stroke-width="1.5" {extra}/>')

    def text(self, x, y, value, size=19, color=INK, weight=400, anchor="start", layer=None, italic=False):
        style = ' font-style="italic"' if italic else ""
        (self.labels if layer is None else layer).append(
            f'<text x="{x}" y="{y}" font-size="{size}" fill="{color}" '
            f'font-weight="{weight}" text-anchor="{anchor}"{style}>{escape(str(value))}</text>')

    def lines(self, x, y, rows, size=19, color=INK, gap=27, weight=400, anchor="start"):
        """Draw rows of text. A row that starts with "~" is an abstract member, shown in italics."""
        for n, row in enumerate(rows):
            abstract = str(row).startswith("~")
            self.text(x, y+n*gap, str(row)[1:] if abstract else row, size, color, weight, anchor, italic=abstract)

    def panel(self, x, y, w, h, label, color=BLUE, fill=PALE):
        self.rect(x, y, w, h, fill, "#bdccd5", 12, self.back)
        self.text(x+22, y+31, label, 18, color, 700)

    def box(self, x, y, w, h, title, rows, color=BLUE, fill="white", stereotype=None, size=19):
        self.geo[title] = (x, y, w, h)
        self.rect(x, y, w, h, fill, color, 6, extra='data-node="box"')
        self.rect(x, y, w, 6, color, "none")
        top = y+35
        if stereotype:
            self.text(x+18, y+29, f"«{stereotype}»", 15, color)
            top = y+57
        self.text(x+18, top, title, 23, color, 700)
        self.lines(x+18, top+34, rows, size, gap=27)

    def cls(self, x, y, w, name, attrs=(), methods=(), stereotype=None, color=BLUE, size=18, abstract=False):
        """UML class box. Abstract classes take an italic name; "~" rows are abstract operations."""
        header = 70 if stereotype else 49
        h = header + (len(attrs)*26+20 if attrs else 0) + (len(methods)*26+20 if methods else 0)
        self.geo[name] = (x, y, w, h)
        self.rect(x, y, w, h, "white", color, 0, extra=f'data-class="{escape(name)}"')
        self.rect(x, y, w, header, PALE if color == BLUE else GREEN, "none")
        if stereotype:
            self.text(x+w/2, y+25, f"«{stereotype}»", 15, color, anchor="middle")
        self.text(x+w/2, y+header-16, name, 22, color, 700, "middle", italic=abstract)
        cursor = y+header
        for rows in (attrs, methods):
            if not rows:
                continue
            self.line([(x, cursor), (x+w, cursor)], color=color, layer=self.nodes)
            self.lines(x+16, cursor+29, rows, size, gap=26)
            cursor += len(rows)*26+20
        return h

    def line(self, points, color=LINE, dashed=False, end=None, start=None, layer=None, width=2):
        attrs = ' stroke-dasharray="7 6"' if dashed else ""
        if end:
            attrs += f' marker-end="url(#{end})"'
        if start:
            attrs += f' marker-start="url(#{start})"'
        (self.edges if layer is None else layer).append(
            f'<polyline points="{" ".join(f"{x},{y}" for x,y in points)}" fill="none" '
            f'stroke="{color}" stroke-width="{width}" stroke-linejoin="round"{attrs}/>')

    def label(self, x, y, rows, size=17, color=MUTED, anchor="middle", fill=BG):
        if isinstance(rows, str):
            rows = [rows]
        width = max(len(row) for row in rows)*size*.54+16
        left = x-width/2 if anchor == "middle" else x-7
        self.rect(left, y-size, width, len(rows)*(size+5)+5, fill, "none", 3, self.labels)
        self.lines(x, y, rows, size, color, gap=size+5, anchor=anchor)

    def note(self, x, y, w, rows, title=None, fill=WARM):
        h = len(rows)*25+24+(30 if title else 0)
        self.rect(x, y, w, h, fill, "#d6c7a8" if fill == WARM else "#c5d2da", 6)
        offset = 28
        if title:
            self.text(x+17, y+28, title, 19, AMBER if fill == WARM else BLUE, 700)
            offset += 30
        self.lines(x+17, y+offset, rows, 18, gap=25)

    def actor(self, x, y, title, note=None):
        self.nodes.append(f'<circle cx="{x}" cy="{y}" r="19" fill="white" stroke="{INK}" stroke-width="2.5"/>')
        self.line([(x,y+19),(x,y+84)], color=INK, layer=self.nodes, width=2.5)
        self.line([(x-38,y+45),(x+38,y+45)], color=INK, layer=self.nodes, width=2.5)
        self.line([(x-34,y+120),(x,y+84),(x+34,y+120)], color=INK, layer=self.nodes, width=2.5)
        self.text(x,y+150,title,22,INK,700,"middle")
        if note:
            # Second line of a two-word actor name.
            self.text(x,y+176,note,22,INK,700,"middle")

    def usecase(self, x, y, w, rows, e2=False):
        self.nodes.append(f'<ellipse cx="{x}" cy="{y}" rx="{w/2}" ry="37" fill="{GREEN if e2 else PALE}" stroke="{TEAL if e2 else BLUE}" stroke-width="1.7"/>')
        self.lines(x, y-(len(rows)-1)*12+6, rows, 18, gap=24, anchor="middle")

    def fragment(self, x, y, w, h, kind, guard):
        self.rect(x,y,w,h,"#ffffff","#a7bac5",0,self.back)
        self.rect(x,y,80,29,PALE,"#a7bac5",0,self.back)
        self.text(x+12,y+21,kind,16,BLUE,700)
        self.text(x+96,y+21,guard,17,MUTED)

    # ---- geometry helpers -------------------------------------------------------------

    def at(self, name, side, t=0.5):
        """Point on one side of a named box: side is l, r, t or b; t runs 0..1 along that side."""
        x, y, w, h = self.geo[name]
        return {"l": (x, y+h*t), "r": (x+w, y+h*t), "t": (x+w*t, y), "b": (x+w*t, y+h)}[side]

    @staticmethod
    def elbow(a, b, mode="hv", mid=None):
        """Right-angled route from a to b: hv, vh, or hvh / vhv through a middle coordinate."""
        (ax, ay), (bx, by) = a, b
        if mode == "hv":
            return [a, (bx, ay), b]
        if mode == "vh":
            return [a, (ax, by), b]
        if mode == "hvh":
            mid = (ax+bx)/2 if mid is None else mid
            return [a, (mid, ay), (mid, by), b]
        mid = (ay+by)/2 if mid is None else mid
        return [a, (ax, mid), (bx, mid), b]

    # ---- UML structure shapes ---------------------------------------------------------

    def package(self, x, y, w, h, name, color=BLUE, fill="white", size=19):
        """UML package: a tabbed folder whose name sits in the tab because contents are shown."""
        tab = len(name)*size*.6+34
        self.rect(x, y-32, tab, 32, fill, color, 0, self.back)
        self.rect(x, y, w, h, fill, color, 0, self.back)
        self.text(x+16, y-10, name, size, color, 700, layer=self.back)
        self.geo[name] = (x, y, w, h)

    def node(self, x, y, w, h, stereotype, name, color=BLUE, fill="white", depth=16):
        """UML deployment node: a box drawn with depth."""
        top = f"{x},{y} {x+depth},{y-depth} {x+w+depth},{y-depth} {x+w},{y}"
        side = f"{x+w},{y} {x+w+depth},{y-depth} {x+w+depth},{y+h-depth} {x+w},{y+h}"
        for points in (top, side):
            self.back.append(f'<polygon points="{points}" fill="#dfe8ee" stroke="{color}" stroke-width="1.5"/>')
        self.rect(x, y, w, h, fill, color, 0, self.back)
        self.text(x+w/2, y+26, f"«{stereotype}»", 15, color, anchor="middle", layer=self.back)
        self.text(x+w/2, y+52, name, 21, color, 700, "middle", self.back)
        self.geo[name] = (x, y, w, h)

    def artifact(self, x, y, w, name, rows=(), stereotype="artifact", color=BLUE):
        h = 62+len(rows)*24+(8 if rows else 0)
        self.rect(x, y, w, h, "white", color, 0)
        self.text(x+w/2, y+24, f"«{stereotype}»", 14, color, anchor="middle")
        self.text(x+w/2, y+48, name, 18, color, 700, "middle")
        self.lines(x+w/2, y+76, rows, 16, MUTED, gap=24, anchor="middle")
        self.geo[name] = (x, y, w, h)
        return h

    # ---- UML state machine shapes -----------------------------------------------------

    def state(self, x, y, w, h, name, rows=(), color=BLUE, fill=PALE):
        self.rect(x, y, w, h, fill, color, 18)
        self.text(x+w/2, y+(34 if rows else h/2+8), name, 22, color, 700, "middle")
        if rows:
            self.line([(x, y+50), (x+w, y+50)], color=color, layer=self.nodes, width=1.5)
            self.lines(x+w/2, y+78, rows, 17, gap=24, anchor="middle")
        self.geo[name] = (x, y, w, h)

    def initial(self, x, y):
        self.nodes.append(f'<circle cx="{x}" cy="{y}" r="11" fill="{INK}"/>')

    def final(self, x, y):
        self.nodes.append(f'<circle cx="{x}" cy="{y}" r="15" fill="white" stroke="{INK}" stroke-width="2.5"/>'
                          f'<circle cx="{x}" cy="{y}" r="8" fill="{INK}"/>')

    # ---- entity-relationship shapes ---------------------------------------------------

    def table(self, x, y, w, name, columns, color=BLUE, size=16):
        """Database table: header band, then (key, column, type) rows; key is PK, FK, UQ or blank."""
        h = 40+len(columns)*24+12
        self.rect(x, y, w, h, "white", color, 0, extra=f'data-table="{escape(name)}"')
        self.rect(x, y, w, 40, color, color)
        self.text(x+w/2, y+27, name, 18, "#ffffff", 700, "middle")
        for n, (key, column, kind) in enumerate(columns):
            row = y+40+24*n+23
            self.text(x+9, row, key, 13, AMBER if key.startswith("PK") else TEAL, 700)
            self.text(x+62, row, column, size, INK, 700 if key.startswith("PK") else 400)
            self.text(x+w-10, row, kind, 15, MUTED, anchor="end")
        self.geo[name] = (x, y, w, h)
        return h

    def crow(self, point, towards, kind):
        """Crow's-foot end at a table edge. kind: 1 (exactly one), 01 (zero or one), 0n, 1n."""
        (px, py), (tx, ty) = point, towards
        length = max(((tx-px)**2+(ty-py)**2)**.5, 1)
        ux, uy = (tx-px)/length, (ty-py)/length          # unit vector away from the table
        nx, ny = -uy, ux
        def bar(distance):
            cx, cy = px+ux*distance, py+uy*distance
            self.line([(cx-nx*9, cy-ny*9), (cx+nx*9, cy+ny*9)], color=INK, layer=self.nodes, width=2)
        if kind.endswith("n"):
            tip = (px+ux*16, py+uy*16)
            for spread in (-9, 0, 9):
                self.line([tip, (px+nx*spread, py+ny*spread)], color=INK, layer=self.nodes, width=2)
        else:
            bar(8)
        if kind.startswith("0"):
            cx, cy = px+ux*25, py+uy*25
            self.nodes.append(f'<circle cx="{cx}" cy="{cy}" r="6" fill="white" stroke="{INK}" stroke-width="2"/>')
        else:
            bar(21 if kind.endswith("n") else 15)

    def relate(self, points, start, end, dashed=False):
        """Relationship line between two tables with a crow's-foot symbol at each end."""
        self.line(points, color=INK, dashed=dashed, width=1.8)
        self.crow(points[0], points[1], start)
        self.crow(points[-1], points[-2], end)

    # ---- technology tiles -------------------------------------------------------------

    def icon(self, x, y, size, slug):
        """Brand mark from brand_icons.py (Simple Icons, CC0), scaled from its 24 by 24 grid."""
        _, colour, path = BRAND_ICONS[slug]
        self.labels.append(f'<path transform="translate({x},{y}) scale({size/24})" fill="{colour}" d="{path}"/>')

    def glyph(self, x, y, size, kind):
        """Neutral drawing for a service with no brand mark in Simple Icons."""
        s = size/24
        shapes = {
            "wave": "M2 12h2M6 8v8M10 4v16M14 7v10M18 10v4M22 12h0",
            "weather": "M7 18h10a4 4 0 0 0 .6-7.96A6 6 0 0 0 6.1 11.1 3.5 3.5 0 0 0 7 18zM17 3v2M21 7h-2M20 4l-1.4 1.4",
            "mock": "M5 8h14l-1.5 11h-11zM9 8V5h6v3M9 12h6",
            "pixel": "M4 4h16v12H4zM9 20h6M12 16v4M8 8h3v3H8zM13 8h3v3h-3z",
        }
        self.labels.append(f'<path transform="translate({x},{y}) scale({s})" fill="none" stroke="{MUTED}" '
                           f'stroke-width="2" stroke-linecap="round" stroke-linejoin="round" d="{shapes[kind]}"/>')

    def tech(self, x, y, w, slug, name, role, h=78, glyph=None):
        """One technology: a bevelled tile with its mark, its name and what the project uses it for."""
        self.rect(x+4, y+4, w, h, "#b9c6ce", "none", 0, self.nodes)
        self.rect(x, y, w, h, "white", INK, 0)
        self.rect(x, y, 70, h, "#f1f4f6", "none", 0)
        self.line([(x+70, y), (x+70, y+h)], color="#c5d2da", layer=self.nodes, width=1.5)
        self.line([(x, y), (x+w, y), (x+w, y+h), (x, y+h), (x, y)], color=INK, layer=self.nodes, width=1.5)
        if glyph:
            self.glyph(x+15, y+(h-40)/2, 40, glyph)
        else:
            self.icon(x+15, y+(h-40)/2, 40, slug)
        self.text(x+84, y+h/2-6, name, 19, INK, 700)
        self.text(x+84, y+h/2+18, role, 15, MUTED)
        self.geo[name] = (x, y, w, h)

    def svg(self):
        return '\n'.join([
            '<?xml version="1.0" encoding="UTF-8"?>',
            f'<svg xmlns="http://www.w3.org/2000/svg" width="{self.width}" height="{self.height}" '
            f'viewBox="0 0 {self.width} {self.height}" role="img" aria-labelledby="title description">',
            f'<title id="title">{escape(self.title)}</title>',
            '<desc id="description">Study Buddy Matcher design diagram. Editable source: '
            'docs/diagrams/render_diagrams.py. Supplementary text and notation are in docs/diagrams/README.md.</desc>',
            '<defs><marker id="arrow" viewBox="0 0 12 12" refX="11" refY="6" markerWidth="8" markerHeight="8" orient="auto-start-reverse">'
            f'<path d="M 1 1 L 11 6 L 1 11" fill="none" stroke="{LINE}" stroke-width="1.6"/></marker>',
            '<marker id="solid" viewBox="0 0 12 12" refX="11" refY="6" markerWidth="9" markerHeight="9" orient="auto-start-reverse">'
            f'<path d="M 1 1 L 11 6 L 1 11 Z" fill="{LINE}" stroke="{LINE}" stroke-width="1"/></marker>',
            '<marker id="triangle" viewBox="0 0 14 14" refX="13" refY="7" markerWidth="12" markerHeight="12" orient="auto-start-reverse">'
            f'<path d="M 1 1 L 13 7 L 1 13 Z" fill="white" stroke="{LINE}" stroke-width="1.4"/></marker>',
            '<marker id="diamond" viewBox="0 0 16 12" refX="1" refY="6" markerWidth="13" markerHeight="10" orient="auto-start-reverse">'
            f'<path d="M 1 6 L 8 1 L 15 6 L 8 11 Z" fill="{LINE}" stroke="{LINE}"/></marker></defs>',
            '<g font-family="Tahoma, Verdana, Arial, sans-serif">',
            *self.back, *self.edges, *self.nodes, *self.labels, '</g></svg>', ''
        ])


class Sequence:
    """UML sequence diagram drawn on a Canvas.

    Notation follows the course slides: a box per participant (":ClassName"), a dashed lifeline,
    activation bars, a solid line with a filled arrowhead for a synchronous call, a dashed line
    with an open arrowhead for a return, and alt / opt / loop frames with guards.
    """

    HALF = 7

    def __init__(self, canvas, name, participants, top, bottom, left=45, right=None):
        self.c, self.xs, self.top, self.bottom = canvas, [], top, bottom
        right = canvas.width-45 if right is None else right
        canvas.rect(left, top-62, right-left, bottom-top+84, "white", "#93a9b6", 0, canvas.back)
        tab = len(name)*9.6+58
        canvas.back.append(f'<polygon points="{left},{top-62} {left+tab},{top-62} {left+tab},{top-44} '
                           f'{left+tab-12},{top-32} {left},{top-32}" fill="{PALE}" stroke="#93a9b6" stroke-width="1.5"/>')
        canvas.text(left+12, top-41, "sd  "+name, 16, BLUE, 700, layer=canvas.back)
        self.labels = [label for _, label, _ in participants]
        for x, label, kind in participants:
            self.xs.append(x)
            if kind == "actor":
                canvas.nodes.append(f'<circle cx="{x}" cy="{top-8}" r="10" fill="white" stroke="{INK}" stroke-width="2"/>')
                for points in ([(x, top+2), (x, top+24)], [(x-16, top+10), (x+16, top+10)],
                               [(x-13, top+40), (x, top+24), (x+13, top+40)]):
                    canvas.line(points, color=INK, layer=canvas.nodes, width=2)
                canvas.text(x, top+60, label, 17, INK, 700, "middle")
                canvas.line([(x, top+68), (x, bottom)], dashed=True, color="#8ca1ad")
            elif kind == "class":
                self._head(x, top, label)
                canvas.line([(x, top+58), (x, bottom)], dashed=True, color="#8ca1ad")
            # kind "late": the object does not exist yet; create() draws its box where it is made.

    def _head(self, x, y, label):
        width = max(len(label)*10.4+34, 150)
        self.c.rect(x-width/2, y, width, 58, "white", BLUE, 0, extra=f'data-class="{escape(label.split(":")[-1])}"')
        self.c.text(x, y+36, label, 18, BLUE, 700, "middle")
        return width

    def bar(self, index, y1, y2, level=0):
        """Activation bar: the participant is executing between y1 and y2."""
        self.c.rect(self.xs[index]-self.HALF+level*self.HALF, y1, 2*self.HALF, y2-y1, "white", BLUE)

    def _edge(self, index, other_x, level):
        x = self.xs[index]+level*self.HALF
        return x+self.HALF if other_x > x else x-self.HALF

    def call(self, a, b, y, text, la=0, lb=0, dashed=False, end="solid", size=16):
        """Synchronous call from participant a to b at height y."""
        x1, x2 = self._edge(a, self.xs[b], la), self._edge(b, self.xs[a], lb)
        self.c.line([(x1, y), (x2, y)], dashed=dashed, end=end)
        self.c.label((x1+x2)/2, y-11, text, size, fill="white")

    def ret(self, a, b, y, text, la=0, lb=0, size=16):
        """Return message: dashed line, open arrowhead."""
        self.call(a, b, y, text, la, lb, dashed=True, end="arrow", size=size)

    def create(self, a, b, y, text, la=0):
        """Creation message: the new object's box sits at the arrow's end, as in the course notation."""
        width = self._head(self.xs[b], y-29, self.labels[b])
        self.c.line([(self.xs[b], y+29), (self.xs[b], self.bottom)], dashed=True, color="#8ca1ad")
        x1, x2 = self._edge(a, self.xs[b], la), self.xs[b]-width/2
        self.c.line([(x1, y), (x2, y)], dashed=True, end="arrow")
        self.c.label((x1+x2)/2, y-11, text, 16, fill="white")

    def self_call(self, index, y, text, level=0, drop=30, size=16):
        """Message to self: loops out to the right and lands on a nested activation bar."""
        x = self.xs[index]+self.HALF+level*self.HALF
        self.c.line([(x, y), (x+44, y), (x+44, y+drop), (x+self.HALF, y+drop)], end="solid")
        self.c.label(x+58, y+drop/2+5, text, size, anchor="start", fill="white")

    def frame(self, kind, guard, x1, x2, y1, y2):
        self.c.rect(x1, y1, x2-x1, y2-y1, "none", "#7f96a3", 0, self.c.nodes)
        self.c.nodes.append(f'<polygon points="{x1},{y1} {x1+66},{y1} {x1+66},{y1+18} {x1+56},{y1+28} {x1},{y1+28}" '
                            f'fill="{PALE}" stroke="#7f96a3" stroke-width="1.5"/>')
        self.c.text(x1+10, y1+20, kind, 16, BLUE, 700)
        self.c.label(x1+82, y1+21, guard, 16, color=INK, anchor="start", fill="white")

    def divider(self, x1, x2, y, guard):
        """Dashed separator between the operands of an alt frame."""
        self.c.line([(x1, y), (x2, y)], dashed=True, color="#7f96a3", layer=self.c.nodes, width=1.5)
        self.c.label(x1+18, y+22, guard, 16, color=INK, anchor="start", fill="white")
