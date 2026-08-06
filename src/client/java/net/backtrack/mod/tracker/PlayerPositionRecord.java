package net.backtrack.mod.tracker;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record PlayerPositionRecord(
	AABB boundingBox,
	Vec3 position,
	long timestampMs,
	int ageTicks
) {}
