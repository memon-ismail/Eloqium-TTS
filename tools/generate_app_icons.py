#!/usr/bin/env python3
import zlib
import struct
import os
import math

SOURCE_PNG = "/storage/emulated/0/Download/Eloqium.png"
RES_DIR = "app/src/main/res"

def read_png(path):
    with open(path, "rb") as f:
        data = f.read()
    assert data[:8] == b"\x89PNG\r\n\x1a\n"
    pos = 8
    idat = []
    w = h = None
    while pos < len(data):
        l, ct = struct.unpack(">I4s", data[pos:pos+8])
        pos += 8
        cd = data[pos:pos+l]
        pos += l + 4
        if ct == b"IHDR":
            w, h = struct.unpack(">II", cd[:8])
        elif ct == b"IDAT":
            idat.append(cd)
        elif ct == b"IEND":
            break
    raw = zlib.decompress(b"".join(idat))
    return w, h, raw

def unfilter_png(w, h, raw):
    bpp = 3
    stride = w * bpp
    out = bytearray(w * h * bpp)
    prev_row = bytearray(stride)
    raw_pos = 0
    for y in range(h):
        filter_type = raw[raw_pos]
        raw_pos += 1
        curr_row = bytearray(raw[raw_pos : raw_pos + stride])
        raw_pos += stride
        if filter_type == 1:
            for x in range(bpp, stride):
                curr_row[x] = (curr_row[x] + curr_row[x - bpp]) & 0xFF
        elif filter_type == 2:
            for x in range(stride):
                curr_row[x] = (curr_row[x] + prev_row[x]) & 0xFF
        elif filter_type == 3:
            for x in range(stride):
                left = curr_row[x - bpp] if x >= bpp else 0
                up = prev_row[x]
                curr_row[x] = (curr_row[x] + ((left + up) >> 1)) & 0xFF
        elif filter_type == 4:
            for x in range(stride):
                left = curr_row[x - bpp] if x >= bpp else 0
                up = prev_row[x]
                up_left = prev_row[x - bpp] if x >= bpp else 0
                p = left + up - up_left
                pa = abs(p - left)
                pb = abs(p - up)
                pc = abs(p - up_left)
                pr = left if (pa <= pb and pa <= pc) else (up if pb <= pc else up_left)
                curr_row[x] = (curr_row[x] + pr) & 0xFF
        out[y * stride : (y + 1) * stride] = curr_row
        prev_row = curr_row
    return out

def write_chunk(chunk_type, data):
    length = len(data)
    crc = zlib.crc32(chunk_type + data) & 0xffffffff
    return struct.pack(">I4s", length, chunk_type) + data + struct.pack(">I", crc)

def write_png(w, h, data, has_alpha=True):
    bpp = 4 if has_alpha else 3
    color_type = 6 if has_alpha else 2
    raw = bytearray()
    stride = w * bpp
    for y in range(h):
        raw.append(0)
        raw.extend(data[y * stride : (y + 1) * stride])
    compressed = zlib.compress(bytes(raw), level=9)
    ihdr_data = struct.pack(">IIBBBBB", w, h, 8, color_type, 0, 0, 0)
    out = b"\x89PNG\r\n\x1a\n"
    out += write_chunk(b"IHDR", ihdr_data)
    out += write_chunk(b"IDAT", compressed)
    out += write_chunk(b"IEND", b"")
    return out

