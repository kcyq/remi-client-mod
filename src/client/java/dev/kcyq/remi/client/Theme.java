package dev.kcyq.remi.client;

import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Remi colours and a few drawing helpers (everything is drawn from plain rectangles, so it's fast and crisp). */
public final class Theme {
	private Theme() {}

	public static final int BG = 0xD80C0A12, BG_SOFT = 0xB80C0A12, LINE = 0x40FFFFFF, TEXT = 0xFFF4F2FA, MUTED = 0xFFA098B8,
			DIM = 0xFF6E6882, ACCENT = 0xFFA77BFF, OK = 0xFF4FD18B, BAD = 0xFFFF5C6C, GOLD = 0xFFFFD34D;

	public static final Map<String, Integer> GEM = Map.ofEntries(
			Map.entry("infernal", 0xFFFF6A2B), Map.entry("guardian", 0xFF6FA8FF), Map.entry("seraph", 0xFFFFE9A6), Map.entry("void", 0xFF9B5CFF),
			Map.entry("terra", 0xFFC89A5A), Map.entry("tempest", 0xFF7FE3FF), Map.entry("stormborn", 0xFF4F7BFF), Map.entry("verdant", 0xFF3DD16B),
			Map.entry("phantom", 0xFFC9C9F0), Map.entry("midas", 0xFFFFCC33), Map.entry("chronos", 0xFFE6C27A), Map.entry("leviathan", 0xFF1FB5C9),
			Map.entry("reaper", 0xFFE0476E), Map.entry("celestial", 0xFFC7B6FF), Map.entry("glacier", 0xFFA8EAFF), Map.entry("origin", 0xFFFFFFFF),
			Map.entry("cataclysm", 0xFFFF3D3D), Map.entry("paradox", 0xFFFF5CE1), Map.entry("null", 0xFF9A9AB8), Map.entry("revenant", 0xFF4FFFB0),
			Map.entry("blank", 0xFF55506A));

	public static boolean mythic(String gem) {
		return gem.equals("cataclysm") || gem.equals("paradox") || gem.equals("null") || gem.equals("revenant");
	}

	public static int gem(String g) { return GEM.getOrDefault(g == null ? "" : g.toLowerCase(), ACCENT); }

	public static int parseHex(String s, int fallback) {
		try {
			String h = s.trim().replace("#", "");
			if (h.length() == 6) return 0xFF000000 | Integer.parseInt(h, 16);
		} catch (Exception ignored) { }
		return fallback;
	}

	/** "&b" or "&#33ccff" (Skript colour codes) to ARGB. */
	public static int fromCode(String code, int fallback) {
		if (code == null || code.isEmpty()) return fallback;
		String c = code.replace("§", "&");
		int i = c.indexOf("&#");
		if (i >= 0 && c.length() >= i + 8) return parseHex(c.substring(i + 2, i + 8), fallback);
		int j = c.lastIndexOf('&');
		if (j >= 0 && c.length() > j + 1) {
			return switch (Character.toLowerCase(c.charAt(j + 1))) {
				case '0' -> 0xFF202020; case '1' -> 0xFF3F3FD8; case '2' -> 0xFF2FA83F; case '3' -> 0xFF2FB6B6;
				case '4' -> 0xFFB82F2F; case '5' -> 0xFFA43FD0; case '6' -> 0xFFFFAA00; case '7' -> 0xFFAAAAAA;
				case '8' -> 0xFF666666; case '9' -> 0xFF5C7CFF; case 'a' -> 0xFF55FF7A; case 'b' -> 0xFF55FFFF;
				case 'c' -> 0xFFFF5555; case 'd' -> 0xFFFF66FF; case 'e' -> 0xFFFFFF55; case 'f' -> 0xFFFFFFFF;
				default -> fallback;
			};
		}
		return fallback;
	}

	public static int alpha(int argb, float a) {
		int base = (argb >>> 24) & 0xFF;
		return ((int) (base * Math.max(0, Math.min(1, a))) << 24) | (argb & 0xFFFFFF);
	}

	public static int mix(int a, int b, float t) {
		t = Math.max(0, Math.min(1, t));
		int aa = (a >>> 24) & 255, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
		int ba = (b >>> 24) & 255, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
		return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
	}

	public static Font font() { return Minecraft.getInstance().font; }

	public static float time() { return (System.currentTimeMillis() % 1_000_000L) / 1000f; }

	/** A panel with softly cut corners and a hairline border. */
	public static void panel(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int bg, int border) {
		g.fill(x1 + 1, y1, x2 - 1, y2, bg);
		g.fill(x1, y1 + 1, x1 + 1, y2 - 1, bg);
		g.fill(x2 - 1, y1 + 1, x2, y2 - 1, bg);
		if (border != 0) {
			g.fill(x1 + 1, y1, x2 - 1, y1 + 1, border);
			g.fill(x1 + 1, y2 - 1, x2 - 1, y2, border);
			g.fill(x1, y1 + 1, x1 + 1, y2 - 1, border);
			g.fill(x2 - 1, y1 + 1, x2, y2 - 1, border);
		}
	}

	/** A filled disc built from horizontal lines. */
	public static void disc(GuiGraphicsExtractor g, int cx, int cy, int r, int color) {
		for (int dy = -r; dy <= r; dy++) {
			int w = (int) Math.round(Math.sqrt(r * r - dy * dy));
			g.fill(cx - w, cy + dy, cx + w + 1, cy + dy + 1, color);
		}
	}

	/** A ring, drawn as dots; the first `fraction` of it (clockwise from the top) in `on`, the rest in `off`. */
	public static void ring(GuiGraphicsExtractor g, int cx, int cy, int r, float fraction, int on, int off) {
		int steps = Math.max(24, r * 7);
		for (int i = 0; i < steps; i++) {
			double a = -Math.PI / 2 + (i / (double) steps) * Math.PI * 2;
			int x = cx + (int) Math.round(Math.cos(a) * r), y = cy + (int) Math.round(Math.sin(a) * r);
			g.fill(x, y, x + 1, y + 1, i < fraction * steps ? on : off);
		}
	}

	/** A bar with a subtle top highlight and a moving shine. */
	public static void bar(GuiGraphicsExtractor g, int x, int y, int w, int h, float frac, int color, boolean shine) {
		g.fill(x, y, x + w, y + h, 0xFF1A1626);
		int fw = (int) (w * Math.max(0, Math.min(1, frac)));
		if (fw <= 0) return;
		g.fill(x, y, x + fw, y + h, color);
		g.fill(x, y, x + fw, y + 1, alpha(0xFFFFFFFF, 0.25f));
		if (shine) {
			int sx = x + (int) ((time() * 60) % (fw + 30)) - 30;
			int a = Math.max(x, sx), b = Math.min(x + fw, sx + 14);
			if (b > a) g.fill(a, y, b, y + h, alpha(0xFFFFFFFF, 0.22f));
		}
	}

	public static void text(GuiGraphicsExtractor g, String s, int x, int y, int color) { g.text(font(), s, x, y, color); }

	public static void centered(GuiGraphicsExtractor g, String s, int cx, int y, int color) { g.centeredText(font(), s, cx, y, color); }

	public static void small(GuiGraphicsExtractor g, String s, int x, int y, int color, float scale) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		g.text(font(), s, 0, 0, color);
		g.pose().popMatrix();
	}

	public static void smallCentered(GuiGraphicsExtractor g, String s, int cx, int y, int color, float scale) {
		small(g, s, cx - (int) (font().width(s) * scale / 2), y, color, scale);
	}

	public static String secs(double s) {
		if (s >= 60) return (int) (s / 60) + "m";
		if (s >= 10) return String.valueOf((int) Math.ceil(s));
		return String.format("%.1f", Math.max(0, s));
	}

	public static String cap(String s) { return s == null || s.isEmpty() ? "" : Character.toUpperCase(s.charAt(0)) + s.substring(1); }
}
