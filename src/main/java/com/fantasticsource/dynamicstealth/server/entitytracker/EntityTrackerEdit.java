package com.fantasticsource.dynamicstealth.server.entitytracker;

import com.fantasticsource.dynamicstealth.server.configdata.EntityVisionData;
import com.fantasticsource.tools.Tools;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.*;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.item.*;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.*;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketEntityAttach;
import net.minecraft.network.play.server.SPacketSetPassengers;
import net.minecraft.util.IntHashMap;
import net.minecraft.util.ReportedException;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EntityTrackerEdit extends EntityTracker
{
    private static final Logger LOGGER = LogManager.getLogger();


    private final Set<EntityTrackerEntry> entries = Sets.newHashSet();
    private final IntHashMap<EntityTrackerEntry> trackedEntityHashTable = new IntHashMap<>();
    private final WorldServer world;


    private int maxDistance;


    public EntityTrackerEdit(WorldServer worldIn)
    {
        super(worldIn);

        world = worldIn;
        maxDistance = Tools.min(worldIn.getMinecraftServer().getPlayerList().getEntityViewDistance(), EntityVisionData.playerMaxVisionDistance);
    }

    public void track(Entity entityIn)
    {
        if (EntityRegistry.instance().tryTrackingEntity(this, entityIn)) return;

        if (entityIn instanceof EntityPlayerMP)
        {
            track(entityIn, 512, 2);
            EntityPlayerMP entityplayermp = (EntityPlayerMP) entityIn;

            for (EntityTrackerEntry entitytrackerentry : entries)
            {
                entitytrackerentry.updatePlayerEntity(entityplayermp);
            }
        }
        else if (entityIn instanceof EntityFishHook)
        {
            track(entityIn, 64, 5, true);
        }
        else if (entityIn instanceof EntityArrow)
        {
            track(entityIn, 64, 20, false);
        }
        else if (entityIn instanceof EntitySmallFireball)
        {
            track(entityIn, 64, 10, false);
        }
        else if (entityIn instanceof EntityFireball)
        {
            track(entityIn, 64, 10, true);
        }
        else if (entityIn instanceof EntitySnowball)
        {
            track(entityIn, 64, 10, true);
        }
        else if (entityIn instanceof EntityLlamaSpit)
        {
            track(entityIn, 64, 10, false);
        }
        else if (entityIn instanceof EntityEnderPearl)
        {
            track(entityIn, 64, 10, true);
        }
        else if (entityIn instanceof EntityEnderEye)
        {
            track(entityIn, 64, 4, true);
        }
        else if (entityIn instanceof EntityEgg)
        {
            track(entityIn, 64, 10, true);
        }
        else if (entityIn instanceof EntityPotion)
        {
            track(entityIn, 64, 10, true);
        }
        else if (entityIn instanceof EntityExpBottle)
        {
            track(entityIn, 64, 10, true);
        }
        else if (entityIn instanceof EntityFireworkRocket)
        {
            track(entityIn, 64, 10, true);
        }
        else if (entityIn instanceof EntityItem)
        {
            track(entityIn, 64, 20, true);
        }
        else if (entityIn instanceof EntityMinecart)
        {
            track(entityIn, 80, 3, true);
        }
        else if (entityIn instanceof EntityBoat)
        {
            track(entityIn, 80, 3, true);
        }
        else if (entityIn instanceof EntitySquid)
        {
            track(entityIn, 64, 3, true);
        }
        else if (entityIn instanceof EntityWither)
        {
            track(entityIn, 80, 3, false);
        }
        else if (entityIn instanceof EntityShulkerBullet)
        {
            track(entityIn, 80, 3, true);
        }
        else if (entityIn instanceof EntityBat)
        {
            track(entityIn, 80, 3, false);
        }
        else if (entityIn instanceof EntityDragon)
        {
            track(entityIn, 160, 3, true);
        }
        else if (entityIn instanceof IAnimals)
        {
            track(entityIn, 80, 3, true);
        }
        else if (entityIn instanceof EntityTNTPrimed)
        {
            track(entityIn, 160, 10, true);
        }
        else if (entityIn instanceof EntityFallingBlock)
        {
            track(entityIn, 160, 20, true);
        }
        else if (entityIn instanceof EntityHanging)
        {
            track(entityIn, 160, Integer.MAX_VALUE, false);
        }
        else if (entityIn instanceof EntityArmorStand)
        {
            track(entityIn, 160, 3, true);
        }
        else if (entityIn instanceof EntityXPOrb)
        {
            track(entityIn, 160, 20, true);
        }
        else if (entityIn instanceof EntityAreaEffectCloud)
        {
            track(entityIn, 160, Integer.MAX_VALUE, true);
        }
        else if (entityIn instanceof EntityEnderCrystal)
        {
            track(entityIn, 256, Integer.MAX_VALUE, false);
        }
        else if (entityIn instanceof EntityEvokerFangs)
        {
            track(entityIn, 160, 2, false);
        }
    }

    public void track(Entity entityIn, int trackingRange, int updateFrequency)
    {
        track(entityIn, trackingRange, updateFrequency, false);
    }

    public void track(Entity entityIn, int trackingRange, final int updateFrequency, boolean sendVelocityUpdates)
    {
        try
        {
            if (trackedEntityHashTable.containsItem(entityIn.getEntityId()))
            {
                throw new IllegalStateException("Entity is already tracked!");
            }

            EntityTrackerEntryEdit entityEntry = new EntityTrackerEntryEdit(entityIn, trackingRange, maxDistance, updateFrequency, sendVelocityUpdates);
            entries.add(entityEntry);
            trackedEntityHashTable.addKey(entityIn.getEntityId(), entityEntry);
            entityEntry.updatePlayerEntities(world.playerEntities);
        }
        catch (Throwable throwable)
        {
            CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Adding entity to track");
            CrashReportCategory crashreportcategory = crashreport.makeCategory("Entity To Track");
            crashreportcategory.addCrashSection("Tracking range", trackingRange + " blocks");
            crashreportcategory.addDetail("Update interval", () ->
            {
                String s = "Once per " + updateFrequency + " ticks";

                if (updateFrequency == Integer.MAX_VALUE)
                {
                    s = "Maximum (" + s + ")";
                }

                return s;
            });

            entityIn.addEntityCrashInfo(crashreportcategory);
            trackedEntityHashTable.lookup(entityIn.getEntityId()).getTrackedEntity().addEntityCrashInfo(crashreport.makeCategory("Entity That Is Already Tracked"));

            LOGGER.error("\"Silently\" catching entity tracking error.", new ReportedException(crashreport));
        }
    }

    public void untrack(Entity entityIn)
    {
        if (entityIn instanceof EntityPlayerMP)
        {
            EntityPlayerMP entityplayermp = (EntityPlayerMP) entityIn;

            for (EntityTrackerEntry entitytrackerentry : entries)
            {
                entitytrackerentry.removeFromTrackedPlayers(entityplayermp);
            }
        }

        EntityTrackerEntry entitytrackerentry = trackedEntityHashTable.removeObject(entityIn.getEntityId());

        if (entitytrackerentry != null)
        {
            entries.remove(entitytrackerentry);
            entitytrackerentry.sendDestroyEntityPacketToTrackedPlayers();
        }
    }

    public void tick()
    {
        List<EntityPlayerMP> list = new ArrayList<>();

        for (EntityTrackerEntry entitytrackerentry : entries)
        {
            entitytrackerentry.updatePlayerList(world.playerEntities);

            if (entitytrackerentry.playerEntitiesUpdated)
            {
                Entity entity = entitytrackerentry.getTrackedEntity();

                if (entity instanceof EntityPlayerMP)
                {
                    list.add((EntityPlayerMP) entity);
                }
            }
        }

        for (EntityPlayerMP player : list)
        {
            for (EntityTrackerEntry trackerEntry : entries)
            {
                trackerEntry.updatePlayerEntity(player);
            }
        }
    }

    public void updateVisibility(EntityPlayerMP player)
    {
        for (EntityTrackerEntry entitytrackerentry : entries)
        {
            if (entitytrackerentry.getTrackedEntity() == player)
            {
                entitytrackerentry.updatePlayerEntities(world.playerEntities);
            }
            else
            {
                entitytrackerentry.updatePlayerEntity(player);
            }
        }
    }

    public void sendToTracking(Entity entityIn, Packet<?> packetIn)
    {
        EntityTrackerEntry entitytrackerentry = trackedEntityHashTable.lookup(entityIn.getEntityId());

        if (entitytrackerentry != null)
        {
            entitytrackerentry.sendPacketToTrackedPlayers(packetIn);
        }
    }

    public Set<? extends EntityPlayer> getTrackingPlayers(Entity entity)
    {
        EntityTrackerEntry entry = trackedEntityHashTable.lookup(entity.getEntityId());
        if (entry == null)
            return java.util.Collections.emptySet();
        else
            return java.util.Collections.unmodifiableSet(entry.trackingPlayers);
    }

    public void sendToTrackingAndSelf(Entity entityIn, Packet<?> packetIn)
    {
        EntityTrackerEntry entitytrackerentry = trackedEntityHashTable.lookup(entityIn.getEntityId());

        if (entitytrackerentry != null)
        {
            entitytrackerentry.sendToTrackingAndSelf(packetIn);
        }
    }

    public void removePlayerFromTrackers(EntityPlayerMP player)
    {
        for (EntityTrackerEntry entitytrackerentry : entries)
        {
            entitytrackerentry.removeTrackedPlayerSymmetric(player);
        }
    }

    public void sendLeashedEntitiesInChunk(EntityPlayerMP player, Chunk chunkIn)
    {
        List<Entity> list = Lists.newArrayList();
        List<Entity> list1 = Lists.newArrayList();

        for (EntityTrackerEntry entitytrackerentry : entries)
        {
            Entity entity = entitytrackerentry.getTrackedEntity();

            if (entity != player && entity.chunkCoordX == chunkIn.x && entity.chunkCoordZ == chunkIn.z)
            {
                entitytrackerentry.updatePlayerEntity(player);

                if (entity instanceof EntityLiving)
                {
                    list.add(entity);
                }

                if (!entity.getPassengers().isEmpty())
                {
                    list1.add(entity);
                }
            }
        }

        for (Entity entity : list)
        {
            player.connection.sendPacket(new SPacketEntityAttach(entity, ((EntityLiving) entity).getLeashHolder()));
        }

        for (Entity entity : list1)
        {
            player.connection.sendPacket(new SPacketSetPassengers(entity));
        }
    }

    public void setViewDistance(int distance)
    {
        maxDistance = Tools.min((distance - 1) * 16, EntityVisionData.playerMaxVisionDistance);

        for (EntityTrackerEntry entitytrackerentry : entries)
        {
            entitytrackerentry.setMaxRange(maxDistance);
        }
    }
}