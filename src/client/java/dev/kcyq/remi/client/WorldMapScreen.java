package dev.kcyq.remi.client;

import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** The Remi map (M): everything you've explored, waypoints, guild members and your party. Drag to pan, scroll to zoom. */
public class WorldMapScreen extends Screen {
	private double cx, cz;          // world position at the centre of the screen
	private double zoom = 2.0;      // screen pixels per block
	private boolean follow = true;
	private final long openedAt = System.currentTimeMillis();

	public WorldMapScreen() {
		super(Component.literal("Remi Map"));
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) { cx = mc.player.getX(); cz = mc.player.getZ(); }
	}

	@Override
	public boolean isPauseScreen() { return false; }

	@Override
	public boolean mouseScrolled(double mx, double my, double sx, double sy) {
		double before = zoom;
		zoom = Math.max(0.25, Math.min(8, zoom * (sy > 0 ? 1.25 : 0.8)));
		// zoom towards the mouse
		double wx = cx + (mx - width / 2.0) / before, wz = cz + (my - height / 2.0) / before;
		cx = wx - (mx - width / 2.0) / zoom;
		cz = wz - (my - height / 2.0) / zoom;
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		follow = false;
		cx -= dx / zoom;
		cz -= dy / zoom;
		return true;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		Minecraft mc = Minecraft.getInstance();
		float open = Math.min(1f, (System.currentTimeMillis() - openedAt) / 220f);
		float ease = 1 - (1 - open) * (1 - open) * (1 - open);
		if (follow && mc.player != null) { cx = mc.player.getX(); cz = mc.player.getZ(); }
		g.fill(0, 0, width, height, Theme.alpha(0xF00A0812, ease));

		// terrain: pick a level of detail so we never draw more than ~15k rectangles
		int step = zoom >= 3 ? 1 : zoom >= 1.5 ? 2 : zoom >= 0.75 ? 4 : 8;
		double half = width / 2.0 / zoom, halfZ = height / 2.0 / zoom;
		int minCx = (int) Math.floor((cx - half) / 16) - 1, maxCx = (int) Math.floor((cx + half) / 16) + 1;
		int minCz = (int) Math.floor((cz - halfZ) / 16) - 1, maxCz = (int) Math.floor((cz + halfZ) / 16) + 1;
		synchronized (WorldMap.class) {
			for (int chx = minCx; chx <= maxCx; chx++) {
				for (int chz = minCz; chz <= maxCz; chz++) {
					int[] c = WorldMap.chunks.get(WorldMap.key(chx, chz));
					if (c == null) continue;
					for (int lz = 0; lz < 16; lz += step) {
						for (int lx = 0; lx < 16; lx += step) {
							int col = c[lz * 16 + lx];
							if (col == 0) continue;
							int x1 = sx(chx * 16 + lx), z1 = sz(chz * 16 + lz), x2 = sx(chx * 16 + lx + step), z2 = sz(chz * 16 + lz + step);
							if (x2 < 0 || z2 < 0 || x1 > width || z1 > height) continue;
							g.fill(x1, z1, Math.max(x1 + 1, x2), Math.max(z1 + 1, z2), Theme.alpha(col, ease));
						}
					}
				}
			}
		}
		// a soft vignette so the edges fade into the dark
		for (int i = 0; i < 24; i++) {
			int a = Theme.alpha(0xFF0A0812, (24 - i) / 24f * 0.55f);
			g.fill(0, i, width, i + 1, a); g.fill(0, height - i - 1, width, height - i, a);
			g.fill(i, 0, i + 1, height, a); g.fill(width - i - 1, 0, width - i, height, a);
		}

		// waypoints
		float t = Theme.time();
		String hover = null;
		synchronized (RemiState.class) {
			for (RemiState.Waypoint w : RemiState.waypoints) {
				int x = sx(w.x), z = sz(w.z);
				if (x < -20 || z < -20 || x > width + 20 || z > height + 20) continue;
				boolean guild = w.kind.equals("guild");
				Theme.disc(g, x, z, guild ? 3 : 5, 0xFF0C0A12);
				Theme.disc(g, x, z, guild ? 2 : 4, w.color);
				if (!guild) Theme.ring(g, x, z, 7 + (int) (Math.sin(t * 3) * 1.5), 1f, Theme.alpha(w.color, 0.5f), 0);
				if (Math.abs(mouseX - x) < 8 && Math.abs(mouseY - z) < 8) hover = w.name + "  " + (int) w.x + ", " + (int) w.z;
				else if (zoom >= 1 || !guild) Theme.smallCentered(g, w.name, x, z + 8, Theme.TEXT, 0.6f);
			}
			// party in a dungeon is on another map, but guild members and you are here
		}
		// you: an arrow pointing where you look
		if (mc.player != null) {
			int px = sx(mc.player.getX()), pz = sz(mc.player.getZ());
			double yaw = Math.toRadians(mc.player.getYRot() + 180);
			Theme.disc(g, px, pz, 5, 0xFF0C0A12);
			Theme.disc(g, px, pz, 4, 0xFFFFFFFF);
			for (int i = 3; i < 11; i++) {
				int ax = px - (int) Math.round(Math.sin(yaw) * i), az = pz + (int) Math.round(Math.cos(yaw) * i);
				g.fill(ax - 1, az - 1, ax + 1, az + 1, Theme.ACCENT);
			}
		}

		// header + help
		Theme.panel(g, 10, 10, 190, 40, Theme.BG, Theme.LINE);
		g.fill(11, 10, 189, 11, Theme.ACCENT);
		Theme.text(g, "Remi Map", 18, 16, Theme.TEXT);
		String pos = mc.player == null ? "" : (int) mc.player.getX() + ", " + (int) mc.player.getY() + ", " + (int) mc.player.getZ();
		Theme.small(g, pos + "   ·   " + WorldMap.chunks.size() + " chunks explored", 18, 28, Theme.MUTED, 0.6f);
		String help = (follow ? "Following you" : "Drag to look around") + " · Scroll to zoom · Esc to close";
		Theme.smallCentered(g, help, width / 2, height - 14, Theme.DIM, 0.65f);
		if (hover != null) {
			int w = Theme.font().width(hover) + 10;
			Theme.panel(g, mouseX + 10, mouseY - 4, mouseX + 10 + w, mouseY + 10, Theme.BG, Theme.LINE);
			Theme.text(g, hover, mouseX + 15, mouseY - 1, Theme.TEXT);
		}
		super.extractRenderState(g, mouseX, mouseY, delta);
	}

	private int sx(double wx) { return (int) Math.floor(width / 2.0 + (wx - cx) * zoom); }
	private int sz(double wz) { return (int) Math.floor(height / 2.0 + (wz - cz) * zoom); }
}
