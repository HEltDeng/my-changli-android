from __future__ import annotations

import argparse
import hashlib
import math
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont


BLUE = (4, 41, 129)
WHITE = (250, 250, 250)
PANEL = (239, 243, 249)
INK = (24, 50, 91)
ADAPTIVE_SIZE = 432
ADAPTIVE_SAFE_INSET = 72
MASTER_SIZE = 1296
FONT_REGULAR = Path(r"C:\Windows\Fonts\msyh.ttc")
FONT_BOLD = Path(r"C:\Windows\Fonts\msyhbd.ttc")


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(str(FONT_BOLD if bold else FONT_REGULAR), size=size)


def label(
    draw: ImageDraw.ImageDraw,
    xy: tuple[int, int],
    text: str,
    size: int,
    bold: bool = False,
    fill: tuple[int, int, int] = INK,
) -> None:
    draw.text(xy, text, font=font(size, bold), fill=fill)


def connected_components(mask: np.ndarray) -> list[tuple[int, int, int, int, int]]:
    height, width = mask.shape
    seen = np.zeros_like(mask, dtype=bool)
    components: list[tuple[int, int, int, int, int]] = []
    for y0, x0 in zip(*np.where(mask & ~seen)):
        if seen[y0, x0]:
            continue
        stack = [(int(y0), int(x0))]
        seen[y0, x0] = True
        min_x = max_x = int(x0)
        min_y = max_y = int(y0)
        area = 0
        while stack:
            y, x = stack.pop()
            area += 1
            min_x = min(min_x, x)
            max_x = max(max_x, x)
            min_y = min(min_y, y)
            max_y = max(max_y, y)
            for ny in range(max(0, y - 1), min(height, y + 2)):
                for nx in range(max(0, x - 1), min(width, x + 2)):
                    if mask[ny, nx] and not seen[ny, nx]:
                        seen[ny, nx] = True
                        stack.append((ny, nx))
        components.append((min_x, min_y, max_x + 1, max_y + 1, area))
    return components


def trim_alpha(image: Image.Image, pad: int = 4) -> Image.Image:
    alpha = image.getchannel("A")
    box = alpha.getbbox()
    if box is None:
        raise RuntimeError("Expected non-empty alpha content")
    x1, y1, x2, y2 = box
    box = (
        max(0, x1 - pad),
        max(0, y1 - pad),
        min(image.width, x2 + pad),
        min(image.height, y2 + pad),
    )
    return image.crop(box)


def extract_logo(master: Image.Image) -> Image.Image:
    # The crop isolates the accepted S mark. Pixels are preserved from the frozen
    # master; only its smooth dark-blue backdrop is removed.
    box = (230, 70, 1025, 930)
    crop = np.asarray(master.convert("RGB"))[box[1] : box[3], box[0] : box[2]].astype(np.int16)
    r, g, b = crop[:, :, 0], crop[:, :, 1], crop[:, :, 2]
    light = (r > 46) & (g > 72) & (b > 105) & ((r + g) > 155)
    teal = (g > 66) & ((g - r) > 25) & (b > 72) & ((b - r) > 36)
    candidate = light | teal
    components = connected_components(candidate)
    kept = np.zeros_like(candidate)
    for x1, y1, x2, y2, area in components:
        center_x = (x1 + x2) / 2
        center_y = (y1 + y2) / 2
        if area >= 70 and 80 <= center_x <= 720 and 20 <= center_y <= 820:
            kept[y1:y2, x1:x2] |= candidate[y1:y2, x1:x2]

    mask = Image.fromarray((kept.astype(np.uint8) * 255), "L")
    mask = mask.filter(ImageFilter.MaxFilter(3)).filter(ImageFilter.MinFilter(3))
    mask = mask.filter(ImageFilter.GaussianBlur(0.65))
    rgba = np.dstack([crop.astype(np.uint8), np.asarray(mask)])
    return trim_alpha(Image.fromarray(rgba, "RGBA"), pad=6)


