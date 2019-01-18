package com.fantasticsource.dynamicstealth.common;

import com.fantasticsource.dynamicstealth.server.Senses.EntityVisionData;
import com.fantasticsource.dynamicstealth.server.Senses.Sight;
import com.fantasticsource.dynamicstealth.server.Threat.Threat;
import com.fantasticsource.tools.datastructures.ExplicitPriorityQueue;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import java.util.LinkedHashMap;

import static com.fantasticsource.dynamicstealth.common.DynamicStealthConfig.serverSettings;
import static com.fantasticsource.dynamicstealth.server.Threat.Threat.bypassesThreat;
import static com.fantasticsource.mctools.MCTools.isOP;

public class Network
{
    public static final SimpleNetworkWrapper WRAPPER = NetworkRegistry.INSTANCE.newSimpleChannel(DynamicStealth.MODID);

    private static int discriminator = 0;

    public static void init()
    {
        WRAPPER.registerMessage(HUDPacketHandler.class, HUDPacket.class, discriminator++, Side.CLIENT);
        WRAPPER.registerMessage(ClientInitPacketHandler.class, ClientInitPacket.class, discriminator++, Side.CLIENT);
        WRAPPER.registerMessage(VisibilityPacketHandler.class, VisibilityPacket.class, discriminator++, Side.CLIENT);
    }


    @SubscribeEvent
    public static void sendClientData(TickEvent.PlayerTickEvent event)
    {
        if (event.side == Side.SERVER && event.phase == TickEvent.Phase.END)
        {
            EntityPlayerMP player = (EntityPlayerMP) event.player;
            if (player != null && player.world.loadedEntityList.contains(player))
            {
                if (serverSettings.senses.usePlayerSenses) WRAPPER.sendTo(new VisibilityPacket(player), player);

                if (isOP(player))
                {
                    boolean detailHUD = serverSettings.threat.hud.allowClientDetailHUD > 0;
                    int onPointHUDMode = serverSettings.threat.hud.opOnPointHUD;
                    if (detailHUD || onPointHUDMode > 0) WRAPPER.sendTo(new HUDPacket(player, detailHUD, onPointHUDMode), player);
                }
                else
                {
                    boolean detailHUD = serverSettings.threat.hud.allowClientDetailHUD > 1;
                    int onPointHUDMode = serverSettings.threat.hud.normalOnPointHUD;
                    if (detailHUD || onPointHUDMode > 0) WRAPPER.sendTo(new HUDPacket(player, detailHUD, onPointHUDMode), player);
                }
            }
        }
    }


    @SubscribeEvent
    public static void sendClientInitData(EntityJoinWorldEvent event)
    {
        Entity entity = event.getEntity();
        if (entity instanceof EntityPlayerMP)
        {
            EntityPlayerMP player = (EntityPlayerMP) entity;
            WRAPPER.sendTo(new ClientInitPacket(player), player);
        }
    }


    public static class VisibilityPacket implements IMessage
    {
        ExplicitPriorityQueue<EntityLivingBase> queue;
        LinkedHashMap<Integer, Float> visibilityMap;

        public VisibilityPacket() //Required; probably for when the packet is received
        {
        }

        public VisibilityPacket(EntityPlayerMP player)
        {
            queue = Sight.seenEntities(player);
        }


        @Override
        public void toBytes(ByteBuf buf)
        {
            int i = queue.size();
            buf.writeInt(i);

            if (serverSettings.senses.usePlayerSenses)
            {
                for (; i > 0; i--)
                {
                    buf.writeFloat((float) (1d - queue.peekPriority()));
                    buf.writeInt(queue.poll().getEntityId());
                }
            }
            else
            {
                for (; i > 0; i--)
                {
                    buf.writeFloat(1);
                    buf.writeInt(queue.poll().getEntityId());
                }
            }
        }

