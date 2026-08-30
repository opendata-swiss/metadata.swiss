#!/usr/bin/env python3
"""Render one view of an Archi model (.archimate) to SVG.

Usage: python3 render-view.py <model.archimate> <view name> <out.svg> [--flow-solid]

--flow-solid draws Flow solid and Triggering dashed, i.e. "solid = data flow,
dashed = orchestration". That inverts ArchiMate's own notation for those two, so
use it only on views whose legend states that convention.

Draws the view with the coordinates stored in the model, using the default
ArchiMate colours and line styles. Nested elements are drawn nested; the
implied composition/aggregation lines between a parent and its own children
are omitted, as Archi does.
"""
import sys
import xml.etree.ElementTree as ET

XSI = '{http://www.w3.org/2001/XMLSchema-instance}type'

FILL = {
    'business': '#ffffb5',
    'application': '#b5ffff',
    'technology': '#c9e7b7',
    'grouping': '#ffffff',
    'note': '#ffffff',
}
BUSINESS = ('Business',)
TECHNOLOGY = ('Node', 'Device', 'SystemSoftware', 'Technology', 'Artifact', 'Path',
              'CommunicationNetwork', 'Equipment', 'Facility', 'Material')


def layer(typ):
    if typ == 'Note':
        return 'note'
    if typ == 'Grouping':
        return 'grouping'
    if typ.startswith('Business') or typ.startswith('Product') or typ.startswith('Contract'):
        return 'business'
    if any(typ.startswith(t) for t in TECHNOLOGY):
        return 'technology'
    return 'application'


def darker(hexcolor, factor=0.45):
    r, g, b = (int(hexcolor[i:i + 2], 16) for i in (1, 3, 5))
    return '#%02x%02x%02x' % (int(r * factor), int(g * factor), int(b * factor))


def esc(t):
    return (t or '').replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')


class Box:
    def __init__(self, did, element, name, typ, x, y, w, h, parent):
        self.did, self.element, self.name, self.typ = did, element, name, typ
        self.x, self.y, self.w, self.h = x, y, w, h
        self.parent = parent

    def ancestors(self):
        p = self.parent
        while p:
            yield p
            p = p.parent

    @property
    def cx(self):
        return self.x + self.w / 2

    @property
    def cy(self):
        return self.y + self.h / 2


def clip(box, tx, ty):
    """Point where the line from the box centre towards (tx, ty) leaves the box."""
    dx, dy = tx - box.cx, ty - box.cy
    if dx == 0 and dy == 0:
        return box.cx, box.cy
    sx = (box.w / 2) / abs(dx) if dx else float('inf')
    sy = (box.h / 2) / abs(dy) if dy else float('inf')
    s = min(sx, sy)
    return box.cx + dx * s, box.cy + dy * s


def shorten(tip, prev, amount):
    """Pull `tip` back towards `prev`, so the line ends behind its arrow head."""
    dx, dy = tip[0] - prev[0], tip[1] - prev[1]
    length = (dx * dx + dy * dy) ** 0.5 or 1
    return (tip[0] - dx / length * amount, tip[1] - dy / length * amount)


def wrap(name, width):
    """Greedy word wrap to the box width (~6.2 px per character at 12 px)."""
    limit = max(1, int((width - 12) / 6.2))
    lines, cur = [], ''
    for word in name.split():
        cand = f'{cur} {word}'.strip()
        if len(cand) <= limit or not cur:
            cur = cand
        else:
            lines.append(cur)
            cur = word
    if cur:
        lines.append(cur)
    return lines


