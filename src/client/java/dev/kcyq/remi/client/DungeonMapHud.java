package dev.kcyq.remi.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The Remi dungeon map, top right. Remi dungeons are a chain of rooms 48 blocks apart, so the map lays them out
 * as a winding path: cleared rooms get a check, the current room pulses, rooms you haven't reached stay "?" until
 * you walk in (the boss room always shows its skull). Party members are dots that move along it live.
 */
public final class DungeonMapHud {
	private DungeonMapHud() {}

	static final int CELL = 20, GAP = 7;

	static int cols(int total) { return total > 12 ? 5 : 4; }

	/** Centre of room i on the map, relative to the map's top left. */
	static int[] cell(int i, int cols) {
		int row = i / cols, c = i % cols;
		if (row % 2 == 1) c = cols - 1 - c;
		return new int[] { c * (CELL + GAP) + CELL / 2, row * (CELL + GAP) + CELL / 2 };
	}

	static String glyph(String arch) {
		if (arch == null) return "?";
		if (arch.equals("boss")) return "☠";
		if (arch.startsWith("m_")) return "✦";
		return switch (arch) {
			case "purge" -> "P"; case "champion" -> "C"; case "hold" -> "H"; case "sigils" -> "S"; case "echo" -> "E";
			case "crossing" -> "X"; case "gauntlet" -> "G"; case "braziers" -> "B"; case "totems" -> "T"; case "start" -> "◆";
			default -> arch.substring(0, 1).toUpperCase();
		};
	}

	static String archName(String arch) {
		if (arch == null) return "";
		if (arch.equals("boss")) return "Boss";
		if (arch.startsWith("m_")) return "Mythic trial";
		return Theme.cap(arch);
	}

	static boolean visible() { return RemiState.live() && RemiState.inDungeon; }
	static int width() { int total = Math.max(2, RemiState.dgRooms + 1), cols = cols(total); return cols * CELL + (cols - 1) * GAP + 14; }
	static int height() { int total = Math.max(2, RemiState.dgRooms + 1), cols = cols(total), rows = (total + cols - 1) / cols; return rows * CELL + (rows - 1) * GAP + 34; }

	public static void draw(GuiGraphicsExtractor g, int x1, int y1) {
		Minecraft mc = Minecraft.getInstance();
		if (!visible()) return;
		synchronized (RemiState.class) {
			int total = RemiState.dgRooms + 1, cols = cols(total);
			int pw = width(), ph = height();
			int ox = x1 + 7, oy = y1 + 24;
			int themeCol = Theme.gem(RemiState.dgTheme);
			float t = Theme.time();

			Theme.panel(g, x1, y1, x1 + pw, y1 + ph, Theme.BG, Theme.LINE);
			g.fill(x1 + 1, y1, x1 + pw - 1, y1 + 1, themeCol);
			String title = Theme.cap(RemiState.dgTheme) + " Rift";
			Theme.small(g, title.toUpperCase(), x1 + 7, y1 + 5, themeCol, 0.7f);
			String cur = RemiState.dgCurrent >= RemiState.dgRooms ? "FINAL" : RemiState.dgCurrent + "/" + RemiState.dgRooms;
			String right = RemiState.dgStage.equals("won") ? "CLEARED" : "Room " + cur;
			Theme.small(g, right, x1 + pw - 7 - (int) (Theme.font().width(right) * 0.6f), y1 + 6, Theme.MUTED, 0.6f);
			RemiState.Room now = RemiState.rooms.get(RemiState.dgCurrent);
			if (now != null) Theme.small(g, archName(now.archetype) + (RemiState.dgRoomState.equals("run") ? " · in progress" : RemiState.dgRoomState.equals("done") ? " · clear, move on" : ""), x1 + 7, y1 + 14, Theme.DIM, 0.55f);

			// corridors
			for (int i = 0; i + 1 < total; i++) {
				int[] a = cell(i, cols), b = cell(i + 1, cols);
				int col = i < RemiState.dgCurrent ? Theme.alpha(Theme.OK, 0.55f) : 0x40FFFFFF;
				int ax = ox + a[0], ay = oy + a[1], bx = ox + b[0], by = oy + b[1];
				g.fill(Math.min(ax, bx), Math.min(ay, by) - 1, Math.max(ax, bx) + 1, Math.max(ay, by) + 1, col);
			}
			// rooms
			for (int i = 0; i < total; i++) {
				int[] c = cell(i, cols);
				int rx = ox + c[0] - CELL / 2, ry = oy + c[1] - CELL / 2;
				RemiState.Room r = RemiState.rooms.get(i);
				String arch = i == 0 ? "start" : r == null ? null : r.archetype;
				int rc = arch != null && arch.equals("boss") ? 0xFFFF4D5E : arch != null && arch.startsWith("m_") ? 0xFFFF5CE1 : r != null ? Theme.gem(r.theme) : themeCol;
				boolean cleared = i < RemiState.dgCurrent || RemiState.dgStage.equals("won");
				boolean current = i == RemiState.dgCurrent && !RemiState.dgStage.equals("won");
				boolean known = cleared || current || i == 0 || (arch != null && arch.equals("boss"));
				int fillCol = cleared ? Theme.mix(rc, 0xFF0C0A12, 0.55f) : current ? Theme.mix(rc, 0xFF0C0A12, 0.25f) : 0xFF1A1626;
				Theme.panel(g, rx, ry, rx + CELL, ry + CELL, fillCol, current ? Theme.alpha(rc, 0.6f + 0.4f * (float) Math.sin(t * 5)) : known ? Theme.alpha(rc, 0.7f) : 0x30FFFFFF);
				String gl = known ? glyph(arch) : "?";
				Theme.centered(g, gl, rx + CELL / 2, ry + CELL / 2 - 4, known ? Theme.TEXT : Theme.DIM);
				if (cleared && i > 0) Theme.small(g, "✔", rx + CELL - 6, ry + 1, Theme.OK, 0.6f);
				if (current && RemiState.dgRoomState.equals("run")) Theme.ring(g, rx + CELL / 2, ry + CELL / 2, CELL / 2 + 2, (t * 0.6f) % 1f, Theme.alpha(rc, 0.9f), 0);
			}
			// party, moving along the path
			String me = mc.player != null ? mc.player.getName().getString() : "";
			for (RemiState.Member m : RemiState.party) {
				double p = Math.max(0, Math.min(total - 1, m.dz / 48.0));
				int r0 = (int) Math.floor(p);
				double f = p - r0;
				int[] a = cell(r0, cols), b = cell(Math.min(total - 1, r0 + 1), cols);
				double px = a[0] + (b[0] - a[0]) * f, py = a[1] + (b[1] - a[1]) * f;
				// sideways in the room: across the path
				boolean horizontal = a[1] == b[1];
				double side = Math.max(-1, Math.min(1, m.dx / 16.0)) * (CELL / 2.0 - 2);
				if (horizontal) py += side; else px += side;
				int dx = ox + (int) Math.round(px), dy = oy + (int) Math.round(py);
				boolean self = m.name.equalsIgnoreCase(me);
				int col = m.down ? Theme.BAD : self ? 0xFFFFFFFF : Theme.gem(m.gem);
				if (self) Theme.disc(g, dx, dy, 3, 0xFF0C0A12);
				Theme.disc(g, dx, dy, 2, col);
				if (m.down) Theme.small(g, "✖", dx - 2, dy - 3, Theme.BAD, 0.6f);
			}
			Theme.small(g, "M: full map", x1 + 7, y1 + ph - 8, Theme.DIM, 0.5f);
		}
	}
}