        @Override
        public void fromBytes(ByteBuf buf)
        {
            visibilityMap = new LinkedHashMap<>();
            float visibility;

            for (int i = buf.readInt(); i > 0; i--)
            {
                visibility = buf.readFloat();
                visibilityMap.put(buf.readInt(), visibility);
            }
        }
    }

    public static class VisibilityPacketHandler implements IMessageHandler<VisibilityPacket, IMessage>
    {
        @Override
        public IMessage onMessage(VisibilityPacket packet, MessageContext ctx)
        {
            if (ctx.side == Side.CLIENT)
            {
                Minecraft.getMinecraft().addScheduledTask(() ->
                {
                    ClientData.visibilityMap = packet.visibilityMap;
                });
            }

            return null;
        }
    }


    public static class ClientInitPacket implements IMessage
    {
        boolean soulSight;
        boolean usePlayerSenses;

        public ClientInitPacket() //Required; probably for when the packet is received
        {
        }

        public ClientInitPacket(EntityPlayerMP player)
        {
            soulSight = EntityVisionData.naturalSoulSight(player);
        }

        @Override
        public void toBytes(ByteBuf buf)
        {
            buf.writeBoolean(soulSight);
            buf.writeBoolean(serverSettings.senses.usePlayerSenses);
        }

        @Override
        public void fromBytes(ByteBuf buf)
        {
            soulSight = buf.readBoolean();
            usePlayerSenses = buf.readBoolean();
        }
    }

    public static class ClientInitPacketHandler implements IMessageHandler<ClientInitPacket, IMessage>
    {
        @Override
        public IMessage onMessage(ClientInitPacket packet, MessageContext ctx)
        {
            if (ctx.side == Side.CLIENT)
            {
                Minecraft.getMinecraft().addScheduledTask(() ->
                {
                    ClientData.soulSight = packet.soulSight;
                    ClientData.usePlayerSenses = packet.usePlayerSenses;
                });
            }

            return null;
        }
    }


    public static class HUDPacket implements IMessage
    {
        EntityPlayerMP player;
        ExplicitPriorityQueue<EntityLivingBase> queue;
        boolean detailHUD;
        int onPointHUDMode;

        String detailSearcherName;
        String detailTargetName;
        int detailPercent;
        int detailColor;

        LinkedHashMap<Integer, ClientData.OnPointData> onPointMap = new LinkedHashMap<>(10);

        public HUDPacket() //Required; probably for when the packet is received
        {
        }

        public HUDPacket(EntityPlayerMP player, boolean detailHUD, int onPointHUDMode)
        {
            this.player = player;
            this.detailHUD = detailHUD;
            this.onPointHUDMode = onPointHUDMode;

            queue = Sight.seenEntities(this.player);
        }