def extract_brand_text(master: Image.Image) -> Image.Image:
    rgb = np.asarray(master.convert("RGB"), dtype=np.int16)
    r, g, b = rgb[:, :, 0], rgb[:, :, 1], rgb[:, :, 2]
    yy, xx = np.indices(r.shape)
    blue = (
        (yy >= 1000)
        & (yy <= 1228)
        & (xx >= 145)
        & (xx <= 1110)
        & ((b - r) >= 28)
        & ((b - g) >= 14)
        & (r <= 190)
    )
    components = connected_components(blue)
    selected = [
        component
        for component in components
        if component[4] >= 80
        and component[1] >= 1025
        and component[0] > 145
        and component[2] < 1110
    ]
    if len(selected) < 4:
        raise RuntimeError(f"Unable to isolate the four frozen brand characters: {len(selected)}")
    left = min(c[0] for c in selected)
    top = min(c[1] for c in selected)
    right = max(c[2] for c in selected)
    bottom = max(c[3] for c in selected)
    crop = rgb[top:bottom, left:right]
    cr, cg, cb = crop[:, :, 0], crop[:, :, 1], crop[:, :, 2]
    alpha = np.clip(np.minimum(cb - cr, cb - cg) * 5, 0, 255).astype(np.uint8)
    alpha[(cb - cr < 18) | (cb - cg < 8)] = 0
    return trim_alpha(Image.fromarray(np.dstack([crop.astype(np.uint8), alpha]), "RGBA"), pad=5)


def fit_layer(image: Image.Image, max_width: int, max_height: int) -> Image.Image:
    scale = min(max_width / image.width, max_height / image.height)
    size = (max(1, round(image.width * scale)), max(1, round(image.height * scale)))
    return image.resize(size, Image.Resampling.LANCZOS)


def strip_mask(
    size: int,
    edge_y: float,
    center_y: float,
    exponent: float = 1.8,
    supersample: int = 4,
) -> Image.Image:
    canvas = Image.new("L", (size * supersample, size * supersample), 0)
    draw = ImageDraw.Draw(canvas)
    half = (size - 1) / 2
    points: list[tuple[int, int]] = []
    for x in range(size * supersample):
        xf = x / supersample
        normalized = min(1.0, abs(xf - half) / half)
        y = center_y - (center_y - edge_y) * (normalized**exponent)
        points.append((x, round(y * supersample)))
    points.extend(((size * supersample - 1, size * supersample - 1), (0, size * supersample - 1)))
    draw.polygon(points, fill=255)
    return canvas.resize((size, size), Image.Resampling.LANCZOS)


def build_adaptive_foreground(logo: Image.Image, text: Image.Image) -> Image.Image:
    canvas = Image.new("RGBA", (ADAPTIVE_SIZE, ADAPTIVE_SIZE), (0, 0, 0, 0))
    white = Image.new("RGBA", canvas.size, (*WHITE, 255))
    # Android exposes the central 288 px of a 432 px foreground at rest. The
    # curve maps to roughly 65-69% of the final masked icon and continues below
    # every mask, preventing the former blue lower-edge remnant.
    mask = strip_mask(ADAPTIVE_SIZE, edge_y=258, center_y=271)
    canvas.alpha_composite(Image.composite(white, Image.new("RGBA", canvas.size), mask))

    logo_layer = fit_layer(logo, max_width=166, max_height=166)
    logo_x = (ADAPTIVE_SIZE - logo_layer.width) // 2
    logo_y = 94
    canvas.alpha_composite(logo_layer, (logo_x, logo_y))

    text_layer = fit_layer(text, max_width=180, max_height=42)
    text_x = (ADAPTIVE_SIZE - text_layer.width) // 2
    text_y = 284
    canvas.alpha_composite(text_layer, (text_x, text_y))
    return canvas


