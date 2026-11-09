package dev.kcyq.remi.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Your own pixel crosshair (15 x 15), drawn instead of the vanilla one. Edit it from the Remi menu. */
public class CrosshairModule extends Module {
	public static final int SIZE = 15;

	public CrosshairModule() { super("crosshair", "Crosshair", "Draw your own crosshair, pixel by pixel. Turns red on mobs and players.", "Visual", "✚", 0xFFFF5C6C, true, false); }

	@Override public int width() { return SIZE; }
	@Override public int height() { return SIZE; }
	@Override public int[] defaultPos(int sw, int sh) { return new int[] { sw / 2 - 7, sh / 2 - 7 }; }
	@Override public void draw(GuiGraphicsExtractor g, int x, int y, boolean preview) { drawGrid(g, x, y, 1, RemiConfig.get().crosshair, false); }

	public static void drawGrid(GuiGraphicsExtractor g, int x, int y, int px, int[] grid, boolean showEmpty) {
		Minecraft mc = Minecraft.getInstance();
		boolean target = RemiConfig.get().crosshairTargetColor && mc.crosshairPickEntity != null;
		for (int i = 0; i < SIZE * SIZE; i++) {
			int c = grid[i];
			int cx = x + (i % SIZE) * px, cy = y + (i / SIZE) * px;
			if (c == 0) { if (showEmpty) g.fill(cx, cy, cx + px, cy + px, ((i % SIZE + i / SIZE) % 2 == 0) ? 0x18FFFFFF : 0x0CFFFFFF); continue; }
			if (target && ((c >>> 24) & 0xFF) > 200) c = RemiConfig.get().crosshairTarget;
			g.fill(cx, cy, cx + px, cy + px, c);
		}
	}

	/** Draws it in the middle of the screen (called in place of the vanilla crosshair). */
	public static void renderCentered(GuiGraphicsExtractor g) {
		drawGrid(g, g.guiWidth() / 2 - SIZE / 2, g.guiHeight() / 2 - SIZE / 2, 1, RemiConfig.get().crosshair, false);
	}

	public static int[] preset(String name) {
		int[] g = new int[SIZE * SIZE];
		int c = SIZE / 2, arm = 0xFFC4A0FF, core = 0xFF2EE6D6, edge = 0xC0140A28, white = 0xFFFFFFFF;
		switch (name) {
			case "dot" -> { set(g, c, c, white); set(g, c + 1, c, white); set(g, c, c + 1, white); set(g, c + 1, c + 1, white); }
			case "cross" -> { for (int d = -5; d <= 5; d++) { set(g, c + d, c, white); set(g, c, c + d, white); } }
			case "plus" -> { for (int d = 2; d <= 5; d++) { set(g, c + d, c, white); set(g, c - d, c, white); set(g, c, c + d, white); set(g, c, c - d, white); } set(g, c, c, white); }
			case "circle" -> {
				for (int i = 0; i < 64; i++) { double a = i / 64.0 * Math.PI * 2; set(g, c + (int) Math.round(Math.cos(a) * 5), c + (int) Math.round(Math.sin(a) * 5), white); }
				set(g, c, c, white);
			}
			default -> { // Remi: four arms with a gap, a bright core, dark outline so it reads on any background
				for (int d = 2; d <= 5; d++) { set(g, c + d, c, arm); set(g, c - d, c, arm); set(g, c, c + d, arm); set(g, c, c - d, arm); }
				set(g, c, c, core);
				int[] copy = g.clone();
				for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++) {
					if (copy[y * SIZE + x] != 0) continue;
					if (lit(copy, x + 1, y) || lit(copy, x - 1, y) || lit(copy, x, y + 1) || lit(copy, x, y - 1)) g[y * SIZE + x] = edge;
				}
			}
		}
		return g;
	}

	private static void set(int[] g, int x, int y, int c) { if (x >= 0 && y >= 0 && x < SIZE && y < SIZE) g[y * SIZE + x] = c; }
	private static boolean lit(int[] g, int x, int y) { return x >= 0 && y >= 0 && x < SIZE && y < SIZE && ((g[y * SIZE + x] >>> 24) & 0xFF) == 0xFF; }
}
