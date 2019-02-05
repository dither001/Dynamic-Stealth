package com.fantasticsource.dynamicstealth.server.threat;

import java.util.ArrayList;

public class EntityThreatDefaults
{
    public static ArrayList<String> threatBypassDefaults = new ArrayList<>();
    public static ArrayList<String> passiveDefaults = new ArrayList<>();

    static
    {
        passiveDefaults.add("shulker, false");
        passiveDefaults.add("snowman, false");

        threatBypassDefaults.add("player");
        threatBypassDefaults.add("slime");
        threatBypassDefaults.add("magma_cube");
        threatBypassDefaults.add("ender_dragon");

        //Compat; these should be added absolutely, not conditionally
        passiveDefaults.add("ebwizardry:wizard, false");

        threatBypassDefaults.add("rafradek_tf2_weapons:sentry");
    }
}
