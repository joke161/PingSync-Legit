package net.backtrack.mod.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.backtrack.mod.config.BacktrackConfig;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.network.chat.Component;

public class BacktrackCommands {

	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommandManager.literal("pingsync")
				.executes(context -> {
					context.getSource().sendFeedback(Component.literal(
						"§a[PingSync] §7Use /pingsync status or open ModMenu settings."
					));
					return 1;
				})
				.then(ClientCommandManager.literal("toggle")
					.executes(context -> {
						BacktrackConfig.enabled = !BacktrackConfig.enabled;
						BacktrackConfig.save();
						context.getSource().sendFeedback(Component.literal(
							"§a[PingSync] §7Rendering state: " + (BacktrackConfig.enabled ? "§eENABLED" : "§cDISABLED")
						));
						return 1;
					})
				)
				.then(ClientCommandManager.literal("color")
					.then(ClientCommandManager.argument("target", StringArgumentType.word())
						.then(ClientCommandManager.argument("preset", StringArgumentType.word())
							.executes(context -> {
								String target = StringArgumentType.getString(context, "target").toLowerCase();
								String preset = StringArgumentType.getString(context, "preset").toUpperCase();
								if ("CYAN".equals(preset) || "RED".equals(preset) || "GREEN".equals(preset) ||
									"PURPLE".equals(preset) || "GOLD".equals(preset) || "RAINBOW".equals(preset) || "ORIGINAL".equals(preset)) {
									
									if ("hitbox".equals(target) || "hitboxes".equals(target)) {
										BacktrackConfig.hitboxColorPreset = preset;
										BacktrackConfig.save();
										context.getSource().sendFeedback(Component.literal(
											"§a[PingSync] §7Hitbox Color Preset set to: §e" + preset
										));
										return 1;
									} else if ("ghost".equals(target) || "model".equals(target)) {
										BacktrackConfig.ghostColorPreset = preset;
										BacktrackConfig.save();
										context.getSource().sendFeedback(Component.literal(
											"§a[PingSync] §7Ghost Model Color Preset set to: §e" + preset
										));
										return 1;
									} else {
										context.getSource().sendFeedback(Component.literal(
											"§c[PingSync] Unknown target! Use: /pingsync color <hitbox|ghost> <preset>"
										));
										return 0;
									}
								} else {
									context.getSource().sendFeedback(Component.literal(
										"§c[PingSync] Unknown color preset! Use: original, cyan, red, green, purple, gold, rainbow"
									));
									return 0;
								}
							})
						)
					)
				)
				.then(ClientCommandManager.literal("distance")
					.then(ClientCommandManager.argument("blocks", IntegerArgumentType.integer(4, 64))
						.executes(context -> {
							int dist = IntegerArgumentType.getInteger(context, "blocks");
							BacktrackConfig.maxRenderDistance = dist;
							BacktrackConfig.save();
							context.getSource().sendFeedback(Component.literal(
								"§a[PingSync] §7Max Render Distance set to: §e" + dist + " blocks"
							));
							return 1;
						})
					)
				)
				.then(ClientCommandManager.literal("chronofade")
					.executes(context -> {
						BacktrackConfig.ghostFadeTicks = !BacktrackConfig.ghostFadeTicks;
						BacktrackConfig.save();
						context.getSource().sendFeedback(Component.literal(
							"§a[PingSync] §7Chrono Fade: " + (BacktrackConfig.ghostFadeTicks ? "§eENABLED" : "§cDISABLED")
						));
						return 1;
					})
				)
				.then(ClientCommandManager.literal("ghostalpha")
					.then(ClientCommandManager.argument("percent", IntegerArgumentType.integer(0, 100))
						.executes(context -> {
							int pct = IntegerArgumentType.getInteger(context, "percent");
							BacktrackConfig.ghostAlpha = pct / 100.0f;
							BacktrackConfig.save();
							context.getSource().sendFeedback(Component.literal(
								"§a[PingSync] §7Ghost Model Transparency set to: §e" + pct + "%"
							));
							return 1;
						})
					)
				)
				.then(ClientCommandManager.literal("render")
					.then(ClientCommandManager.argument("target", StringArgumentType.word())
						.then(ClientCommandManager.argument("mode", StringArgumentType.word())
							.executes(context -> {
								String target = StringArgumentType.getString(context, "target").toLowerCase();
								String rawMode = StringArgumentType.getString(context, "mode").toUpperCase();
								String mode = ("LAST_TICK".equals(rawMode) || "LAST".equals(rawMode) || "ONLY_OLDEST".equals(rawMode)) ? "LAST_TICK" : "ALL_TICKS";
								
								if ("hitboxes".equals(target) || "hitbox".equals(target)) {
									BacktrackConfig.hitboxRenderMode = mode;
									BacktrackConfig.save();
									context.getSource().sendFeedback(Component.literal(
										"§a[PingSync] §7Hitbox render mode set to: §e" + mode
									));
									return 1;
								} else if ("ghost".equals(target) || "model".equals(target)) {
									BacktrackConfig.ghostRenderMode = mode;
									BacktrackConfig.save();
									context.getSource().sendFeedback(Component.literal(
										"§a[PingSync] §7Ghost model render mode set to: §e" + mode
									));
									return 1;
								} else {
									context.getSource().sendFeedback(Component.literal(
										"§c[PingSync] Unknown render target! Use: hitboxes or ghost"
									));
									return 0;
								}
							})
						)
					)
				)
				.then(ClientCommandManager.literal("status")
					.executes(context -> {
						context.getSource().sendFeedback(Component.literal(
							"§a=== PingSync Status ===\n" +
							"§7Rendering: " + (BacktrackConfig.enabled ? "§aENABLED" : "§cDISABLED") + "\n" +
							"§7Hitboxes: " + (BacktrackConfig.renderHitboxes ? "§aON (" + BacktrackConfig.hitboxRenderMode + ", Color=" + BacktrackConfig.hitboxColorPreset + ")" : "§cOFF") + "\n" +
							"§7Ghost Model: " + (BacktrackConfig.renderGhostModel ? "§aON (" + BacktrackConfig.ghostRenderMode + ", Color=" + BacktrackConfig.ghostColorPreset + ", Fade=" + (BacktrackConfig.ghostFadeTicks ? "ON" : "OFF") + ", Alpha=" + (int)(BacktrackConfig.ghostAlpha * 100) + "%)" : "§cOFF") + "\n" +
							"§7Max Render Dist: §e" + (int) BacktrackConfig.maxRenderDistance + " blocks\n" +
							"§7Calculation Mode: §eNATURAL (Your Ping + Target Ping)\n" +
							"§7History Window: §eAUTO (Real Network Latency Sync)"
						));
						return 1;
					})
				)
			);

			dispatcher.register(ClientCommandManager.literal("backtrack")
				.executes(context -> {
					context.getSource().sendFeedback(Component.literal(
						"§a[PingSync] §7PingSync active. Use /pingsync for commands."
					));
					return 1;
				})
			);
		});
	}
}
