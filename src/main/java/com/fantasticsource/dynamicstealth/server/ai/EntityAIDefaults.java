package com.fantasticsource.dynamicstealth.server.ai;

import java.util.ArrayList;

public class EntityAIDefaults
{
    public static ArrayList<String> fleeThresholdDefaults = new ArrayList<>();

    static
    {
        fleeThresholdDefaults.add("player, 0");
        fleeThresholdDefaults.add("zombie, 0");
        fleeThresholdDefaults.add("zombie_villager, 0");
        fleeThresholdDefaults.add("elder_guardian, 0");
        fleeThresholdDefaults.add("husk, 0");
        fleeThresholdDefaults.add("skeleton, 0");
        fleeThresholdDefaults.add("stray, 0");
        fleeThresholdDefaults.add("wither_skeleton, 0");
        fleeThresholdDefaults.add("creeper, 0");
        fleeThresholdDefaults.add("ghast, 0");
        fleeThresholdDefaults.add("slime, 0");
        fleeThresholdDefaults.add("magma_cube, 0");
        fleeThresholdDefaults.add("enderman, 0");
        fleeThresholdDefaults.add("ender_dragon, 0");
        fleeThresholdDefaults.add("wither, 0");
        fleeThresholdDefaults.add("skeleton_horse, 0");
        fleeThresholdDefaults.add("zombie_horse, 0");
        fleeThresholdDefaults.add("blaze, 0");
        fleeThresholdDefaults.add("snowman, 0");
        fleeThresholdDefaults.add("villager_golem, 0");


        //Compat; these should be added absolutely, not conditionally

        fleeThresholdDefaults.add("harvestersnight:harvester, 0");
        fleeThresholdDefaults.add("defiledlands:the_destroyer, 0");
        fleeThresholdDefaults.add("defiledlands:the_mourner, 0");

        fleeThresholdDefaults.add("ebwizardry:skeleton_minion, 0");
        fleeThresholdDefaults.add("ebwizardry:spirit_wolf, 0");
        fleeThresholdDefaults.add("ebwizardry:ice_wraith, 0");
        fleeThresholdDefaults.add("ebwizardry:lightning_wraith, 0");
        fleeThresholdDefaults.add("ebwizardry:shadow_wraith, 0");
        fleeThresholdDefaults.add("ebwizardry:magic_slime, 0");
        fleeThresholdDefaults.add("ebwizardry:spirit_horse, 0");
        fleeThresholdDefaults.add("ebwizardry:phoenix, 0");
        fleeThresholdDefaults.add("ebwizardry:storm_elemental, 0");
        fleeThresholdDefaults.add("ebwizardry:wither_skeleton_minion, 0");
        fleeThresholdDefaults.add("emberroot:rainbowslime, 0");
        fleeThresholdDefaults.add("emberroot:rainbow_golem, 0");
        fleeThresholdDefaults.add("emberroot:hero, 0");
        fleeThresholdDefaults.add("emberroot:creeper, 0");
        fleeThresholdDefaults.add("emberroot:slime, 0");
        fleeThresholdDefaults.add("emberroot:dire_wolf, 0");
        fleeThresholdDefaults.add("emberroot:withercat, 0");
        fleeThresholdDefaults.add("emberroot:enderminy, 0");
        fleeThresholdDefaults.add("emberroot:knight_fallen, 0");
        fleeThresholdDefaults.add("emberroot:fallenmount, 0");
        fleeThresholdDefaults.add("emberroot:rootsonespriteboss, 0");
        fleeThresholdDefaults.add("emberroot:skeleton_frozen, 0");
        fleeThresholdDefaults.add("endreborn:endguard, 0");
        fleeThresholdDefaults.add("endreborn:watcher, 0");
        fleeThresholdDefaults.add("endreborn:endlord, 0");
        fleeThresholdDefaults.add("endreborn:angry_enderman, 0");
        fleeThresholdDefaults.add("endreborn:chronologist, 0");
        fleeThresholdDefaults.add("thermalfoundation:blizz, 0");
        fleeThresholdDefaults.add("thermalfoundation:blitz, 0");
        fleeThresholdDefaults.add("thermalfoundation:basalz, 0");
        fleeThresholdDefaults.add("goblinencounter:goblinking, 0");
        fleeThresholdDefaults.add("nex:gold_golem, 0");
        fleeThresholdDefaults.add("nex:wight, 0");
        fleeThresholdDefaults.add("nex:spinout, 0");
        fleeThresholdDefaults.add("nex:spore_creeper, 0");
        fleeThresholdDefaults.add("nex:ghastling, 0");
        fleeThresholdDefaults.add("nex:bone_spider, 0");
        fleeThresholdDefaults.add("nex:ghast_queen, 0");
        fleeThresholdDefaults.add("nethergoldplus:zombiepigmanwarrior, 0");
        fleeThresholdDefaults.add("primitivemobs:treasure_slime, 0");
        fleeThresholdDefaults.add("primitivemobs:haunted_tool, 0");
        fleeThresholdDefaults.add("primitivemobs:bewitched_tome, 0");
        fleeThresholdDefaults.add("primitivemobs:brain_slime, 0");
        fleeThresholdDefaults.add("primitivemobs:rocket_creeper, 0");
        fleeThresholdDefaults.add("primitivemobs:festive_creeper, 0");
        fleeThresholdDefaults.add("primitivemobs:support_creeper, 0");
        fleeThresholdDefaults.add("primitivemobs:skeleton_warrior, 0");
        fleeThresholdDefaults.add("primitivemobs:blazing_juggernaut, 0");
        fleeThresholdDefaults.add("primitivemobs:void_eye, 0");
        fleeThresholdDefaults.add("varodd:giant_zombie, 0");
        fleeThresholdDefaults.add("natura:nitrocreeper, 0");
        fleeThresholdDefaults.add("betteranimalsplus:feralwolf, 0");
        fleeThresholdDefaults.add("betteranimalsplus:hirschgeist, 0");
        fleeThresholdDefaults.add("defiledlands:shambler, 0");
        fleeThresholdDefaults.add("defiledlands:shambler_twisted, 0");
        fleeThresholdDefaults.add("defiledlands:host, 0");
        fleeThresholdDefaults.add("defiledlands:slime_defiled, 0");
        fleeThresholdDefaults.add("eerieentities:pumpkin_slime, 0");
        fleeThresholdDefaults.add("eerieentities:nether_knight, 0");
        fleeThresholdDefaults.add("eerieentities:cursed_armor, 0");
        fleeThresholdDefaults.add("pvj:pvj_ghost, 0");
        fleeThresholdDefaults.add("pvj:pvj_shade, 0");
        fleeThresholdDefaults.add("pvj:pvj_banshee, 0");
        fleeThresholdDefaults.add("pvj:pvj_skeletal_knight, 0");
        fleeThresholdDefaults.add("pvj:pvj_goon, 0");
        fleeThresholdDefaults.add("arcticmobs:wendigo, 0");
        fleeThresholdDefaults.add("demonmobs:rahovart, 0");
        fleeThresholdDefaults.add("desertmobs:cryptzombie, 0");
        fleeThresholdDefaults.add("elementalmobs:spriggan, 0");
        fleeThresholdDefaults.add("elementalmobs:reiver, 0");
        fleeThresholdDefaults.add("elementalmobs:djinn, 0");
        fleeThresholdDefaults.add("elementalmobs:eechetik, 0");
        fleeThresholdDefaults.add("elementalmobs:spectre, 0");
        fleeThresholdDefaults.add("elementalmobs:zephyr, 0");
        fleeThresholdDefaults.add("elementalmobs:tremor, 0");
        fleeThresholdDefaults.add("elementalmobs:geonach, 0");
        fleeThresholdDefaults.add("elementalmobs:wraith, 0");
        fleeThresholdDefaults.add("elementalmobs:vapula, 0");
        fleeThresholdDefaults.add("elementalmobs:grue, 0");
        fleeThresholdDefaults.add("elementalmobs:cinder, 0");
        fleeThresholdDefaults.add("elementalmobs:banshee, 0");
        fleeThresholdDefaults.add("elementalmobs:aegis, 0");
        fleeThresholdDefaults.add("elementalmobs:sylph, 0");
        fleeThresholdDefaults.add("elementalmobs:volcan, 0");
        fleeThresholdDefaults.add("elementalmobs:jengu, 0");
        fleeThresholdDefaults.add("elementalmobs:xaphan, 0");
        fleeThresholdDefaults.add("elementalmobs:argus, 0");
        fleeThresholdDefaults.add("forestmobs:shambler, 0");
        fleeThresholdDefaults.add("infernomobs:gorger, 0");
        fleeThresholdDefaults.add("infernomobs:ignibus, 0");
        fleeThresholdDefaults.add("junglemobs:dawon, 0");
        fleeThresholdDefaults.add("mountainmobs:jabberwock, 0");
        fleeThresholdDefaults.add("mountainmobs:beholder, 0");
        fleeThresholdDefaults.add("plainsmobs:morock, 0");
        fleeThresholdDefaults.add("plainsmobs:feradon, 0");
        fleeThresholdDefaults.add("shadowmobs:geist, 0");
        fleeThresholdDefaults.add("shadowmobs:phantom, 0");
        fleeThresholdDefaults.add("swampmobs:ghoulzombie, 0");
        fleeThresholdDefaults.add("switchbow:littleirongolem, 0");
        fleeThresholdDefaults.add("quark:wraith, 0");
        fleeThresholdDefaults.add("quark:ashen, 0");
    }
}
