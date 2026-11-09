package dev.kcyq.remi.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Lunar-style crosshair maker: paint a 15 x 15 grid, pick colours, start from a preset, see it live. */
public class CrosshairEditorScreen extends Screen {
	private final Screen back;
	private int colour = 0xFFFFFFFF;
	private int painting = 0; // 0 none, 1 paint, 2 erase
	private static final int[] PALETTE = { 0xFFFFFFFF, 0xFF000000, 0xFFC4A0FF, 0xFFA77BFF, 0xFF2EE6D6, 0xFF4FD18B, 0xFFFFD34D, 0xFFFF6A2B, 0xFFFF5C6C, 0xFFFF8CE6, 0xFF6FA8FF, 0xC0140A28 };
	private static final String[] PRESETS = { "remi", "dot", "cross", "plus", "circle", "clear" };

	public CrosshairEditorScreen(Screen back) { super(Component.literal("Crosshair")); this.back = back; }

	@Override public boolean isPauseScreen() { return false; }

	private int cell() { return Math.max(6, Math.min(12, (height - 90) / CrosshairModule.SIZE)); }
	private int gx() { return width / 2 - 110; }
	private int gy() { return height / 2 - cell() * CrosshairModule.SIZE / 2; }

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
		// (Minecraft draws the blurred background itself just before this; drawing it again crashes)
		int c = cell(), n = CrosshairModule.SIZE, x = gx(), y = gy();
		int pw = n * c + 220, px = width / 2 - pw / 2 + 30 - 30, py = y - 30;
		Theme.panel(g, x - 14, py, x + n * c + 200, y + n * c + 16, 0xF20E0C16, 0x30FFFFFF);
		g.fillGradient(x - 13, py, x + n * c + 199, py + 1, Theme.ACCENT, 0xFF2EE6D6);
		g.text(font, "Crosshair Editor", x, py + 10, Theme.TEXT, false);
		Theme.small(g, "Left-click paints · right-click erases", x + 100, py + 11, Theme.DIM, 0.6f);

		// the grid (with centre guides)
		CrosshairModule.drawGrid(g, x, y, c, RemiConfig.get().crosshair, true);
		g.fill(x + (n / 2) * c, y - 3, x + (n / 2) * c + c, y - 1, Theme.alpha(Theme.ACCENT, 0.7f));
		g.fill(x - 3, y + (n / 2) * c, x - 1, y + (n / 2) * c + c, Theme.alpha(Theme.ACCENT, 0.7f));
		int hx = (mx - x) / c, hy = (my - y) / c;
		if (mx >= x && my >= y && hx < n && hy < n) g.outline(x + hx * c, y + hy * c, c, c, 0xFFFFFFFF);

		// right side: palette, presets, preview
		int rx = x + n * c + 16, ry = y;
		Theme.small(g, "COLOUR", rx, ry, Theme.MUTED, 0.6f);
		for (int i = 0; i < PALETTE.length; i++) {
			int sx = rx + (i % 6) * 18, sy = ry + 9 + (i / 6) * 18;
			if (PALETTE[i] == 0xFF000000) g.fill(sx, sy, sx + 14, sy + 14, 0xFF3A3448);
			Theme.panel(g, sx + 1, sy + 1, sx + 13, sy + 13, PALETTE[i], 0);
			if (PALETTE[i] == colour) g.outline(sx - 1, sy - 1, 16, 16, 0xFFFFFFFF);
		}
		Theme.small(g, "START FROM", rx, ry + 52, Theme.MUTED, 0.6f);
		for (int i = 0; i < PRESETS.length; i++) {
			int bx = rx + (i % 3) * 56, by = ry + 62 + (i / 3) * 20;
			boolean h = mx >= bx && mx < bx + 52 && my >= by && my < by + 16;
			Theme.panel(g, bx, by, bx + 52, by + 16, h ? 0xFF24202F : 0xFF1A1626, 0x30FFFFFF);
			Theme.centered(g, Theme.cap(PRESETS[i]), bx + 26, by + 4, Theme.TEXT);
		}
		Theme.small(g, "PREVIEW", rx, ry + 110, Theme.MUTED, 0.6f);
		g.fill(rx, ry + 120, rx + 50, ry + 170, 0xFF5E8F4A); // grass
		g.fill(rx + 54, ry + 120, rx + 104, ry + 170, 0xFF6FA8FF); // sky
		g.fill(rx + 108, ry + 120, rx + 158, ry + 170, 0xFF1A1626); // night
		for (int k = 0; k < 3; k++) CrosshairModule.drawGrid(g, rx + 18 + k * 54, ry + 138, 1, RemiConfig.get().crosshair, false);
		CrosshairModule.drawGrid(g, rx + 54 + 2, ry + 175, 3, RemiConfig.get().crosshair, false);
		Theme.small(g, "Esc to save", rx, y + n * c + 4, Theme.DIM, 0.6f);
		super.extractRenderState(g, mx, my, delta);
	}

	private void paintAt(double mx, double my) {
		int c = cell(), n = CrosshairModule.SIZE;
		int hx = (int) Math.floor((mx - gx()) / c), hy = (int) Math.floor((my - gy()) / c);
		if (hx < 0 || hy < 0 || hx >= n || hy >= n) return;
		RemiConfig.get().crosshair[hy * n + hx] = painting == 2 ? 0 : colour;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
		double mx = e.x(), my = e.y();
		int c = cell(), n = CrosshairModule.SIZE, rx = gx() + n * c + 16, ry = gy();
		for (int i = 0; i < PALETTE.length; i++) {
			int sx = rx + (i % 6) * 18, sy = ry + 9 + (i / 6) * 18;
			if (mx >= sx && mx < sx + 14 && my >= sy && my < sy + 14) { colour = PALETTE[i]; return true; }
		}
		for (int i = 0; i < PRESETS.length; i++) {
			int bx = rx + (i % 3) * 56, by = ry + 62 + (i / 3) * 20;
			if (mx >= bx && mx < bx + 52 && my >= by && my < by + 16) {
				RemiConfig.get().crosshair = PRESETS[i].equals("clear") ? new int[n * n] : CrosshairModule.preset(PRESETS[i]);
				return true;
			}
		}
		painting = e.button() == 1 ? 2 : 1;
		paintAt(mx, my);
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
		if (painting != 0) { paintAt(e.x(), e.y()); return true; }
		return super.mouseDragged(e, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent e) { painting = 0; return true; }

	@Override
	public void onClose() { RemiConfig.save(); minecraft.setScreen(back); }
}
