package dev.kcyq.remi.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import org.lwjgl.glfw.GLFW;

/**
 * The Remi menu (Right Shift), in the style of Lunar's mod menu: categories on the left, a search bar,
 * a grid of module tiles with an ENABLED / DISABLED button and a settings gear, and "Edit HUD Layout".
 */
public class RemiMenuScreen extends Screen {
	private final long openedAt = System.currentTimeMillis();
	private String category = "All", search = "";
	private Module settingsFor = null;
	private double scroll = 0, scrollTarget = 0;
	private EditBox searchBox;
	private boolean draggingScale = false;

	private int px, py, pw, ph; // panel
	private static final int SIDE = 96, TOP = 34, TILE_MIN = 112, TILE_H = 78, GAP = 8;
	private static final int[] SWATCHES = { 0xFFF4F2FA, 0xFFA77BFF, 0xFF2EE6D6, 0xFF4FD18B, 0xFFFFD34D, 0xFFFF6A2B, 0xFFFF5C6C, 0xFFFF8CE6, 0xFF6FA8FF, 0xFF1FB5C9, 0xFFAAAAAA, 0xFF202020 };

	public RemiMenuScreen() { super(Component.literal("Remi Client")); }

	@Override public boolean isPauseScreen() { return false; }

	@Override
	protected void init() {
		pw = Math.min(560, width - 30);
		ph = Math.min(330, height - 30);
		px = (width - pw) / 2;
		py = (height - ph) / 2;
		searchBox = new EditBox(font, px + SIDE + 22, py + 10, 150, 14, Component.literal("Search"));
		searchBox.setBordered(false);
		searchBox.setTextColor(Theme.TEXT);
		searchBox.setHint(Component.literal("Search mods..."));
		searchBox.setMaxLength(32);
		searchBox.setValue(search);
		searchBox.setResponder(s -> { search = s.toLowerCase(); scrollTarget = 0; });
		addRenderableWidget(searchBox);
	}

	private List<Module> shown() {
		List<Module> out = new ArrayList<>();
		for (Module m : Modules.ALL) {
			if (!category.equals("All") && !m.category.equals(category)) continue;
			if (!search.isEmpty() && !(m.name.toLowerCase().contains(search) || m.desc.toLowerCase().contains(search))) continue;
			out.add(m);
		}
		return out;
	}

	private int avail() { return pw - SIDE - 24; }
	private int cols() { return Math.max(1, (avail() + GAP) / (TILE_MIN + GAP)); }
	/** tiles stretch so the grid fills the panel edge to edge */
	private int tw() { return (avail() - (cols() - 1) * GAP) / cols(); }

	private float ease() {
		float t = Math.min(1f, (System.currentTimeMillis() - openedAt) / 200f);
		return 1 - (1 - t) * (1 - t) * (1 - t);
	}

	// ------------------------------------------------------------ drawing
	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
		this.extractBackground(g, mx, my, delta);
		float e = ease();
		scroll += (scrollTarget - scroll) * 0.35;
		g.fill(0, 0, width, height, Theme.alpha(0x900A0812, e));

		g.pose().pushMatrix();
		float s = 0.94f + 0.06f * e;
		g.pose().translate(width / 2f, height / 2f);
		g.pose().scale(s, s);
		g.pose().translate(-width / 2f, -height / 2f);

		// panel + glow line
		Theme.panel(g, px, py, px + pw, py + ph, 0xF20E0C16, 0x30FFFFFF);
		g.fillGradient(px + 1, py, px + pw - 1, py + 1, Theme.ACCENT, 0xFF2EE6D6);

