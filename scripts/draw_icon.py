#!/usr/bin/env python3
"""Generate the 64x64 pixel-art Tapetum icon (nearest-neighbor upscaled to 512x512).

Design:
- Family beveled rounded badge: abyss outline, obsidian-slate metal bevel lit from the
  top-left, corner rivets with chamfer engravings, teal inner trim with warm gold glints
- Deep night radial field with a soft volumetric light shaft from the top-left
- Hero: a domed cat's-eye "tapetum lucidum" lens in a dark teal metal almond socket,
  emerald -> ice-cyan faceted iris, and a glowing molten-gold slit pupil
- Causality: an incident ray enters from a top-left light source, hits the tapetum
  pupil, and is reflected out to the bottom-right as three sharp parallel prism beams
  (gold core flanked by ice-cyan), with family 4-point pixel sparkles

Pure stdlib, fully deterministic.
"""
from pathlib import Path
import math
import struct
import zlib

ROOT = Path(__file__).resolve().parents[1]
SIZE = 64
SCALE = 8
OUTPUTS = [
    ROOT / "common/src/main/resources/icon.png",
    ROOT / "common/src/main/resources/assets/tapetum/icon.png",
    ROOT / "docs/icon.png",
]


def hex_rgba(h: str, a: int = 255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def blend(dst, src):
    sr, sg, sb, sa = src
    if sa == 0:
        return dst
    if sa == 255:
        return src
    dr, dg, db, da = dst
    inv = 255 - sa
    out_a = sa + (da * inv) // 255
    if out_a == 0:
        return (0, 0, 0, 0)
    r = (sr * sa + dr * da * inv // 255) // out_a
    g = (sg * sa + dg * da * inv // 255) // out_a
    b = (sb * sa + db * da * inv // 255) // out_a
    return (r, g, b, out_a)


def lerp_rgb(c0, c1, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(round(c0[i] + (c1[i] - c0[i]) * t)) for i in range(3)) + (255,)


def write_png(path: Path, width: int, height: int, rgba_bytes: bytes):
    def chunk(tag: bytes, data: bytes) -> bytes:
        return (
            struct.pack("!I", len(data))
            + tag
            + data
            + struct.pack("!I", zlib.crc32(tag + data) & 0xFFFFFFFF)
        )

    raw = bytearray()
    stride = width * 4
    for y in range(height):
        raw.append(0)
        raw.extend(rgba_bytes[y * stride : (y + 1) * stride])

    png = bytearray(b"\x89PNG\r\n\x1a\n")
    png.extend(chunk(b"IHDR", struct.pack("!IIBBBBB", width, height, 8, 6, 0, 0, 0)))
    png.extend(chunk(b"IDAT", zlib.compress(bytes(raw), 9)))
    png.extend(chunk(b"IEND", b""))
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(bytes(png))


def inside_rounded_rect(x: int, y: int, x0: int, y0: int, x1: int, y1: int, r: int) -> bool:
    if x < x0 or x > x1 or y < y0 or y > y1:
        return False
    cx = x0 + r if x < x0 + r else (x1 - r if x > x1 - r else x)
    cy = y0 + r if y < y0 + r else (y1 - r if y > y1 - r else y)
    dx = x - cx
    dy = y - cy
    return dx * dx + dy * dy <= r * r


N4 = ((1, 0), (-1, 0), (0, 1), (0, -1))


def dilate(mask, r):
    offs = [(dx, dy) for dy in range(-r, r + 1) for dx in range(-r, r + 1) if dx * dx + dy * dy <= r * r + 1]
    out = set()
    for x, y in mask:
        for dx, dy in offs:
            out.add((x + dx, y + dy))
    return out


def ring(mask):
    """4-connected 1px outline just outside mask."""
    out = set()
    for x, y in mask:
        for dx, dy in N4:
            p = (x + dx, y + dy)
            if p not in mask:
                out.add(p)
    return out


# ---------------------------------------------------------------- palette
ABYSS = hex_rgba("#0B1018")
BEV_L0 = hex_rgba("#6482A6")
BEV_L1 = hex_rgba("#536F92")
BEV_L2 = hex_rgba("#435C7A")
BEV_D0 = hex_rgba("#1D2A3B")
BEV_D1 = hex_rgba("#18232F")
BEV_D2 = hex_rgba("#131C28")
BEV_MITER = hex_rgba("#2F4058")
TRIM = hex_rgba("#2A4B5C")
TRIM_DARK = hex_rgba("#1A3140")
TRIM_GOLD = hex_rgba("#D9A84E")
TRIM_GOLD_HI = hex_rgba("#FFE29A")
RIVET_HI = hex_rgba("#9DB6D2")
RIVET = hex_rgba("#5E7B9C")
RIVET_LO = hex_rgba("#22303F")

BG0 = hex_rgba("#090E17")
BG1 = hex_rgba("#101D2C")
BG2 = hex_rgba("#162E3F")

SELOUT = hex_rgba("#091118")

RIM_SPEC = hex_rgba("#CFF5F0")
RIM_HI = hex_rgba("#A2D0DA")
RIM_MH = hex_rgba("#5F98A8")
RIM_MID = hex_rgba("#386A7A")
RIM_LO = hex_rgba("#21404E")
RIM_DK = hex_rgba("#10222C")
LIP_GOLD = hex_rgba("#E0AE52")
LIP_BRONZE = hex_rgba("#93652E")

SCLERA = hex_rgba("#0A3236")
SCLERA_LO = hex_rgba("#072428")
IRIS_RIM = hex_rgba("#08302F")
IRIS_0 = hex_rgba("#0D4F52")
IRIS_1 = hex_rgba("#188C86")
IRIS_2 = hex_rgba("#3AD4BE")
IRIS_3 = hex_rgba("#8CFCE8")
WHITE = hex_rgba("#FFFFFF")

GOLD_W = hex_rgba("#FFFDF0")
GOLD_HI = hex_rgba("#FFE066")
GOLD_MID = hex_rgba("#F0A024")
GOLD_DK = hex_rgba("#9E5410")
GOLD_OUT = hex_rgba("#2A1606")

ICE_W = hex_rgba("#E6FFFA")
ICE_HI = hex_rgba("#7AF5E0")
ICE_MID = hex_rgba("#2EC2B0")
ICE_DK = hex_rgba("#16756F")


def draw_icon():
    canvas = [[(0, 0, 0, 0) for _ in range(SIZE)] for _ in range(SIZE)]

    def put(x: int, y: int, color):
        if 0 <= x < SIZE and 0 <= y < SIZE:
            canvas[y][x] = blend(canvas[y][x], color)

    def inner(x, y):
        return inside_rounded_rect(x, y, 7, 7, 56, 56, 6)

    def put_in(x, y, color):
        if inner(x, y):
            put(x, y, color)

    # ------------------------------------------------------------ 1. badge frame
    for y in range(SIZE):
        for x in range(SIZE):
            if not inside_rounded_rect(x, y, 2, 2, 61, 61, 10):
                continue
            if not inside_rounded_rect(x, y, 3, 3, 60, 60, 9):
                put(x, y, ABYSS)
            elif not inside_rounded_rect(x, y, 6, 6, 57, 57, 7):
                # 3px chamfered metal bevel, mitred at 45 degrees on the TR / BL corners
                if not inside_rounded_rect(x, y, 4, 4, 59, 59, 8):
                    depth = 0
                elif not inside_rounded_rect(x, y, 5, 5, 58, 58, 7):
                    depth = 1
                else:
                    depth = 2
                dt, dl, db, dr = y - 3, x - 3, 60 - y, 60 - x
                m = min(dt, dl, db, dr)
                lit = m in (dt, dl) and not (m == db or m == dr)
                miter = (m == dt and m == dr) or (m == dl and m == db)
                if miter:
                    c = BEV_MITER
                elif lit:
                    c = (BEV_L0, BEV_L1, BEV_L2)[depth]
                    # subtle hammered-metal ticks on the lit faces
                    if depth == 1 and (x * 5 + y * 3) % 11 == 0:
                        c = BEV_L2
                else:
                    c = (BEV_D0, BEV_D1, BEV_D2)[depth]
                    if depth == 1 and (x * 3 + y * 5) % 13 == 0:
                        c = BEV_D2
                put(x, y, c)
            elif not inner(x, y):
                put(x, y, TRIM if x + y < 63 else TRIM_DARK)
            else:
                # deep night radial field, brightest behind the lens
                dx = (x - 30) / 30.0
                dy = (y - 31) / 30.0
                d = math.sqrt(dx * dx + dy * dy)
                if d < 0.55:
                    c = lerp_rgb(BG2, BG1, d / 0.55)
                else:
                    c = lerp_rgb(BG1, BG0, (d - 0.55) / 0.5)
                put(x, y, c)

    # volumetric light shaft from the top-left (diagonal band along x - y ~ 0)
    for y in range(SIZE):
        for x in range(SIZE):
            if not inner(x, y):
                continue
            off = abs((x - y) - 1)
            along = (x + y) / 2.0
            fade = max(0.0, 1.0 - along / 46.0)
            if off <= 2:
                put(x, y, hex_rgba("#5FA8C8", int(34 * fade)))
            elif off <= 5:
                put(x, y, hex_rgba("#5FA8C8", int(18 * fade)))
            elif off <= 8 and (x + y) % 2 == 0:
                put(x, y, hex_rgba("#5FA8C8", int(10 * fade)))

    # warm gold glints on the inner trim (top-left light catch)
    for gx, gy, gc in [
        (12, 7, TRIM_GOLD), (13, 7, TRIM_GOLD_HI), (14, 7, TRIM_GOLD), (18, 7, TRIM_GOLD),
        (7, 12, TRIM_GOLD), (7, 13, TRIM_GOLD_HI), (7, 14, TRIM_GOLD), (7, 18, TRIM_GOLD),
        (9, 8, TRIM_GOLD), (8, 9, TRIM_GOLD),
    ]:
        put(gx, gy, gc)

    # corner rivets + chamfer engraving lines
    for rx, ry in [(6, 6), (56, 6), (6, 56), (56, 56)]:
        lit_corner = rx < 32 and ry < 32
        dark_corner = rx > 32 and ry > 32
        put(rx, ry, RIVET if not dark_corner else BEV_L2)
        put(rx + 1, ry, RIVET_LO)
        put(rx, ry + 1, RIVET_LO)
        put(rx + 1, ry + 1, RIVET_LO)
        put(rx, ry, RIVET_HI if lit_corner else (RIVET if not dark_corner else BEV_L2))
    for (ax, ay, sx, sy, col) in [
        (4, 9, 1, -1, BEV_L2), (55, 4, 1, 1, BEV_D2), (4, 54, 1, 1, BEV_D2), (54, 59, 1, -1, BEV_D2),
    ]:
        for i in range(3):
            px, py = ax + i * sx, ay + i * sy
            if inside_rounded_rect(px, py, 3, 3, 60, 60, 9) and not inside_rounded_rect(px, py, 6, 6, 57, 57, 7):
                if (px, py) not in [(6, 6), (56, 6), (6, 56), (56, 56)]:
                    put(px, py, col)

    # ------------------------------------------------------------ 2. geometry
    ECX, ECY = 27.5, 30.0  # eye centre (pixel-centre coordinates)
    EX0, EX1 = 10.0, 47.0
    HT, HB = 13.6, 11.4

    def lids(xc):
        u = (xc - EX0) / (EX1 - EX0)
        if u <= 0.0 or u >= 1.0:
            return None
        tilt = -2.2 * (u - 0.5)  # outer (right) corner lifted: cat-eye flick
        s = math.sin(math.pi * u)
        return ECY + tilt - HT * s ** 0.78, ECY + tilt + HB * s ** 0.92

    almond = set()
    for y in range(SIZE):
        for x in range(SIZE):
            lv = lids(x + 0.5)
            if lv and lv[0] <= y + 0.5 <= lv[1]:
                almond.add((x, y))
    socket = dilate(almond, 3)
    rim = socket - almond
    rim_inner = ring(almond) & rim
    eye_out = ring(socket)
    rim_outer = ring(eye_out) & rim
    rim_crest = rim - rim_inner - rim_outer

    # beams: parallel prism shards along a clean 2:1 pixel slope, reflected to the bottom-right
    DX, DY = 2 / math.sqrt(5), 1 / math.sqrt(5)
    NX, NY = 1 / math.sqrt(5), -2 / math.sqrt(5)  # normal pointing up-right (toward the light)
    PX0, PY0 = 28.5, 30.5  # pupil reflection point
    beams = []
    for off, tip_x, hw, kind in [(5.2, 53.5, 1.3, "ice"), (0.0, 55.5, 1.55, "gold"), (-5.2, 50.0, 1.3, "ice")]:
        bx, by = PX0 + NX * off, PY0 + NY * off
        t = 0.0
        while (int(math.floor(bx + DX * t)), int(math.floor(by + DY * t))) in socket or t < 4:
            t += 0.25
        ox, oy = bx + DX * (t - 1.2), by + DY * (t - 1.2)
        beams.append((ox, oy, (tip_x - ox) / DX, hw, kind))

    def beam_hit(px, py):
        best = None
        for idx, (ox, oy, ln, hw, kind) in enumerate(beams):
            rx, ry = px - ox, py - oy
            t = rx * DX + ry * DY
            s = rx * NX + ry * NY
            if t < 0 or t > ln:
                continue
            w = hw * min(1.0, 0.45 + t / 5.0, (ln - t) / 4.2)
            if abs(s) <= w:
                best = (idx, t, s / max(hw, 0.01), kind, ln)
        return best

    beam_px = {}
    for y in range(SIZE):
        for x in range(SIZE):
            if (x, y) in almond:
                continue
            h = beam_hit(x + 0.5, y + 0.5)
            if h and inner(x, y):
                beam_px[(x, y)] = h
    beam_mask = set(beam_px)

    # ------------------------------------------------------------ 3. glows + shadows
    # cast shadow of the lens socket (light from top-left -> shadow bottom-right)
    for x, y in socket | eye_out:
        put_in(x + 1, y + 2, hex_rgba("#03060B", 110))
    # soft teal halo hugging the socket
    for x, y in dilate(socket, 3) - socket:
        put_in(x, y, hex_rgba("#2FC7B4", 16))
    # beam bloom on the night field
    for x, y in dilate(beam_mask, 2) - beam_mask:
        if (x, y) in socket:
            continue
        h = beam_hit(x + 0.5, y + 0.5) or None
        kind = None
        best = 99.0
        for ox, oy, ln, hw, k in beams:
            rx, ry = x + 0.5 - ox, y + 0.5 - oy
            t = rx * DX + ry * DY
            if -1 <= t <= ln + 1:
                dd = abs(rx * NX + ry * NY)
                if dd < best:
                    best, kind = dd, k
        if kind:
            put_in(x, y, hex_rgba("#FFC24A", 34) if kind == "gold" else hex_rgba("#4FF0D8", 28))

    # incident light source + ray (top-left -> pupil), drawn under the lens
    SRC = (12, 11)
    ray = [(SRC[0] + i, SRC[1] + i) for i in range(1, 22)]
    for i, (x, y) in enumerate(ray):
        a = min(255, 120 + i * 16)
        if (x, y) in almond:
            continue
        if (x, y) in rim:
            put(x, y, hex_rgba("#FFF4C8", min(255, a + 20)))
            continue
        put_in(x, y, GOLD_W if i > 2 else hex_rgba("#FFF6D6", a))
        put_in(x + 1, y, hex_rgba("#FFD870", 70 + i * 4))
        put_in(x, y + 1, hex_rgba("#FFD870", 70 + i * 4))

    # ------------------------------------------------------------ 4. cat's-eye lens
    for x, y in eye_out:
        put(x, y, SELOUT)

    # metal almond socket: raised ring, outer slope lit top-left, inner slope inverted
    for x, y in rim:
        nx = (x + 0.5 - ECX) / 20.0
        ny = (y + 0.5 - ECY) / 12.0
        ln = math.hypot(nx, ny) or 1.0
        lam = -(nx / ln) * 0.62 - (ny / ln) * 0.78
        if (x, y) in rim_inner:
            lam = -lam * 0.8
            upper = (y + 0.5) < ECY
            if upper and lam < 0.2:
                c = RIM_DK
            elif not upper and lam > 0.3 and x < 40:
                c = LIP_GOLD if lam > 0.55 else LIP_BRONZE
            elif lam > 0.45:
                c = RIM_MH
            elif lam > 0.0:
                c = RIM_MID
            else:
                c = RIM_LO
        elif (x, y) in rim_crest:
            if lam > 0.55:
                c = RIM_SPEC if lam > 0.9 else RIM_HI
            elif lam > 0.0:
                c = RIM_MH
            elif lam > -0.6:
                c = RIM_MID
            else:
                c = RIM_LO
        else:
            if lam > 0.72:
                c = RIM_HI
            elif lam > 0.25:
                c = RIM_MH
            elif lam > -0.3:
                c = RIM_MID
            elif lam > -0.75:
                c = RIM_LO
            else:
                c = RIM_DK
        put(x, y, c)
    # metal specular glints on the upper-left lid
    for sx, sy in [(14, 20), (15, 19), (19, 17)]:
        if (sx, sy) in rim:
            put(sx, sy, RIM_SPEC)

    ICX, ICY, IR = 27.5, 30.5, 12.6
    LX, LY, LZ = -0.62, -0.72, 0.36
    lnorm = math.sqrt(LX * LX + LY * LY + LZ * LZ)
    LX, LY, LZ = LX / lnorm, LY / lnorm, LZ / lnorm
    for x, y in almond:
        px, py = x + 0.5, y + 0.5
        dx, dy = (px - ICX) / IR, (py - ICY) / IR
        r = math.hypot(dx, dy)
        lv = lids(px)
        if r > 1.0:
            # dark glassy sclera in the almond corners
            c = SCLERA if py < ECY + 1 else SCLERA_LO
            put(x, y, c)
            continue
        nz = math.sqrt(max(0.0, 1.0 - r * r))
        lam = dx * LX + dy * LY + nz * LZ
        ang = math.atan2(dy, dx)
        if r > 0.3:
            fiber = int((ang + math.pi) / (2 * math.pi) * 30) % 2
            lam += 0.07 if fiber else -0.05
        lam += 0.10 * max(0.0, 0.5 - r)  # tapetum inner glow
        if lam < 0.12:
            c = IRIS_0
        elif lam < 0.5:
            c = IRIS_1
        elif lam < 0.84:
            c = IRIS_2
        else:
            c = IRIS_3
        if r > 0.9:
            c = IRIS_RIM if lam < 0.75 else IRIS_0
        # upper lid casts a short shadow over the top of the lens
        if lv and py - lv[0] < 1.6 and py < ECY:
            c = IRIS_RIM if c in (IRIS_0, IRIS_RIM, IRIS_1) else IRIS_1
        put(x, y, c)
        # specular highlight arc (top-left) and bounce light (bottom-right)
        deg = (math.degrees(ang) + 360.0) % 360.0
        if 0.6 <= r <= 0.8 and 196 <= deg <= 252:
            put(x, y, IRIS_3)
        if 0.76 <= r <= 0.9 and 28 <= deg <= 70:
            put(x, y, IRIS_2)

    # crisp white reflection points
    for wx, wy, wc in [(21, 25, WHITE), (22, 25, WHITE), (21, 26, WHITE), (23, 24, IRIS_3), (20, 27, IRIS_3), (33, 36, WHITE)]:
        put(wx, wy, wc)

    # molten tapetum slit pupil
    PCX = 28
    half = {0: 2, 1: 2, 2: 2, 3: 1, 4: 1, 5: 1, 6: 1, 7: 1, 8: 0, 9: 0, 10: 0}
    pupil = {}
    for dy in range(-10, 11):
        hw = half[abs(dy)]
        y = 30 + dy
        for x in range(PCX - hw, PCX + hw + 1):
            pupil[(x, y)] = (x - PCX, dy, hw)
    pmask = set(pupil)
    # warm glow bleeding into the iris, then a dark warm edge for crispness
    for x, y in ring(pmask):
        if (x, y) in almond:
            put(x, y, GOLD_OUT)
    for (x, y), (ox, dy, hw) in pupil.items():
        low = dy >= 5
        if ox == 0:
            c = GOLD_W if -6 <= dy <= 3 else (GOLD_HI if dy < 7 else GOLD_MID)
        elif ox == -1:
            c = GOLD_HI if not low else GOLD_MID
        elif ox == 1:
            c = GOLD_MID if not low else GOLD_DK
        elif ox < 0:
            c = GOLD_MID
        else:
            c = GOLD_DK
        if hw == 0:
            c = GOLD_HI if dy < 0 else GOLD_MID
        put(x, y, c)
    # ------------------------------------------------------------ 5. reflected prism beams
    for x, y in ring(beam_mask):
        if inner(x, y) and (x, y) not in almond and (x, y) not in beam_mask:
            put(x, y, SELOUT)
    for (x, y), (idx, t, sn, kind, ln) in beam_px.items():
        if kind == "gold":
            if sn > 0.35:
                c = GOLD_HI
            elif sn > -0.35:
                c = GOLD_W
            else:
                c = GOLD_MID
        else:
            if sn > 0.2:
                c = ICE_W
            elif sn > -0.55:
                c = ICE_HI
            else:
                c = ICE_MID
        if t > ln - 2.2 and kind == "gold":
            c = GOLD_W if sn > -0.6 else GOLD_HI
        put(x, y, c)

    # exit flare where the beams leave the socket
    for ox, oy, ln, hw, kind in beams:
        fx, fy = int(math.floor(ox + DX * 0.6)), int(math.floor(oy + DY * 0.6))
        put(fx, fy, GOLD_W if kind == "gold" else ICE_W)

    # trailing prism shards / speed dashes
    for sx, sy, ln, col in [(46, 31, 3, ICE_HI), (52, 44, 2, GOLD_HI), (44, 47, 3, ICE_MID)]:
        for i in range(ln):
            for j in (0, 1):
                put_in(sx + i * 2 + j, sy + i, col if i < ln - 1 or j == 0 else hex_rgba("#7AF5E0", 120))

    # ------------------------------------------------------------ 6. sparkles
    def star(cx, cy, core, arm, size):
        put_in(cx, cy, core)
        for k in range(1, size + 1):
            a = arm if k < size else (arm[0], arm[1], arm[2], 130)
            put_in(cx - k, cy, a)
            put_in(cx + k, cy, a)
            put_in(cx, cy - k, a)
            put_in(cx, cy + k, a)

    star(SRC[0], SRC[1], GOLD_W, GOLD_HI, 2)      # incident light source
    star(50, 15, GOLD_W, ICE_HI, 2)
    star(14, 47, GOLD_W, ICE_HI, 1)
    star(51, 51, ICE_W, GOLD_HI, 1)
    put_in(44, 13, hex_rgba("#7AF5E0", 200))
    put_in(10, 37, hex_rgba("#7AF5E0", 150))

    # ------------------------------------------------------------ upscale + write
    out_w = SIZE * SCALE
    out_h = SIZE * SCALE
    buf = bytearray(out_w * out_h * 4)
    for y in range(SIZE):
        row = canvas[y]
        for x in range(SIZE):
            r, g, b, a = row[x]
            px = bytes((r, g, b, a)) * SCALE
            for sy in range(SCALE):
                base = ((y * SCALE + sy) * out_w + x * SCALE) * 4
                buf[base : base + SCALE * 4] = px

    data = bytes(buf)
    for path in OUTPUTS:
        write_png(path, out_w, out_h, data)
        print(f"wrote {path.relative_to(ROOT)} ({out_w}x{out_h})")


if __name__ == "__main__":
    draw_icon()
