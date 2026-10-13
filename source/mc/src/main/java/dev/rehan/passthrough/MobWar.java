package dev.rehan.passthrough;

import dev.rehan.passthrough.mixin.MobAccessor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

/**
 * Minecraft's mobs vs the host's people. The host lists its people ("peds"); each gets an invisible, AI-less
 * villager "proxy" here that follows it and that hostile mobs hunt. A mob's hit on a proxy goes back to the host
 * ("mobhit"), which hurts the real person. The other way round, the host keeps a stand-in for every mob ("mobs") that
 * its police shoot at, and sends the damage back ("mobdmg").
 */
public final class MobWar {
	public static final String PROXY_TAG = "gta_proxy";
	public static final String PROXY_HOSTILE_TAG = "gta_hostile";
	/** Mobs hunt proxies this far away (blocks). */
	private static final double FOLLOW_RANGE = 48.0;
	/** Host people further than this from the player aren't mirrored, nor mobs reported (blocks). */
	private static final double REPORT_RANGE = 96.0;

	/** The newest "peds" list from the host, flattened [handle, x, y, z, ...] (WebSocket thread -> server tick). */
	private static final AtomicReference<double[]> peds = new AtomicReference<>();
	private static volatile long pedsNanos;
	/** Host damage to mobs, [entity id, amount] (WebSocket thread -> server tick). */
	private static final ConcurrentLinkedQueue<double[]> damage = new ConcurrentLinkedQueue<>();
	/** Host damage to the player (WebSocket thread -> server tick). */
	private static final ConcurrentLinkedQueue<Double> playerDamage = new ConcurrentLinkedQueue<>();

	// server thread only
	private static final Map<Integer, Villager> proxies = new HashMap<>();
	private static final Map<Integer, Integer> handleOf = new HashMap<>();
	private static final Map<Integer, Integer> missingTicks = new HashMap<>();
	private static final Map<Integer, Long> dragonSweepHits = new HashMap<>();
	private static boolean reportedMobs;
	private static boolean reportedWaterPeds;

	private enum DragonCombatState {
		CIRCLING,
		SWEEP_DIVE,
		PULL_UP
	}

	private static class DragonData {
		DragonCombatState state = DragonCombatState.CIRCLING;
		int stateTicks = 0;
		double circleAngle = 0.0;
		Vec3 currentFlightTarget = null;
		int lastFireballTick = 0;
		Vec3 sweepDir = null;
	}

	private static final Map<Integer, DragonData> dragonStates = new HashMap<>();

	public static Vec3 getDragonFlightTarget(final net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon) {
		DragonData data = dragonStates.get(dragon.getId());
		return data != null ? data.currentFlightTarget : null;
	}

	public static boolean hasActiveDragonTarget(final net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon) {
		return dragonStates.containsKey(dragon.getId());
	}

	private MobWar() {
	}

	/** Brain-driven hostiles: they pick targets from brain memories, not goals, so they'd never join the fight. */
	private static final java.util.Set<String> BRAIN_MOBS = java.util.Set.of("piglin", "piglin_brute", "hoglin", "zoglin", "breeze", "creaking", "warden");

	/**
	 * A host person's stand-in. The server knows its tag; the client only sees that it's an invisible villager (a
	 * permanent hidden invisibility effect keeps that flag set: Minecraft resets it on sync otherwise).
	 */
	public static boolean isProxy(final Entity e) {
		return e instanceof Villager && (e.isInvisible() || e.entityTags().contains(PROXY_TAG));
	}

	public static boolean isHostileProxy(final Entity e) {
		return isProxy(e) && e.entityTags().contains(PROXY_HOSTILE_TAG);
	}

	public static boolean isNetherMob(final Entity e) {
		return e instanceof net.minecraft.world.entity.monster.Ghast
			|| e instanceof net.minecraft.world.entity.monster.Blaze
			|| e instanceof net.minecraft.world.entity.monster.cubemob.MagmaCube
			|| e instanceof net.minecraft.world.entity.monster.skeleton.WitherSkeleton
			|| e instanceof net.minecraft.world.entity.monster.zombie.ZombifiedPiglin
			|| e instanceof net.minecraft.world.entity.monster.piglin.AbstractPiglin
			|| e instanceof net.minecraft.world.entity.monster.hoglin.Hoglin
			|| e instanceof net.minecraft.world.entity.monster.Strider;
	}

