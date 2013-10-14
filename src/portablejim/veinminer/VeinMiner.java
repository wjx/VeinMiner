/* This file is part of VeinMiner.
 *
 *    VeinMiner is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as
 *    published by the Free Software Foundation, either version 3 of
 *     the License, or (at your option) any later version.
 *
 *    VeinMiner is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with VeinMiner.
 *    If not, see <http://www.gnu.org/licenses/>.
 */

package portablejim.veinminer;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.Mod.Metadata;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.registry.LanguageRegistry;
import cpw.mods.fml.common.versioning.ArtifactVersion;
import cpw.mods.fml.common.versioning.DefaultArtifactVersion;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import portablejim.veinminer.configuration.ConfigurationValues;
import portablejim.veinminer.core.MinerInstance;
import portablejim.veinminer.event.EntityDropHook;
import portablejim.veinminer.lib.ModInfo;
import portablejim.veinminer.network.ConnectionHandler;
import portablejim.veinminer.network.PacketHandler;
import portablejim.veinminer.proxy.CommonProxy;
import portablejim.veinminer.server.MinerCommand;
import portablejim.veinminer.server.MinerServer;
import portablejim.veinminer.util.BlockID;

/**
 * This class is the main mod class for Veinminer. It is loaded as a mod
 * through ForgeModLoader.
 */

@Mod(modid = ModInfo.MOD_ID,
        name = ModInfo.MOD_NAME,
        version = ModInfo.VERSION,
        acceptedMinecraftVersions = ModInfo.VALID_MC_VERSIONS,
        certificateFingerprint = "AD915AF2D8BFA7BFF330F4BB5A0A4551EF9E0AED")
@NetworkMod(clientSideRequired = false, serverSideRequired = false, channels = { ModInfo.CHANNEL },
        packetHandler = PacketHandler.class, connectionHandler = ConnectionHandler.class)
public class VeinMiner {

    ConfigurationValues configurationValues;

    @Metadata(value = ModInfo.MOD_ID)
    public static ModMetadata metadata;

    @Instance(ModInfo.MOD_ID)
    public static VeinMiner instance;

    @SidedProxy(clientSide = ModInfo.PROXY_CLIENT_CLASS, serverSide = ModInfo.PROXY_SERVER_CLASS)
    public static CommonProxy proxy;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        configurationValues = new ConfigurationValues(event.getSuggestedConfigurationFile());
        proxy.setupConfig(configurationValues);
        proxy.registerKeybind();

        metadata = event.getModMetadata();
        metadata.modId = ModInfo.MOD_ID;
        metadata.name = ModInfo.MOD_NAME;
        metadata.description = ModInfo.DESCRIPTION;
        metadata.version = ModInfo.VERSION;
        metadata.url = ModInfo.URL;
        metadata.updateUrl = ModInfo.UPDATE_URL;
        metadata.authorList = Lists.newArrayList(ModInfo.AUTHOR);
        metadata.credits = ModInfo.CREDITS;
        metadata.requiredMods = Sets.newHashSet((ArtifactVersion)new DefaultArtifactVersion("Forge", true));
        metadata.autogenerated = false; // Needed, otherwise will not work.Y
    }

    @EventHandler
    public void fingerprintWarning(FMLFingerprintViolationEvent event) {
        // Signing is just for updates. No crashes here.
        FMLLog.getLogger().warning(String.format("%s mod is not properly signed.", ModInfo.MOD_ID));
        FMLLog.getLogger().warning("This may be a copy somebody has modified, or it may be I just forgot to sign it myself.");
        FMLLog.getLogger().warning("Whatever the reason, it's probably ok.");
        FMLLog.getLogger().warning(String.format("Expected fingerprint: %s", event.expectedFingerprint));
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new EntityDropHook());

    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {

    }

    @EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        new MinerServer(configurationValues);

        ServerCommandManager serverCommandManger = (ServerCommandManager) MinecraftServer.getServer().getCommandManager();
        serverCommandManger.registerCommand(new MinerCommand());
    }

    public void blockMined(World world, EntityPlayerMP player, int x, int y, int z, boolean harvestBlockSuccess, BlockID blockId) {
        MinerInstance ins = new MinerInstance(world, player, x, y, z, blockId, MinerServer.instance);
        ins.mineVein(x, y, z);

        if(ModInfo.DEBUG_MODE) {
            String output = String.format("Block mined at %d,%d,%d, result %b, block id is %d:%d", x, y, z, harvestBlockSuccess, blockId.id, blockId.metadata);
            FMLLog.getLogger().info(output);
        }
    }
}
