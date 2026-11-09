package dev.kcyq.remi.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Remembers the terrain you've explored, one colour per block column (like a vanilla map, but endless and
 * shaded by height). A few chunks around you are sampled every tick, so it never costs FPS.
 * Saved per server and per dimension in config/remiclient/maps.
 */
public final class WorldMap {
	private WorldMap() {}

	/** chunk key -> 256 ARGB colours (0 = unknown) */
	static final Map<Long, int[]> chunks = new HashMap<>();
	static String loadedFor = "";
	private static int ring = 0, scanIndex = 0;
	private static long lastSave = 0;

	static long key(int cx, int cz) { return ((long) cx << 32) ^ (cz & 0xFFFFFFFFL); }

	static String place() {
		Minecraft mc = Minecraft.getInstance();
		String server = mc.getCurrentServer() != null ? mc.getCurrentServer().ip : "local";
		String dim = mc.level != null ? mc.level.dimension().identifier().toString() : "none";
		return (server + "_" + dim).replaceAll("[^A-Za-z0-9._-]", "_");
	}

	static Path file(String place) { return FabricLoader.getInstance().getConfigDir().resolve("remiclient").resolve("maps").resolve(place + ".bin"); }

	public static void tick() {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null || mc.player == null) return;
		String p = place();
		if (!p.equals(loadedFor)) { save(); load(p); }
		int pcx = mc.player.blockPosition().getX() >> 4, pcz = mc.player.blockPosition().getZ() >> 4;
		int radius = Math.min(12, mc.options.getEffectiveRenderDistance());
		// walk outward in rings, a handful of chunks per tick
		for (int n = 0; n < 6; n++) {
			int side = ring * 2 + 1, perim = ring == 0 ? 1 : side * 4 - 4;
			if (scanIndex >= perim) { scanIndex = 0; ring = ring >= radius ? 0 : ring + 1; continue; }
			int[] off = ringOffset(ring, scanIndex++);
			sample(level, pcx + off[0], pcz + off[1]);
		}
		if (System.currentTimeMillis() - lastSave > 60_000) save();
	}

	private static int[] ringOffset(int r, int i) {
		if (r == 0) return new int[] { 0, 0 };
		int side = r * 2;
		if (i < side) return new int[] { -r + i, -r };
		i -= side;
		if (i < side) return new int[] { r, -r + i };
		i -= side;
		if (i < side) return new int[] { r - i, r };
		i -= side;
		return new int[] { -r, r - i };
	}

	private static void sample(ClientLevel level, int cx, int cz) {
		if (!level.hasChunk(cx, cz)) return;
		int[] out = chunks.computeIfAbsent(key(cx, cz), k -> new int[256]);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int lz = 0; lz < 16; lz++) {
			int prevH = Integer.MIN_VALUE;
			for (int lx = 0; lx < 16; lx++) {
				int x = (cx << 4) + lx, z = (cz << 4) + lz;
				int h = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
				pos.set(x, h, z);
				BlockState st = level.getBlockState(pos);
				MapColor mc = st.getMapColor(level, pos);
				if (mc == MapColor.NONE) { out[lz * 16 + lx] = 0; continue; }
				int base = 0xFF000000 | mc.col;
				// shade by slope against the block to the north, like vanilla maps
				int north = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z - 1) - 1;
				float shade = h > north ? 1.08f : h < north ? 0.82f : 0.95f;
				if (st.getFluidState() != null && !st.getFluidState().isEmpty()) shade = 0.9f;
				out[lz * 16 + lx] = shadeColor(base, shade);
				prevH = h;
			}
		}
	}

	static int shadeColor(int argb, float f) {
		int r = Math.min(255, (int) (((argb >> 16) & 255) * f)), g = Math.min(255, (int) (((argb >> 8) & 255) * f)), b = Math.min(255, (int) ((argb & 255) * f));
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}

	static synchronized void load(String place) {
		chunks.clear();
		loadedFor = place;
		Path f = file(place);
		if (!Files.exists(f)) return;
		try (DataInputStream in = new DataInputStream(new GZIPInputStream(Files.newInputStream(f)))) {
			int n = in.readInt();
			for (int i = 0; i < n; i++) {
				long k = in.readLong();
				int[] c = new int[256];
				for (int j = 0; j < 256; j++) c[j] = in.readInt();
				chunks.put(k, c);
			}
		} catch (IOException ignored) { }
	}

	public static synchronized void save() {
		lastSave = System.currentTimeMillis();
		if (loadedFor.isEmpty() || chunks.isEmpty()) return;
		Path f = file(loadedFor);
		try {
			Files.createDirectories(f.getParent());
			try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(f)))) {
				out.writeInt(chunks.size());
				for (Map.Entry<Long, int[]> e : chunks.entrySet()) {
					out.writeLong(e.getKey());
					for (int v : e.getValue()) out.writeInt(v);
				}
			}
		} catch (IOException ignored) { }
	}
}