	public static boolean isDefender(final Entity e) {
		return (e instanceof net.minecraft.world.entity.animal.golem.IronGolem golem && golem.isAlive())
			|| (e instanceof net.minecraft.world.entity.animal.wolf.Wolf wolf && wolf.isAlive());
	}

	/** Mobs that join the fight: hostile, goal-driven ones as well as friendly defenders (wolves, iron golems). */
	private static boolean fighter(final Entity e) {
		if (e == null || isProxy(e) || !e.isAlive()) {
			return false;
		}
		if (isDefender(e)) {
			return true;
		}
		return e instanceof Mob && e instanceof Enemy && !BRAIN_MOBS.contains(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath());
	}

	public static void peds(final double[] flat) {
		peds.set(flat);
		pedsNanos = System.nanoTime();
	}

	public static void damage(final int id, final double amount) {
		damage.add(new double[] {id, amount});
	}

	public static void playerDamage(final double amount) {
		playerDamage.add(amount);
	}

	/** New mobs (and mobs loaded again) hunt proxies; stale proxies from an earlier session are removed. */
	static void onEntityLoad(final Entity e, final ServerLevel level) {
		if (e instanceof Villager v && v.entityTags().contains(PROXY_TAG) && !proxies.containsValue(v)) {
			v.discard();
			return;
		}

		if (e instanceof net.minecraft.world.entity.animal.golem.IronGolem || e instanceof net.minecraft.world.entity.animal.wolf.Wolf) {
			Mob mob = (Mob) e;
			GoalSelector targets = ((MobAccessor) mob).passthrough$targetSelector();
			if (targets.getAvailableGoals().stream().noneMatch(w -> w.getGoal() instanceof HostilePedTargetGoal)) {
				targets.addGoal(1, new HostilePedTargetGoal(mob));
			}
			AttributeInstance range = mob.getAttribute(Attributes.FOLLOW_RANGE);
			if (range != null && range.getBaseValue() < FOLLOW_RANGE) {
				range.setBaseValue(FOLLOW_RANGE);
			}
			return;
		}

		if (!fighter(e)) {
			return;
		}

		Mob mob = (Mob) e;
		GoalSelector targets = ((MobAccessor) mob).passthrough$targetSelector();
		if (targets.getAvailableGoals().stream().noneMatch(w -> w.getGoal() instanceof ProxyTargetGoal)) {
			targets.addGoal(2, new ProxyTargetGoal(mob));
		}

		AttributeInstance range = mob.getAttribute(Attributes.FOLLOW_RANGE);
		if (range != null && range.getBaseValue() < FOLLOW_RANGE) {
			range.setBaseValue(FOLLOW_RANGE);
		}
	}

	/** A proxy was hurt (server thread, from LivingEntity.hurtServer; the damage itself is always cancelled). */
	public static void onProxyHit(final LivingEntity proxy, final DamageSource source, final float amount) {
		Integer handle = handleOf.get(proxy.getId());
		Entity attacker = source.getEntity();
		if (source.getDirectEntity() instanceof Projectile projectile && !(attacker instanceof Player)) {
			projectile.discard(); // a skeleton's arrow ends in the person it hit
		}

		// explosions are the host's own already (every Minecraft explosion is mirrored); Steve's hits are handled there too
		if (handle == null || attacker == null || attacker instanceof Player || source.is(DamageTypeTags.IS_EXPLOSION)) {
			return;
		}

		String kind = BuiltInRegistries.ENTITY_TYPE.getKey(attacker.getType()).getPath();
		Passthrough.events.accept(String.format(Locale.ROOT, "{\"t\":\"mobhit\",\"h\":%d,\"d\":%.2f,\"from\":[%.3f,%.3f,%.3f],\"k\":\"%s\"}",
			handle, amount, attacker.getX(), attacker.getY(), attacker.getZ(), kind));
	}

	/** Every server tick (server thread). */
	static void tick(final MinecraftServer s) {
		ServerLevel level = s.overworld();
		syncProxies(level);
		updateDefenders(level);
		applyDamage(level);
		protectBosses(level);
		if (Passthrough.active) {
			reportMobs(s, level);
			reportWaterCurrents(level);
		}
	}

