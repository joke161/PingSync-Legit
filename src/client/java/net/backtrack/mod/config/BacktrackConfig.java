package net.backtrack.mod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class BacktrackConfig {
	public static boolean enabled = true;

	// Hitbox Rendering Settings
	public static boolean renderHitboxes = true;
	public static String hitboxRenderMode = "LAST_TICK"; // "LAST_TICK" vs "ALL_TICKS"
	public static String hitboxColorPreset = "CYAN"; // "CYAN", "RED", "GREEN", "PURPLE", "GOLD", "RAINBOW"

	// 3D Player Ghost Model Settings
	public static boolean renderGhostModel = true;
	public static String ghostRenderMode = "LAST_TICK"; // "LAST_TICK" vs "ALL_TICKS"
	public static String ghostColorPreset = "ORIGINAL"; // "ORIGINAL", "CYAN", "RED", "GREEN", "PURPLE", "GOLD", "RAINBOW"
	public static boolean ghostFadeTicks = true; // Dynamic fade / opacity gradient over time for ALL TICKS
	public static float ghostAlpha = 0.4f;

	// Optimization Settings
	public static double maxRenderDistance = 16.0; // Max render distance in blocks (default: 16)

	// Target Entity Filters
	public static boolean trackPlayers = true;
	public static boolean trackHostile = true;
	public static boolean trackPassive = true;
	public static boolean trackOther = false;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("pingsync.json");

	private static class ConfigData {
		boolean enabled = BacktrackConfig.enabled;

		boolean renderHitboxes = BacktrackConfig.renderHitboxes;
		String hitboxRenderMode = BacktrackConfig.hitboxRenderMode;
		String hitboxColorPreset = BacktrackConfig.hitboxColorPreset;

		boolean renderGhostModel = BacktrackConfig.renderGhostModel;
		String ghostRenderMode = BacktrackConfig.ghostRenderMode;
		String ghostColorPreset = BacktrackConfig.ghostColorPreset;
		boolean ghostFadeTicks = BacktrackConfig.ghostFadeTicks;
		float ghostAlpha = BacktrackConfig.ghostAlpha;

		double maxRenderDistance = BacktrackConfig.maxRenderDistance;

		boolean trackPlayers = BacktrackConfig.trackPlayers;
		boolean trackHostile = BacktrackConfig.trackHostile;
		boolean trackPassive = BacktrackConfig.trackPassive;
		boolean trackOther = BacktrackConfig.trackOther;
	}

	public static void load() {
		try {
			if (Files.exists(CONFIG_FILE)) {
				try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
					ConfigData data = GSON.fromJson(reader, ConfigData.class);
					if (data != null) {
						enabled = data.enabled;

						renderHitboxes = data.renderHitboxes;
						if (data.hitboxRenderMode != null) {
							hitboxRenderMode = "ONLY_OLDEST".equalsIgnoreCase(data.hitboxRenderMode) ? "LAST_TICK" : data.hitboxRenderMode;
						}
						if (data.hitboxColorPreset != null && !"CUSTOM".equalsIgnoreCase(data.hitboxColorPreset)) {
							hitboxColorPreset = data.hitboxColorPreset;
						}

						renderGhostModel = data.renderGhostModel;
						if (data.ghostRenderMode != null) {
							ghostRenderMode = "ONLY_OLDEST".equalsIgnoreCase(data.ghostRenderMode) ? "LAST_TICK" : data.ghostRenderMode;
						}
						if (data.ghostColorPreset != null && !"CUSTOM".equalsIgnoreCase(data.ghostColorPreset)) {
							ghostColorPreset = data.ghostColorPreset;
						}
						ghostFadeTicks = data.ghostFadeTicks;
						ghostAlpha = data.ghostAlpha;

						if (data.maxRenderDistance > 0) maxRenderDistance = data.maxRenderDistance;

						trackPlayers = data.trackPlayers;
						trackHostile = data.trackHostile;
						trackPassive = data.trackPassive;
						trackOther = data.trackOther;
					}
				}
			} else {
				save();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void save() {
		try {
			ConfigData data = new ConfigData();
			data.enabled = enabled;

			data.renderHitboxes = renderHitboxes;
			data.hitboxRenderMode = hitboxRenderMode;
			data.hitboxColorPreset = hitboxColorPreset;

			data.renderGhostModel = renderGhostModel;
			data.ghostRenderMode = ghostRenderMode;
			data.ghostColorPreset = ghostColorPreset;
			data.ghostFadeTicks = ghostFadeTicks;
			data.ghostAlpha = ghostAlpha;

			data.maxRenderDistance = maxRenderDistance;

			data.trackPlayers = trackPlayers;
			data.trackHostile = trackHostile;
			data.trackPassive = trackPassive;
			data.trackOther = trackOther;

			try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
				GSON.toJson(data, writer);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