		// sidebar
		g.fill(px + 1, py + 1, px + SIDE, py + ph - 1, 0xFF0A0910);
		g.text(font, "REMI", px + 12, py + 12, Theme.TEXT, false);
		Theme.small(g, "CLIENT", px + 12 + font.width("REMI") + 4, py + 13, Theme.ACCENT, 0.75f);
		int cy = py + TOP + 6;
		for (String c : Modules.CATEGORIES) {
			boolean on = c.equals(category) && settingsFor == null;
			boolean hover = mx >= px + 6 && mx < px + SIDE - 6 && my >= cy && my < cy + 18;
			if (on) { Theme.panel(g, px + 6, cy, px + SIDE - 6, cy + 18, 0x30A77BFF, 0); g.fill(px + 6, cy + 3, px + 8, cy + 15, Theme.ACCENT); }
			else if (hover) Theme.panel(g, px + 6, cy, px + SIDE - 6, cy + 18, 0x14FFFFFF, 0);
			g.text(font, c, px + 14, cy + 5, on ? Theme.TEXT : hover ? 0xFFD8D2EA : Theme.MUTED, false);
			cy += 22;
		}
		int en = 0; for (Module m : Modules.ALL) if (m.on()) en++;
		Theme.small(g, en + " of " + Modules.ALL.size() + " on", px + 12, py + ph - 26, Theme.DIM, 0.6f);
		Theme.small(g, "Right Shift to close", px + 12, py + ph - 16, Theme.DIM, 0.6f);

		// top bar: search + Edit HUD
		int sx = px + SIDE + 14;
		Theme.panel(g, sx, py + 6, sx + 168, py + 26, 0xFF14111E, searchBox.isFocused() ? Theme.alpha(Theme.ACCENT, 0.8f) : 0x24FFFFFF);
		g.text(font, "⌕", sx + 5, py + 12, Theme.DIM, false);
		int bx2 = px + pw - 10, bx1 = bx2 - 96;
		boolean hb = mx >= bx1 && mx < bx2 && my >= py + 6 && my < py + 26;
		Theme.panel(g, bx1, py + 6, bx2, py + 26, hb ? 0xFF8B5CF6 : 0xFF6C3CF0, 0);
		Theme.centered(g, "Edit HUD Layout", (bx1 + bx2) / 2, py + 12, 0xFFFFFFFF);

		if (settingsFor != null) drawSettings(g, mx, my);
		else drawGrid(g, mx, my);