	/** Prevents Wither and Ender Dragon bosses from falling through unloaded collision/ground, regulates Wither height, and runs dragon tactical sweeping combat. */
	private static void protectBosses(final ServerLevel level) {
		for (Entity e : level.getAllEntities()) {
			if (e instanceof net.minecraft.world.entity.boss.wither.WitherBoss wither && wither.isAlive()) {
				double ground = findGroundLevel(level, wither);
				// A. Anti-clip ground protection: ONLY trigger if wither actually clips below ground surface
				if (wither.getY() < ground) {
					wither.setPos(wither.getX(), ground + 0.5, wither.getZ());
					Vec3 vel = wither.getDeltaMovement();
					if (vel.y < 0) {
						wither.setDeltaMovement(vel.x, Math.max(0.2, -vel.y * 0.5), vel.z);
					}
				}

				// B. Prevent Wither from floating off into the stratosphere:
				LivingEntity target = nearestProxy(level, wither);
				if (target == null && !level.getServer().getPlayerList().getPlayers().isEmpty()) {
					target = level.getServer().getPlayerList().getPlayers().get(0);
				}

				if (target != null) {
					double targetY = target.getY();
					double desiredY = targetY + 3.5; // ideal strafe/bombing height: 3.5 blocks above target
					double heightDiff = wither.getY() - desiredY;
					Vec3 vel = wither.getDeltaMovement();

					// If wither is floating too high above its target, gently pull it down into combat range
					if (heightDiff > 2.5) {
						wither.setDeltaMovement(vel.x, Math.max(-0.4, vel.y - 0.08), vel.z);
					}
				}
				continue;
			}

			if (e instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon && dragon.isAlive()) {
				double ground = findGroundLevel(level, dragon);
				// Anti-clip ground protection: keep dragon from dropping under unstreamed host terrain
				if (dragon.getY() < ground + 0.5) {
					dragon.setPos(dragon.getX(), ground + 1.2, dragon.getZ());
					Vec3 vel = dragon.getDeltaMovement();
					if (vel.y < 0) {
						dragon.setDeltaMovement(vel.x * 1.1, Math.max(0.3, -vel.y * 0.5), vel.z * 1.1);
					}
				}

				// Find combat target: nearest proxy or player
				LivingEntity target = nearestProxy(level, dragon);
				if (target == null && !level.getServer().getPlayerList().getPlayers().isEmpty()) {
					target = level.getServer().getPlayerList().getPlayers().get(0);
				}
				if (target == null) {
					continue;
				}

				DragonData data = dragonStates.computeIfAbsent(dragon.getId(), id -> new DragonData());
				data.stateTicks++;
				Vec3 targetPos = target.position();
				double targetGround = findGroundLevel(level, target);

				if (data.state == DragonCombatState.CIRCLING) {
					// Orbit above combat center at radius 35 blocks, height +22 blocks
					data.circleAngle += 0.035;
					double ox = Math.cos(data.circleAngle) * 35.0;
					double oz = Math.sin(data.circleAngle) * 35.0;
					data.currentFlightTarget = new Vec3(targetPos.x + ox, targetGround + 22.0, targetPos.z + oz);

					// Fireball barrage every ~4-5 seconds
					if (data.stateTicks - data.lastFireballTick > 90) {
						data.lastFireballTick = data.stateTicks;
						Vec3 dPos = dragon.position();
						Vec3 aim = targetPos.subtract(dPos).normalize();
						var fireball = new net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball(level, dragon, aim);
						fireball.setPos(dPos.x, dPos.y - 1.2, dPos.z);
						level.addFreshEntity(fireball);
						level.playSound(null, dragon.blockPosition(), net.minecraft.sounds.SoundEvents.ENDER_DRAGON_SHOOT, net.minecraft.sounds.SoundSource.HOSTILE, 2.5f, 0.9f);
					}

					// After circling for ~8 seconds (160 ticks), initiate sweeping dive
					if (data.stateTicks > 160) {
						data.state = DragonCombatState.SWEEP_DIVE;
						data.stateTicks = 0;
						Vec3 dFlat = targetPos.subtract(dragon.position());
						Vec3 horizontal = new Vec3(dFlat.x, 0, dFlat.z).normalize();
						if (horizontal.lengthSqr() < 0.01) {
							horizontal = new Vec3(1, 0, 0);
						}
						data.sweepDir = horizontal;
						// Aim for ground level right past target (+20 blocks forward)
						data.currentFlightTarget = new Vec3(targetPos.x + horizontal.x * 20.0, targetGround + 1.5, targetPos.z + horizontal.z * 20.0);
						level.playSound(null, dragon.blockPosition(), net.minecraft.sounds.SoundEvents.ENDER_DRAGON_GROWL, net.minecraft.sounds.SoundSource.HOSTILE, 3.5f, 0.85f);
					}
				} else if (data.state == DragonCombatState.SWEEP_DIVE) {
					// Low altitude dive: aggressively pull down towards ground and accelerate forward
					Vec3 toAim = data.currentFlightTarget.subtract(dragon.position()).normalize();
					Vec3 vel = dragon.getDeltaMovement();
					dragon.setDeltaMovement(vel.x * 0.8 + toAim.x * 0.45, Math.max(-0.6, vel.y * 0.8 + toAim.y * 0.45), vel.z * 0.8 + toAim.z * 0.45);

					double distToDiveTarget = dragon.position().subtract(data.currentFlightTarget).horizontalDistance();
					if (distToDiveTarget < 10.0 || data.stateTicks > 80) {
						// Finished low altitude sweep pass, pull up!
						data.state = DragonCombatState.PULL_UP;
						data.stateTicks = 0;
						Vec3 fwd = data.sweepDir != null ? data.sweepDir : new Vec3(dragon.getDeltaMovement().x, 0, dragon.getDeltaMovement().z).normalize();
						data.currentFlightTarget = new Vec3(dragon.getX() + fwd.x * 35.0, targetGround + 26.0, dragon.getZ() + fwd.z * 35.0);
					}
				} else if (data.state == DragonCombatState.PULL_UP) {
					// Rapidly ascend back into the sky
					Vec3 vel = dragon.getDeltaMovement();
					dragon.setDeltaMovement(vel.x * 0.9, Math.min(0.65, vel.y + 0.12), vel.z * 0.9);
					if (dragon.getY() > targetGround + 18.0 || data.stateTicks > 60) {
						data.state = DragonCombatState.CIRCLING;
						data.stateTicks = 0;
					}
				}

				// Sweeping collision attack detection: hits peds when dragon flies close
				long now = System.currentTimeMillis();
				for (Map.Entry<Integer, Villager> entry : proxies.entrySet()) {
					Villager v = entry.getValue();
					if (v == null || v.isRemoved()) {
						continue;
					}

					double distSq = v.distanceToSqr(dragon);
					// Hit radius: 10.5 blocks around dragon body
					if (distSq < 10.5 * 10.5) {
						int handle = entry.getKey();
						Long lastHit = dragonSweepHits.get(handle);
						if (lastHit == null || now - lastHit > 1000L) {
							dragonSweepHits.put(handle, now);
							Vec3 dVel = dragon.getDeltaMovement();
							double speed = dVel.length();
							double sweepDmg = Math.max(16.0, speed * 28.0);
							Passthrough.events.accept(String.format(Locale.ROOT,
								"{\"t\":\"mobhit\",\"h\":%d,\"d\":%.2f,\"from\":[%.3f,%.3f,%.3f],\"k\":\"ender_dragon\"}",
								handle, sweepDmg, dragon.getX(), dragon.getY(), dragon.getZ()));
							level.playSound(null, v.blockPosition(),
								net.minecraft.sounds.SoundEvents.ENDER_DRAGON_GROWL,
								net.minecraft.sounds.SoundSource.HOSTILE, 2.0f, 0.95f + level.getRandom().nextFloat() * 0.1f);
						}
					}
				}
			}
		}
	}

