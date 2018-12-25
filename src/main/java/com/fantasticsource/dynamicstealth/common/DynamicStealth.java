package com.fantasticsource.dynamicstealth.common;

import com.fantasticsource.dynamicstealth.server.ai.*;
import com.fantasticsource.dynamicstealth.client.HUD;
import com.fantasticsource.dynamicstealth.server.EntitySensesEdit;
import com.fantasticsource.dynamicstealth.server.Threat;
import com.fantasticsource.dynamicstealth.server.newai.AISearchLastKnownPosition;
import com.fantasticsource.mctools.Speedometer;
import com.fantasticsource.tools.ReflectionTool;
import com.fantasticsource.tools.TrigLookupTable;
import com.fantasticsource.tools.datastructures.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.EntityLlama;
import net.minecraft.entity.passive.EntityRabbit;
import net.minecraft.init.MobEffects;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import static com.fantasticsource.dynamicstealth.common.DynamicStealthConfig.*;

@Mod(modid = DynamicStealth.MODID, name = DynamicStealth.NAME, version = DynamicStealth.VERSION, acceptableRemoteVersions = "*")
public class DynamicStealth
{
    public static final String MODID = "dynamicstealth";
    public static final String NAME = "Dynamic Stealth";
    public static final String VERSION = "1.12.2.007";

    private static Logger logger;

    private static Field sensesField, abstractSkeletonAIArrowAttackField, abstractSkeletonAIAttackOnCollideField;
    private static Class aiSlimeFaceRandomClass, aiEvilAttackClass, aiBearMeleeClass, aiSpiderAttackClass, aiSpiderTargetClass, aiBearAttackPlayerClass, aiLlamaDefendTarget,
            aiPigmanHurtByAggressorClass, aiLlamaHurtByTargetClass, aiPigmanTargetAggressorClass, aiVindicatorJohnnyAttackClass, aiBearHurtByTargetClass;

    public static final TrigLookupTable TRIG_TABLE = new TrigLookupTable(1024);

    public DynamicStealth()
    {
        MinecraftForge.EVENT_BUS.register(DynamicStealth.class);
        MinecraftForge.EVENT_BUS.register(Speedometer.class);
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) throws NoSuchFieldException, IllegalAccessException, IOException
    {
        logger = event.getModLog();

        if (serverSettings.senses.vision.e_angles.angleSmall > serverSettings.senses.vision.e_angles.angleLarge) throw new IllegalArgumentException("angleLarge must be greater than or equal to angleSmall");
        if (serverSettings.senses.vision.f_distances.distanceNear > serverSettings.senses.vision.f_distances.distanceFar) throw new IllegalArgumentException("distanceFar must be greater than or equal to distanceNear");
        if (serverSettings.senses.vision.c_lighting.lightLow > serverSettings.senses.vision.c_lighting.lightHigh) throw new IllegalArgumentException("lightHigh must be greater than or equal to lightLow");
        if (serverSettings.senses.vision.d_speeds.speedLow > serverSettings.senses.vision.d_speeds.speedHigh) throw new IllegalArgumentException("speedHigh must be greater than or equal to speedLow");

        sensesField = ReflectionTool.getField(EntityLiving.class, "field_70723_bA", "senses");
        abstractSkeletonAIArrowAttackField = ReflectionTool.getField(AbstractSkeleton.class, "field_85037_d", "aiArrowAttack");
        abstractSkeletonAIAttackOnCollideField = ReflectionTool.getField(AbstractSkeleton.class, "field_85038_e", "aiAttackOnCollide");

        aiSlimeFaceRandomClass = ReflectionTool.getInternalClass(EntitySlime.class, "AISlimeFaceRandom");
        aiEvilAttackClass = ReflectionTool.getInternalClass(EntityRabbit.class, "AIEvilAttack");
        aiBearMeleeClass = ReflectionTool.getInternalClass(EntityPolarBear.class, "AIMeleeAttack");
        aiSpiderAttackClass = ReflectionTool.getInternalClass(EntitySpider.class, "AISpiderAttack");
        aiSpiderTargetClass = ReflectionTool.getInternalClass(EntitySpider.class, "AISpiderTarget");
        aiBearAttackPlayerClass = ReflectionTool.getInternalClass(EntityPolarBear.class, "AIAttackPlayer");
        aiLlamaDefendTarget = ReflectionTool.getInternalClass(EntityLlama.class, "AIDefendTarget");
        aiPigmanHurtByAggressorClass = ReflectionTool.getInternalClass(EntityPigZombie.class, "AIHurtByAggressor");
        aiLlamaHurtByTargetClass = ReflectionTool.getInternalClass(EntityLlama.class, "AIHurtByTarget");
        aiPigmanTargetAggressorClass = ReflectionTool.getInternalClass(EntityPigZombie.class, "AITargetAggressor");
        aiVindicatorJohnnyAttackClass = ReflectionTool.getInternalClass(EntityVindicator.class, "AIJohnnyAttack");
        aiBearHurtByTargetClass = ReflectionTool.getInternalClass(EntityPolarBear.class, "AIHurtByTarget");
    }