def main():
    args = [a for a in sys.argv[1:] if not a.startswith('--')]
    flags = {a for a in sys.argv[1:] if a.startswith('--')}
    model_path, view_name, out_path = args[0], args[1], args[2]
    root = ET.parse(model_path).getroot()

    elements, rels = {}, {}
    for e in root.iter('element'):
        typ = (e.get(XSI) or '').split(':')[-1]
        if typ.endswith('Relationship'):
            rels[e.get('id')] = (typ, e.get('name'))
        elif typ != 'ArchimateDiagramModel':
            elements[e.get('id')] = (typ, e.get('name'))

    views = [e for e in root.iter('element')
             if (e.get(XSI) or '').endswith('ArchimateDiagramModel') and e.get('name') == view_name]
    if not views:
        sys.exit(f'view not found: {view_name}')
    view = views[0]

    boxes, conns = {}, []

    def walk(node, ox, oy, parent):
        for child in node.findall('child'):
            b = child.find('bounds')
            x = ox + int(b.get('x') or 0)
            y = oy + int(b.get('y') or 0)
            w = int(b.get('width') or 120)
            h = int(b.get('height') or 55)
            if (child.get(XSI) or '').endswith('Note'):
                typ, name = 'Note', (child.findtext('content') or '')
            else:
                typ, name = elements.get(child.get('archimateElement'), ('Grouping', '?'))
            box = Box(child.get('id'), child.get('archimateElement'), name, typ, x, y, w, h, parent)
            boxes[box.did] = box
            for sc in child.findall('sourceConnection'):
                conns.append((sc.get('source'), sc.get('target'), sc.get('archimateRelationship')))
            walk(child, x, y, box)

    walk(view, 0, 0, None)

    maxx = max(b.x + b.w for b in boxes.values()) + 80
    maxy = max(b.y + b.h for b in boxes.values()) + 60
    out = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{maxx}" height="{maxy}" '
           f'viewBox="0 0 {maxx} {maxy}" font-family="Segoe UI, Arial, sans-serif">',
           '<rect width="100%" height="100%" fill="#ffffff"/>']

    # --- boxes, outermost first so nested ones stay on top
    for box in sorted(boxes.values(), key=lambda b: len(list(b.ancestors()))):
        lay = layer(box.typ)
        fill = FILL[lay]
        stroke = darker(fill) if lay not in ('grouping', 'note') else '#8a8a8a'
        if lay == 'note':
            out.append(f'<rect x="{box.x}" y="{box.y}" width="{box.w}" height="{box.h}" fill="#ffffff" '
                       f'stroke="#999999" stroke-width="1" stroke-dasharray="3 3"/>')
            ty = box.y + 22
            for para in box.name.split('\n'):
                for line in (wrap(para, box.w) or ['']):
                    out.append(f'<text x="{box.x + 12}" y="{ty}" font-size="12" fill="#333">{esc(line)}</text>')
                    ty += 17
            continue
        if lay == 'grouping':
            out.append(f'<rect x="{box.x}" y="{box.y}" width="{box.w}" height="{box.h}" fill="#fbfbfb" '
                       f'stroke="{stroke}" stroke-width="1" stroke-dasharray="6 4"/>')
            out.append(f'<text x="{box.x + 8}" y="{box.y + 18}" font-size="12" fill="#333">{esc(box.name)}</text>')
            continue

        out.append(f'<rect x="{box.x}" y="{box.y}" width="{box.w}" height="{box.h}" rx="2" fill="{fill}" '
                   f'stroke="{stroke}" stroke-width="1.2"/>')
        # type icon, top right
        ix, iy = box.x + box.w - 20, box.y + 7
        if box.typ == 'ApplicationComponent':
            out.append(f'<g stroke="{stroke}" fill="none" stroke-width="1.2">'
                       f'<rect x="{ix + 3}" y="{iy}" width="11" height="10"/>'
                       f'<rect x="{ix}" y="{iy + 2}" width="5" height="2.5" fill="{fill}"/>'
                       f'<rect x="{ix}" y="{iy + 6}" width="5" height="2.5" fill="{fill}"/></g>')
        elif box.typ.startswith('Business'):
            out.append(f'<path d="M{ix} {iy + 11} L{ix + 5} {iy} L{ix + 13} {iy} L{ix + 8} {iy + 11} z" '
                       f'fill="none" stroke="{stroke}" stroke-width="1.2"/>')
        else:
            out.append(f'<rect x="{ix}" y="{iy}" width="13" height="10" rx="5" '
                       f'fill="none" stroke="{stroke}" stroke-width="1.2"/>')

        nested = any(b.parent is box for b in boxes.values())
        lines = wrap(box.name, box.w)
        if nested:
            base = box.y + 20
            anchor, tx = 'start', box.x + 8
        else:
            base = box.cy - (len(lines) - 1) * 7 + 5
            anchor, tx = 'middle', box.cx
        for i, line in enumerate(lines):
            out.append(f'<text x="{tx}" y="{base + i * 14}" font-size="12" fill="#1a1a1a" '
                       f'text-anchor="{anchor}">{esc(line)}</text>')

    # --- connections
    # Arrow heads are drawn as explicit geometry rather than SVG markers, because
    # several rasterisers (ImageMagick, LibreOffice) silently drop markers.
    STYLE = {
        'CompositionRelationship': ('solid', None, 'diamond-filled'),
        'AggregationRelationship': ('solid', None, 'diamond-open'),
        'RealizationRelationship': ('4 4', 'triangle-hollow', None),
        'ServingRelationship': ('solid', 'arrow-open', None),
        'TriggeringRelationship': ('solid', 'arrow-filled', None),
        'FlowRelationship': ('4 4', 'arrow-filled', None),
        'AssociationRelationship': ('solid', None, None),
    }
    if '--flow-solid' in flags:
        # Draw the Mermaid legend's convention -- solid = data flow, dashed =
        # orchestration -- which is the inverse of ArchiMate's own notation for
        # these two. Opt-in per view, so views that use Triggering for coarse
        # data flow (Overview) keep the standard styling.
        STYLE['FlowRelationship'] = ('solid', 'arrow-filled', None)
        STYLE['TriggeringRelationship'] = ('4 4', 'arrow-filled', None)
    LINE = '#5a5a5a'

    def head(kind, tip, other):
        """Arrow head / diamond at `tip`, pointing away from `other`.

        Returns (svg, trim) where trim is how far the line must stop short.
        """
        dx, dy = tip[0] - other[0], tip[1] - other[1]
        length = (dx * dx + dy * dy) ** 0.5 or 1
        ux, uy = dx / length, dy / length
        px, py = -uy, ux          # perpendicular

        def pt(back, side):
            return (tip[0] - ux * back + px * side, tip[1] - uy * back + py * side)

        if kind == 'arrow-open':
            a, b_ = pt(9, 4.5), pt(9, -4.5)
            return (f'<path d="M{a[0]:.1f},{a[1]:.1f} L{tip[0]:.1f},{tip[1]:.1f} L{b_[0]:.1f},{b_[1]:.1f}" '
                    f'fill="none" stroke="{LINE}" stroke-width="1.2"/>', 0)
        if kind == 'arrow-filled':
            a, b_ = pt(10, 4), pt(10, -4)
            return (f'<path d="M{tip[0]:.1f},{tip[1]:.1f} L{a[0]:.1f},{a[1]:.1f} L{b_[0]:.1f},{b_[1]:.1f} z" '
                    f'fill="{LINE}"/>', 9)
        if kind == 'triangle-hollow':
            a, b_ = pt(12, 6), pt(12, -6)
            return (f'<path d="M{tip[0]:.1f},{tip[1]:.1f} L{a[0]:.1f},{a[1]:.1f} L{b_[0]:.1f},{b_[1]:.1f} z" '
                    f'fill="#ffffff" stroke="{LINE}" stroke-width="1.2"/>', 11)
        # diamonds sit at the source end
        a, b_, c = pt(7, 4.5), pt(14, 0), pt(7, -4.5)
        fill = LINE if kind == 'diamond-filled' else '#ffffff'
        return (f'<path d="M{tip[0]:.1f},{tip[1]:.1f} L{a[0]:.1f},{a[1]:.1f} L{b_[0]:.1f},{b_[1]:.1f} '
                f'L{c[0]:.1f},{c[1]:.1f} z" fill="{fill}" stroke="{LINE}" stroke-width="1.2"/>', 13)

    # Orthogonal routing, as Archi draws it: a connection leaves a box at a right
    # angle and only ever runs horizontally or vertically. Where the two boxes
    # overlap on the perpendicular axis the connection is a single straight
    # segment; otherwise it takes one step sideways halfway between them.
    SPREAD = 14        # distance between connections leaving the same box side
    MIN_OVERLAP = 26   # overlap needed before a straight segment is used

    routes = []
    for src, tgt, rid in conns:
        a, b = boxes.get(src), boxes.get(tgt)
        if not a or not b or a in b.ancestors() or b in a.ancestors():
            continue
        dx, dy = b.cx - a.cx, b.cy - a.cy
        # Leave sideways whenever the boxes stand next to each other, and only
        # leave top/bottom when they are stacked. Comparing the centres alone
        # would send connections straight through the boxes in between.
        apart_x = b.x > a.x + a.w or a.x > b.x + b.w
        apart_y = b.y > a.y + a.h or a.y > b.y + b.h
        if apart_x and not apart_y:
            axis = 'h'
        elif apart_y and not apart_x:
            axis = 'v'
        elif apart_x and apart_y:
            axis = 'h'
        else:
            axis = 'h' if abs(dx) >= abs(dy) else 'v'
        if axis == 'h':
            side_a, side_b = ('right', 'left') if dx > 0 else ('left', 'right')
        else:
            side_a, side_b = ('bottom', 'top') if dy > 0 else ('top', 'bottom')
        routes.append({'a': a, 'b': b, 'rid': rid, 'axis': axis,
                       'sa': side_a, 'sb': side_b})

    # Spread the connections that leave, and that arrive at, the same box side.
    # Big boxes spread their connections over the whole edge, small ones keep the
    # fixed distance, so a container and one of its children never receive two
    # connections at the same height.
    for key, side_key, field in (('a', 'sa', 'off'), ('b', 'sb', 'off_in')):
        slots = {}
        for r in routes:
            slots.setdefault((r[key].did, r[side_key]), []).append(r)
        for (did, _side), group in slots.items():
            box = boxes[did]
            span = box.h if group[0]['axis'] == 'h' else box.w
            step = max(SPREAD, span / (len(group) + 1))
            for i, r in enumerate(group):
                r[field] = (i - (len(group) - 1) / 2) * step

    def edge(box, side, along):
        if side == 'right':
            return (box.x + box.w, along)
        if side == 'left':
            return (box.x, along)
        if side == 'bottom':
            return (along, box.y + box.h)
        return (along, box.y)

    def blocked(pts, a, b):
        """True if any segment crosses a box that is not an endpoint or its container."""
        skip = {a.did, b.did}
        skip |= {x.did for x in a.ancestors()} | {x.did for x in b.ancestors()}
        for box in boxes.values():
            if box.did in skip:
                continue
            for (x1, y1), (x2, y2) in zip(pts, pts[1:]):
                lox, hix = sorted((x1, x2))
                loy, hiy = sorted((y1, y2))
                if (lox < box.x + box.w - 2 and hix > box.x + 2
                        and loy < box.y + box.h - 2 and hiy > box.y + 2):
                    return True
        return False

    def route(r, off, off_in):
        a, b = r['a'], r['b']
        if r['axis'] == 'h':
            lo, hi = max(a.y, b.y), min(a.y + a.h, b.y + b.h)
            if hi - lo >= MIN_OVERLAP:
                y = min(max((lo + hi) / 2 + off + off_in, lo + 8), hi - 8)
                return [edge(a, r['sa'], y), edge(b, r['sb'], y)]
            p = edge(a, r['sa'], a.cy + off)
            q = edge(b, r['sb'], b.cy + off_in)
            mid = (p[0] + q[0]) / 2
            return [p, (mid, p[1]), (mid, q[1]), q]
        lo, hi = max(a.x, b.x), min(a.x + a.w, b.x + b.w)
        if hi - lo >= MIN_OVERLAP:
            x = min(max((lo + hi) / 2 + off + off_in, lo + 8), hi - 8)
            return [edge(a, r['sa'], x), edge(b, r['sb'], x)]
        p = edge(a, r['sa'], a.cx + off)
        q = edge(b, r['sb'], b.cx + off_in)
        mid = (p[1] + q[1]) / 2
        return [p, (p[0], mid), (q[0], mid), q]

    def detour(a, b, side, off):
        """Route out of both boxes on `side` and along a free lane beside them."""
        if side == 'right':
            lane = max(a.x + a.w, b.x + b.w) + 24 + abs(off)
        else:
            lane = min(a.x, b.x) - 24 - abs(off)
        p = edge(a, side, a.cy + off)
        q = edge(b, side, b.cy)
        return [p, (lane, p[1]), (lane, q[1]), q]

    # First pass: route everything, then bundle. Connections whose middle segment
    # runs through the same corridor are snapped onto one shared trunk, so they
    # travel together and branch off only at their ends.
    TRUNK_RANGE = 70
    for r in routes:
        pts = route(r, r.get('off', 0), r.get('off_in', 0))
        if blocked(pts, r['a'], r['b']):
            for side in ('right', 'left'):
                alt = detour(r['a'], r['b'], side, r.get('off', 0))
                if not blocked(alt, r['a'], r['b']):
                    pts = alt
                    break
        r['pts'] = pts

    def bend_axis(pts):
        """0 when the middle segment runs vertically, 1 when it runs horizontally.

        Read off the geometry, not the declared axis: a detour turns a vertical
        connection into a horizontal middle segment and vice versa.
        """
        return 0 if abs(pts[0][1] - pts[1][1]) < 0.6 else 1

    for coord in (0, 1):
        bends = [r for r in routes if len(r['pts']) == 4 and bend_axis(r['pts']) == coord]
        clusters = []
        for r in sorted(bends, key=lambda r: r['pts'][1][coord]):
            value = r['pts'][1][coord]
            if clusters and value - clusters[-1][0][-1] <= TRUNK_RANGE:
                clusters[-1][0].append(value)
                clusters[-1][1].append(r)
            else:
                clusters.append(([value], [r]))
        for values, members in clusters:
            if len(members) < 2:
                continue
            trunk = sum(values) / len(values)
            for r in members:
                pts = r['pts']
                if coord == 0:
                    r['pts'] = [pts[0], (trunk, pts[1][1]), (trunk, pts[2][1]), pts[3]]
                else:
                    r['pts'] = [pts[0], (pts[1][0], trunk), (pts[2][0], trunk), pts[3]]

    for r in routes:
        a, b, pts = r['a'], r['b'], r['pts']
        typ, label = rels.get(r['rid'], ('AssociationRelationship', None))
        dash, end_kind, start_kind = STYLE.get(typ, ('solid', None, None))

        heads = []
        if end_kind:
            svg, trim = head(end_kind, pts[-1], pts[-2])
            heads.append(svg)
            pts[-1] = shorten(pts[-1], pts[-2], trim)
        if start_kind:
            svg, trim = head(start_kind, pts[0], pts[1])
            heads.append(svg)
            pts[0] = shorten(pts[0], pts[1], trim)

        attrs = f'stroke="{LINE}" stroke-width="1.2" fill="none"'
        if dash != 'solid':
            attrs += f' stroke-dasharray="{dash}"'
        path = ' '.join(f'{x:.1f},{y:.1f}' for x, y in pts)
        out.append(f'<polyline points="{path}" {attrs}/>')
        out.extend(heads)
        if label:
            (x1, y1), (x2, y2) = pts[len(pts) // 2 - 1], pts[len(pts) // 2]
            mx, my = (x1 + x2) / 2, (y1 + y2) / 2
            w = len(label) * 5.5
            out.append(f'<rect x="{mx - w / 2:.1f}" y="{my - 14:.1f}" width="{w:.1f}" height="13" fill="#ffffff"/>')
            out.append(f'<text x="{mx:.1f}" y="{my - 4:.1f}" font-size="10" '
                       f'fill="{LINE}" text-anchor="middle">{esc(label)}</text>')

    out.append('</svg>')
    open(out_path, 'w', encoding='utf-8').write('\n'.join(out))
    print(out_path)


if __name__ == '__main__':
    main()