def build_launcher_master(logo: Image.Image, text: Image.Image) -> Image.Image:
    canvas = Image.new("RGBA", (MASTER_SIZE, MASTER_SIZE), (*BLUE, 255))
    white = Image.new("RGBA", canvas.size, (*WHITE, 255))
    mask = strip_mask(MASTER_SIZE, edge_y=838, center_y=884)
    canvas.alpha_composite(Image.composite(white, Image.new("RGBA", canvas.size), mask))

    logo_layer = fit_layer(logo, max_width=710, max_height=710)
    canvas.alpha_composite(logo_layer, ((MASTER_SIZE - logo_layer.width) // 2, 90))

    text_layer = fit_layer(text, max_width=790, max_height=176)
    canvas.alpha_composite(text_layer, ((MASTER_SIZE - text_layer.width) // 2, 950))
    return canvas.convert("RGB")


def mask_image(kind: str, size: int) -> Image.Image:
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    if kind == "circle":
        draw.ellipse((0, 0, size - 1, size - 1), fill=255)
    elif kind == "rounded-square":
        draw.rounded_rectangle((0, 0, size - 1, size - 1), radius=round(size * 0.19), fill=255)
    elif kind == "rounded-rectangle":
        draw.rounded_rectangle((0, 0, size - 1, size - 1), radius=round(size * 0.27), fill=255)
    elif kind == "squircle":
        half = size / 2
        exponent = 4.6
        points = []
        for degree in range(361):
            theta = math.radians(degree)
            cosine, sine = math.cos(theta), math.sin(theta)
            x = math.copysign(abs(cosine) ** (2 / exponent), cosine) * (half - 1) + half
            y = math.copysign(abs(sine) ** (2 / exponent), sine) * (half - 1) + half
            points.append((x, y))
        draw.polygon(points, fill=255)
    else:
        draw.rectangle((0, 0, size, size), fill=255)
    return mask


def render_adaptive(foreground: Image.Image, size: int = 432) -> Image.Image:
    safe = foreground.crop(
        (
            ADAPTIVE_SAFE_INSET,
            ADAPTIVE_SAFE_INSET,
            ADAPTIVE_SIZE - ADAPTIVE_SAFE_INSET,
            ADAPTIVE_SIZE - ADAPTIVE_SAFE_INSET,
        )
    ).resize((size, size), Image.Resampling.LANCZOS)
    base = Image.new("RGBA", (size, size), (*BLUE, 255))
    base.alpha_composite(safe)
    return base.convert("RGB")


def apply_mask(icon: Image.Image, kind: str, size: int = 360, background: tuple[int, int, int] = PANEL) -> Image.Image:
    rendered = icon.resize((size, size), Image.Resampling.LANCZOS)
    mask = mask_image(kind, size)
    canvas = Image.new("RGB", (size, size), background)
    canvas.paste(rendered, (0, 0), mask)
    return canvas


def old_adaptive_render(old_foreground: Image.Image) -> Image.Image:
    return render_adaptive(old_foreground.convert("RGBA"))


def preview_sheet(adaptive: Image.Image, output: Path) -> None:
    sheet = Image.new("RGB", (2000, 1340), PANEL)
    draw = ImageDraw.Draw(sheet)
    label(draw, (60, 34), "我的长理 V1 Launcher Icon Structure Polish", 48, True)
    label(draw, (62, 96), "完整上下分区 · 非内嵌画布 · 原 Logo / 原字形 · Adaptive safe-zone mapping", 24)
    panels = (
        ("Circle", "circle"),
        ("Squircle", "squircle"),
        ("Rounded Square", "rounded-square"),
        ("常见圆角矩形", "rounded-rectangle"),
    )
    for index, (title, kind) in enumerate(panels):
        x = 60 + index * 480
        draw.rounded_rectangle((x, 160, x + 430, 650), radius=28, fill=(255, 255, 255), outline=(193, 204, 220), width=2)
        preview = apply_mask(adaptive, kind, 374, (255, 255, 255))
        sheet.paste(preview, (x + 28, 185))
        label(draw, (x + 28, 590), title, 22, True)

    backgrounds = (("浅色桌面", (232, 235, 241)), ("深色桌面", (21, 28, 42)))
    for index, (title, bg) in enumerate(backgrounds):
        x = 60 + index * 950
        y = 715
        draw.rounded_rectangle((x, y, x + 890, y + 550), radius=28, fill=bg)
        for small_index, small_size in enumerate((72, 96, 144)):
            preview = apply_mask(adaptive, "squircle", small_size, bg)
            px = x + 95 + small_index * 245
            py = y + 105 + (144 - small_size) // 2
            sheet.paste(preview, (px, py))
            label(draw, (px - 10, y + 280), f"{small_size}px", 20, fill=(255, 255, 255) if index else INK)
        label(draw, (x + 62, y + 410), "我的长理 QA", 34, fill=(255, 255, 255) if index else INK)
        label(draw, (x + 62, y + 474), title, 23, True, fill=(255, 255, 255) if index else INK)
    sheet.save(output, optimize=True)


def comparison_sheet(old: Image.Image, new: Image.Image, output: Path) -> None:
    sheet = Image.new("RGB", (1900, 1200), PANEL)
    draw = ImageDraw.Draw(sheet)
    label(draw, (60, 34), "我的长理 Launcher：安全内嵌版 → 完整结构版", 48, True)
    label(draw, (62, 96), "对比重点：画布边界、品牌条连续性、底部蓝边、桌面图标成型感", 24)
    entries = (("上一版：完整画布缩进 foreground", old, (126, 54, 54)), ("本轮：蓝色 background + 结构化 foreground", new, (14, 110, 83)))
    kinds = (("Circle", "circle"), ("Squircle", "squircle"), ("Rounded Square", "rounded-square"))
    for row, (title, icon, color) in enumerate(entries):
        y = 160 + row * 490
        label(draw, (60, y), title, 30, True, fill=color)
        for column, (mask_title, kind) in enumerate(kinds):
            x = 190 + column * 560
            preview = apply_mask(icon, kind, 390, (255, 255, 255))
            sheet.paste(preview, (x, y + 50))
            label(draw, (x, y + 448), mask_title, 21)
    label(draw, (62, 1130), "新方案让色块直接参与系统 mask：白色品牌条延伸至底边，外围不再补出第二层容器。", 23)
    sheet.save(output, optimize=True)


def write_individual_previews(adaptive: Image.Image, artifact_dir: Path) -> None:
    for name, kind in (
        ("circle", "circle"),
        ("squircle", "squircle"),
        ("rounded-square", "rounded-square"),
        ("rounded-rectangle", "rounded-rectangle"),
    ):
        apply_mask(adaptive, kind, 512, (255, 255, 255)).save(artifact_dir / f"preview-{name}.png", optimize=True)

    for name, bg in (("light", (232, 235, 241)), ("dark", (21, 28, 42))):
        board = Image.new("RGB", (768, 384), bg)
        for index, size in enumerate((72, 96, 144)):
            icon = apply_mask(adaptive, "squircle", size, bg)
            board.paste(icon, (85 + index * 215, 82 + (144 - size) // 2))
        draw = ImageDraw.Draw(board)
        label(draw, (78, 274), "我的长理 QA", 31, fill=(255, 255, 255) if name == "dark" else INK)
        board.save(artifact_dir / f"preview-small-{name}.png", optimize=True)


def write_resources(repo: Path, foreground: Image.Image, master: Image.Image) -> None:
    foreground.save(repo / "app/src/main/res/drawable-nodpi/my_changli_icon_foreground.png", optimize=True)
    for density, size in (("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)):
        master.resize((size, size), Image.Resampling.LANCZOS).save(
            repo / f"app/src/main/res/mipmap-{density}/my_changli_icon.png",
            optimize=True,
        )


def validate_geometry(foreground: Image.Image, launcher_master: Image.Image) -> dict[str, object]:
    alpha_box = foreground.getchannel("A").getbbox()
    if alpha_box is None or alpha_box[0] != 0 or alpha_box[2] != ADAPTIVE_SIZE or alpha_box[3] != ADAPTIVE_SIZE:
        raise RuntimeError(f"Adaptive strip must reach both sides and the bottom: {alpha_box}")

    adaptive = np.asarray(render_adaptive(foreground), dtype=np.int16)
    legacy = np.asarray(launcher_master.convert("RGB"), dtype=np.int16)
    adaptive_bottom = adaptive[-6:, :, :]
    legacy_bottom = legacy[-12:, :, :]
    adaptive_bottom_blue = int(((adaptive_bottom[:, :, 2] - adaptive_bottom[:, :, 0]) > 30).sum())
    legacy_bottom_blue = int(((legacy_bottom[:, :, 2] - legacy_bottom[:, :, 0]) > 30).sum())
    if adaptive_bottom_blue or legacy_bottom_blue:
        raise RuntimeError(
            f"Blue-edge regression detected: adaptive={adaptive_bottom_blue}, legacy={legacy_bottom_blue}"
        )

    return {
        "foreground_alpha_box": alpha_box,
        "adaptive_bottom_blue_pixels": adaptive_bottom_blue,
        "legacy_bottom_blue_pixels": legacy_bottom_blue,
        "adaptive_safe_inset": ADAPTIVE_SAFE_INSET,
    }


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest().upper()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", type=Path, required=True)
    parser.add_argument("--master", type=Path, required=True)
    parser.add_argument("--artifact-dir", type=Path, required=True)
    parser.add_argument("--old-foreground", type=Path)
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()

    args.artifact_dir.mkdir(parents=True, exist_ok=True)
    master_source = Image.open(args.master).convert("RGB")
    old_foreground_path = args.repo / "app/src/main/res/drawable-nodpi/my_changli_icon_foreground.png"
    old_foreground = Image.open(args.old_foreground or old_foreground_path).convert("RGBA")
    logo = extract_logo(master_source)
    brand_text = extract_brand_text(master_source)
    foreground = build_adaptive_foreground(logo, brand_text)
    launcher_master = build_launcher_master(logo, brand_text)
    geometry = validate_geometry(foreground, launcher_master)
    adaptive = render_adaptive(foreground)
    old_adaptive = old_adaptive_render(old_foreground)

    master_path = args.artifact_dir / "my-changli-launcher-polish-master-1296.png"
    foreground_path = args.artifact_dir / "my-changli-launcher-polish-adaptive-foreground-432.png"
    preview_path = args.artifact_dir / "MyChangli-V1.0.0-Icon-Polish-QA-preview.png"
    comparison_path = args.artifact_dir / "MyChangli-V1.0.0-Icon-Polish-before-after.png"
    launcher_master.save(master_path, optimize=True)
    foreground.save(foreground_path, optimize=True)
    preview_sheet(adaptive, preview_path)
    comparison_sheet(old_adaptive, adaptive, comparison_path)
    write_individual_previews(adaptive, args.artifact_dir)
    if args.apply:
        write_resources(args.repo, foreground, launcher_master)

    print(f"master={master_path} sha256={sha256(master_path)}")
    print(f"foreground={foreground_path} sha256={sha256(foreground_path)}")
    print(f"preview={preview_path} sha256={sha256(preview_path)}")
    print(f"comparison={comparison_path} sha256={sha256(comparison_path)}")
    print(f"logo_size={logo.size} text_size={brand_text.size}")
    print(f"geometry={geometry}")


if __name__ == "__main__":
    main()
