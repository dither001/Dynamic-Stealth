package com.fantasticsource.dynamicstealth.server.senses.sight;

import com.fantasticsource.dynamicstealth.compat.CompatDissolution;
import com.fantasticsource.dynamicstealth.config.server.senses.SensesConfig;
import com.fantasticsource.dynamicstealth.config.server.senses.sight.SightConfig;
import com.fantasticsource.dynamicstealth.server.Attributes;
import com.fantasticsource.dynamicstealth.server.threat.Threat;
import com.fantasticsource.mctools.MCTools;
import com.fantasticsource.tools.Tools;
import com.fantasticsource.tools.datastructures.ExplicitPriorityQueue;
import com.fantasticsource.tools.datastructures.Pair;
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
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.fantasticsource.dynamicstealth.common.DynamicStealth.TRIG_TABLE;
import static com.fantasticsource.dynamicstealth.config.DynamicStealthConfig.serverSettings;
import static com.fantasticsource.dynamicstealth.server.senses.sight.EntitySightData.*;
import static com.fantasticsource.mctools.ServerTickTimer.currentTick;

public class Sight
{
    private static final int SEEN_RECENT_TIMER = 60;

    private static final SensesConfig senses = serverSettings.senses;
    private static final SightConfig sight = senses.sight;


    private static Map<EntityLivingBase, Map<Entity, SeenData>> recentlySeenMap = new LinkedHashMap<>();

    private static Map<Pair<EntityPlayerMP, Boolean>, ExplicitPriorityQueue<EntityLivingBase>> playerSeenThisTickMap = new LinkedHashMap<>();
    private static Map<EntityLiving, ArrayList<EntityLivingBase>> entitySeenThisTickMap = new LinkedHashMap<>();