	private static double findGroundLevel(final ServerLevel level, final Entity entity) {
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		int ex = (int) Math.floor(entity.getX());
		int ez = (int) Math.floor(entity.getZ());
		int ey = (int) Math.floor(entity.getY());

		// 1. Scan downwards from entity Y down to bottom of world for real solid ground/barriers
		int startY = Math.min(ey, level.getMaxY() - 1);
		int minY = Math.max(level.getMinY(), startY - 128);
		for (int y = startY; y >= minY; y--) {
			p.set(ex, y, ez);
			if (level.isInWorldBounds(p)) {
				var state = level.getBlockState(p);
				if (!state.isAir() && state.getFluidState().isEmpty()) {
					return p.getY() + 1.0;
				}
			}
		}

		// 2. Fallback when collision isn't loaded: use nearest GTA ped proxy height
		double bestDistSq = Double.MAX_VALUE;
		double refGroundY = Double.NaN;
		for (Villager v : proxies.values()) {
			if (v != null && !v.isRemoved()) {
				double dx = v.getX() - entity.getX();
				double dz = v.getZ() - entity.getZ();
				double d2 = dx * dx + dz * dz;
				if (d2 < bestDistSq) {
					bestDistSq = d2;
					refGroundY = v.getY();
				}
			}
		}

		if (!Double.isNaN(refGroundY) && bestDistSq < 150.0 * 150.0) {
			int gy = (int) Math.floor(refGroundY);
			WorldBridge.solid(new int[] {
				ex - 1, ez - 1, gy - 1, gy,
				ex,     ez - 1, gy - 1, gy,
				ex + 1, ez - 1, gy - 1, gy,
				ex - 1, ez,     gy - 1, gy,
				ex,     ez,     gy - 1, gy,
				ex + 1, ez,     gy - 1, gy,
				ex - 1, ez + 1, gy - 1, gy,
				ex,     ez + 1, gy - 1, gy,
				ex + 1, ez + 1, gy - 1, gy
			});
			return refGroundY;
		}

		// Fallback to first player's feet height
		ServerPlayer player = level.getServer().getPlayerList().getPlayers().isEmpty() ? null : level.getServer().getPlayerList().getPlayers().get(0);
		if (player != null) {
			return player.getY();
		}

		return 0.0;
	}