    @SubscribeEvent
    public static void saveConfig(ConfigChangedEvent.OnConfigChangedEvent event)
    {
        if (event.getModID().equals(MODID))
        {
            ConfigManager.sync(MODID, Config.Type.INSTANCE);
        }
    }


    @SubscribeEvent
    public static void drawGUI(RenderGameOverlayEvent.Post event)
    {
        if (event.getType() == RenderGameOverlayEvent.ElementType.EXPERIENCE)
        {
            new HUD(Minecraft.getMinecraft());
        }
    }


    @SubscribeEvent
    public static void despawn(LivingSpawnEvent.AllowDespawn event)
    {
        EntityLivingBase livingBase = event.getEntityLiving();
        Event.Result result = event.getResult();
        if (livingBase instanceof EntityLiving && result != Event.Result.DENY && result != Event.Result.DEFAULT)
        {
            Threat.remove((EntityLiving) livingBase);
        }
    }

    @SubscribeEvent
    public static void entityDead(LivingDeathEvent event)
    {
        EntityLivingBase deadOne = event.getEntityLiving();
        if (deadOne != null)
        {
            Threat.removeTargetFromAll(deadOne);
            if (deadOne instanceof EntityLiving) Threat.remove((EntityLiving) deadOne);
        }
    }

    @SubscribeEvent
    public static void chunkUnload(ChunkEvent.Unload event)
    {
        Chunk chunk = event.getChunk();
        Set<Entity>[] sets = chunk.getEntityLists();
        for (Set<Entity> set : sets)
        {
            for (Entity entity : set)
            {
                if (entity instanceof EntityLiving) Threat.remove((EntityLiving) entity);
            }
        }
    }

    @SubscribeEvent
    public static void worldUnload(WorldEvent.Unload event)
    {
        for (Entity entity : event.getWorld().loadedEntityList)
        {
            if (entity instanceof EntityLiving) Threat.remove((EntityLiving) entity);
        }
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event)
    {
        Threat.update();
    }


    @SubscribeEvent
    public static void entityAttacked(LivingHurtEvent event) throws InvocationTargetException, IllegalAccessException
    {
        EntityLivingBase target = event.getEntityLiving();

        Entity source = event.getSource().getTrueSource();
        if (source == null) source = event.getSource().getImmediateSource();
        if (source == null) return;

        if (target instanceof EntityLiving && source instanceof EntityLivingBase)
        {
            EntityLiving livingTarget = (EntityLiving) target;
            EntityLivingBase livingBaseSource = (EntityLivingBase) source;
            boolean updateTarget = true;

            //Threat
            Pair<EntityLivingBase, Integer> threatEntry = Threat.get(livingTarget);
            EntityLivingBase threatTarget = threatEntry.getKey();
            int threat = threatEntry.getValue();
            if (threatTarget == null || threat == 0)
            {
                //Hit by entity when no target is set; this includes...
                //...getting hit while out-of-combat
                //...getting hit after previous target has been killed
                Threat.set(livingTarget, livingBaseSource, (int) (event.getAmount() * serverSettings.threat.attackedThreatMultiplierInitial / livingTarget.getMaxHealth()));
            }
            else if (threatTarget != source)
            {
                //In combat, and hit by an entity besides our threat target
                double threatChangeFactor = event.getAmount() / livingTarget.getMaxHealth();
                threat -= threatChangeFactor * serverSettings.threat.attackedThreatMultiplierOther;
                if (threat <= 0) Threat.set(livingTarget, livingBaseSource, (int) (threatChangeFactor * serverSettings.threat.attackedThreatMultiplierInitial));
                else
                {
                    Threat.setThreat(livingTarget, threat);
                    updateTarget = false;
                }
            }
            else
            {
                //In combat, and hit by threat target
                Threat.setThreat(livingTarget, threat + (int) (event.getAmount() * serverSettings.threat.attackedThreatMultiplierTarget / livingTarget.getMaxHealth()));
            }

            if (updateTarget)
            {
                //Threat targeting already updated

                //Update vanilla targeting
                livingTarget.setAttackTarget(livingBaseSource);

                //Look toward damage
                float newYaw = (float) (TRIG_TABLE.arctanFullcircle(target.posZ, target.posX, source.posZ, source.posX) / Math.PI * 180);
                target.rotationYaw = newYaw;
                target.prevRotationYaw = newYaw;
                target.rotationYawHead = newYaw;
                target.prevRotationYawHead = newYaw;

                if (target instanceof EntitySlime)
                {
                    //Look toward damage (slime)
                    for (EntityAITasks.EntityAITaskEntry task : ((EntitySlime) target).tasks.taskEntries)
                    {
                        if (task.action instanceof AISlimeFaceRandomEdit)
                        {
                            ((AISlimeFaceRandomEdit) task.action).setDirection(newYaw, true);
                            break;
                        }
                    }
                }
                else
                {
                    //This is mostly for setting/resetting things when eg. you hit an entity that is in the middle of a task, and they don't see you (even after you hit them)

                    livingTarget.getNavigator().clearPath();

                    for (EntityAITasks.EntityAITaskEntry task : livingTarget.targetTasks.taskEntries)
                    {
                        if (task.action instanceof AISearchLastKnownPosition)
                        {
                            AISearchLastKnownPosition searchAI = (AISearchLastKnownPosition) task.action;
                            int distance = (int) Math.sqrt(source.getDistanceSq(target));
                            searchAI.lastKnownPosition = searchAI.randomPath(source.getPosition(), distance / 2, distance / 4);
                        }
                    }
                }
            }
        }

        if (source instanceof EntityLivingBase)
        {
            EntityLivingBase sourceBase = (EntityLivingBase) source;
            if (serverSettings.z_otherSettings.removeInvisibilityOnHit)
            {
                sourceBase.removePotionEffect(MobEffects.INVISIBILITY);
                target.removePotionEffect(MobEffects.INVISIBILITY);
            }
            if (serverSettings.z_otherSettings.removeBlindnessOnHit)
            {
                sourceBase.removePotionEffect(MobEffects.BLINDNESS);
                target.removePotionEffect(MobEffects.BLINDNESS);
            }
        }
    }

