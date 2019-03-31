package com.fantasticsource.dynamicstealth.server.senses.sight;

import com.fantasticsource.dynamicstealth.compat.CompatDissolution;
import com.fantasticsource.dynamicstealth.config.server.senses.sight.SightConfig;
import com.fantasticsource.dynamicstealth.server.Attributes;
import com.fantasticsource.dynamicstealth.server.threat.EntityThreatData;
import com.fantasticsource.dynamicstealth.server.threat.Threat;
import com.fantasticsource.mctools.MCTools;
import com.fantasticsource.mctools.ServerTickTimer;
import com.fantasticsource.tools.Tools;
import com.fantasticsource.tools.datastructures.ExplicitPriorityQueue;
import com.fantasticsource.tools.datastructures.Pair;
import com.fantasticsource.tools.datastructures.WrappingQueue;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.fantasticsource.dynamicstealth.DynamicStealth.TRIG_TABLE;
import static com.fantasticsource.dynamicstealth.config.DynamicStealthConfig.serverSettings;
import static com.fantasticsource.dynamicstealth.server.senses.sight.EntitySightData.*;
import static com.fantasticsource.mctools.ServerTickTimer.currentTick;

public class Sight
{
    private static final int SEEN_RECENT_TIMER = 60, GLOBAL_STEALTH_SMOOTHING = 3;

    private static Map<EntityPlayer, Pair<WrappingQueue<Double>, Long>> globalPlayerStealthHistory = new LinkedHashMap<>();

    private static Map<EntityLivingBase, Map<Entity, SeenData>> recentlySeenMap = new LinkedHashMap<>();
    private static Map<Pair<EntityPlayerMP, Boolean>, LinkedHashMap<EntityLivingBase, Double>> playerSeenThisTickMap = new LinkedHashMap<>();


