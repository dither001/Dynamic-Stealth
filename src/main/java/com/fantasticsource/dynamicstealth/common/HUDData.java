package com.fantasticsource.dynamicstealth.common;

import com.fantasticsource.mctools.MCTools;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

import java.util.Map;

import static com.fantasticsource.dynamicstealth.common.DynamicStealthConfig.serverSettings;
import static com.fantasticsource.dynamicstealth.server.Threat.bypassesThreat;

public class HUDData
{
    public static final int COLOR_NULL = 0x777777;
    public static final int COLOR_ATTACKING_YOU = 0xFF0000;
    public static final int COLOR_ALERT = 0xFF8800;
    public static final int COLOR_ATTACKING_OTHER = 0xFFFF00;
    public static final int COLOR_IDLE = 0x4444FF;
    public static final int COLOR_PASSIVE = 0x00CC00;

    public static final String EMPTY = "----------";
    public static final String UNKNOWN = "???";

    public static String detailSearcher = EMPTY;
    public static String detailTarget = EMPTY;
    public static int detailPercent = 0;
    public static int detailColor = COLOR_NULL;

    public static Map<Integer, OnPointData> onPointDataMap;

    public static int getColor(EntityPlayer player, EntityLivingBase searcher, EntityLivingBase target, int threatLevel)
    {
        if (searcher == null) return COLOR_NULL;
        if (bypassesThreat(searcher)) return COLOR_ALERT;
        if (serverSettings.threat.recognizePassive && MCTools.isPassive(searcher)) return COLOR_PASSIVE;
        if (threatLevel <= 0) return COLOR_IDLE;
        if (target == null) return COLOR_ALERT;
        if (target == player) return COLOR_ATTACKING_YOU;
        return COLOR_ATTACKING_OTHER;
    }

    public static class OnPointData
    {
        public int color, percent, priority;

        public OnPointData(int color, int percent, int priority)
        {
            this.color = color;
            this.percent = percent;
            this.priority = priority;
        }
    }
}