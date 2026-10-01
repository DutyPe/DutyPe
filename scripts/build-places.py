"""
Builds functions/data/districts.json — every Indian district (current LGD boundaries, Survey of
India / Bharatmaps via github.com/ramSeraph/indian_admin_boundaries, CC0 with attribution to
datameet and the government source) simplified to ~100 m, for the server's place lookup.

    pip install py7zr shapely
    python scripts/build-places.py path/to/LGD_Districts.geojsonl

Download: https://github.com/ramSeraph/indian_admin_boundaries/releases/download/districts/LGD_Districts.geojsonl.7z
Re-run when districts are reorganised (the LGD release is updated upstream).

Output: {"v": <date>, "d": [[lgd, name, stateLgd, stateName, [minLng, minLat, maxLng, maxLat], rings], ...]}
where each ring is a flat, delta-encoded list of integer 1e-4 degree coordinates
[lng0, lat0, dLng1, dLat1, ...] (~11 m resolution). Polygons of one district are separate rings;
holes are not kept (a point in a hole would be claimed by its own district anyway).
"""
import datetime
import json
import sys
from collections import defaultdict
from pathlib import Path

from shapely.geometry import shape
from shapely.ops import unary_union

TOLERANCE_DEG = 0.001      # ~110 m
SCALE = 10_000             # 1e-4 degree integer grid

STATE_NAMES = {
    'ANDAMAN & NICOBAR': 'Andaman and Nicobar Islands',
    'DADRA,NAGAR HAVELI,DAMAN & DIU': 'Dadra and Nagar Haveli and Daman and Diu',
    'JAMMU & KASHMIR': 'Jammu and Kashmir',
}
DISTRICT_FIXES = {'Ntr': 'NTR', 'Y.S.R.': 'YSR Kadapa', 'Spsr Nellore': 'SPSR Nellore',
                  'Visakhapatanam': 'Visakhapatnam'}


def state_name(raw: str) -> str:
    return STATE_NAMES.get(raw) or ' '.join(w.capitalize() for w in raw.lower().split())


def encode_ring(coords) -> list:
    out, px, py = [], 0, 0
    for i, (x, y) in enumerate(coords):
        ix, iy = round(x * SCALE), round(y * SCALE)
        if i and ix == px and iy == py:
            continue
        out += [ix - px, iy - py] if i else [ix, iy]
        px, py = ix, iy
    return out


def main(src: str) -> None:
    parts = defaultdict(list)
    meta = {}
    for line in open(src, encoding='utf8'):
        f = json.loads(line)
        p = f['properties']
        lgd = int(p['dist_lgd'])
        parts[lgd].append(shape(f['geometry']))
        meta[lgd] = (DISTRICT_FIXES.get(p['dtname'].strip(), p['dtname'].strip()),
                     int(p['state_lgd']), state_name(p['stname'].strip()))

    rows = []
    for lgd, geoms in sorted(parts.items()):
        geom = unary_union(geoms).simplify(TOLERANCE_DEG, preserve_topology=True)
        polys = [geom] if geom.geom_type == 'Polygon' else list(geom.geoms)
        rings = [encode_ring(poly.exterior.coords) for poly in polys if poly.area > 1e-7]
        name, st, st_name = meta[lgd]
        minx, miny, maxx, maxy = geom.bounds
        rows.append([lgd, name, st, st_name,
                     [round(minx, 4), round(miny, 4), round(maxx, 4), round(maxy, 4)], rings])

    out = Path(__file__).resolve().parent.parent / 'functions' / 'data' / 'districts.json'
    out.parent.mkdir(exist_ok=True)
    out.write_text(json.dumps({'v': datetime.date.today().isoformat(), 'd': rows},
                              separators=(',', ':')), encoding='utf8')
    print(f'{len(rows)} districts, {out.stat().st_size / 1e6:.1f} MB -> {out}')


if __name__ == '__main__':
    main(sys.argv[1])
