package com.greencodes.greensky.world;

import com.greencodes.greensky.core.config.WorldSettings;
import com.greencodes.greensky.generation.SingleBiomeProvider;
import com.greencodes.greensky.generation.VoidGenerator;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Biome;

/**
 * Cria/carrega o mundo SkyBlock e cuida do ciclo de vida. O servidor não recarrega
 * mundos de plugin sozinho, então {@link #load()} roda a cada boot, sempre com o
 * mesmo gerador. Todas as chamadas devem ocorrer na thread do servidor.
 */
public final class WorldManager {

    /** Semente fixa: o void não usa, mas mantém o mundo determinístico. */
    private static final long SEED = 0L;

    private final Server server;
    private final Logger logger;
    private final WorldSettings settings;
    private SkyWorld skyWorld;

    public WorldManager(Server server, Logger logger, WorldSettings settings) {
        this.server = server;
        this.logger = logger;
        this.settings = settings;
    }

    /**
     * @throws IllegalStateException se o servidor não conseguir criar/carregar o mundo
     */
    public SkyWorld load() {
        World world = server.getWorld(settings.name());
        if (world == null) {
            world = new WorldCreator(settings.name())
                    .environment(World.Environment.NORMAL)
                    .type(WorldType.NORMAL)
                    .seed(SEED)
                    .generator(new VoidGenerator(settings.spawnY()))
                    .biomeProvider(new SingleBiomeProvider(Biome.PLAINS))
                    .generateStructures(false)
                    .createWorld();
        }
        if (world == null) {
            throw new IllegalStateException("O servidor não criou o mundo '" + settings.name() + "'");
        }

        world.setSpawnLocation(0, settings.spawnY(), 0);
        this.skyWorld = new SkyWorld(world, settings.spawnY());
        logger.info("Mundo '" + world.getName() + "' pronto (void, spawn y=" + settings.spawnY() + ").");
        return skyWorld;
    }

    /** Carrega o chunk do spawn e confirma que ele está vazio (void). Apenas diagnóstico. */
    public void verifyVoid() {
        SkyWorld sky = requireLoaded();
        sky.bukkit().getChunkAtAsync(0, 0).thenAccept(chunk -> {
            var snapshot = chunk.getChunkSnapshot(false, false, false);
            int sections = (sky.bukkit().getMaxHeight() - sky.bukkit().getMinHeight()) >> 4;
            int filled = 0;
            for (int i = 0; i < sections; i++) {
                if (!snapshot.isSectionEmpty(i)) {
                    filled++;
                }
            }
            if (filled == 0) {
                logger.info("Chunk 0,0 verificado: void (" + sections + " seções vazias).");
            } else {
                logger.warning("Chunk 0,0 NÃO está vazio: " + filled + " de " + sections + " seções com blocos.");
            }
        });
    }

    public SkyWorld requireLoaded() {
        if (skyWorld == null) {
            throw new IllegalStateException("O mundo SkyBlock ainda não foi carregado");
        }
        return skyWorld;
    }
}
