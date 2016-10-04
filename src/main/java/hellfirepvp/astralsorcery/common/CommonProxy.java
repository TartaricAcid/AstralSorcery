package hellfirepvp.astralsorcery.common;

import hellfirepvp.astralsorcery.AstralSorcery;
import hellfirepvp.astralsorcery.common.auxiliary.link.LinkHandler;
import hellfirepvp.astralsorcery.common.auxiliary.tick.TickManager;
import hellfirepvp.astralsorcery.common.constellation.CelestialHandler;
import hellfirepvp.astralsorcery.common.container.ContainerAltarDiscovery;
import hellfirepvp.astralsorcery.common.crafting.altar.AltarRecipeRegistry;
import hellfirepvp.astralsorcery.common.data.SyncDataHolder;
import hellfirepvp.astralsorcery.common.data.world.WorldCacheManager;
import hellfirepvp.astralsorcery.common.event.listener.EventHandlerAchievements;
import hellfirepvp.astralsorcery.common.event.listener.EventHandlerNetwork;
import hellfirepvp.astralsorcery.common.event.listener.EventHandlerServer;
import hellfirepvp.astralsorcery.common.lib.BlocksAS;
import hellfirepvp.astralsorcery.common.network.PacketChannel;
import hellfirepvp.astralsorcery.common.registry.RegistryAchievements;
import hellfirepvp.astralsorcery.common.registry.RegistryBlocks;
import hellfirepvp.astralsorcery.common.registry.RegistryConstellations;
import hellfirepvp.astralsorcery.common.registry.RegistryEntities;
import hellfirepvp.astralsorcery.common.registry.RegistryItems;
import hellfirepvp.astralsorcery.common.registry.RegistryRecipes;
import hellfirepvp.astralsorcery.common.registry.RegistryResearch;
import hellfirepvp.astralsorcery.common.registry.RegistryStructures;
import hellfirepvp.astralsorcery.common.ritual.RitualComponentRegistry;
import hellfirepvp.astralsorcery.common.starlight.network.StarlightTransmissionHandler;
import hellfirepvp.astralsorcery.common.starlight.network.StarlightUpdateHandler;
import hellfirepvp.astralsorcery.common.starlight.network.TransmissionChunkTracker;
import hellfirepvp.astralsorcery.common.starlight.transmission.registry.SourceClassRegistry;
import hellfirepvp.astralsorcery.common.starlight.transmission.registry.TransmissionClassRegistry;
import hellfirepvp.astralsorcery.common.tile.TileAltar;
import hellfirepvp.astralsorcery.common.util.LootTableUtil;
import hellfirepvp.astralsorcery.common.util.MiscUtils;
import hellfirepvp.astralsorcery.common.world.AstralWorldGenerator;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;

/**
 * This class is part of the Astral Sorcery Mod
 * The complete source code for this mod can be found on github.
 * Class: CommonProxy
 * Created by HellFirePvP
 * Date: 07.05.2016 / 00:23
 */
public class CommonProxy implements IGuiHandler {

    public void preInit() {
        RegistryItems.setupDefaults();

        RegistryConstellations.init();

        PacketChannel.init();

        RegistryBlocks.init();
        RegistryItems.init();
        RegistryEntities.init();
        RegistryStructures.init();

        //Transmission registry
        SourceClassRegistry.setupRegistry();
        TransmissionClassRegistry.setupRegistry();
        //StarlightNetworkRegistry.setupRegistry();

        RitualComponentRegistry.setupRegistry();

        RegistryBlocks.initRenderRegistry();
        RegistryRecipes.init();
        RegistryResearch.init();

        GameRegistry.registerWorldGenerator(new AstralWorldGenerator().init(), 0);
        LootTableUtil.initLootTable();

        registerOreDictEntries();
        RegistryAchievements.init();
    }

    private void registerOreDictEntries() {
        OreDictionary.registerOre("blockMarble", BlocksAS.blockMarble);
    }

    public void init() {
        NetworkRegistry.INSTANCE.registerGuiHandler(AstralSorcery.instance, this);

        MinecraftForge.EVENT_BUS.register(new EventHandlerNetwork());
        MinecraftForge.EVENT_BUS.register(new EventHandlerServer());
        MinecraftForge.EVENT_BUS.register(new EventHandlerAchievements());
        MinecraftForge.EVENT_BUS.register(TransmissionChunkTracker.getInstance());
        MinecraftForge.EVENT_BUS.register(TickManager.getInstance());
        MinecraftForge.EVENT_BUS.register(StarlightTransmissionHandler.getInstance());
        MinecraftForge.EVENT_BUS.register(new LootTableUtil());

        TickManager manager = TickManager.getInstance();
        registerTickHandlers(manager);

        SyncDataHolder.initialize();
    }

    protected void registerTickHandlers(TickManager manager) {
        manager.register(new CelestialHandler.CelestialTickHandler());
        manager.register(StarlightTransmissionHandler.getInstance());
        manager.register(StarlightUpdateHandler.getInstance());
        manager.register(WorldCacheManager.getInstance());
        manager.register(new LinkHandler()); //Only used as instance for tick handling
        manager.register(SyncDataHolder.getTickInstance());
    }

    public void postInit() {
        AstralSorcery.log.info("[AstralSorcery] Post compile recipes");

        AltarRecipeRegistry.compileRecipes();
    }

    public void registerVariantName(Item item, String name) {}

    public void registerBlockRender(Block block, int metadata, String name) {}

    public void registerItemRender(Item item, int metadata, String name) {}

    public <T extends Item> void registerItemRender(T item, int metadata, String name, boolean variant) {}

    public void registerFromSubItems(Item item, String name) {}

    public void scheduleClientside(Runnable r, int tickDelay) {}

    public void scheduleClientside(Runnable r) {
        scheduleClientside(r, 0);
    }

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if(id < 0 || id >= EnumGuiId.values().length) return null; //Out of range.
        EnumGuiId guiType = EnumGuiId.values()[id];

        TileEntity t = null;
        if(guiType.getTileClass() != null) {
            t = MiscUtils.getTileAt(world, new BlockPos(x, y, z), guiType.getTileClass(), true);
            if(t == null) {
                return null;
            }
        }

        switch (guiType) {
            case ALTAR_DISCOVERY:
                return new ContainerAltarDiscovery(player.inventory, (TileAltar) t);
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return null;
    }

    public void openGui(EnumGuiId guiId, EntityPlayer player, World world, int x, int y, int z) {
        player.openGui(AstralSorcery.instance, guiId.ordinal(), world, x, y, z);
    }

    public static enum EnumGuiId {

        TELESCOPE,
        CONSTELLATION_PAPER,
        ALTAR_DISCOVERY(TileAltar.class),
        JOURNAL;

        private final Class<? extends TileEntity> tileClass;

        private EnumGuiId() {
            this(null);
        }

        private EnumGuiId(Class<? extends TileEntity> tileClass) {
            this.tileClass = tileClass;
        }

        public Class<? extends TileEntity> getTileClass() {
            return tileClass;
        }
    }

}
