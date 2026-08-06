package net.backtrack.mod.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.backtrack.mod.render.BacktrackRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {

	@Inject(method = "renderNameTag", at = @At("HEAD"), cancellable = true)
	private void onRenderNameTag(
		Entity entity,
		Component text,
		PoseStack poseStack,
		MultiBufferSource bufferSource,
		int packedLight,
		float partialTick,
		CallbackInfo ci
	) {
		if (BacktrackRenderer.isRenderingGhost) {
			ci.cancel(); // Suppress nametag rendering on ghost player models
		}
	}
}
