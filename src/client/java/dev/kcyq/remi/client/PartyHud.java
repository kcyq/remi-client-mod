package dev.kcyq.remi.client;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.world.BossEvent;

/** Party frames (in dungeons) and Remi-styled boss bars (Wardens, Mythic bosses, events). */
public final class PartyHud {
	private PartyHud() {}

	public static void party(GuiGraphicsExtractor g, DeltaTracker dt) {
		Minecraft mc = Minecraft.getInstance();
		if (!RemiClient.hudOn || !RemiState.live() || !RemiState.inDungeon || mc.options.hideGui) return;
		synchronized (RemiState.class) {
			if (RemiState.party.isEmpty()) return;
			int w = 112, rowH = 20, n = RemiState.party.size();
			int x = 6, y = g.guiHeight() / 2 - (n * rowH) / 2;
			Theme.small(g, "PARTY", x + 2, y - 9, Theme.MUTED, 0.6f);
			for (RemiState.Member m : RemiState.party) {
				int col = Theme.gem(m.gem);
				Theme.panel(g, x, y, x + w, y + rowH - 3, Theme.BG_SOFT, m.down ? Theme.alpha(Theme.BAD, 0.8f) : 0x30FFFFFF);
				g.fill(x + 1, y + 1, x + 3, y + rowH - 4, col);
				Theme.panel(g, x + 6, y + 3, x + 17, y + 14, Theme.mix(col, 0xFF000000, 0.3f), 0);
				Theme.centered(g, m.name.substring(0, 1).toUpperCase(), x + 12, y + 5, 0xFF0C0A12);
				Theme.small(g, m.name, x + 21, y + 3, m.down ? Theme.BAD : Theme.TEXT, 0.7f);
				float frac = (float) (m.hp / m.max);
				int hpCol = m.down ? Theme.BAD : Theme.mix(0xFFFF5C6C, 0xFF4FD18B, frac);
				Theme.bar(g, x + 21, y + 11, w - 26, 3, m.down ? 0 : frac, hpCol, false);
				String hp = m.down ? "DOWN" : (int) Math.ceil(m.hp) + "";
				Theme.small(g, hp, x + w - 4 - (int) (Theme.font().width(hp) * 0.55f), y + 3, m.down ? Theme.BAD : Theme.MUTED, 0.55f);
				y += rowH;
			}
		}
	}

	// ---- boss bars: vanilla keeps them in a private map, so we read it once by type
	private static Field eventsField;

	@SuppressWarnings("unchecked")
	private static Collection<LerpingBossEvent> events() {
		try {
			BossHealthOverlay o = Minecraft.getInstance().gui.getBossOverlay();
			if (eventsField == null) {
				for (Field f : BossHealthOverlay.class.getDeclaredFields()) {
					if (Map.class.isAssignableFrom(f.getType())) { f.setAccessible(true); eventsField = f; break; }
				}
			}
			if (eventsField == null) return null;
			return ((Map<?, LerpingBossEvent>) eventsField.get(o)).values();
		} catch (Throwable e) {
			return null;
		}
	}

	private static int color(BossEvent.BossBarColor c) {
		return switch (c) {
			case PINK -> 0xFFFF6FD8; case BLUE -> 0xFF4F9BFF; case RED -> 0xFFFF4D5E; case GREEN -> 0xFF4FD18B;
			case YELLOW -> 0xFFFFD34D; case PURPLE -> 0xFFA77BFF; default -> 0xFFE8E4F2;
		};
	}

	/** Returns false if it couldn't draw, so the normal boss bars show instead. */
	public static boolean bosses(GuiGraphicsExtractor g) {
		Collection<LerpingBossEvent> list = events();
		if (list == null) return false;
		int w = Math.min(240, g.guiWidth() - 40), x = g.guiWidth() / 2 - w / 2, y = 8;
		for (LerpingBossEvent e : list) {
			int col = color(e.getColor());
			String name = e.getName().getString();
			float p = e.getProgress();
			Theme.panel(g, x - 4, y - 2, x + w + 4, y + 21, Theme.BG_SOFT, Theme.alpha(col, 0.35f));
			Theme.centered(g, name, x + w / 2, y + 1, 0xFFFFFFFF);
			Theme.bar(g, x, y + 12, w, 5, p, col, true);
			String pct = Math.round(p * 100) + "%";
			Theme.small(g, pct, x + w - (int) (Theme.font().width(pct) * 0.55f), y + 18, Theme.MUTED, 0.55f);
			y += 28;
			if (y > g.guiHeight() / 3) break;
		}
		return true;
	}
}