    @SubscribeEvent
    public static void update(TickEvent.ServerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END)
        {
            playerSeenThisTickMap.clear();
            entitySeenThisTickMap.clear();
            recentlySeenMap.entrySet().removeIf(Sight::entityRemoveIfEmpty);
        }
    }

    private static boolean entityRemoveIfEmpty(Map.Entry<EntityLivingBase, Map<Entity, SeenData>> entry)
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

        Map<Entity, SeenData> map = recentlySeenMap.get(searcher);

        //If applicable, load from cache and return
        if (map != null && useCache)
        {
            SeenData data = map.get(target);
            if (data != null && data.lastUpdateTime == currentTick()) return data.lastStealthLevel;
        }

        //Calculate
        searcher.world.profiler.startSection("DS Sight checks");
        double result = visualStealthLevelInternal(searcher, target, yaw, pitch);
        searcher.world.profiler.endSection();

        //Save cache
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
                data.lastUpdateTime = currentTick();
                data.lastStealthLevel = result;
                if (result <= 1)
                {
                    data.seen = true;
                    data.lastSeenTime = currentTick();
                }
            }
        }

        return result;
    }


    public static double lightLevelTotal(Entity entity)
    {
        BlockPos blockpos = new BlockPos(entity.posX, entity.getEntityBoundingBox().minY, entity.posZ);
        if (!entity.world.isAreaLoaded(entity.getPosition(), 1)) return 0;
        return entity.world.getLightFromNeighbors(blockpos);
    }

    public static boolean los(Entity searcher, Entity target)
    {
        if (searcher.world != target.world) return false;

        double halfWidth = target.width / 2;
        double halfHeight = target.height / 2;

        double x = target.posX;
        double y = target.posY + halfHeight;
        double z = target.posZ;

        //Center
        if (LOS.rayTraceBlocks(searcher.world, new Vec3d(searcher.posX, searcher.posY + searcher.getEyeHeight(), searcher.posZ), new Vec3d(x, y, z), false))
        {
            return true;
        }

        //+Y
        if (LOS.rayTraceBlocks(searcher.world, new Vec3d(searcher.posX, searcher.posY + searcher.getEyeHeight(), searcher.posZ), new Vec3d(x, y + halfHeight, z), false))
        {
            return true;
        }

        //-Y
        if (LOS.rayTraceBlocks(searcher.world, new Vec3d(searcher.posX, searcher.posY + searcher.getEyeHeight(), searcher.posZ), new Vec3d(x, y - halfHeight, z), false))
        {
            return true;
        }

        //+X
        if (LOS.rayTraceBlocks(searcher.world, new Vec3d(searcher.posX, searcher.posY + searcher.getEyeHeight(), searcher.posZ), new Vec3d(x + halfWidth, y, z), false))
        {
            return true;
        }

        //-X
        if (LOS.rayTraceBlocks(searcher.world, new Vec3d(searcher.posX, searcher.posY + searcher.getEyeHeight(), searcher.posZ), new Vec3d(x - halfWidth, y, z), false))
        {
            return true;
        }

        //+Z
        if (LOS.rayTraceBlocks(searcher.world, new Vec3d(searcher.posX, searcher.posY + searcher.getEyeHeight(), searcher.posZ), new Vec3d(x, y, z + halfWidth), false))
        {
            return true;
        }

        //-Z
        if (LOS.rayTraceBlocks(searcher.world, new Vec3d(searcher.posX, searcher.posY + searcher.getEyeHeight(), searcher.posZ), new Vec3d(x, y, z - halfWidth), false))
        {
            return true;
        }

        return false;
    }


    public static ExplicitPriorityQueue<EntityLivingBase> seenEntities(EntityPlayerMP player, boolean isForHUD)
    {
        ExplicitPriorityQueue<EntityLivingBase> queue = playerSeenThisTickMap.get(new Pair<>(player, isForHUD));
        if (queue != null) return queue.clone();

        ExplicitPriorityQueue<EntityLivingBase>[] queues = seenEntitiesInternal(player, isForHUD);
        playerSeenThisTickMap.put(new Pair<>(player, isForHUD), queues[1]);
        return queues[0];
    }

    private static ExplicitPriorityQueue<EntityLivingBase>[] seenEntitiesInternal(EntityPlayerMP player, boolean isForHUD)
    {
        ExplicitPriorityQueue<EntityLivingBase>[] queues = new ExplicitPriorityQueue[]{new ExplicitPriorityQueue<>(10), new ExplicitPriorityQueue<>(10)};
        double stealthLevel;
        Entity[] loadedEntities = player.world.loadedEntityList.toArray(new Entity[player.world.loadedEntityList.size()]);

        if (EntitySightData.hasSoulSight(player))
        {
            for (Entity entity : loadedEntities)
            {
                if (!isForHUD || !(MCTools.isRidingOrRiddenBy(player, entity) || CompatDissolution.isPossessing(player, entity) || !entity.isEntityAlive()))
                {
                    if (entity instanceof EntityLivingBase && entity != player)
                    {
                        stealthLevel = visualStealthLevel(player, entity);
                        if (stealthLevel <= 1)
                        {
                            if (isForHUD)
                            {
                                double angleDif = Vec3d.fromPitchYaw(player.rotationPitch, player.rotationYawHead).normalize().dotProduct(new Vec3d(entity.posX - player.posX, entity.posY - player.posY, entity.posZ - player.posZ).normalize());

                                //And because Vec3d.fromPitchYaw occasionally returns values barely out of the range of (-1, 1)...
                                if (angleDif < -1) angleDif = -1;
                                else if (angleDif > 1) angleDif = 1;

                                angleDif = TRIG_TABLE.arccos(angleDif); //0 in front, pi in back

                                double distSquared = player.getDistanceSq(entity);
                                double priority = Math.pow(angleDif, 4) * distSquared;
                                queues[0].add((EntityLivingBase) entity, priority); //Returned to external call
                                queues[1].add((EntityLivingBase) entity, priority); //Used for playerSeenThisTickMap (result caching)
                            }
                            else
                            {
                                queues[0].add((EntityLivingBase) entity, stealthLevel); //Returned to external call
                                queues[1].add((EntityLivingBase) entity, stealthLevel); //Used for playerSeenThisTickMap (result caching)
                            }
                        }
                    }
                }
            }
        }
        else if (serverSettings.senses.usePlayerSenses)
        {
            for (Entity entity : loadedEntities)
            {
                if (entity instanceof EntityLivingBase && entity != player)
                {
                    if (!isForHUD || !(MCTools.isRidingOrRiddenBy(player, entity) || CompatDissolution.isPossessing(player, entity) || !entity.isEntityAlive()))
                    {
                        stealthLevel = visualStealthLevel(player, entity);
                        if (stealthLevel <= 1)
                        {
                            if (isForHUD)
                            {
                                double angleDif = Vec3d.fromPitchYaw(player.rotationPitch, player.rotationYawHead).normalize().dotProduct(new Vec3d(entity.posX - player.posX, entity.posY - player.posY, entity.posZ - player.posZ).normalize());

                                //And because Vec3d.fromPitchYaw occasionally returns values barely out of the range of (-1, 1)...
                                if (angleDif < -1) angleDif = -1;
                                else if (angleDif > 1) angleDif = 1;

                                angleDif = TRIG_TABLE.arccos(angleDif); //0 in front, pi in back

                                double distSquared = player.getDistanceSq(entity);
                                double priority = Math.pow(angleDif, 4) * distSquared;
                                queues[0].add((EntityLivingBase) entity, priority); //Returned to external call
                                queues[1].add((EntityLivingBase) entity, priority); //Used for playerSeenThisTickMap (result caching)
                            }
                            else
                            {
                                queues[0].add((EntityLivingBase) entity, stealthLevel); //Returned to external call
                                queues[1].add((EntityLivingBase) entity, stealthLevel); //Used for playerSeenThisTickMap (result caching)
                            }
                        }
                    }
                }
            }
        }
        else
        {
            Map<Entity, SeenData> map = recentlySeenMap.computeIfAbsent(player, k -> new LinkedHashMap<>());

            for (Entity entity : loadedEntities)
            {
                if (entity instanceof EntityLivingBase && entity != player)
                {
                    if (!isForHUD || !(MCTools.isRidingOrRiddenBy(player, entity) || CompatDissolution.isPossessing(player, entity) || !entity.isEntityAlive()))
                    {
                        double distSquared = player.getDistanceSq(entity);
                        if (distSquared <= Math.pow(playerMaxSightDistance, 2) && los(player, entity))
                        {
                            double angleDif = Vec3d.fromPitchYaw(player.rotationPitch, player.rotationYawHead).normalize().dotProduct(new Vec3d(entity.posX - player.posX, entity.posY - player.posY, entity.posZ - player.posZ).normalize());

                            //And because Vec3d.fromPitchYaw occasionally returns values barely out of the range of (-1, 1)...
                            if (angleDif < -1) angleDif = -1;
                            else if (angleDif > 1) angleDif = 1;

                            angleDif = TRIG_TABLE.arccos(angleDif); //0 in front, pi in back

                            if (angleDif / Math.PI * 180 <= 70)
                            {
                                double priority = Math.pow(angleDif, 4) * distSquared;
                                queues[0].add((EntityLivingBase) entity, priority); //Returned to external call
                                queues[1].add((EntityLivingBase) entity, priority); //Used for playerSeenThisTickMap (result caching)
                                map.put(entity, new SeenData(priority, true)); //visualStealthLevelInternal was not called, so need to add to map manually
                            }
                        }
                    }
                }
            }
        }

        return queues;
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


        //Soul Sight (absolute)
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
        if (sight.g_absolutes.seeGlowing && isLivingBase && targetLivingBase.getActivePotionEffect(MobEffects.GLOWING) != null) return 0;


        //LOS check (absolute, after Angles, after Glowing)
        if (!los(searcher, target)) return 777;


        //Lighting (absolute, factor, after Angles, after Glowing, after LOS)
        double lightFactor = isLivingBase && isBright(targetLivingBase) ? 15 : lightLevelTotal(target);
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