		g.pose().popMatrix();
		super.extractRenderState(g, mx, my, delta); // the search box
	}

	private int[] tileAt(int i) {
		int c = cols(), gx = px + SIDE + 12, gy = py + TOP + 4 - (int) scroll;
		return new int[] { gx + (i % c) * (tw() + GAP), gy + (i / c) * (TILE_H + GAP) };
	}

	private void drawGrid(GuiGraphicsExtractor g, int mx, int my) {
		List<Module> list = shown();
		int top = py + TOP, bottom = py + ph - 6;
		g.enableScissor(px + SIDE + 1, top, px + pw - 1, bottom);
		float t = Theme.time();
		for (int i = 0; i < list.size(); i++) {
			Module m = list.get(i);
			int[] p = tileAt(i);
			int x = p[0], y = p[1];
			if (y > bottom || y + TILE_H < top) continue;
			float appear = Math.min(1f, Math.max(0f, (System.currentTimeMillis() - openedAt - i * 25) / 220f));
			y += (int) ((1 - appear) * 10);
			boolean hover = mx >= x && mx < x + tw() && my >= y && my < y + TILE_H && my > top && my < bottom;
			boolean on = m.on();
			Theme.panel(g, x, y, x + tw(), y + TILE_H, Theme.alpha(hover ? 0xFF1C1828 : 0xFF15121F, appear), Theme.alpha(hover ? Theme.alpha(m.accent, 0.8f) : 0x22FFFFFF, appear));
			// icon
			Theme.disc(g, x + 16, y + 16, 9, Theme.alpha(Theme.mix(m.accent, 0xFF000000, 0.55f), appear));
			Theme.ring(g, x + 16, y + 16, 9, 1f, Theme.alpha(m.accent, appear * (on ? 1f : 0.4f)), 0);
			Theme.centered(g, m.glyph, x + 16, y + 12, Theme.alpha(on ? m.accent : Theme.MUTED, appear));
			g.text(font, m.name, x + 30, y + 8, Theme.alpha(Theme.TEXT, appear), false);
			Theme.small(g, m.category.toUpperCase(), x + 30, y + 19, Theme.alpha(Theme.DIM, appear), 0.55f);
			// description, two short lines
			String[] words = m.desc.split(" ");
			StringBuilder l1 = new StringBuilder(), l2 = new StringBuilder();
			for (String w : words) {
				StringBuilder target = (font.width(l1 + " " + w) * 0.6f < tw() - 14 && l2.length() == 0) ? l1 : l2;
				if (target.length() > 0) target.append(' ');
				target.append(w);
			}
			Theme.small(g, l1.toString(), x + 7, y + 32, Theme.alpha(Theme.MUTED, appear), 0.6f);
			String second = l2.toString();
			while (font.width(second) * 0.6f > tw() - 14 && second.length() > 3) second = second.substring(0, second.length() - 4) + "...";
			Theme.small(g, second, x + 7, y + 40, Theme.alpha(Theme.MUTED, appear), 0.6f);
			// gear
			boolean gear = m.movable || m.hasColor() || m.id.equals("crosshair");
			if (gear) {
				boolean hg = mx >= x + tw() - 18 && mx < x + tw() - 4 && my >= y + 4 && my < y + 18;
				g.text(font, "⚙", x + tw() - 15, y + 6, hg ? Theme.TEXT : Theme.DIM, false);
			}
			// the big ENABLED / DISABLED button
			int by = y + TILE_H - 20;
			boolean hbtn = mx >= x + 6 && mx < x + tw() - 6 && my >= by && my < by + 14;
			int col = on ? (hbtn ? 0xFF3FBF78 : 0xFF2FA866) : (hbtn ? 0xFFB83A4A : 0xFF8E2A38);
			Theme.panel(g, x + 6, by, x + tw() - 6, by + 14, Theme.alpha(col, appear), 0);
			if (on) g.fill(x + 7, by, x + tw() - 7, by + 1, Theme.alpha(0x60FFFFFF, appear));
			Theme.centered(g, on ? "ENABLED" : "DISABLED", x + tw() / 2, by + 3, Theme.alpha(0xFFFFFFFF, appear));
			if (on && m.accent != 0) g.fill(x + 6, by + 13, x + 6 + (int) ((tw() - 12) * ((Math.sin(t * 2 + i) + 1) / 2) * 0), by + 14, 0);
		}
		g.disableScissor();
		if (list.isEmpty()) Theme.centered(g, "Nothing matches \"" + search + "\"", px + SIDE + (pw - SIDE) / 2, py + ph / 2, Theme.MUTED);
	}

	// ------------------------------------------------------------ one module's settings
	private int settingsRowY(int i) { return py + TOP + 34 + i * 26; }

	private void drawSettings(GuiGraphicsExtractor g, int mx, int my) {
		Module m = settingsFor;
		RemiConfig.ModuleCfg c = m.cfg();
		int x = px + SIDE + 14, w = pw - SIDE - 28;
		boolean hb = mx >= x && mx < x + 50 && my >= py + TOP + 4 && my < py + TOP + 20;
		g.text(font, "← Back", x, py + TOP + 8, hb ? Theme.TEXT : Theme.MUTED, false);
		g.text(font, m.name, x + 60, py + TOP + 8, m.accent, false);
		int row = 0;
		// enabled
		toggleRow(g, mx, my, x, settingsRowY(row++), w, "Enabled", c.enabled);
		if (m.movable) {
			// scale slider
			int y = settingsRowY(row++);
			g.text(font, "Size", x, y + 5, Theme.TEXT, false);
			int sx = x + 90, sw = w - 140;
			g.fill(sx, y + 8, sx + sw, y + 10, 0xFF2A2440);
			float f = (c.scale - 0.5f) / 1.5f;
			g.fill(sx, y + 8, sx + (int) (sw * f), y + 10, Theme.ACCENT);
			Theme.disc(g, sx + (int) (sw * f), y + 9, 4, 0xFFFFFFFF);
			g.text(font, Math.round(c.scale * 100) + "%", sx + sw + 10, y + 5, Theme.MUTED, false);
		}
		if (m.hasBackground()) toggleRow(g, mx, my, x, settingsRowY(row++), w, "Background", c.background);
		if (m.hasColor()) {
			int y = settingsRowY(row++);
			g.text(font, "Colour", x, y + 5, Theme.TEXT, false);
			for (int i = 0; i < SWATCHES.length; i++) {
				int sx = x + 90 + i * 18;
				Theme.panel(g, sx, y + 1, sx + 14, y + 15, SWATCHES[i], SWATCHES[i] == c.color ? 0xFFFFFFFF : 0x30FFFFFF);
			}
		}
		if (m.id.equals("crosshair")) {
			int y = settingsRowY(row++);
			button(g, mx, my, x, y, 150, "Open crosshair editor", true);
			CrosshairModule.drawGrid(g, x + 170, y - 2, 1, RemiConfig.get().crosshair, false);
			toggleRow(g, mx, my, x, settingsRowY(row++), w, "Turn red when aiming at a mob or player", RemiConfig.get().crosshairTargetColor);
		}
		if (m.movable) {
			int y = settingsRowY(row++);
			button(g, mx, my, x, y, 110, "Reset position", false);
			button(g, mx, my, x + 118, y, 110, "Edit HUD Layout", true);
		}
		Theme.small(g, m.desc, x, py + ph - 18, Theme.DIM, 0.7f);
	}

	private void toggleRow(GuiGraphicsExtractor g, int mx, int my, int x, int y, int w, String label, boolean on) {
		g.text(font, label, x, y + 5, Theme.TEXT, false);
		int tx = x + w - 30;
		Theme.panel(g, tx, y + 2, tx + 26, y + 14, on ? 0xFF2FA866 : 0xFF2A2440, 0);
		Theme.disc(g, on ? tx + 20 : tx + 6, y + 8, 4, 0xFFFFFFFF);
	}

	private void button(GuiGraphicsExtractor g, int mx, int my, int x, int y, int w, String label, boolean primary) {
		boolean h = mx >= x && mx < x + w && my >= y && my < y + 18;
		Theme.panel(g, x, y, x + w, y + 18, primary ? (h ? 0xFF8B5CF6 : 0xFF6C3CF0) : (h ? 0xFF24202F : 0xFF1A1626), primary ? 0 : 0x30FFFFFF);
		Theme.centered(g, label, x + w / 2, y + 5, 0xFFFFFFFF);
	}

	private static boolean in(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }

	// ------------------------------------------------------------ input
	@Override
	public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
		double mx = e.x(), my = e.y();
		// sidebar categories
		int cy = py + TOP + 6;
		for (String c : Modules.CATEGORIES) {
			if (in(mx, my, px + 6, cy, SIDE - 12, 18)) { category = c; settingsFor = null; scrollTarget = 0; click(); return true; }
			cy += 22;
		}
		// Edit HUD Layout
		if (in(mx, my, px + pw - 106, py + 6, 96, 20)) { openEditor(); return true; }
		if (settingsFor != null) return settingsClick(mx, my, e) || super.mouseClicked(e, doubleClick);
		List<Module> list = shown();
		for (int i = 0; i < list.size(); i++) {
			Module m = list.get(i);
			int[] p = tileAt(i);
			int x = p[0], y = p[1];
			if (y + TILE_H < py + TOP || y > py + ph) continue;
			if (in(mx, my, x + 6, y + TILE_H - 20, tw() - 12, 14)) { m.cfg().enabled = !m.cfg().enabled; RemiConfig.save(); click(); return true; }
			if (in(mx, my, x + tw() - 18, y + 4, 14, 14) || (in(mx, my, x, y, tw(), TILE_H - 22) && doubleClick)) { settingsFor = m; click(); return true; }
			if (in(mx, my, x, y, tw(), TILE_H)) { settingsFor = m; click(); return true; }
		}
		return super.mouseClicked(e, doubleClick);
	}

	private boolean settingsClick(double mx, double my, MouseButtonEvent e) {
		Module m = settingsFor;
		RemiConfig.ModuleCfg c = m.cfg();
		int x = px + SIDE + 14, w = pw - SIDE - 28;
		if (in(mx, my, x, py + TOP + 4, 50, 16)) { settingsFor = null; click(); return true; }
		int row = 0;
		if (in(mx, my, x, settingsRowY(row++), w, 18)) { c.enabled = !c.enabled; RemiConfig.save(); click(); return true; }
		if (m.movable) {
			int y = settingsRowY(row++);
			if (in(mx, my, x + 86, y, w - 132, 18)) { draggingScale = true; setScale(mx); return true; }
		}
		if (m.hasBackground()) if (in(mx, my, x, settingsRowY(row++), w, 18)) { c.background = !c.background; RemiConfig.save(); click(); return true; }
		if (m.hasColor()) {
			int y = settingsRowY(row++);
			for (int i = 0; i < SWATCHES.length; i++) if (in(mx, my, x + 90 + i * 18, y, 14, 16)) { c.color = SWATCHES[i]; RemiConfig.save(); click(); return true; }
		}
		if (m.id.equals("crosshair")) {
			if (in(mx, my, x, settingsRowY(row++), 150, 18)) { minecraft.setScreen(new CrosshairEditorScreen(this)); return true; }
			if (in(mx, my, x, settingsRowY(row++), w, 18)) { RemiConfig.get().crosshairTargetColor = !RemiConfig.get().crosshairTargetColor; RemiConfig.save(); click(); return true; }
		}
		if (m.movable) {
			int y = settingsRowY(row++);
			if (in(mx, my, x, y, 110, 18)) { c.x = -1; c.y = -1; c.scale = 1f; RemiConfig.save(); click(); return true; }
			if (in(mx, my, x + 118, y, 110, 18)) { openEditor(); return true; }
		}
		return false;
	}

	private void setScale(double mx) {
		int x = px + SIDE + 14, w = pw - SIDE - 28, sx = x + 90, sw = w - 140;
		float f = (float) Math.max(0, Math.min(1, (mx - sx) / sw));
		settingsFor.cfg().scale = Math.round((0.5f + f * 1.5f) * 20) / 20f;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
		if (draggingScale && settingsFor != null) { setScale(e.x()); return true; }
		return super.mouseDragged(e, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent e) {
		if (draggingScale) { draggingScale = false; RemiConfig.save(); return true; }
		return super.mouseReleased(e);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double sx, double sy) {
		if (settingsFor == null) {
			int rows = (shown().size() + cols() - 1) / cols();
			double max = Math.max(0, rows * (TILE_H + GAP) - (ph - TOP - 10));
			scrollTarget = Math.max(0, Math.min(max, scrollTarget - sy * 30));
			return true;
		}
		return super.mouseScrolled(mx, my, sx, sy);
	}

	@Override
	public boolean keyPressed(KeyEvent e) {
		if (e.key() == GLFW.GLFW_KEY_RIGHT_SHIFT && !searchBox.isFocused()) { onClose(); return true; }
		if (e.key() == GLFW.GLFW_KEY_ESCAPE && settingsFor != null) { settingsFor = null; return true; }
		return super.keyPressed(e);
	}

	@Override
	public void onClose() { RemiConfig.save(); super.onClose(); }

	private void openEditor() { RemiConfig.save(); minecraft.setScreen(new HudEditorScreen(this)); }

	private void click() {
		try { minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f)); } catch (Throwable ignored) { }
	}
}