    @SubscribeEvent
    public static void setSensesAndTasks(EntityJoinWorldEvent event) throws Exception
    {
        if (event != null)
        {
            Entity entity = event.getEntity();
            if (entity != null)
            {
                if (entity instanceof EntityLiving)
                {
                    EntityLiving living = (EntityLiving) entity;

                    //Set the new senses handler for all living entities (not including players)
                    try
                    {
                        sensesField.set(living, new EntitySensesEdit(living));

                        if (living instanceof AbstractSkeleton)
                        {
                            abstractSkeletonAIArrowAttackField.set(living, new AIAttackRangedBowEdit<AbstractSkeleton>((EntityAIAttackRangedBow) abstractSkeletonAIArrowAttackField.get(living)));
                            abstractSkeletonAIAttackOnCollideField.set(living, new AIAttackMeleeEdit((EntityAIAttackMelee) abstractSkeletonAIAttackOnCollideField.get(living)));
                        }
                    }
                    catch (ReflectionHelper.UnableToFindFieldException | ReflectionHelper.UnableToAccessFieldException e)
                    {
                        e.printStackTrace();
                        FMLCommonHandler.instance().exitJava(137, false);
                    }

                    //Entity AI task replacements
                    replaceTasks(living.tasks, living);
                    replaceTasks(living.targetTasks, living);

                    //Entity AI task additions
                    addTasks(living.targetTasks, living.tasks, living);
                }
            }
        }
    }

