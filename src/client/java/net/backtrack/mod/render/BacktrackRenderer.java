package net.backtrack.mod.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.backtrack.mod.config.BacktrackConfig;
import net.backtrack.mod.tracker.BacktrackTracker;
import net.backtrack.mod.tracker.PlayerPositionRecord;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class BacktrackRenderer {

	public static boolean isRenderingGhost = false;

	public static void register() {
		WorldRenderEvents.LAST.register(BacktrackRenderer::render);
	}

	private static class TintedVertexConsumer implements VertexConsumer {
		private final VertexConsumer delegate;
		private final float rTint, gTint, bTint, aTint;

		public TintedVertexConsumer(VertexConsumer delegate, float rTint, float gTint, float bTint, float aTint) {
			this.delegate = delegate;
			this.rTint = rTint;
			this.gTint = gTint;
			this.bTint = bTint;
			this.aTint = aTint;
		}

		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			delegate.addVertex(x, y, z);
			return this;
		}

		@Override
		public VertexConsumer setColor(int r, int g, int b, int a) {
			// Ensure minimum alpha (80/255 = ~31%) so shader passes don't cause ghost model to flicker or vanish
			int finalA = Math.max(80, (int) (a * aTint));
			delegate.setColor(
				(int) (r * rTint),
				(int) (g * gTint),
				(int) (b * bTint),
				finalA
			);
			return this;
		}

		@Override
		public VertexConsumer setUv(float u, float v) {
			delegate.setUv(u, v);
			return this;
		}

		@Override
		public VertexConsumer setUv1(int u, int v) {
			delegate.setUv1(u, v);
			return this;
		}

		@Override
		public VertexConsumer setUv2(int u, int v) {
			delegate.setUv2(u, v);
			return this;
		}

		@Override
		public VertexConsumer setNormal(float x, float y, float z) {
			delegate.setNormal(x, y, z);
			return this;
		}
	}

	private static void render(WorldRenderContext context) {
		Minecraft client = Minecraft.getInstance();
		if (client.level == null || client.player == null) return;

		boolean isF3BEnabled = client.getEntityRenderDispatcher().shouldRenderHitBoxes();
		if (!isF3BEnabled && !BacktrackConfig.enabled) return;

		PoseStack poseStack = context.matrixStack();
		Camera camera = context.camera();
		Vec3 camPos = camera.getPosition();
		MultiBufferSource consumers = context.consumers();
		if (consumers == null || poseStack == null) return;

		float partialTick = context.tickCounter().getGameTimeDeltaPartialTick(true);
		double maxDistSq = BacktrackConfig.maxRenderDistance * BacktrackConfig.maxRenderDistance;

		poseStack.pushPose();
		poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

		for (Entity entity : client.level.entitiesForRendering()) {
			if (entity == client.player || !entity.isAlive()) continue;

			// OPTIMIZATION 1: Distance Culling (skip entities beyond maxRenderDistance)
			if (client.player.distanceToSqr(entity) > maxDistSq) continue;

			List<PlayerPositionRecord> records = BacktrackTracker.getValidRecordsForEntity(entity);
			if (records.isEmpty()) continue;

			double entityX = Mth.lerp(partialTick, entity.xo, entity.getX());
			double entityY = Mth.lerp(partialTick, entity.yo, entity.getY());
			double entityZ = Mth.lerp(partialTick, entity.zo, entity.getZ());
			double halfWidth = entity.getBbWidth() / 2.0;

			double margin = entity.getPickRadius();
			AABB liveBox = new AABB(
				entityX - halfWidth - margin, entityY - margin, entityZ - halfWidth - margin,
				entityX + halfWidth + margin, entityY + entity.getBbHeight() + margin, entityZ + halfWidth + margin
			);

			// --- 1. RENDER 3D HITBOXES ---
			if (BacktrackConfig.renderHitboxes) {
				List<PlayerPositionRecord> recordsToRender;
				if ("ALL_TICKS".equalsIgnoreCase(BacktrackConfig.hitboxRenderMode)) {
					recordsToRender = records;
				} else {
					recordsToRender = List.of(records.get(records.size() - 1));
				}

				for (PlayerPositionRecord targetRecord : recordsToRender) {
					int age = targetRecord.ageTicks();
					int recordIndex = records.indexOf(targetRecord);

					AABB targetBox = targetRecord.boundingBox().inflate(margin);

					AABB nextBox;
					if (recordIndex > 0) {
						nextBox = records.get(recordIndex - 1).boundingBox().inflate(margin);
					} else {
						nextBox = liveBox;
					}

					double minX = Mth.lerp(partialTick, targetBox.minX, nextBox.minX);
					double minY = Mth.lerp(partialTick, targetBox.minY, nextBox.minY);
					double minZ = Mth.lerp(partialTick, targetBox.minZ, nextBox.minZ);
					double maxX = Mth.lerp(partialTick, targetBox.maxX, nextBox.maxX);
					double maxY = Mth.lerp(partialTick, targetBox.maxY, nextBox.maxY);
					double maxZ = Mth.lerp(partialTick, targetBox.maxZ, nextBox.maxZ);

					AABB box = new AABB(minX, minY, minZ, maxX, maxY, maxZ);

					float[] rgb = resolveHitboxColor(BacktrackConfig.hitboxColorPreset);
					float r = rgb[0], g = rgb[1], b = rgb[2];
					float fillAlpha = 0.25f;

					VertexConsumer lineConsumer = consumers.getBuffer(RenderType.lines());
					LevelRenderer.renderLineBox(poseStack, lineConsumer, box, r, g, b, 0.9f);

					VertexConsumer fillConsumer = consumers.getBuffer(RenderType.debugFilledBox());
					LevelRenderer.addChainedFilledBoxVertices(
						poseStack, fillConsumer,
						box.minX, box.minY, box.minZ,
						box.maxX, box.maxY, box.maxZ,
						r, g, b, fillAlpha
					);

					if (recordIndex == recordsToRender.size() - 1) {
						String text = String.format("Backtrack: -%dms (%dt)", age * 50, age);
						render3DText(poseStack, consumers, camera, box.getCenter().add(0, box.getYsize() / 2.0 + 0.2, 0), text, r, g, b);
					}
				}
			}

			// --- 2. RENDER 3D PLAYER GHOST MODEL (CHAMS) ---
			if (BacktrackConfig.renderGhostModel) {
				List<PlayerPositionRecord> ghostRecords;
				if ("ALL_TICKS".equalsIgnoreCase(BacktrackConfig.ghostRenderMode)) {
					if (records.size() > 5) {
						ghostRecords = new ArrayList<>();
						int step = Math.max(1, records.size() / 5);
						for (int i = records.size() - 1; i >= 0; i -= step) {
							ghostRecords.add(records.get(i));
						}
					} else {
						ghostRecords = records;
					}
				} else {
					ghostRecords = List.of(records.get(records.size() - 1));
				}

				for (PlayerPositionRecord targetRecord : ghostRecords) {
					int age = targetRecord.ageTicks();
					int recordIndex = records.indexOf(targetRecord);

					Vec3 targetPos = targetRecord.position();
					Vec3 nextPos = recordIndex > 0 ? records.get(recordIndex - 1).position() : new Vec3(entityX, entityY, entityZ);

					double lerpX = Mth.lerp(partialTick, targetPos.x, nextPos.x);
					double lerpY = Mth.lerp(partialTick, targetPos.y, nextPos.y);
					double lerpZ = Mth.lerp(partialTick, targetPos.z, nextPos.z);

					float[] rgb = resolveGhostColor(BacktrackConfig.ghostColorPreset);
					final float gr = rgb[0];
					final float gg = rgb[1];
					final float gb = rgb[2];

					float baseAlpha = Math.max(0.35f, BacktrackConfig.ghostAlpha); // Solid minimum opacity
					if (BacktrackConfig.ghostFadeTicks && records.size() > 1 && "ALL_TICKS".equalsIgnoreCase(BacktrackConfig.ghostRenderMode)) {
						float fadeRatio = 1.0f - ((float) age / Math.max(1, records.size()));
						baseAlpha = Math.max(0.2f, baseAlpha * fadeRatio);
					}
					final float ga = baseAlpha;

					MultiBufferSource ghostConsumers = renderType -> {
						VertexConsumer parent = consumers.getBuffer(renderType);
						return new TintedVertexConsumer(parent, gr, gg, gb, ga);
					};

					poseStack.pushPose();
					poseStack.translate(lerpX, lerpY, lerpZ);

					isRenderingGhost = true; // Set flag to suppress ghost nametag rendering
					try {
						client.getEntityRenderDispatcher().render(
							entity, 0.0, 0.0, 0.0,
							entity.getYRot(), partialTick,
							poseStack, ghostConsumers, 0xF000F0
						);
					} finally {
						isRenderingGhost = false;
					}

					poseStack.popPose();
				}
			}
		}

		poseStack.popPose();
	}

	private static float[] resolveHitboxColor(String preset) {
		return switch (preset.toUpperCase()) {
			case "RED" -> new float[]{1.0f, 0.2f, 0.2f};
			case "GREEN" -> new float[]{0.2f, 1.0f, 0.3f};
			case "PURPLE" -> new float[]{0.8f, 0.2f, 1.0f};
			case "GOLD" -> new float[]{1.0f, 0.8f, 0.1f};
			case "RAINBOW" -> {
				float hue = (System.currentTimeMillis() % 2000) / 2000.0f;
				int rgbInt = Color.HSBtoRGB(hue, 0.8f, 1.0f);
				yield new float[]{
					((rgbInt >> 16) & 0xFF) / 255.0f,
					((rgbInt >> 8) & 0xFF) / 255.0f,
					(rgbInt & 0xFF) / 255.0f
				};
			}
			default -> new float[]{0.2f, 0.8f, 1.0f}; // CYAN default
		};
	}

	private static float[] resolveGhostColor(String preset) {
		return switch (preset.toUpperCase()) {
			case "ORIGINAL" -> new float[]{1.0f, 1.0f, 1.0f}; // Original skin texture with transparency
			case "CYAN" -> new float[]{0.2f, 0.8f, 1.0f};
			case "RED" -> new float[]{1.0f, 0.2f, 0.2f};
			case "GREEN" -> new float[]{0.2f, 1.0f, 0.3f};
			case "PURPLE" -> new float[]{0.8f, 0.2f, 1.0f};
			case "GOLD" -> new float[]{1.0f, 0.8f, 0.1f};
			case "RAINBOW" -> {
				float hue = (System.currentTimeMillis() % 2000) / 2000.0f;
				int rgbInt = Color.HSBtoRGB(hue, 0.8f, 1.0f);
				yield new float[]{
					((rgbInt >> 16) & 0xFF) / 255.0f,
					((rgbInt >> 8) & 0xFF) / 255.0f,
					(rgbInt & 0xFF) / 255.0f
				};
			}
			default -> new float[]{1.0f, 1.0f, 1.0f}; // ORIGINAL fallback
		};
	}

	private static void render3DText(PoseStack poseStack, MultiBufferSource consumers, Camera camera, Vec3 pos, String text, float r, float g, float b) {
		Minecraft client = Minecraft.getInstance();
		Font font = client.font;

		poseStack.pushPose();
		poseStack.translate(pos.x, pos.y, pos.z);
		poseStack.mulPose(camera.rotation());
		poseStack.scale(-0.025f, -0.025f, 0.025f);

		Matrix4f matrix = poseStack.last().pose();
		float xOffset = -font.width(text) / 2.0f;

		int colorRGB = ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | ((int)(b * 255));
		font.drawInBatch(Component.literal(text), xOffset, 0, colorRGB, false, matrix, consumers, Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);

		poseStack.popPose();
	}
}
