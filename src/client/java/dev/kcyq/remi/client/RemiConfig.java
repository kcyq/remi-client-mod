package dev.kcyq.remi.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

/** Every setting from the Remi menu, saved to config/remiclient/settings.json. */
public final class RemiConfig {
	public static final class ModuleCfg {
		public boolean enabled = true;
		public float x = -1, y = -1;   // top left as a fraction of the screen; -1 = default spot
		public float scale = 1f;
		public int color = 0xFFF4F2FA;
		public boolean background = true;
	}

	public Map<String, ModuleCfg> modules = new LinkedHashMap<>();
	public int[] crosshair = CrosshairModule.preset("remi");
	public int crosshairTarget = 0xFFFF5C6C;
	public boolean crosshairTargetColor = true;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static RemiConfig instance;

	static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("remiclient").resolve("settings.json"); }

	public static RemiConfig get() {
		if (instance == null) {
			try {
				if (Files.exists(file())) instance = GSON.fromJson(Files.readString(file()), RemiConfig.class);
			} catch (Exception ignored) { }
			if (instance == null) instance = new RemiConfig();
			if (instance.modules == null) instance.modules = new LinkedHashMap<>();
			if (instance.crosshair == null || instance.crosshair.length != 225) instance.crosshair = CrosshairModule.preset("remi");
		}
		return instance;
	}

	public static ModuleCfg of(Module m) {
		return get().modules.computeIfAbsent(m.id, k -> {
			ModuleCfg c = new ModuleCfg();
			c.enabled = m.onByDefault;
			c.color = m.defaultColor;
			return c;
		});
	}

	public static void save() {
		try {
			Files.createDirectories(file().getParent());
			Files.writeString(file(), GSON.toJson(get()));
		} catch (Exception ignored) { }
	}
}
