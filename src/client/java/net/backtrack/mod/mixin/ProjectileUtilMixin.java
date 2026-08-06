package net.backtrack.mod.mixin;

import net.backtrack.mod.config.BacktrackConfig;
import net.backtrack.mod.tracker.BacktrackTracker;
import net.backtrack.mod.tracker.PlayerPositionRecord;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Mixin(ProjectileUtil.class)
public class ProjectileUtilMixin {

	@Inject(
		method = "getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;",
		at = @At("RETURN"),
		cancellable = true
	)
	private static void onGetEntityHitResult(
		Entity shooter,
		Vec3 startPos,
		Vec3 endPos,
		AABB boundingBox,
		Predicate<Entity> filter,
		double maxDistanceSquare,
		CallbackInfoReturnable<EntityHitResult> cir
	) {
		Minecraft client = Minecraft.getInstance();
		if (client.level == null || client.player == null) return;

		// Check if backtrack crosshair targeting is active
		boolean isF3BEnabled = client.getEntityRenderDispatcher().shouldRenderHitBoxes();
		if (!isF3BEnabled && !BacktrackConfig.enabled) return;

		EntityHitResult currentHit = cir.getReturnValue();
		double minDistanceSq = currentHit != null ? startPos.distanceToSqr(currentHit.getLocation()) : maxDistanceSquare;
		EntityHitResult backtrackHit = null;

		for (Entity targetEntity : client.level.entitiesForRendering()) {
			if (targetEntity == client.player || !targetEntity.isAlive()) continue;

			double margin = targetEntity.getPickRadius();
			List<PlayerPositionRecord> records = BacktrackTracker.getValidRecordsForEntity(targetEntity);
			if (records.isEmpty()) continue;

			// Match crosshair hit testing to rendered mode for 100% visual consistency
			List<PlayerPositionRecord> testRecords;
			boolean isLastTickOnly = "LAST_TICK".equalsIgnoreCase(BacktrackConfig.hitboxRenderMode) && "LAST_TICK".equalsIgnoreCase(BacktrackConfig.ghostRenderMode);
			if (isLastTickOnly) {
				testRecords = List.of(records.get(records.size() - 1));
			} else {
				testRecords = records;
			}

			for (PlayerPositionRecord record : testRecords) {
				AABB box = record.boundingBox().inflate(margin);
				Optional<Vec3> clip = box.clip(startPos, endPos);
				if (clip.isPresent()) {
					Vec3 hitVec = clip.get();
					double distSq = startPos.distanceToSqr(hitVec);
					if (distSq < minDistanceSq) {
						minDistanceSq = distSq;
						backtrackHit = new EntityHitResult(targetEntity, hitVec);
					}
				}
			}
		}

		if (backtrackHit != null) {
			cir.setReturnValue(backtrackHit);
		}
	}
}
