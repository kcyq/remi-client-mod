package dev.kcyq.remi.client;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;

/** Every module in the Remi menu. */
public final class Modules {
	private Modules() {}

	public static final List<Module> ALL = new ArrayList<>();
	public static final String[] CATEGORIES = { "All", "Remi", "HUD", "Visual" };

	public static Module byId(String id) {
		for (Module m : ALL) if (m.id.equals(id)) return m;
		return null;
	}

	/** Small helper for the simple text modules. */
	abstract static class TextModule extends Module {
		TextModule(String id, String name, String desc, String glyph, int accent) { super(id, name, desc, "HUD", glyph, accent, false, true); }
		abstract String text();
		@Override public int width() { return Math.max(40, Theme.font().width(text()) + 12); }
		@Override public int height() { return 16; }
		@Override public boolean hasColor() { return true; }
		@Override public boolean hasBackground() { return true; }
		@Override public void draw(GuiGraphicsExtractor g, int x, int y, boolean preview) {
			if (cfg().background) Theme.panel(g, x, y, x + width(), y + height(), Theme.BG_SOFT, 0x22FFFFFF);
			Theme.centered(g, text(), x + width() / 2, y + 4, cfg().color);
		}
	}

	static {
		// ---- Remi
		ALL.add(new Module("essence", "Essence HUD", "Your Essence, ability cooldown rings and the True meter.", "Remi", "✦", 0xFFA77BFF, true, true) {
			@Override public int width() { return EssenceHud.width(); }
			@Override public int height() { return EssenceHud.height(); }
			@Override public int[] defaultPos(int sw, int sh) {
				int x = sw / 2 - 91 - 8 - width();
				return x < 4 ? new int[] { 4, sh - 26 - height() } : new int[] { x, sh - 4 - height() };
			}
			@Override public boolean visible() { return EssenceHud.visible(); }
			@Override public void draw(GuiGraphicsExtractor g, int x, int y, boolean p) { EssenceHud.draw(g, x, y); }
		});
		ALL.add(new Module("dungeon", "Dungeon Map", "A live map of the rift you're in, with your party as dots.", "Remi", "▦", 0xFF2EE6D6, true, true) {
			@Override public int width() { return DungeonMapHud.width(); }
			@Override public int height() { return DungeonMapHud.height(); }
			@Override public int[] defaultPos(int sw, int sh) { return new int[] { sw - width() - 6, 6 }; }
			@Override public boolean visible() { return DungeonMapHud.visible(); }
			@Override public void draw(GuiGraphicsExtractor g, int x, int y, boolean p) { DungeonMapHud.draw(g, x, y); }
		});
		ALL.add(new Module("party", "Party Frames", "Your dungeon party's health, and who's down.", "Remi", "♥", 0xFF4FD18B, true, true) {
			@Override public int width() { return PartyHud.width(); }
			@Override public int height() { return PartyHud.height(); }
			@Override public int[] defaultPos(int sw, int sh) { return new int[] { 6, sh / 2 - height() / 2 }; }
			@Override public boolean visible() { return PartyHud.visible(); }
			@Override public void draw(GuiGraphicsExtractor g, int x, int y, boolean p) { PartyHud.party(g, x, y); }
		});
		ALL.add(new Module("compass", "Waypoint Compass", "A compass across the top with every Remi waypoint.", "Remi", "➶", 0xFFFFD34D, true, true) {
			@Override public int width() { return CompassHud.width(); }
			@Override public int height() { return CompassHud.height(); }
			@Override public int[] defaultPos(int sw, int sh) { return new int[] { sw / 2 - width() / 2, 3 }; }
			@Override public boolean visible() { return CompassHud.visible(); }
			@Override public void draw(GuiGraphicsExtractor g, int x, int y, boolean p) { CompassHud.draw(g, x, y); }
		});
		ALL.add(new Module("bosses", "Boss Bars", "Remi-styled boss bars for Wardens, Mythics and events.", "Remi", "☠", 0xFFFF4D5E, true, false) {
			@Override public int width() { return 0; }
			@Override public int height() { return 0; }
			@Override public int[] defaultPos(int sw, int sh) { return new int[] { 0, 0 }; }
			@Override public void draw(GuiGraphicsExtractor g, int x, int y, boolean p) { }
		});

		// ---- HUD
		ALL.add(new TextModule("fps", "FPS", "Frames per second.", "⚡", 0xFF7FE3FF) {
			@Override String text() { return Minecraft.getInstance().getFps() + " FPS"; }
			@Override public int[] defaultPos(int sw, int sh) { return new int[] { 6, 6 }; }
		});
		ALL.add(new TextModule("coords", "Coordinates", "Where you are, and which way you face.", "⌖", 0xFF3DD16B) {
			@Override String text() {
				var p = Minecraft.getInstance().player;
				if (p == null) return "0, 64, 0";
				return (int) Math.floor(p.getX()) + ", " + (int) Math.floor(p.getY()) + ", " + (int) Math.floor(p.getZ()) + "  " + Theme.cap(p.getDirection().getName());
			}
			@Override public int[] defaultPos(int sw, int sh) { return new int[] { 6, 26 }; }
		});
		ALL.add(new TextModule("ping", "Ping", "Your connection to the server.", "◉", 0xFFFF8CE6) {
			@Override String text() {
				Minecraft mc = Minecraft.getInstance();
				if (mc.getConnection() == null || mc.player == null) return "0 ms";
				PlayerInfo i = mc.getConnection().getPlayerInfo(mc.player.getUUID());
				return (i == null ? 0 : i.getLatency()) + " ms";
			}
			@Override public int[] defaultPos(int sw, int sh) { return new int[] { 6, 46 }; }
		});
		ALL.add(new TextModule("clock", "Clock", "The time on your PC.", "◷", 0xFFC7B6FF) {
			@Override String text() { return LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a")); }
			@Override public int[] defaultPos(int sw, int sh) { return new int[] { 6, 66 }; }
		});
		ALL.add(new Module("keys", "Keystrokes", "WASD, jump and both mouse buttons, lit when pressed.", "HUD", "⌨", 0xFFFF6A2B, false, true) {
			@Override public int width() { return 3 * 20 + 2 * 2; }
			@Override public int height() { return 20 * 3 + 2 * 2 + 10; }
			@Override public int[] defaultPos(int sw, int sh) { return new int[] { 6, sh / 2 - 30 }; }
			@Override public boolean hasColor() { return true; }
			@Override public void draw(GuiGraphicsExtractor g, int x, int y, boolean preview) {
				var o = Minecraft.getInstance().options;
				key(g, x + 22, y, 20, 20, "W", o.keyUp.isDown());
				key(g, x, y + 22, 20, 20, "A", o.keyLeft.isDown());
				key(g, x + 22, y + 22, 20, 20, "S", o.keyDown.isDown());
				key(g, x + 44, y + 22, 20, 20, "D", o.keyRight.isDown());
				key(g, x, y + 44, 31, 18, "LMB", o.keyAttack.isDown());
				key(g, x + 33, y + 44, 31, 18, "RMB", o.keyUse.isDown());
				key(g, x, y + 64, 64, 10, "▬", o.keyJump.isDown());
			}
			private void key(GuiGraphicsExtractor g, int x, int y, int w, int h, String s, boolean down) {
				Theme.panel(g, x, y, x + w, y + h, down ? Theme.alpha(cfg().color, 0.85f) : Theme.BG_SOFT, down ? 0 : 0x22FFFFFF);
				Theme.centered(g, s, x + w / 2, y + h / 2 - 4, down ? 0xFF0C0A12 : cfg().color);
			}
		});

		// ---- Visual
		ALL.add(new CrosshairModule());
	}
}