    @SubscribeEvent
    public static void update(TickEvent.ServerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END)
        {
            playerSeenThisTickMap.clear();
            recentlySeenMap.entrySet().removeIf(Sight::updateRecentlySeen);
            globalPlayerStealthHistory.entrySet().removeIf(Sight::updateStealthHistory);
        }
    }

    private static boolean updateStealthHistory(Map.Entry<EntityPlayer, Pair<WrappingQueue<Double>, Long>> entry)
    {
        if (!entry.getKey().isEntityAlive()) return true;

        Pair<WrappingQueue<Double>, Long> pair = entry.getValue();
        long tick = currentTick();
        if (pair.getValue() != tick)
        {
            pair.getKey().add(1d);
            pair.setValue(tick);
        }
        return false;
    }

    private static boolean updateRecentlySeen(Map.Entry<EntityLivingBase, Map<Entity, SeenData>> entry)
    {
        if (!entry.getKey().isEntityAlive()) return true;

        entry.getValue().entrySet().removeIf(e -> !e.getKey().isEntityAlive());
        return false;
    }


    public static boolean recentlySeen(EntityLivingBase searcher, Entity target)
    {
        if (searcher == null || target == null) return false;

        Map<Entity, SeenData> map = recentlySeenMap.get(searcher);
        if (map == null) return false;

        SeenData data = map.get(target);
        if (data == null) return false;

        return data.seen && currentTick() - data.lastSeenTime < SEEN_RECENT_TIMER;
    }


    public static boolean canSee(EntityLivingBase searcher, Entity target)
    {
        return visualStealthLevel(searcher, target, true, searcher.rotationYawHead, searcher.rotationPitch) <= 1;
    }

    public static boolean canSee(EntityLivingBase searcher, Entity target, boolean useCache)
    {
        return visualStealthLevel(searcher, target, useCache, searcher.rotationYawHead, searcher.rotationPitch) <= 1;
    }

    public static boolean canSee(EntityLivingBase searcher, Entity target, boolean useCache, double yaw, double pitch)
    {
        return visualStealthLevel(searcher, target, useCache, yaw, pitch) <= 1;
    }

    public static double visualStealthLevel(EntityLivingBase searcher, Entity target)
    {
        return visualStealthLevel(searcher, target, true, searcher.rotationYawHead, searcher.rotationPitch);
    }

    public static double visualStealthLevel(EntityLivingBase searcher, Entity target, boolean useCache, double yaw, double pitch)
    {
        if (searcher == null || target == null || !searcher.world.isBlockLoaded(searcher.getPosition()) || !target.world.isBlockLoaded(target.getPosition())) return 777;
        if (searcher.world != target.world) return 777;

        searcher.world.profiler.startSection("DStealth: Visual Stealth");
        Map<Entity, SeenData> map = recentlySeenMap.get(searcher);
        long tick = ServerTickTimer.currentTick();

        //If applicable, load from cache and return
        if (map != null && useCache)
        {
            SeenData data = map.get(target);
            if (data != null && data.lastUpdateTime == tick)
            {
                searcher.world.profiler.endSection();
                return data.lastStealthLevel;
            }
        }

        //Calculate
        double result = visualStealthLevelInternal(searcher, target, yaw, pitch);

        //Save first cache
        if (target instanceof EntityPlayer && ((searcher instanceof EntityLiving && ((EntityLiving) searcher).getAttackTarget() == target) || (!EntityThreatData.isPassive(searcher) && !EntityThreatData.bypassesThreat(searcher))))
        {
            EntityPlayer player = (EntityPlayer) target;
            Pair<WrappingQueue<Double>, Long> pair = globalPlayerStealthHistory.computeIfAbsent(player, k -> new Pair<>(new WrappingQueue<>(GLOBAL_STEALTH_SMOOTHING + 2), tick - 1));
            WrappingQueue<Double> queue = pair.getKey();

            double clampedResult = Tools.min(Tools.max(-1, result - 1), 1);
            if (queue.size() != 0 && pair.getValue() == tick)
            {
                queue.setNewestToOldest(0, Tools.min(clampedResult, queue.getNewestToOldest(0)));
            }
            else queue.add(clampedResult);

            pair.setValue(tick);
        }

        //Save second cache
        if (map == null)
        {
            map = new LinkedHashMap<>();
            recentlySeenMap.put(searcher, map);
            map.put(target, new SeenData(result));
        }
        else
        {
            SeenData data = map.get(target);
            if (data == null) map.put(target, new SeenData(result));
            else
            {
                data.lastUpdateTime = tick;
                data.lastStealthLevel = result;
                if (result <= 1)
                {
                    data.seen = true;
                    data.lastSeenTime = tick;
                }
            }
        }

        searcher.world.profiler.endSection();
        return result;
    }


    public static LinkedHashMap<EntityLivingBase, Double> seenEntities(EntityPlayerMP player)
    {
        player.world.profiler.startSection("DStealth: Seen Entities");
        LinkedHashMap<EntityLivingBase, Double> map = playerSeenThisTickMap.get(new Pair<>(player, false));
        if (map != null)
        {
            player.world.profiler.endSection();
            return (LinkedHashMap<EntityLivingBase, Double>) map.clone();
        }

        map = seenEntitiesInternal(player);
        playerSeenThisTickMap.put(new Pair<>(player, false), (LinkedHashMap<EntityLivingBase, Double>) map.clone());
        player.world.profiler.endSection();
        return map;
    }

    private static LinkedHashMap<EntityLivingBase, Double> seenEntitiesInternal(EntityPlayerMP player)
    {
        LinkedHashMap<EntityLivingBase, Double> result = new LinkedHashMap<>();
        Entity[] loadedEntities = player.world.loadedEntityList.toArray(new Entity[player.world.loadedEntityList.size()]);

        if (serverSettings.senses.usePlayerSenses)
        {
            for (Entity entity : loadedEntities)
            {
                if (entity instanceof EntityLivingBase && entity != player)
                {
                    double stealthLevel = visualStealthLevel(player, entity);
                    if (stealthLevel <= 1) result.put((EntityLivingBase) entity, stealthLevel);
                }
            }
        }
        else
        {
            for (Entity entity : loadedEntities)
            {
                if (entity instanceof EntityLivingBase && entity != player)
                {
                    result.put((EntityLivingBase) entity, -888d);
                }
            }
        }

        return result;
    }


    private static double visualStealthLevelInternal(EntityLivingBase searcher, Entity target, double yaw, double pitch)
    {
        //Hard checks (absolute)
        if (searcher.world != target.world || target.isDead || target instanceof FakePlayer) return 777;

        if (Tracking.isTracking(searcher, target)) return -777;
        if (target instanceof EntityDragon || target instanceof EntityWither) return -777;
        if (searcher instanceof EntityPlayer && CompatDissolution.isPossessing((EntityPlayer) searcher, target)) return -777;
        if (MCTools.isRidingOrRiddenBy(searcher, target)) return -777;

        if (target instanceof EntityPlayerMP && ((EntityPlayerMP) target).capabilities.disableDamage) return 777;

        if (hasSoulSight(searcher)) return -777;


        //Angles and Distances (absolute, base FOV)
        int angleLarge = angleLarge(searcher);
        if (angleLarge == 0) return 777;

        double distSquared = searcher.getDistanceSq(target);
        int distanceFar = distanceFar(searcher);
        if (distSquared > Math.pow(distanceFar, 2)) return 777;

        double distanceThreshold;
        int angleSmall = angleSmall(searcher);
        if (angleSmall == 180) distanceThreshold = distanceFar;
        else
        {
            //Using previous values here to give the player a chance, because client-side rendering always runs behind what's actually happening
            double angleDif = Vec3d.fromPitchYaw((float) pitch, (float) yaw).normalize().dotProduct(new Vec3d(target.posX - searcher.posX, (target.posY + target.height / 2) - (searcher.posY + searcher.getEyeHeight()), target.posZ - searcher.posZ).normalize());

            //And because Vec3d.fromPitchYaw occasionally returns values barely out of the range of (-1, 1)...
            if (angleDif < -1) angleDif = -1;
            else if (angleDif > 1) angleDif = 1;

            angleDif = Tools.radtodeg(TRIG_TABLE.arccos(angleDif)); //0 in front, 180 in back
            if (angleDif > angleLarge) return 777;
            if (angleDif < angleSmall) distanceThreshold = distanceFar;
            else
            {
                int distanceNear = distanceNear(searcher);
                distanceThreshold = distanceNear + (distanceFar - distanceNear) * (angleLarge - angleDif) / (angleLarge - angleSmall);
            }
        }


        //Setup for checks against EntityLivingBase-only targets
        boolean isLivingBase = target instanceof EntityLivingBase;
        EntityLivingBase targetLivingBase = isLivingBase ? (EntityLivingBase) target : null;

        //Glowing (absolute, after Angles)
        SightConfig sight = serverSettings.senses.sight;
        if (sight.g_absolutes.seeGlowing && isLivingBase && targetLivingBase.getActivePotionEffect(MobEffects.GLOWING) != null) return -777;


        //Lighting and LOS checks (absolute, factor, after Angles, after Glowing)
        double lightFactor = bestLightingAtLOSHit(searcher, target, isLivingBase && isBright(targetLivingBase));
        if (target instanceof EntityPlayer && searcher instanceof EntitySpider) System.out.println(lightFactor);
        if (lightFactor == -777) return 777;

        if (hasNightvision(searcher))
        {
            lightFactor = Math.min(15, lightFactor + sight.c_lighting.nightvisionBonus);
        }

        int lightLow = lightLow(searcher);
        if (lightFactor <= lightLow) return 777;
        int lightHigh = lightHigh(searcher);
        lightFactor = lightFactor >= lightHigh ? 1 : lightFactor / (lightHigh - lightLow);


        //Blindness (multiplier)
        double blindnessMultiplier = searcher.getActivePotionEffect(MobEffects.BLINDNESS) != null ? sight.a_stealthMultipliers.blindnessMultiplier : 1;


        //Invisibility (multiplier)
        double invisibilityMultiplier = isLivingBase && targetLivingBase.getActivePotionEffect(MobEffects.INVISIBILITY) != null ? sight.a_stealthMultipliers.invisibilityMultiplier : 1;


        //Alerted multiplier
        double alertMultiplier = searcher instanceof EntityLiving && Threat.get(searcher).threatLevel > 0 ? sight.b_visibilityMultipliers.alertMultiplier : 1;

        //Seen multiplier
        double seenMultiplier = recentlySeen(searcher, target) ? sight.b_visibilityMultipliers.seenMultiplier : 1;

        //Crouching (multiplier)
        double crouchingMultiplier = target.isSneaking() ? sight.a_stealthMultipliers.crouchingMultiplier : 1;


        //Mob Heads (multiplier)
        double mobHeadMultiplier = 1;
        if (isLivingBase)
        {
            ItemStack helmet = targetLivingBase.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
            if (helmet.getItem() == Items.SKULL)
            {
                int damage = helmet.getItemDamage();
                if (target instanceof EntitySkeleton && damage == 0 || target instanceof EntityWitherSkeleton && damage == 1 || target instanceof EntityZombie && damage == 2 || target instanceof EntityCreeper && damage == 4)
                {
                    mobHeadMultiplier = sight.a_stealthMultipliers.mobHeadMultiplier;
                }
            }
            else if ((helmet.getItem() == Item.getItemFromBlock(Blocks.PUMPKIN) && helmet.getItem() == Item.getItemFromBlock(Blocks.LIT_PUMPKIN)) && target instanceof EntitySnowman) mobHeadMultiplier = sight.a_stealthMultipliers.mobHeadMultiplier;
        }


        //Armor
        double armorMultiplier = !isLivingBase ? 1 : Math.max(0, 1 + sight.b_visibilityMultipliers.armorMultiplierCumulative * MathHelper.floor(MCTools.getAttribute(targetLivingBase, SharedMonsterAttributes.ARMOR, 0)));


        //Combine multipliers
        double stealthMultiplier = blindnessMultiplier * Tools.min(invisibilityMultiplier, crouchingMultiplier, mobHeadMultiplier);
        double visibilityMultiplier = armorMultiplier * alertMultiplier * seenMultiplier;
        double configMultipliers = Tools.min(Tools.max(stealthMultiplier * visibilityMultiplier, 0), 1);

        double visReduction = !isLivingBase ? 0 : targetLivingBase.getEntityAttribute(Attributes.VISIBILITY_REDUCTION).getAttributeValue();
        double attributeMultipliers = visReduction == 0 ? Double.MAX_VALUE : searcher.getEntityAttribute(Attributes.SIGHT).getAttributeValue() / visReduction;


        //Final calculation
        return Math.sqrt(distSquared) / (distanceThreshold * lightFactor * configMultipliers * attributeMultipliers);
    }


    private static double bestLightingAtLOSHit(Entity searcher, Entity target, boolean forceMaxLight)
    {
        World world = searcher.world;
        if (world != target.world) return -777;

        double halfWidth = target.width / 2;
        double halfHeight = target.height / 2;

        double x = target.posX;
        double y = target.posY + halfHeight;
        double z = target.posZ;


        ExplicitPriorityQueue<Vec3d> queue = new ExplicitPriorityQueue<>();
        Vec3d testVec = new Vec3d(x, y, z); //Center

        if (forceMaxLight)
        {
            queue.add(testVec, 0);
            testVec = new Vec3d(x, y + halfHeight, z); //+Y
            queue.add(testVec, 0);
            testVec = new Vec3d(x, y - halfHeight, z); //-Y
            queue.add(testVec, 0);
            testVec = new Vec3d(x + halfWidth, y, z); //+X
            queue.add(testVec, 0);
            testVec = new Vec3d(x - halfWidth, y, z); //-X
            queue.add(testVec, 0);
            testVec = new Vec3d(x, y, z + halfWidth); //+Z
            queue.add(testVec, 0);
            testVec = new Vec3d(x, y, z - halfWidth); //-Z
            queue.add(testVec, 0);
        }
        else
        {
            queue.add(testVec, 15 - lightLevelTotal(world, testVec));
            testVec = new Vec3d(x, y + halfHeight, z); //+Y
            queue.add(testVec, 15 - lightLevelTotal(world, testVec));
            testVec = new Vec3d(x, y - halfHeight, z); //-Y
            queue.add(testVec, 15 - lightLevelTotal(world, testVec));
            testVec = new Vec3d(x + halfWidth, y, z); //+X
            queue.add(testVec, 15 - lightLevelTotal(world, testVec));
            testVec = new Vec3d(x - halfWidth, y, z); //-X
            queue.add(testVec, 15 - lightLevelTotal(world, testVec));
            testVec = new Vec3d(x, y, z + halfWidth); //+Z
            queue.add(testVec, 15 - lightLevelTotal(world, testVec));
            testVec = new Vec3d(x, y, z - halfWidth); //-Z
            queue.add(testVec, 15 - lightLevelTotal(world, testVec));
        }

        double result;
        while (queue.size() > 0)
        {
            result = queue.peekPriority();
            testVec = queue.poll();
            if (LOS.rayTraceBlocks(searcher.world, new Vec3d(searcher.posX, searcher.posY + searcher.getEyeHeight(), searcher.posZ), testVec, false))
            {
                return 15 - result;
            }
        }

        return -777;
    }

    public static double lightLevelTotal(World world, Vec3d vec)
    {
        BlockPos blockpos = new BlockPos(vec);
        if (!world.isAreaLoaded(blockpos, 1)) return 0;
        return world.getLightFromNeighbors(blockpos);
    }


    public static double globalPlayerStealthLevel(EntityPlayer player)
    {
        WrappingQueue<Double> queue;
        long tick = currentTick();

        Pair<WrappingQueue<Double>, Long> pair = globalPlayerStealthHistory.get(player);
        if (pair == null)
        {
            queue = new WrappingQueue<>(GLOBAL_STEALTH_SMOOTHING + 2);
            queue.add(1d);
            globalPlayerStealthHistory.put(player, new Pair<>(queue, tick));
            return 1;
        }

        queue = pair.getKey();
        if (pair.getValue() != tick)
        {
            queue.add(1d);
            pair.setValue(tick);
        }

        int size = queue.size();
        if (size == 1) return 1;
        if (size == 2) return queue.getOldestToNewest(0);

        if (size < GLOBAL_STEALTH_SMOOTHING + 2)
        {
            double result = 1;
            for (int i = size - 2; i >= 0; i--)
            {
                result = Tools.min(result, queue.getOldestToNewest(i));
            }
            return result;
        }

        double first = queue.getOldestToNewest(0), result = 1;
        for (int i = 1; i < size - 1; i++)
        {
            result = Tools.min(result, queue.getOldestToNewest(i));
            if (result < first) return result;
        }
        return (first + result) / 2;
    }

    private static class SeenData
    {
        boolean seen = false;
        long lastSeenTime;
        long lastUpdateTime = currentTick();
        double lastStealthLevel;

        SeenData(double stealthLevel)
        {
            this(stealthLevel, false);
        }

        SeenData(double stealthLevel, boolean forceSeen)
        {
            lastStealthLevel = stealthLevel;
            if (forceSeen || stealthLevel <= 1)
            {
                seen = true;
                lastSeenTime = currentTick();
            }
        }
    }
}
