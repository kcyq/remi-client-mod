package dev.kcyq.remi.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * One entry in the Remi menu (Right Shift). Modules that draw on screen can be moved in the HUD editor:
 * each draws at its saved spot, or at its default spot until you move it.
 */
public abstract class Module {
	public final String id, name, desc, category, glyph;
	public final int accent;
	public final boolean onByDefault, movable;
	public int defaultColor = 0xFFF4F2FA;

	// where it was drawn last frame (for the HUD editor)
	public int lastX, lastY, lastW, lastH;
	public long lastDrawn;

	protected Module(String id, String name, String desc, String category, String glyph, int accent, boolean onByDefault, boolean movable) {
		this.id = id; this.name = name; this.desc = desc; this.category = category; this.glyph = glyph;
		this.accent = accent; this.onByDefault = onByDefault; this.movable = movable;
	}

	public RemiConfig.ModuleCfg cfg() { return RemiConfig.of(this); }
	public boolean on() { return cfg().enabled; }

	/** Size in GUI pixels at scale 1 (used for placement and the editor's placeholder box). */
	public abstract int width();
	public abstract int height();

	/** Default top-left corner for this screen size. */
	public abstract int[] defaultPos(int sw, int sh);

	/** Draw at (x, y), already scaled. `preview` = in the HUD editor with no live data: draw sample content. */
	public abstract void draw(GuiGraphicsExtractor g, int x, int y, boolean preview);

	/** Whether there's anything to show right now (e.g. the dungeon map only in a dungeon). */
	public boolean visible() { return true; }

	/** Settings this module offers in its gear menu. */
	public boolean hasColor() { return false; }
	public boolean hasBackground() { return false; }

	public int[] place(int sw, int sh) {
		RemiConfig.ModuleCfg c = cfg();
		float s = c.scale;
		int w = (int) (width() * s), h = (int) (height() * s);
		int x, y;
		if (c.x < 0 || c.y < 0) { int[] d = defaultPos(sw, sh); x = d[0]; y = d[1]; }
		else { x = Math.round(c.x * sw); y = Math.round(c.y * sh); }
		x = Math.max(0, Math.min(sw - w, x));
		y = Math.max(0, Math.min(sh - h, y));
		return new int[] { x, y, w, h };
	}

	/** Draws the module where it belongs, scaled, and remembers where (for the editor). */
	public void render(GuiGraphicsExtractor g, boolean preview) {
		int sw = g.guiWidth(), sh = g.guiHeight();
		int[] p = place(sw, sh);
		float s = cfg().scale;
		g.pose().pushMatrix();
		g.pose().translate(p[0], p[1]);
		g.pose().scale(s, s);
		draw(g, 0, 0, preview);
		g.pose().popMatrix();
		lastX = p[0]; lastY = p[1]; lastW = p[2]; lastH = p[3]; lastDrawn = System.currentTimeMillis();
	}
}