    private static void replaceTasks(EntityAITasks tasks, EntityLiving living) throws Exception
    {
        Set<EntityAITasks.EntityAITaskEntry> taskSet = tasks.taskEntries;
        EntityAITasks.EntityAITaskEntry[] taskArray = new EntityAITasks.EntityAITaskEntry[taskSet.size()];
        taskSet.toArray(taskArray);
        for (EntityAITasks.EntityAITaskEntry task : taskArray)
        {
            Class actionClass = task.action.getClass();

            if (actionClass == EntityAILookIdle.class) tasks.removeTask(task.action);

                //All done (excluded EntityAIVillagerInteract)
                //EntityAIVillagerInteract is fine as-is, because "they can talk to each other to find each other when not visible"
            else if (actionClass == EntityAIWatchClosest.class) replaceTask(tasks, task, new AIWatchClosestEdit((EntityAIWatchClosest) task.action));
            else if (actionClass == EntityAIWatchClosest2.class) replaceTask(tasks, task, new AIWatchClosestEdit((EntityAIWatchClosest) task.action, true));

                //All done
            else if (actionClass == EntityAIAttackMelee.class) replaceTask(tasks, task, new AIAttackMeleeEdit((EntityAIAttackMelee) task.action));
            else if (actionClass == aiEvilAttackClass) replaceTask(tasks, task, new AIAttackMeleeEdit((EntityAIAttackMelee) task.action));
            else if (actionClass == aiBearMeleeClass) replaceTask(tasks, task, new AIBearAttackEdit((EntityAIAttackMelee) task.action));
            else if (actionClass == aiSpiderAttackClass) replaceTask(tasks, task, new AISpiderAttackEdit((EntityAIAttackMelee) task.action));
            else if (actionClass == EntityAIZombieAttack.class) replaceTask(tasks, task, new AIZombieAttackEdit((EntityAIZombieAttack) task.action));

                //All done (excluded shulker and enderman stuff)
                //Shulker and Enderman are fine as they are, because they're telepaths, or maybe just because I say so
            else if (actionClass == EntityAINearestAttackableTarget.class) replaceTask(tasks, task, new AINearestAttackableTargetEdit((EntityAINearestAttackableTarget) task.action));
            else if (actionClass == aiBearAttackPlayerClass) replaceTask(tasks, task, new AIBearAttackPlayerEdit((EntityAINearestAttackableTarget) task.action));
            else if (actionClass == aiSpiderTargetClass) replaceTask(tasks, task, new AISpiderTargetEdit((EntityAINearestAttackableTarget) task.action));
            else if (actionClass == aiLlamaDefendTarget) replaceTask(tasks, task, new AILlamaDefendEdit((EntityAINearestAttackableTarget) task.action));
            else if (actionClass == aiVindicatorJohnnyAttackClass) replaceTask(tasks, task, new AIJohnnyAttackEdit((EntityAINearestAttackableTarget) task.action));
            else if (actionClass == aiPigmanTargetAggressorClass) replaceTask(tasks, task, new AIPigmanTargetAggressorEdit((EntityAINearestAttackableTarget) task.action));

                //All done
            else if (actionClass == EntityAIHurtByTarget.class) replaceTask(tasks, task, new AIHurtByTargetEdit((EntityAIHurtByTarget) task.action));
            else if (actionClass == aiPigmanHurtByAggressorClass) replaceTask(tasks, task, new AIPigmanHurtByAggressorEdit((EntityAIHurtByTarget) task.action));
            else if (actionClass == aiLlamaHurtByTargetClass) replaceTask(tasks, task, new AILlamaHurtByTargetEdit((EntityAIHurtByTarget) task.action));
            else if (actionClass == aiBearHurtByTargetClass) replaceTask(tasks, task, new AIBearHurtByTargetEdit((EntityAIHurtByTarget) task.action));

                //Random section
            else if (actionClass == EntityAIAttackRanged.class) replaceTask(tasks, task, new AIAttackRangedEdit((EntityAIAttackRanged) task.action));
            else if (actionClass == EntityAIAttackRangedBow.class) replaceTask(tasks, task, new AIAttackRangedBowEdit<>((EntityAIAttackRangedBow) task.action));
            else if (actionClass == EntityAIFindEntityNearestPlayer.class) replaceTask(tasks, task, new AIFindEntityNearestPlayerEdit((EntityAIFindEntityNearestPlayer) task.action));
            else if (actionClass == EntityAIFindEntityNearest.class) replaceTask(tasks, task, new AIFindEntityNearestEdit((EntityAIFindEntityNearest) task.action));
            else if (actionClass == EntityAITargetNonTamed.class) replaceTask(tasks, task, new AITargetNonTamedEdit((EntityAITargetNonTamed) task.action));
            else if (actionClass == EntityAIFollow.class) replaceTask(tasks, task, new AIParrotFollowEdit((EntityAIFollow) task.action));
            else if (actionClass == EntityAIDefendVillage.class) replaceTask(tasks, task, new AIDefendVillageEdit((EntityAIDefendVillage) task.action));
            else if (actionClass == EntityAIOwnerHurtByTarget.class) replaceTask(tasks, task, new AIOwnerHurtByTargetEdit((EntityAIOwnerHurtByTarget) task.action));
            else if (actionClass == EntityAIOwnerHurtTarget.class) replaceTask(tasks, task, new AIOwnerHurtTargetEdit((EntityAIOwnerHurtTarget) task.action));
            else if (actionClass == EntityAICreeperSwell.class) replaceTask(tasks, task, new AICreeperSwellEdit((EntityCreeper) living));
            else if (actionClass == aiSlimeFaceRandomClass) replaceTask(tasks, task, new AISlimeFaceRandomEdit((EntitySlime) living));
            else if (actionClass == EntityAIOcelotAttack.class) replaceTask(tasks, task, new AIOcelotAttackEdit(living));
        }
    }

    private static void replaceTask(EntityAITasks tasks, EntityAITasks.EntityAITaskEntry oldTask, EntityAIBase newTask)
    {
        tasks.addTask(oldTask.priority, newTask);
        tasks.removeTask(oldTask.action);
    }

    private static void addTasks(EntityAITasks targetTasks, EntityAITasks tasks, EntityLiving living)
    {
        if (!(living instanceof EntitySlime))
        {
            targetTasks.addTask(77777, new AISearchLastKnownPosition(living, 1));
        }
    }
}