	private static void syncProxies(final ServerLevel level) {
		if (!proxies.isEmpty() && System.nanoTime() - pedsNanos > 1_500_000_000L) {
			clearProxies(); // the host stopped sending: nobody to hunt
			return;
		}

		double[] p = peds.getAndSet(null);
		if (p == null) {
			return;
		}

		Map<Integer, Boolean> seen = new HashMap<>();
		int stride = (p.length % 5 == 0) ? 5 : 4;
		for (int i = 0; i + (stride - 1) < p.length; i += stride) {
			int handle = (int) p[i];
			double x = p[i + 1], y = p[i + 2], z = p[i + 3];
			boolean hostile = stride >= 5 && p[i + 4] > 0.5;
			seen.put(handle, true);
			Villager v = proxies.get(handle);
			if (v == null || v.isRemoved()) {
				v = EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
				if (v == null) {
					continue;
				}

				v.setInvisible(true);
				v.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, MobEffectInstance.INFINITE_DURATION, 0, false, false), null);
				v.setNoAi(true);
				v.setNoGravity(true);
				v.setSilent(true);
				v.addTag(PROXY_TAG);
				if (hostile) {
					v.addTag(PROXY_HOSTILE_TAG);
				}
				v.snapTo(x, y, z, 0.0F, 0.0F);
				proxies.put(handle, v); // before adding: the load event must not take it for a stale one
				if (!level.addFreshEntity(v)) {
					proxies.remove(handle);
					continue;
				}

				handleOf.put(v.getId(), handle);
			} else {
				v.setPos(x, y, z);
				if (hostile) {
					v.addTag(PROXY_HOSTILE_TAG);
				} else {
					v.removeTag(PROXY_HOSTILE_TAG);
				}
			}