def resize_rgba(src_w, src_h, src_rgba, dst_w, dst_h, circular_mask=False):
    dst = bytearray(dst_w * dst_h * 4)
    cx = dst_w / 2.0
    cy = dst_h / 2.0
    radius = min(dst_w, dst_h) / 2.0

    x_ratio = float(src_w) / float(dst_w)
    y_ratio = float(src_h) / float(dst_h)

    for y in range(dst_h):
        src_y = int(y * y_ratio)
        if src_y >= src_h:
            src_y = src_h - 1
        for x in range(dst_w):
            src_x = int(x * x_ratio)
            if src_x >= src_w:
                src_x = src_w - 1

            src_idx = (src_y * src_w + src_x) * 4
            dst_idx = (y * dst_w + x) * 4

            r = src_rgba[src_idx]
            g = src_rgba[src_idx + 1]
            b = src_rgba[src_idx + 2]
            a = src_rgba[src_idx + 3]

            if circular_mask:
                dist = math.sqrt((x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2)
                if dist > radius:
                    a = 0
                elif dist > radius - 1.0:
                    alpha_factor = max(0.0, min(1.0, radius - dist))
                    a = int(a * alpha_factor)

            dst[dst_idx] = r
            dst[dst_idx + 1] = g
            dst[dst_idx + 2] = b
            dst[dst_idx + 3] = a

    return dst

def main():
    print(f"Reading source logo: {SOURCE_PNG}")
    w, h, raw = read_png(SOURCE_PNG)
    pixels = unfilter_png(w, h, raw)

    crop_x1 = 150
    crop_y1 = 150
    crop_size = 954
    crop_w = crop_size
    crop_h = crop_size

    cropped_rgba = bytearray(crop_w * crop_h * 4)
    for y in range(crop_h):
        src_y = crop_y1 + y
        for x in range(crop_w):
            src_x = crop_x1 + x
            src_idx = (src_y * w + src_x) * 3
            r = pixels[src_idx]
            g = pixels[src_idx + 1]
            b = pixels[src_idx + 2]

            is_white = (r > 248 and g > 248 and b > 248)
            dst_idx = (y * crop_w + x) * 4
            cropped_rgba[dst_idx] = r
            cropped_rgba[dst_idx + 1] = g
            cropped_rgba[dst_idx + 2] = b
            cropped_rgba[dst_idx + 3] = 0 if is_white else 255

    logo_512 = resize_rgba(crop_w, crop_h, cropped_rgba, 512, 512)
    os.makedirs(f"{RES_DIR}/drawable", exist_ok=True)
    with open(f"{RES_DIR}/drawable/eloqium_logo.png", "wb") as f:
        f.write(write_png(512, 512, logo_512, has_alpha=True))
    print("Wrote eloqium_logo.png (512x512)")

    fg_size = 432
    fg_rgba = bytearray(fg_size * fg_size * 4)
    icon_inner_size = 260
    icon_inner = resize_rgba(crop_w, crop_h, cropped_rgba, icon_inner_size, icon_inner_size)
    offset_x = (fg_size - icon_inner_size) // 2
    offset_y = (fg_size - icon_inner_size) // 2

    for y in range(icon_inner_size):
        for x in range(icon_inner_size):
            src_i = (y * icon_inner_size + x) * 4
            dst_i = ((y + offset_y) * fg_size + (x + offset_x)) * 4
            fg_rgba[dst_i:dst_i+4] = icon_inner[src_i:src_i+4]

    with open(f"{RES_DIR}/drawable/ic_launcher_foreground.png", "wb") as f:
        f.write(write_png(fg_size, fg_size, fg_rgba, has_alpha=True))
    print("Wrote ic_launcher_foreground.png (432x432)")

    densities = {
        "mdpi": 48,
        "hdpi": 72,
        "xhdpi": 96,
        "xxhdpi": 144,
        "xxxhdpi": 192
    }

    for density, size in densities.items():
        density_dir = f"{RES_DIR}/mipmap-{density}"
        os.makedirs(density_dir, exist_ok=True)

        bg_white_icon = bytearray(size * size * 4)
        for i in range(0, len(bg_white_icon), 4):
            bg_white_icon[i] = 255
            bg_white_icon[i+1] = 255
            bg_white_icon[i+2] = 255
            bg_white_icon[i+3] = 255

        pad = max(2, int(size * 0.12))
        inner_size = size - 2 * pad
        inner_icon = resize_rgba(crop_w, crop_h, cropped_rgba, inner_size, inner_size)

        for y in range(inner_size):
            for x in range(inner_size):
                src_i = (y * inner_size + x) * 4
                dst_i = ((y + pad) * size + (x + pad)) * 4
                alpha = inner_icon[src_i + 3] / 255.0
                if alpha > 0:
                    r = int(inner_icon[src_i] * alpha + 255 * (1 - alpha))
                    g = int(inner_icon[src_i+1] * alpha + 255 * (1 - alpha))
                    b = int(inner_icon[src_i+2] * alpha + 255 * (1 - alpha))
                    bg_white_icon[dst_i] = r
                    bg_white_icon[dst_i+1] = g
                    bg_white_icon[dst_i+2] = b
                    bg_white_icon[dst_i+3] = 255

        normal_icon = resize_rgba(size, size, bg_white_icon, size, size, circular_mask=False)
        with open(f"{density_dir}/ic_launcher.png", "wb") as f:
            f.write(write_png(size, size, normal_icon, has_alpha=True))

        round_icon = resize_rgba(size, size, bg_white_icon, size, size, circular_mask=True)
        with open(f"{density_dir}/ic_launcher_round.png", "wb") as f:
            f.write(write_png(size, size, round_icon, has_alpha=True))

        print(f"Wrote mipmap-{density} ic_launcher.png & ic_launcher_round.png ({size}x{size})")

    os.makedirs(f"{RES_DIR}/mipmap-anydpi-v26", exist_ok=True)
    adaptive_xml = '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
</adaptive-icon>
'''
    with open(f"{RES_DIR}/mipmap-anydpi-v26/ic_launcher.xml", "w") as f:
        f.write(adaptive_xml)
    with open(f"{RES_DIR}/mipmap-anydpi-v26/ic_launcher_round.xml", "w") as f:
        f.write(adaptive_xml)

    print("All branding assets generated successfully!")

if __name__ == "__main__":
    main()
