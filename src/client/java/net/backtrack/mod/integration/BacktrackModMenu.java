package net.backtrack.mod.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.backtrack.mod.config.BacktrackConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractOptionSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class BacktrackModMenu implements ModMenuApi {

	private static class RenderDistanceSlider extends AbstractOptionSliderButton {
		public RenderDistanceSlider(int x, int y, int width, int height) {
			super(Minecraft.getInstance().options, x, y, width, height, (BacktrackConfig.maxRenderDistance - 8.0) / 24.0);
			this.updateMessage();
		}

		@Override
		protected void updateMessage() {
			this.setMessage(Component.literal("Render Distance: " + (int) BacktrackConfig.maxRenderDistance + " blocks"));
		}

		@Override
		protected void applyValue() {
			BacktrackConfig.maxRenderDistance = Math.round(8.0 + this.value * 24.0);
			BacktrackConfig.save();
		}
	}

	private static class GhostAlphaSlider extends AbstractOptionSliderButton {
		public GhostAlphaSlider(int x, int y, int width, int height) {
			super(Minecraft.getInstance().options, x, y, width, height, BacktrackConfig.ghostAlpha);
			this.updateMessage();
		}

		@Override
		protected void updateMessage() {
			int val = (int) Math.round(this.value * 100.0);
			this.setMessage(Component.literal("Ghost Transparency: " + val + "%"));
		}

		@Override
		protected void applyValue() {
			BacktrackConfig.ghostAlpha = (float) this.value;
			BacktrackConfig.save();
		}
	}

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> new Screen(Component.literal("PingSync Settings")) {

			@Override
			protected void init() {
				int y = 32;
				int leftX = this.width / 2 - 152;
				int rightX = this.width / 2 + 2;
				int btnW = 150;

				// --- SECTION 1: MASTER TOGGLE ---
				this.addRenderableWidget(Button.builder(
					Component.literal("PingSync: " + (BacktrackConfig.enabled ? "§aON" : "§cOFF")),
					button -> {
						BacktrackConfig.enabled = !BacktrackConfig.enabled;
						button.setMessage(Component.literal("PingSync: " + (BacktrackConfig.enabled ? "§aON" : "§cOFF")));
						BacktrackConfig.save();
					}
				).bounds(leftX, y, 304, 20).build());

				// --- SECTION 2: HITBOX SETTINGS ---
				y += 28;
				this.addRenderableWidget(Button.builder(
					Component.literal("Hitboxes: " + (BacktrackConfig.renderHitboxes ? "§aON" : "§cOFF")),
					button -> {
						BacktrackConfig.renderHitboxes = !BacktrackConfig.renderHitboxes;
						button.setMessage(Component.literal("Hitboxes: " + (BacktrackConfig.renderHitboxes ? "§aON" : "§cOFF")));
						BacktrackConfig.save();
					}
				).bounds(leftX, y, btnW, 20).build());

				this.addRenderableWidget(Button.builder(
					Component.literal("Hitbox Ticks: " + ("LAST_TICK".equalsIgnoreCase(BacktrackConfig.hitboxRenderMode) ? "LAST TICK" : "ALL TICKS")),
					button -> {
						BacktrackConfig.hitboxRenderMode = "LAST_TICK".equalsIgnoreCase(BacktrackConfig.hitboxRenderMode) ? "ALL_TICKS" : "LAST_TICK";
						button.setMessage(Component.literal("Hitbox Ticks: " + ("LAST_TICK".equalsIgnoreCase(BacktrackConfig.hitboxRenderMode) ? "LAST TICK" : "ALL TICKS")));
						BacktrackConfig.save();
					}
				).bounds(rightX, y, btnW, 20).build());

				y += 24;
				String[] hitboxPresets = {"CYAN", "RED", "GREEN", "PURPLE", "GOLD", "RAINBOW"};
				this.addRenderableWidget(Button.builder(
					Component.literal("Hitbox Color: §e" + BacktrackConfig.hitboxColorPreset),
					button -> {
						int idx = 0;
						for (int i = 0; i < hitboxPresets.length; i++) {
							if (hitboxPresets[i].equalsIgnoreCase(BacktrackConfig.hitboxColorPreset)) {
								idx = (i + 1) % hitboxPresets.length;
								break;
							}
						}
						BacktrackConfig.hitboxColorPreset = hitboxPresets[idx];
						button.setMessage(Component.literal("Hitbox Color: §e" + BacktrackConfig.hitboxColorPreset));
						BacktrackConfig.save();
					}
				).bounds(leftX, y, 304, 20).build());

				// --- SECTION 3: GHOST MODEL SETTINGS ---
				y += 28;
				this.addRenderableWidget(Button.builder(
					Component.literal("Ghost Model: " + (BacktrackConfig.renderGhostModel ? "§aON" : "§cOFF")),
					button -> {
						BacktrackConfig.renderGhostModel = !BacktrackConfig.renderGhostModel;
						button.setMessage(Component.literal("Ghost Model: " + (BacktrackConfig.renderGhostModel ? "§aON" : "§cOFF")));
						BacktrackConfig.save();
					}
				).bounds(leftX, y, btnW, 20).build());

				this.addRenderableWidget(Button.builder(
					Component.literal("Ghost Ticks: " + ("LAST_TICK".equalsIgnoreCase(BacktrackConfig.ghostRenderMode) ? "LAST TICK" : "ALL TICKS")),
					button -> {
						BacktrackConfig.ghostRenderMode = "LAST_TICK".equalsIgnoreCase(BacktrackConfig.ghostRenderMode) ? "ALL_TICKS" : "LAST_TICK";
						button.setMessage(Component.literal("Ghost Ticks: " + ("LAST_TICK".equalsIgnoreCase(BacktrackConfig.ghostRenderMode) ? "LAST TICK" : "ALL TICKS")));
						BacktrackConfig.save();
					}
				).bounds(rightX, y, btnW, 20).build());

				y += 24;
				String[] ghostPresets = {"ORIGINAL", "PURPLE", "CYAN", "RED", "GREEN", "GOLD", "RAINBOW"};
				this.addRenderableWidget(Button.builder(
					Component.literal("Ghost Color: §d" + BacktrackConfig.ghostColorPreset),
					button -> {
						int idx = 0;
						for (int i = 0; i < ghostPresets.length; i++) {
							if (ghostPresets[i].equalsIgnoreCase(BacktrackConfig.ghostColorPreset)) {
								idx = (i + 1) % ghostPresets.length;
								break;
							}
						}
						BacktrackConfig.ghostColorPreset = ghostPresets[idx];
						button.setMessage(Component.literal("Ghost Color: §d" + BacktrackConfig.ghostColorPreset));
						BacktrackConfig.save();
					}
				).bounds(leftX, y, btnW, 20).build());

				this.addRenderableWidget(Button.builder(
					Component.literal("Chrono Fade: " + (BacktrackConfig.ghostFadeTicks ? "§aON" : "§cOFF")),
					button -> {
						BacktrackConfig.ghostFadeTicks = !BacktrackConfig.ghostFadeTicks;
						button.setMessage(Component.literal("Chrono Fade: " + (BacktrackConfig.ghostFadeTicks ? "§aON" : "§cOFF")));
						BacktrackConfig.save();
					}
				).bounds(rightX, y, btnW, 20).build());

				y += 24;
				this.addRenderableWidget(new GhostAlphaSlider(leftX, y, btnW, 20));
				this.addRenderableWidget(new RenderDistanceSlider(rightX, y, btnW, 20));

				// --- DONE BUTTON ---
				y += 32;
				this.addRenderableWidget(Button.builder(
					Component.literal("Done"),
					button -> {
						BacktrackConfig.save();
						this.minecraft.setScreen(parent);
					}
				).bounds(this.width / 2 - 100, y, 200, 20).build());
			}

			@Override
			public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
				super.render(guiGraphics, mouseX, mouseY, partialTick);
				guiGraphics.drawCenteredString(this.font, "§lPingSync Settings", this.width / 2, 12, 0xFFFFFF);
			}
		};
	}
}
