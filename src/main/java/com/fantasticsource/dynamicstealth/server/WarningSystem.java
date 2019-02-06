package com.fantasticsource.dynamicstealth.server;

import com.fantasticsource.dynamicstealth.config.DynamicStealthConfig;
import com.fantasticsource.dynamicstealth.server.ai.AIDynamicStealth;
import com.fantasticsource.dynamicstealth.server.threat.EntityThreatData;
import com.fantasticsource.dynamicstealth.server.threat.Threat;
import com.fantasticsource.mctools.MCTools;
import com.fantasticsource.tools.Tools;
import com.fantasticsource.tools.datastructures.Pair;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.LinkedHashMap;
import java.util.Map;

public class WarningSystem
{
    private static LinkedHashMap<EntityLivingBase, Pair<World, BlockPos>> warners = new LinkedHashMap<>();


    public static void warn(EntityLivingBase livingBase, BlockPos blockPos)
    {
        warners.put(livingBase, new Pair<>(livingBase.world, blockPos));
    }

    @SubscribeEvent
    public static void update(TickEvent.ServerTickEvent event)
    {
        warners.entrySet().removeIf(WarningSystem::processAndRemove);
    }

    private static boolean processAndRemove(Map.Entry<EntityLivingBase, Pair<World, BlockPos>> entry)
    {
        EntityLivingBase warner = entry.getKey();
        Pair<World, BlockPos> data = entry.getValue();
        World world = data.getKey();
        if (warner.isEntityAlive() && warner.world == world)
        {
            for (Entity entity : world.loadedEntityList.toArray(new Entity[world.loadedEntityList.size()]))
            {
                tryWarn(warner, entity, data.getValue());
            }
        }

        return true;
    }

    private static void tryWarn(EntityLivingBase warner, Entity helper, BlockPos warnPos)
    {
        if (helper != warner && helper instanceof EntityLiving && helper.isEntityAlive())
        {
            EntityLiving livingHelper = (EntityLiving) helper;

            if (!EntityThreatData.bypassesThreat(livingHelper))
            {
                Threat.ThreatData data = Threat.get(livingHelper);
                if (data.target == null && HelperSystem.shouldHelp(livingHelper, warner, true, Math.pow(30d * livingHelper.getEntityAttribute(Attributes.HEARING).getAttributeValue() / 100, 2)))
                {
                    int distance = (int) warner.getDistance(helper);

                    if (data.threatLevel < DynamicStealthConfig.serverSettings.threat.warnedThreat) Threat.setThreat(livingHelper, DynamicStealthConfig.serverSettings.threat.warnedThreat);

                    AIDynamicStealth stealthAI = AIDynamicStealth.getStealthAI(livingHelper);
                    if (stealthAI != null)
                    {
                        stealthAI.fleeIfYouShould(0);
                        if (stealthAI.isFleeing()) stealthAI.lastKnownPosition = MCTools.randomPos(warnPos, Tools.min(3 + (distance >> 1), 7), Tools.min(1 + (distance >> 2), 4));
                    }
                }
            }
        }
    }
}
