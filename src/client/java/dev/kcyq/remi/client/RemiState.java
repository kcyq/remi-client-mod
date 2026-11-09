package dev.kcyq.remi.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything the Remi server last told us. The server (remi_hud.sk) sends a full frame twice a second:
 *
 *   F                                              start of a frame (everything below replaces the old state)
 *   E|gem|Name|&code|tier|state                    your Essence (state: ok, off, suppressed, blank)
 *   A|slot|label|left|base|note                    one ability: seconds left, full cooldown, optional note (ACTIVE, MARKED...)
 *   T|meter|awkLeft|awkTotal                       True Awakening meter 0-100, seconds of Awakening left
 *   D|slot|rooms|current|roomState|theme|stage     the dungeon you're in
 *   DR|index|archetype|theme                       one room of it
 *   DP|name|dx|dz|hp|max|down|gem                  one party member (dx/dz from the dungeon's start)
 *   W|id|name|x|y|z|#rrggbb|kind                   a waypoint (kind: spawn, rift, event, guild, dungeon, custom)
 */
public final class RemiState {
	private RemiState() {}

	public static final class Ability {
		public String slot, label, note;
		public double left, base;
	}
	public static final class Room { public int index; public String archetype, theme; }
	public static final class Member { public String name, gem; public double dx, dz, hp, max; public boolean down; }
	public static final class Waypoint { public String id, name, kind; public double x, y, z; public int color; }

	public static volatile boolean onRemi = false;
	public static long frameAt = 0;

	public static String gem = "", gemName = "", gemCode = "&f", essState = "blank";
	public static int tier = 0;
	public static final List<Ability> abilities = new ArrayList<>();
	public static double trueMeter = 0, awkLeft = 0, awkTotal = 0;

	public static boolean inDungeon = false;
	public static int dgSlot, dgRooms, dgCurrent;
	public static String dgRoomState = "", dgTheme = "", dgStage = "";
	public static final Map<Integer, Room> rooms = new LinkedHashMap<>();
	public static final List<Member> party = new ArrayList<>();
	public static final List<Waypoint> waypoints = new ArrayList<>();

	private static double num(String s) {
		try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0; }
	}

	public static synchronized void apply(String data) {
		String[] lines = data.split("\n");
		boolean full = lines.length > 0 && lines[0].trim().equals("F");
		if (!full) return;
		onRemi = true;
		frameAt = System.currentTimeMillis();
		abilities.clear();
		rooms.clear();
		party.clear();
		waypoints.clear();
		inDungeon = false;
		trueMeter = awkLeft = awkTotal = 0;
		for (String line : lines) {
			String[] f = line.split("\\|", -1);
			if (f.length == 0) continue;
			try {
				switch (f[0]) {
					case "E" -> { gem = f[1]; gemName = f[2]; gemCode = f[3]; tier = (int) num(f[4]); essState = f[5]; }
					case "A" -> {
						Ability a = new Ability();
						a.slot = f[1]; a.label = f[2]; a.left = num(f[3]); a.base = Math.max(1, num(f[4])); a.note = f.length > 5 ? f[5] : "";
						abilities.add(a);
					}
					case "T" -> { trueMeter = num(f[1]); awkLeft = num(f[2]); awkTotal = Math.max(1, num(f[3])); }
					case "D" -> {
						inDungeon = true; dgSlot = (int) num(f[1]); dgRooms = (int) num(f[2]); dgCurrent = (int) num(f[3]);
						dgRoomState = f[4]; dgTheme = f[5]; dgStage = f[6];
					}
					case "DR" -> { Room r = new Room(); r.index = (int) num(f[1]); r.archetype = f[2]; r.theme = f[3]; rooms.put(r.index, r); }
					case "DP" -> {
						Member m = new Member();
						m.name = f[1]; m.dx = num(f[2]); m.dz = num(f[3]); m.hp = num(f[4]); m.max = Math.max(1, num(f[5]));
						m.down = f[6].equals("true"); m.gem = f.length > 7 ? f[7] : "";
						party.add(m);
					}
					case "W" -> {
						Waypoint w = new Waypoint();
						w.id = f[1]; w.name = f[2]; w.x = num(f[3]); w.y = num(f[4]); w.z = num(f[5]);
						w.color = Theme.parseHex(f[6], 0xFFA77BFF); w.kind = f[7];
						waypoints.add(w);
					}
					default -> { }
				}
			} catch (ArrayIndexOutOfBoundsException ignored) {
				// a line from a newer server script: skip it
			}
		}
	}

	/** Seconds since the last frame, so cooldowns keep counting down smoothly between frames. */
	public static double since() { return (System.currentTimeMillis() - frameAt) / 1000.0; }

	/** No frame for 10 s (left Remi, or the script stopped): hide everything. */
	public static boolean live() { return onRemi && System.currentTimeMillis() - frameAt < 10_000; }

	public static synchronized void reset() {
		onRemi = false;
		abilities.clear(); rooms.clear(); party.clear(); waypoints.clear();
		inDungeon = false;
	}
}
