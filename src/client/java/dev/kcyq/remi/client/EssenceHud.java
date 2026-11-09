package dev.kcyq.remi.client;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Your Essence, left of the hotbar: an orb in its colour, a cooldown ring per ability, the True Awakening meter. */
public final class EssenceHud {
	private EssenceHud() {}

	private static final Map<String, Long> readyFlash = new HashMap<>();
	private static final Map<String, Double> lastLeft = new HashMap<>();

	private static String key(String slot) {
		return switch (slot) { case "a" -> "RC"; case "b" -> "⇧RC"; case "c" -> "F"; case "u" -> "⇧F"; default -> slot.toUpperCase(); };
	}

	public static void render(GuiGraphicsExtractor g, DeltaTracker dt) {
		Minecraft mc = Minecraft.getInstance();
		if (!RemiClient.hudOn || !RemiState.live() || mc.options.hideGui || mc.player == null) return;
		if (RemiState.gem.isEmpty() || RemiState.gem.equals("blank")) return;
		synchronized (RemiState.class) {
			int col = Theme.gem(RemiState.gem);
			int n = RemiState.abilities.size();
			int chip = 24, w = 44 + Math.max(2, n) * chip + 6, h = 42;
			int x2 = g.guiWidth() / 2 - 91 - 8, x1 = x2 - w, y2 = g.guiHeight() - 4, y1 = y2 - h;
			if (x1 < 4) { x1 = 4; x2 = x1 + w; y2 = g.guiHeight() - 26; y1 = y2 - h; } // tiny window: sit above the hotbar
			float t = Theme.time();
			boolean awakened = RemiState.awkLeft > 0;

			Theme.panel(g, x1, y1, x2, y2, Theme.BG, awakened ? Theme.alpha(col, 0.55f + 0.35f * (float) Math.sin(t * 4)) : Theme.LINE);
			g.fill(x1 + 1, y1, x2 - 1, y1 + 1, Theme.alpha(col, 0.9f));

			// the orb
			int ox = x1 + 20, oy = y1 + 19;
			Theme.disc(g, ox, oy, 15, Theme.alpha(col, 0.10f + (awakened ? 0.12f : 0)));
			Theme.disc(g, ox, oy, 11, Theme.mix(col, 0xFF000000, 0.35f));
			Theme.disc(g, ox, oy, 9, col);
			Theme.disc(g, ox - 3, oy - 3, 3, Theme.alpha(0xFFFFFFFF, 0.45f));
			if (Theme.mythic(RemiState.gem)) Theme.ring(g, ox, oy, 13, 1f, Theme.alpha(0xFFFF8CE6, 0.85f), 0);
			if (awakened) Theme.ring(g, ox, oy, 15, (float) (RemiState.awkLeft / RemiState.awkTotal), Theme.GOLD, Theme.alpha(Theme.GOLD, 0.15f));
			String glyph = Theme.mythic(RemiState.gem) ? "✦" : RemiState.gem.substring(0, 1).toUpperCase();
			Theme.centered(g, glyph, ox, oy - 4, 0xFF0C0A12);
			Theme.smallCentered(g, "T" + RemiState.tier, ox, y2 - 9, Theme.MUTED, 0.6f);

			// name + state
			int tx = x1 + 40;
			String name = RemiState.gemName.isEmpty() ? Theme.cap(RemiState.gem) : RemiState.gemName;
			Theme.small(g, name.toUpperCase(), tx, y1 + 4, col, 0.75f);
			if (!RemiState.essState.equals("ok")) {
				String s = RemiState.essState.equals("off") ? "ABILITIES OFF" : RemiState.essState.equals("suppressed") ? "SUPPRESSED" : "";
				Theme.small(g, s, tx + (int) (Theme.font().width(name.toUpperCase()) * 0.75f) + 6, y1 + 4, Theme.BAD, 0.75f);
			}

			// one ring per ability
			for (int i = 0; i < n; i++) {
				RemiState.Ability a = RemiState.abilities.get(i);
				double left = Math.max(0, a.left - RemiState.since());
				Double prev = lastLeft.put(a.slot, left);
				if (prev != null && prev > 0.05 && left <= 0.05) readyFlash.put(a.slot, System.currentTimeMillis());
				int cx = tx + 10 + i * chip, cy = y1 + 24;
				boolean ready = left <= 0.05;
				boolean noted = a.note != null && !a.note.isEmpty();
				float frac = ready ? 1f : (float) (1 - left / a.base);
				int ringOn = noted ? Theme.GOLD : ready ? col : Theme.mix(col, 0xFF2A2440, 0.45f);
				Theme.disc(g, cx, cy, 9, ready ? Theme.alpha(col, 0.16f) : 0x66000000);
				Theme.ring(g, cx, cy, 10, frac, ringOn, 0x40FFFFFF);
				Theme.ring(g, cx, cy, 9, frac, Theme.alpha(ringOn, 0.6f), 0);
				Long fl = readyFlash.get(a.slot);
				if (fl != null) {
					float k = (System.currentTimeMillis() - fl) / 500f;
					if (k < 1) Theme.ring(g, cx, cy, 10 + (int) (k * 6), 1f, Theme.alpha(col, 1 - k), 0);
				}
				if (ready) Theme.smallCentered(g, noted ? a.note : "✔", cx, cy - 3, noted ? Theme.GOLD : Theme.TEXT, noted ? 0.5f : 0.8f);
				else Theme.smallCentered(g, Theme.secs(left), cx, cy - 3, Theme.TEXT, 0.7f);
				Theme.smallCentered(g, key(a.slot), cx, y2 - 7, Theme.DIM, 0.5f);
			}

			// True Awakening meter along the bottom edge
			float m = (float) Math.min(1, RemiState.trueMeter / 100.0);
			if (awakened) {
				Theme.bar(g, x1 + 1, y2 - 2, w - 2, 2, (float) (RemiState.awkLeft / RemiState.awkTotal), Theme.GOLD, true);
				Theme.small(g, "AWAKENED " + Theme.secs(RemiState.awkLeft) + "s", x2 - 66, y1 + 4, Theme.GOLD, 0.6f);
			} else if (m > 0) {
				Theme.bar(g, x1 + 1, y2 - 2, w - 2, 2, m, m >= 1 ? Theme.GOLD : Theme.alpha(Theme.GOLD, 0.7f), m >= 1);
				if (m >= 1) Theme.small(g, "TRUE READY", x2 - 52, y1 + 4, Theme.GOLD, 0.6f);
			}
		}
	}
}
