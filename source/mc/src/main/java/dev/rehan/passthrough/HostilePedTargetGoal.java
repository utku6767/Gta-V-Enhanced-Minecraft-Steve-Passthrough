package dev.rehan.passthrough;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.npc.villager.Villager;

/** Hunt the nearest host person's proxy that is hostile towards the GTA player. */
final class HostilePedTargetGoal extends NearestAttackableTargetGoal<Villager> {
	HostilePedTargetGoal(final Mob mob) {
		super(mob, Villager.class, 10, false, false, (target, level) -> MobWar.isHostileProxy(target));
		this.targetConditions.ignoreInvisibilityTesting().ignoreLineOfSight();
	}

	@Override
	public boolean canUse() {
		if (this.mob instanceof Wolf wolf) {
			if (!wolf.isTame() || wolf.isOrderedToSit()) {
				return false;
			}
		}
		return super.canUse();
	}

	@Override
	public boolean canContinueToUse() {
		if (this.mob instanceof Wolf wolf) {
			if (!wolf.isTame() || wolf.isOrderedToSit()) {
				return false;
			}
		}
		return super.canContinueToUse();
	}
}