        @Override
        public void toBytes(ByteBuf buf)
        {
            EntityLivingBase searcher;
            int maxThreat = serverSettings.threat.maxThreat;

            buf.writeBoolean(detailHUD);
            buf.writeInt(onPointHUDMode);

            if (detailHUD)
            {
                searcher = queue.poll();
                if (searcher == null) buf.writeInt(ClientData.COLOR_NULL); //if color == COLOR_NULL then searcher is null
                else if (bypassesThreat(searcher))
                {
                    buf.writeInt(ClientData.COLOR_ALERT);
                    buf.writeInt(-1); //else if threatLevel == -1 then searcher bypasses threat
                    ByteBufUtils.writeUTF8String(buf, searcher.getName());

                    if (onPointHUDMode > 0) buf.writeInt(searcher.getEntityId());
                }
                else
                {
                    Threat.ThreatData data = Threat.get(searcher);

                    buf.writeInt(ClientData.getColor(player, searcher, data.target, data.threatLevel)); //else this is a normal entry
                    buf.writeInt((int) (100D * data.threatLevel / maxThreat));

                    ByteBufUtils.writeUTF8String(buf, searcher.getName());
                    ByteBufUtils.writeUTF8String(buf, data.target == null ? ClientData.EMPTY : data.target.getName());

                    if (onPointHUDMode > 0) buf.writeInt(searcher.getEntityId());
                }
            }
            else //!detailHUD
            {
                if (onPointHUDMode == 1)
                {
                    searcher = queue.poll();
                    if (searcher == null) buf.writeInt(ClientData.COLOR_NULL);
                    else
                    {
                        Threat.ThreatData data = Threat.get(searcher);

                        buf.writeInt(ClientData.getColor(player, searcher, data.target, data.threatLevel));
                        buf.writeInt(searcher.getEntityId());
                        buf.writeInt((int) (100D * data.threatLevel / maxThreat));
                    }
                }
            }

            if (onPointHUDMode == 2)
            {
                buf.writeInt(queue.size());

                while (queue.size() > 0)
                {
                    searcher = queue.poll();
                    if (searcher == null) buf.writeInt(ClientData.COLOR_NULL); //if color == COLOR_NULL then searcher is null
                    else if (bypassesThreat(searcher))
                    {
                        buf.writeInt(ClientData.COLOR_ALERT);
                        buf.writeInt(searcher.getEntityId());
                        buf.writeInt(-1); //else if threatLevel == -1 then searcher bypasses threat
                    }
                    else
                    {
                        Threat.ThreatData data = Threat.get(searcher);

                        buf.writeInt(ClientData.getColor(player, searcher, data.target, data.threatLevel)); //else this is a normal entry
                        buf.writeInt(searcher.getEntityId());
                        buf.writeInt((int) (100D * data.threatLevel / maxThreat));
                    }
                }
            }
        }

        @Override
        public void fromBytes(ByteBuf buf)
        {
            onPointMap.clear();
            int color;
            int priority = 0;

            detailHUD = buf.readBoolean();
            onPointHUDMode = buf.readInt();

            if (detailHUD)
            {
                detailColor = buf.readInt();

                if (detailColor != ClientData.COLOR_NULL)
                {
                    detailPercent = buf.readInt();

                    detailSearcherName = ByteBufUtils.readUTF8String(buf);
                    detailTargetName = detailPercent == -1 ? ClientData.UNKNOWN : ByteBufUtils.readUTF8String(buf);

                    if (onPointHUDMode > 0) onPointMap.put(buf.readInt(), new ClientData.OnPointData(detailColor, detailPercent, priority++));
                }
            }
            else
            {
                if (onPointHUDMode == 1)
                {
                    color = buf.readInt();
                    if (color != ClientData.COLOR_NULL) onPointMap.put(buf.readInt(), new ClientData.OnPointData(color, buf.readInt(), priority++));
                }
            }

            if (onPointHUDMode == 2)
            {
                for (int i = buf.readInt(); i > 0; i--)
                {
                    color = buf.readInt();
                    if (color != ClientData.COLOR_NULL) onPointMap.put(buf.readInt(), new ClientData.OnPointData(color, buf.readInt(), priority++));
                }
            }
        }
    }

    public static class HUDPacketHandler implements IMessageHandler<HUDPacket, IMessage>
    {
        @Override
        public IMessage onMessage(HUDPacket packet, MessageContext ctx)
        {
            if (ctx.side == Side.CLIENT)
            {
                Minecraft.getMinecraft().addScheduledTask(() ->
                {
                    ClientData.detailColor = packet.detailColor;

                    if (ClientData.detailColor == ClientData.COLOR_NULL)
                    {
                        ClientData.detailPercent = -1;
                        ClientData.detailSearcher = ClientData.EMPTY;
                        ClientData.detailTarget = ClientData.EMPTY;
                    }
                    else
                    {
                        ClientData.detailSearcher = I18n.format(packet.detailSearcherName);
                        ClientData.detailPercent = packet.detailPercent;

                        ClientData.detailTarget = ClientData.detailPercent == -1 ? ClientData.EMPTY : I18n.format(packet.detailTargetName);
                    }

                    ClientData.onPointDataMap = packet.onPointMap;
                });
            }

            return null;
        }
    }
}
