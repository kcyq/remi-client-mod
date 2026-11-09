package dev.kcyq.remi.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** A thin compass across the top of the screen: N/E/S/W plus every Remi waypoint, with distances. */
public final class CompassHud {
	private CompassHud() {}

	static boolean visible() { return RemiState.live(); }
	static int width() { return 260; }
	static int height() { return 24; }

	public static void draw(GuiGraphicsExtractor g, int x1, int y) {
		Minecraft mc = Minecraft.getInstance();
		if (!visible() || mc.player == null) return;
		int w = width(), h = 12;
		double yaw = ((mc.player.getYRot() % 360) + 360) % 360; // 0 = south in Minecraft
		Theme.panel(g, x1, y, x1 + w, y + h, Theme.BG_SOFT, 0x24FFFFFF);
		g.fill(x1 + w / 2, y + h - 3, x1 + w / 2 + 1, y + h, Theme.ACCENT);
		String[] names = { "S", "SW", "W", "NW", "N", "NE", "E", "SE" };
		for (int i = 0; i < 8; i++) draw(g, names[i], i * 45, yaw, x1, w, y, i % 2 == 0 ? Theme.TEXT : Theme.DIM, false);
		synchronized (RemiState.class) {
			for (RemiState.Waypoint wp : RemiState.waypoints) {
				if (wp.kind.equals("guild") && RemiState.waypoints.size() > 12) continue;
				double dx = wp.x - mc.player.getX(), dz = wp.z - mc.player.getZ();
				double ang = Math.toDegrees(Math.atan2(-dx, dz)); // same convention as player yaw
				int dist = (int) Math.sqrt(dx * dx + dz * dz);
				draw(g, "●", ang, yaw, x1, w, y, wp.color, true);
				int px = pos(ang, yaw, x1, w);
				if (px > 0 && Math.abs(px - (x1 + w / 2)) < 14) {
					Theme.smallCentered(g, wp.name + " · " + dist + "m", x1 + w / 2, y + h + 3, wp.color, 0.6f);
				}
			}
		}
	}

	private static int pos(double ang, double yaw, int x1, int w) {
		double d = ((ang - yaw + 540) % 360) - 180; // -180..180
		if (Math.abs(d) > 90) return -1;
		return x1 + w / 2 + (int) (d / 90 * (w / 2 - 6));
	}

	private static void draw(GuiGraphicsExtractor g, String s, double ang, double yaw, int x1, int w, int y, int col, boolean small) {
		int px = pos(ang, yaw, x1, w);
		if (px < 0) return;
		float edge = 1 - Math.abs(px - (x1 + w / 2)) / (float) (w / 2);
		Theme.smallCentered(g, s, px, y + 3, Theme.alpha(col, 0.35f + 0.65f * edge), small ? 0.6f : 0.65f);
	}
}