			missingTicks.remove(handle);
		}

		// hysteresis: a person missing from a few lists in a row (out of range, dead, despawned) loses their proxy
		for (Iterator<Map.Entry<Integer, Villager>> it = proxies.entrySet().iterator(); it.hasNext();) {
			Map.Entry<Integer, Villager> e = it.next();
			if (seen.containsKey(e.getKey())) {
				continue;
			}

			int missing = missingTicks.merge(e.getKey(), 1, Integer::sum);
			if (missing > 6 || e.getValue().isRemoved()) {
				handleOf.remove(e.getValue().getId());
				e.getValue().discard();
				missingTicks.remove(e.getKey());
				it.remove();
			}
		}
	}

	private static void applyDamage(final ServerLevel level) {
		for (double[] d; (d = damage.poll()) != null;) {
			if (!(level.getEntity((int) d[0]) instanceof Mob mob) || !mob.isAlive()) {
				continue;
			}

			// from the nearest person (a cop, most likely), so the mob turns on them
			LivingEntity from = nearestProxy(level, mob);
			DamageSource source = from != null ? level.damageSources().mobAttack(from) : level.damageSources().generic();
			mob.damageCooldownTime = 0; // automatic fire lands several hits inside the usual 10-tick cooldown
			mob.hurtServer(level, source, (float) d[1]);
		}
		
		for (Double d; (d = playerDamage.poll()) != null;) {
			for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
				if (player.isBlocking()) {
					level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.SHIELD_BLOCK.value(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 0.8f + level.getRandom().nextFloat() * 0.4f);
				} else {
					player.hurtServer(level, level.damageSources().generic(), d.floatValue());
				}
			}
		}
	}

	private static LivingEntity nearestProxy(final ServerLevel level, final Mob mob) {
		LivingEntity best = null;
		double bestD = 48.0 * 48.0;
		for (Villager v : proxies.values()) {
			double dd = v.distanceToSqr(mob);
			if (!v.isRemoved() && dd < bestD) {
				best = v;
				bestD = dd;
			}
		}

		return best;
	}

	private static void updateDefenders(final ServerLevel level) {
		for (Entity e : level.getAllEntities()) {
			if (e instanceof net.minecraft.world.entity.animal.golem.IronGolem golem && golem.isAlive()) {
				checkDefenderTarget(golem, 32.0);
				protectDefender(level, golem);
			} else if (e instanceof net.minecraft.world.entity.animal.wolf.Wolf wolf && wolf.isAlive() && wolf.isTame() && !wolf.isOrderedToSit()) {
				checkDefenderTarget(wolf, 24.0);
				protectDefender(level, wolf);
			} else if (e instanceof net.minecraft.world.entity.animal.wolf.Wolf wolf && wolf.isAlive()) {
				protectDefender(level, wolf);
			}
		}
	}

	private static void protectDefender(final ServerLevel level, final Mob defender) {
		double ground = findDefenderGroundLevel(level, defender);
		if (Double.isNaN(ground)) {
			return;
		}

		// Anti-fall ground anchoring: prevent clipping or falling below world ground
		if (defender.getY() < ground) {
			defender.setPos(defender.getX(), ground, defender.getZ());
			defender.fallDistance = 0.0F;
			Vec3 vel = defender.getDeltaMovement();
			if (vel.y < 0) {
				defender.setDeltaMovement(vel.x, 0.0, vel.z);
			}
		}

		int ex = (int) Math.floor(defender.getX());
		int ez = (int) Math.floor(defender.getZ());
		int gy = (int) Math.floor(ground) - 1;

		Vec3 vel = defender.getDeltaMovement();
		int nextX = (int) Math.floor(defender.getX() + vel.x * 2.0);
		int nextZ = (int) Math.floor(defender.getZ() + vel.z * 2.0);

		ensureDefenderGround(ex, ez, gy);
		if (nextX != ex || nextZ != ez) {
			ensureDefenderGround(nextX, nextZ, gy);
		}
	}

	private static double findDefenderGroundLevel(final ServerLevel level, final Entity entity) {
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		int ex = (int) Math.floor(entity.getX());
		int ez = (int) Math.floor(entity.getZ());
		int ey = (int) Math.floor(entity.getY());

		// 1. Scan locally (including up to 6 blocks above entity Y in case it clipped down, and 32 blocks below)
		int startY = Math.min(ey + 6, level.getMaxY() - 1);
		int minY = Math.max(level.getMinY(), ey - 32);
		double highestGround = Double.NaN;

		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				for (int y = startY; y >= minY; y--) {
					p.set(ex + dx, y, ez + dz);
					if (level.isInWorldBounds(p)) {
						var state = level.getBlockState(p);
						if (!state.isAir() && state.getFluidState().isEmpty()) {
							double g = p.getY() + 1.0;
							if (Double.isNaN(highestGround) || g > highestGround) {
								highestGround = g;
							}
							break;
						}
					}
				}
			}
		}

		if (!Double.isNaN(highestGround)) {
			return highestGround;
		}

		// 2. Fallback when collision isn't loaded: use nearest GTA ped proxy height
		double bestDistSq = Double.MAX_VALUE;
		double refGroundY = Double.NaN;
		for (Villager v : proxies.values()) {
			if (v != null && !v.isRemoved()) {
				double dx = v.getX() - entity.getX();
				double dz = v.getZ() - entity.getZ();
				double d2 = dx * dx + dz * dz;
				if (d2 < bestDistSq) {
					bestDistSq = d2;
					refGroundY = v.getY();
				}
			}
		}

		if (!Double.isNaN(refGroundY) && bestDistSq < 150.0 * 150.0) {
			return refGroundY;
		}

		// 3. Fallback to first player's feet height
		ServerPlayer player = level.getServer().getPlayerList().getPlayers().isEmpty() ? null : level.getServer().getPlayerList().getPlayers().get(0);
		if (player != null) {
			return player.getY();
		}

		return 0.0;
	}

	private static void ensureDefenderGround(final int cx, final int cz, final int gy) {
		WorldBridge.solid(new int[] {
			cx - 1, cz - 1, gy - 1, gy,
			cx,     cz - 1, gy - 1, gy,
			cx + 1, cz - 1, gy - 1, gy,
			cx - 1, cz,     gy - 1, gy,
			cx,     cz,     gy - 1, gy,
			cx + 1, cz,     gy - 1, gy,
			cx - 1, cz + 1, gy - 1, gy,
			cx,     cz + 1, gy - 1, gy,
			cx + 1, cz + 1, gy - 1, gy
		});
	}

	private static void checkDefenderTarget(final Mob defender, final double range) {
		LivingEntity target = defender.getTarget();
		if (target != null && target.isAlive() && isHostileProxy(target)) {
			return;
		}
		LivingEntity nearest = nearestHostileProxy(defender, range);
		if (nearest != null) {
			defender.setTarget(nearest);
		}
	}

	private static LivingEntity nearestHostileProxy(final Mob mob, final double maxDist) {
		LivingEntity best = null;
		double bestD = maxDist * maxDist;
		for (Villager v : proxies.values()) {
			if (v != null && !v.isRemoved() && isHostileProxy(v)) {
				double dd = v.distanceToSqr(mob);
				if (dd < bestD) {
					best = v;
					bestD = dd;
				}
			}
		}
		return best;
	}

	/** {"t":"mobs","m":[[id,"zombie",x,y,z],...]}: every fighting mob near the player, every tick while there are any. */
	private static void reportMobs(final MinecraftServer s, final ServerLevel level) {
		ServerPlayer player = s.getPlayerList().getPlayers().isEmpty() ? null : s.getPlayerList().getPlayers().get(0);
		if (player == null) {
			return;
		}

		List<Entity> near = level.getEntities((Entity) null, player.getBoundingBox().inflate(REPORT_RANGE), MobWar::fighter);
		if (near.isEmpty() && !reportedMobs) {
			return;
		}

		StringBuilder b = new StringBuilder("{\"t\":\"mobs\",\"m\":[");
		int n = 0;
		for (Entity e : near) {
			if (!e.isAlive()) {
				continue;
			}

			b.append(n++ == 0 ? "" : ",").append(String.format(Locale.ROOT, "[%d,\"%s\",%.3f,%.3f,%.3f]",
				e.getId(), BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath(), e.getX(), e.getY(), e.getZ()));
		}

		Passthrough.events.accept(b.append("]}").toString());
		reportedMobs = n > 0;
	}

	/** {"t":"waterpeds","p":[[handle,dirX,dirY,dirZ],...]}: GTA peds whose proxies touch water (source or flowing). */
	private static void reportWaterCurrents(final ServerLevel level) {
		if (proxies.isEmpty()) {
			if (reportedWaterPeds) {
				Passthrough.events.accept("{\"t\":\"waterpeds\",\"p\":[]}");
				reportedWaterPeds = false;
			}
			return;
		}

		StringBuilder b = new StringBuilder("{\"t\":\"waterpeds\",\"p\":[");
		int count = 0;
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();

		for (Map.Entry<Integer, Villager> entry : proxies.entrySet()) {
			Villager v = entry.getValue();
			if (v == null || v.isRemoved()) {
				continue;
			}

			AABB box = v.getBoundingBox();
			int minX = (int) Math.floor(box.minX);
			int maxX = (int) Math.floor(box.maxX);
			int minY = (int) Math.floor(box.minY - 0.15); // detect feet touching water
			int maxY = (int) Math.floor(box.maxY);
			int minZ = (int) Math.floor(box.minZ);
			int maxZ = (int) Math.floor(box.maxZ);

			boolean inWater = false;
			double flowX = 0.0;
			double flowY = 0.0;
			double flowZ = 0.0;

			for (int x = minX; x <= maxX; x++) {
				for (int y = minY; y <= maxY; y++) {
					for (int z = minZ; z <= maxZ; z++) {
						p.set(x, y, z);
						FluidState fluid = level.getFluidState(p);
						if (fluid.is(FluidTags.WATER)) {
							inWater = true;
							Vec3 f = fluid.getFlow(level, p);
							flowX += f.x;
							flowY += f.y;
							flowZ += f.z;
						}
					}
				}
			}

			if (inWater) {
				double lenSq = flowX * flowX + flowY * flowY + flowZ * flowZ;
				double gx, gy, gz;
				if (lenSq > 1e-4) {
					double len = Math.sqrt(lenSq);
					double nx = flowX / len;
					double ny = flowY / len;
					double nz = flowZ / len;
					// Minecraft (x, y, z) -> GTA (x, -z, y)
					gx = nx;
					gy = -nz;
					gz = ny;
				} else {
					// Standing / source water: default downward settle, ASI pushes forward
					gx = 0.0;
					gy = 0.0;
					gz = -0.15;
				}

				b.append(count++ == 0 ? "" : ",").append(String.format(Locale.ROOT, "[%d,%.3f,%.3f,%.3f]",
					entry.getKey(), gx, gy, gz));
			}
		}

		if (count > 0 || reportedWaterPeds) {
			Passthrough.events.accept(b.append("]}").toString());
			reportedWaterPeds = count > 0;
		}
	}

	/**
	 * Spawn `count` mobs of `kind` (e.g. "zombie") on the ground `minR`..`maxR` blocks from the player, within `arc`
	 * degrees either side of where the player faces (or of `yawOffset` from it); or, given `at` ({x, y, z}: a fixed
	 * spot, y near its ground), all round that spot instead, wherever the player is.
	 */
	public static void spawn(final String kind, final int count, final double minR, final double maxR, final double arc, final double yawOffset,
		final double[] at) {
		MinecraftServer s = serverOrNull();
		if (s == null) {
			return;
		}

		s.execute(() -> {
			ServerLevel level = s.overworld();
			ServerPlayer player = s.getPlayerList().getPlayers().isEmpty() ? null : s.getPlayerList().getPlayers().get(0);
			Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.withDefaultNamespace(kind));
			if (player == null || type.isEmpty()) {
				Passthrough.LOG.warn("spawnmobs: no player or unknown mob {}", kind);
				return;
			}

			ThreadLocalRandom random = ThreadLocalRandom.current();
			double cx = at != null ? at[0] : player.getX(), cy = at != null ? at[1] : player.getY(), cz = at != null ? at[2] : player.getZ();
			double baseYaw = at != null ? 0.0 : player.getYRot() + yawOffset, spread = at != null ? 180.0 : arc;
			int spawned = 0;
			for (int i = 0; i < count * 4 && spawned < count; i++) {
				double yaw = Math.toRadians(baseYaw + (random.nextDouble() * 2.0 - 1.0) * spread);
				double r = minR + random.nextDouble() * (maxR - minR);
				int x = (int) Math.floor(cx - Math.sin(yaw) * r), z = (int) Math.floor(cz + Math.cos(yaw) * r);
				BlockPos ground = groundAt(level, x, (int) Math.floor(cy), z);
				if (ground == null) {
					continue; // no host ground there (yet)
				}

				Entity e = type.get().spawn(level, ground, EntitySpawnReason.COMMAND);
				if (e instanceof Mob mob) {
					mob.setPersistenceRequired();
					spawned++;
				}
			}

			Passthrough.LOG.info("spawnmobs: {} x {}", spawned, kind);
		});
	}

	/** The free block above the host's ground near height y in column (x, z), or null. */
	private static BlockPos groundAt(final ServerLevel level, final int x, final int y, final int z) {
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for (int dy = 6; dy >= -12; dy--) {
			p.set(x, y + dy, z);
			if (!level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir() && level.getBlockState(p.above(2)).isAir()) {
				return p.above().immutable();
			}
		}

		return null;
	}

	/** Remove every fighting mob (not the player's other things). */
	public static void clearMobs() {
		MinecraftServer s = serverOrNull();
		if (s != null) {
			s.execute(() -> discardFighters(s.overworld()));
		}
	}

	/** Server thread. */
	static void discardFighters(final ServerLevel level) {
		List<Entity> all = new ArrayList<>();
		level.getAllEntities().forEach(all::add);
		all.stream().filter(e -> (fighter(e) && !isDefender(e)) || isProxy(e) || isNetherMob(e)).forEach(Entity::discard);
	}

	/** The host went away: nobody left to hunt. */
	public static void hostGone() {
		MinecraftServer s = serverOrNull();
		if (s != null) {
			s.execute(MobWar::clearProxies);
		}
	}

	private static void clearProxies() {
		proxies.values().forEach(Entity::discard);
		proxies.clear();
		handleOf.clear();
		missingTicks.clear();
		dragonSweepHits.clear();
		dragonStates.clear();
		reportedWaterPeds = false;
	}

	static void detach(final MinecraftServer s) {
		// the world is saved next: none of the fight is kept in it
		discardFighters(s.overworld());
		proxies.clear();
		handleOf.clear();
		missingTicks.clear();
		dragonSweepHits.clear();
		dragonStates.clear();
		peds.set(null);
		damage.clear();
		reportedMobs = false;
		reportedWaterPeds = false;
	}

	private static MinecraftServer serverOrNull() {
		return WorldBridge.server();
	}
}
