package com.fantasticsource.dynamicstealth.server.ai.edited;

import com.fantasticsource.mctools.MCTools;
import com.fantasticsource.tools.ReflectionTool;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.monster.EntityPigZombie;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AIPigmanHurtByAggressorEdit extends AIHurtByTargetEdit
{
    private static Method pigmanBecomeAngryAtMethod;

    static
    {
        pigmanBecomeAngryAtMethod = ReflectionTool.getMethod(EntityPigZombie.class, "func_70835_c", "becomeAngryAt");
    }


    public AIPigmanHurtByAggressorEdit(EntityAIHurtByTarget oldAI)
    {
        super(oldAI);
    }

    @Override
    protected void setEntityAttackTarget(EntityCreature attacker, EntityLivingBase target)
    {
        super.setEntityAttackTarget(attacker, target);

        if (attacker instanceof EntityPigZombie)
        {
            try
            {
                pigmanBecomeAngryAtMethod.invoke(attacker, target);
            }
            catch (IllegalAccessException | InvocationTargetException e)
            {
                MCTools.crash(e, 130, false);
            }
        }
    }
}
