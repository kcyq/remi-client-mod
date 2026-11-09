package dev.kcyq.remi.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Drag your HUD pieces anywhere. They snap to the centre lines; scroll on one to resize it; right-click resets it. */
public class HudEditorScreen extends Screen {
	private final Screen back;
	private Module dragging = null;
	private double grabX, grabY;
	private boolean snapX, snapY;

	public HudEditorScreen(Screen back) { super(Component.literal("Edit HUD Layout")); this.back = back; }

	@Override public boolean isPauseScreen() { return false; }

	private int[] box(Module m) {
		return m.place(width, height);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
		g.fill(0, 0, width, height, 0x40000000);
		// centre guides
		g.fill(width / 2, 0, width / 2 + 1, height, snapX && dragging != null ? 0xC0A77BFF : 0x18FFFFFF);
		g.fill(0, height / 2, width, height / 2 + 1, snapY && dragging != null ? 0xC0A77BFF : 0x18FFFFFF);
		for (Module m : Modules.ALL) {
			if (!m.movable || !m.on()) continue;
			int[] b = box(m);
			boolean hover = mx >= b[0] && mx < b[0] + b[2] && my >= b[1] && my < b[1] + b[3];
			if (m.visible()) m.render(g, true);
			else {
				Theme.panel(g, b[0], b[1], b[0] + b[2], b[1] + b[3], 0x60171320, 0);
				Theme.centered(g, m.name, b[0] + b[2] / 2, b[1] + b[3] / 2 - 4, Theme.MUTED);
			}
			int col = m == dragging ? Theme.ACCENT : hover ? 0xC0FFFFFF : 0x50FFFFFF;
			dashed(g, b[0] - 1, b[1] - 1, b[0] + b[2] + 1, b[1] + b[3] + 1, col);
			if (hover || m == dragging) Theme.small(g, m.name + "  " + Math.round(m.cfg().scale * 100) + "%", b[0], b[1] - 8, Theme.TEXT, 0.6f);
		}
		Theme.panel(g, width / 2 - 150, height - 30, width / 2 + 150, height - 8, Theme.BG, Theme.LINE);
		Theme.centered(g, "Drag to move · Scroll to resize · Right-click to reset · Esc when done", width / 2, height - 23, Theme.MUTED);
		super.extractRenderState(g, mx, my, delta);
	}

	private void dashed(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int c) {
		for (int x = x1; x < x2; x += 4) { g.fill(x, y1, Math.min(x + 2, x2), y1 + 1, c); g.fill(x, y2 - 1, Math.min(x + 2, x2), y2, c); }
		for (int y = y1; y < y2; y += 4) { g.fill(x1, y, x1 + 1, Math.min(y + 2, y2), c); g.fill(x2 - 1, y, x2, Math.min(y + 2, y2), c); }
	}

	private Module at(double mx, double my) {
		for (int i = Modules.ALL.size() - 1; i >= 0; i--) {
			Module m = Modules.ALL.get(i);
			if (!m.movable || !m.on()) continue;
			int[] b = box(m);
			if (mx >= b[0] && mx < b[0] + b[2] && my >= b[1] && my < b[1] + b[3]) return m;
		}
		return null;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
		Module m = at(e.x(), e.y());
		if (m == null) return super.mouseClicked(e, doubleClick);
		if (e.button() == 1) { m.cfg().x = -1; m.cfg().y = -1; m.cfg().scale = 1f; RemiConfig.save(); return true; }
		int[] b = box(m);
		dragging = m; grabX = e.x() - b[0]; grabY = e.y() - b[1];
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
		if (dragging == null) return super.mouseDragged(e, dx, dy);
		int[] b = box(dragging);
		double x = e.x() - grabX, y = e.y() - grabY;
		// snap the centre of the piece to the screen's centre lines
		snapX = Math.abs(x + b[2] / 2.0 - width / 2.0) < 5;
		snapY = Math.abs(y + b[3] / 2.0 - height / 2.0) < 5;
		if (snapX) x = width / 2.0 - b[2] / 2.0;
		if (snapY) y = height / 2.0 - b[3] / 2.0;
		x = Math.max(0, Math.min(width - b[2], x));
		y = Math.max(0, Math.min(height - b[3], y));
		dragging.cfg().x = (float) (x / width);
		dragging.cfg().y = (float) (y / height);
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent e) {
		if (dragging != null) { dragging = null; snapX = snapY = false; RemiConfig.save(); return true; }
		return super.mouseReleased(e);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double sx, double sy) {
		Module m = at(mx, my);
		if (m == null) return false;
		m.cfg().scale = Math.max(0.5f, Math.min(2f, Math.round((m.cfg().scale + (sy > 0 ? 0.05f : -0.05f)) * 20) / 20f));
		RemiConfig.save();
		return true;
	}

	@Override
	public void onClose() { RemiConfig.save(); minecraft.setScreen(back); }
}
