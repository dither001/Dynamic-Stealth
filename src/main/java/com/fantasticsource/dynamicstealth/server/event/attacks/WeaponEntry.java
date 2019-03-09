package com.fantasticsource.dynamicstealth.server.event.attacks;

import com.fantasticsource.mctools.potions.Potions;
import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static com.fantasticsource.dynamicstealth.config.DynamicStealthConfig.serverSettings;

public class WeaponEntry
{
    public static final int TYPE_NORMAL = 0, TYPE_STEALTH = 1, TYPE_ASSASSINATION = 2;

    public boolean armorPenetration = false;
    public double damageMultiplier = 1;
    public ArrayList<PotionEffect> attackerEffects = new ArrayList<>();
    public ArrayList<PotionEffect> victimEffects = new ArrayList<>();
    public boolean consumeItem = false;

    public ItemStack itemStack = null;
    private LinkedHashMap<String, String> tags = new LinkedHashMap<>();

    private WeaponEntry(int type)
    {
        //Defaults
        if (type == TYPE_NORMAL)
        {
            armorPenetration = serverSettings.interactions.attack.armorPenetration;
            damageMultiplier = serverSettings.interactions.attack.damageMultiplier;
            attackerEffects = AttackData.normalAttackerEffects;
            victimEffects = AttackData.normalVictimEffects;
        }
        else if (type == TYPE_STEALTH)
        {
            armorPenetration = serverSettings.interactions.stealthAttack.armorPenetration;
            damageMultiplier = serverSettings.interactions.stealthAttack.damageMultiplier;
            attackerEffects = AttackData.stealthAttackerEffects;
            victimEffects = AttackData.stealthVictimEffects;
        }
        else if (type == TYPE_ASSASSINATION)
        {
            attackerEffects = AttackData.assassinationAttackerEffects;
        }
    }

    public WeaponEntry(String configEntry, int type)
    {
        //Defaults
        this(type);


        String[] tokens = configEntry.split(Pattern.quote(","));
        String token;

        if (tokens.length < 2)
        {
            System.err.println("Not enough arguments for weapon entry: " + configEntry);
            return;
        }
        if (((type == TYPE_NORMAL || type == TYPE_STEALTH) && tokens.length > 6) || (type == TYPE_ASSASSINATION && tokens.length > 2))
        {
            System.err.println("Too many arguments for weapon entry: " + configEntry);
            return;
        }

        String[] registryAndNBT = tokens[0].trim().split(Pattern.quote(">"));
        if (registryAndNBT.length > 2)
        {
            System.err.println("Too many arguments for name/NBT in weapon entry: " + tokens[0]);
            return;
        }


        //Item and meta
        token = registryAndNBT[0].trim();
        if (token.equals("")) itemStack = new ItemStack(Items.AIR);
        else
        {
            ResourceLocation resourceLocation;
            int meta = 0;

            String[] innerTokens = token.split(Pattern.quote(":"));
            if (innerTokens.length > 3)
            {
                System.err.println("Bad item name: " + token);
                return;
            }
            if (innerTokens.length == 3)
            {
                resourceLocation = new ResourceLocation(innerTokens[0], innerTokens[1]);
                meta = Integer.parseInt(innerTokens[2]);
            }
            else if (innerTokens.length == 1) resourceLocation = new ResourceLocation("minecraft", innerTokens[0]);
            else
            {
                try
                {
                    meta = Integer.parseInt(innerTokens[1]);
                    resourceLocation = new ResourceLocation("minecraft", innerTokens[0]);
                }
                catch (NumberFormatException e)
                {
                    meta = 0;
                    resourceLocation = new ResourceLocation(innerTokens[0], innerTokens[1]);
                }
            }


            Item item = ForgeRegistries.ITEMS.getValue(resourceLocation);
            if (item != null)
            {
                itemStack = new ItemStack(item, 1, meta);
            }
            else
            {
                Block block = ForgeRegistries.BLOCKS.containsKey(resourceLocation) ? ForgeRegistries.BLOCKS.getValue(resourceLocation) : null;
                if (block != null) itemStack = new ItemStack(block, 1, Integer.parseInt(innerTokens[2]));
            }
        }

        if (itemStack == null)
        {
            if (type == TYPE_NORMAL && !AttackDefaults.normalAttackDefaults.contains(configEntry)) System.err.println("Item for normal attack weapon entry not found: " + token);
            if (type == TYPE_STEALTH && !AttackDefaults.stealthAttackDefaults.contains(configEntry)) System.err.println("Item for stealth attack weapon entry not found: " + token);
            if (type == TYPE_ASSASSINATION && !AttackDefaults.assassinationDefaults.contains(configEntry)) System.err.println("Item for assassination weapon entry not found: " + token);
            return;
        }


        //NBT
        if (registryAndNBT.length > 1)
        {
            String[] tags = registryAndNBT[1].trim().split(Pattern.quote("&"));
            for (String tag : tags)
            {
                tag = tag.trim();
                if (tag.equals("")) continue;

                String[] keyValue = tag.split(Pattern.quote("="));
                if (keyValue.length > 2)
                {
                    System.err.println("Each NBT tag can only be set to one value!  Error in weapon entry: " + configEntry);
                    return;
                }

                String key = keyValue[0].trim();
                if (!key.equals("")) this.tags.put(key, keyValue.length == 2 ? keyValue[1].trim() : null);
            }
        }


        //Easy stuff...
        if (type == TYPE_NORMAL || type == TYPE_STEALTH)
        {
            armorPenetration = Boolean.parseBoolean(tokens[1]);
            if (tokens.length > 2) damageMultiplier = Double.parseDouble(tokens[2]);
        }


        //Potion effects
        if (type == TYPE_ASSASSINATION) attackerEffects = Potions.parsePotions(tokens[1]);
        else if (tokens.length > 3)
        {
            attackerEffects = Potions.parsePotions(tokens[3]);
            if (tokens.length > 4)
            {
                victimEffects = Potions.parsePotions(tokens[4]);
            }
        }


        //More easy stuff...
        if (tokens.length > 5) consumeItem = Boolean.parseBoolean(tokens[5].trim());
    }

    public static WeaponEntry get(ItemStack itemStack, int type)
    {
        NBTTagCompound compound;
        boolean match;

        LinkedHashMap<ItemStack, WeaponEntry> map = null;
        if (type == TYPE_NORMAL) map = AttackData.normalWeaponSpecific;
        else if (type == TYPE_STEALTH) map = AttackData.stealthWeaponSpecific;
        else if (type == TYPE_ASSASSINATION) map = AttackData.assassinationWeaponSpecific;

        for (Map.Entry<ItemStack, WeaponEntry> weaponMapping : map.entrySet())
        {
            WeaponEntry weaponEntry = weaponMapping.getValue();
            ItemStack item = weaponMapping.getKey();
            if (item.getItem().equals(itemStack.getItem()) && (itemStack.isItemStackDamageable() || item.getMetadata() == itemStack.getMetadata()))
            {
                match = true;
                Set<Map.Entry<String, String>> entrySet = weaponEntry.tags.entrySet();

                if (entrySet.size() > 0)
                {
                    compound = itemStack.getTagCompound();
                    if (compound == null) match = false;
                    else
                    {
                        for (Map.Entry<String, String> entry : entrySet)
                        {
                            if (!compound.hasKey(entry.getKey()) || (entry.getValue() != null && !compound.getTag(entry.getKey()).toString().equals(entry.getValue())))
                            {
                                match = false;
                                break;
                            }
                        }
                    }
                }

                if (match) return weaponEntry;
            }
        }

        return new WeaponEntry(type);
    }
}
