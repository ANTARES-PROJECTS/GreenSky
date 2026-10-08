package com.greencodes.greensky;

import com.greencodes.greensky.border.IslandBorderListener;
import com.greencodes.greensky.border.IslandBorderService;
import com.greencodes.greensky.collection.CollectionCatalog;
import com.greencodes.greensky.collection.CollectionCommand;
import com.greencodes.greensky.collection.CollectionListener;
import com.greencodes.greensky.collection.CollectionRepository;
import com.greencodes.greensky.collection.CollectionService;
import com.greencodes.greensky.collection.CollectionTracker;
import com.greencodes.greensky.content.ContentRegistry;
import com.greencodes.greensky.content.ContentYaml;
import com.greencodes.greensky.content.ItemCommand;
import com.greencodes.greensky.content.ItemFactory;
import com.greencodes.greensky.core.GreenScheduler;
import com.greencodes.greensky.fishing.FishLoader;
import com.greencodes.greensky.fishing.FishTable;
import com.greencodes.greensky.farming.CropCatalog;
import com.greencodes.greensky.farming.FarmingListener;
import com.greencodes.greensky.farming.CropItemFactory;
import com.greencodes.greensky.farming.AncientPlants;
import com.greencodes.greensky.farming.CropMarkers;
import com.greencodes.greensky.farming.FertilizerListener;
import com.greencodes.greensky.farming.FarmingCommand;
import com.greencodes.greensky.gui.GameMenus;
import com.greencodes.greensky.farming.AncientPlantListener;
import com.greencodes.greensky.fishing.FishingListener;
import com.greencodes.greensky.core.config.AuthSettings;
import com.greencodes.greensky.core.config.ConfigException;
import com.greencodes.greensky.core.config.DatabaseSettings;
import com.greencodes.greensky.core.config.GreenSkyConfig;
import com.greencodes.greensky.database.Database;
import com.greencodes.greensky.database.DatabaseException;
import com.greencodes.greensky.integration.auth.AuthBridge;
import com.greencodes.greensky.integration.auth.NLoginAuthBridge;
import com.greencodes.greensky.integration.auth.NoAuthBridge;
import com.greencodes.greensky.island.IslandCommand;
import com.greencodes.greensky.island.IslandRepository;
import com.greencodes.greensky.island.IslandService;
import com.greencodes.greensky.island.StarterIslandBuilder;
import com.greencodes.greensky.player.PlayerRepository;
import com.greencodes.greensky.player.PlayerService;
import com.greencodes.greensky.player.PlayerSessionListener;
import com.greencodes.greensky.player.PlayerSessionService;
import com.greencodes.greensky.protection.DenyNotifier;
import com.greencodes.greensky.protection.IslandProtectionService;
import com.greencodes.greensky.protection.PlayerProtectionListener;
import com.greencodes.greensky.protection.ProtectionSessionListener;
import com.greencodes.greensky.protection.WorldProtectionListener;
import com.greencodes.greensky.visit.HomeListener;
import com.greencodes.greensky.visit.VisitorExpeller;
import com.greencodes.greensky.world.WorldManager;
import java.io.File;
import java.util.Random;
import org.bukkit.Material;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class GreenSkyPlugin extends JavaPlugin {

    /** Intervalo do lote de gravação das coleções (10 s). */
    private static final long FLUSH_TICKS = 200L;

    private GreenSkyConfig config;
    private CollectionService collectionService;
    private GreenScheduler scheduler;
    private Database database;
    private WorldManager worldManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        try {
            this.config = GreenSkyConfig.load(getConfig());
        } catch (ConfigException e) {
            abort("config.yml inválido: " + e.getMessage());
            return;
        }
        this.scheduler = new GreenScheduler(this);

        // Conteúdo (itens e coleções): validado no boot; um erro diz o arquivo e a chave.
        ContentRegistry content;
        CollectionCatalog catalog;
        FishTable fishTable;
        CropCatalog crops;
        try {
            content = ContentRegistry.load(contentFile("content/items.yml"));
            catalog = CollectionCatalog.load(contentFile("content/collections.yml"));
            fishTable = FishLoader.load(contentFile("content/fish.yml"), catalog, Material::isItem);
            crops = CropCatalog.load(contentFile("content/crops.yml"), catalog, Material::isItem);
            content = content.withItems(crops.bonusItems());
        } catch (IllegalArgumentException e) {
            abort("Conteúdo inválido: " + e.getMessage());
            return;
        }

        // Autenticação primeiro: com online-mode=false, subir sem plugin de login seria inseguro.
        AuthBridge auth;
        try {
            auth = createAuthBridge();
        } catch (IllegalStateException e) {
            abort(e.getMessage());
            return;
        }

        String password = System.getenv(DatabaseSettings.PASSWORD_ENV);
        if (password == null || password.isBlank()) {
            abort("Variável de ambiente " + DatabaseSettings.PASSWORD_ENV + " não definida.");
            return;
        }
        try {
            // Boot: conexão e migrations são bloqueantes aqui; depois disso todo SQL é assíncrono.
            this.database = Database.open(config.database(), password, getClassLoader());
        } catch (DatabaseException e) {
            getLogger().severe(e.getMessage() + ": " + rootMessage(e));
            abort("Banco de dados indisponível.");
            return;
        }

        try {
            this.worldManager = new WorldManager(getServer(), getLogger(), config.world());
            this.worldManager.load();
            this.worldManager.verifyVoid();
        } catch (IllegalStateException e) {
            abort("Mundo SkyBlock indisponível: " + e.getMessage());
            return;
        }

        PluginManager plugins = getServer().getPluginManager();
        PlayerRepository playerRepository = new PlayerRepository();
        PlayerService playerService = new PlayerService(database, playerRepository);
        // Sessão: só depois da autenticação o jogador fica "pronto" (GreenSkyPlayerReadyEvent).
        PlayerSessionService sessions = new PlayerSessionService(playerService, scheduler, plugins, getLogger());
        plugins.registerEvents(new PlayerSessionListener(playerService, sessions, getLogger()), this);

        IslandService islandService = new IslandService(
                database,
                new IslandRepository(),
                playerRepository,
                new StarterIslandBuilder(worldManager.requireLoaded(), scheduler, config.islands()),
                config.islands(),
                config.expansion(),
                config.defaultVisibility());
        IslandCommand islandCommand = new IslandCommand(
                getServer(),
                getLogger(),
                islandService,
                worldManager.requireLoaded(),
                config.islands(),
                scheduler,
                playerService,
                sessions::isReady);

        // Proteção: índice em memória alimentado pelo banco; até carregar, tudo é negado.
        DenyNotifier notifier = new DenyNotifier();
        IslandProtectionService protection = new IslandProtectionService();
        islandService.addListener(protection);
        plugins.registerEvents(new PlayerProtectionListener(protection, worldManager.requireLoaded(), notifier), this);
        plugins.registerEvents(new WorldProtectionListener(protection, worldManager.requireLoaded()), this);
        plugins.registerEvents(new ProtectionSessionListener(protection, islandService, notifier, getLogger()), this);

        // Borda visual por jogador (fase 6). Registrada depois da proteção: usa o mesmo índice.
        IslandBorderService borders =
                new IslandBorderService(getServer(), worldManager.requireLoaded(), protection, scheduler);
        islandService.addListener(borders);
        plugins.registerEvents(new IslandBorderListener(borders, sessions::isReady), this);

        // Visitas e "casa" (fase 7): expulsa visitantes ao ficar privada; renasce na ilha; dica de início.
        islandService.addListener(new VisitorExpeller(getServer(), worldManager.requireLoaded(), protection, scheduler));
        plugins.registerEvents(
                new HomeListener(islandService, protection, worldManager.requireLoaded(), config.islands(), scheduler),
                this);
        islandService.loadAll().whenComplete((all, error) -> {
            if (error != null) {
                getLogger().log(Level.SEVERE, "Falha ao carregar as ilhas; a proteção negará tudo.", error);
            } else {
                protection.load(all);
                getLogger().info("Proteção ativa: " + all.size() + " ilha(s) indexada(s).");
            }
        });
        // Coleções (fase 8.1): progresso em memória, gravado em lote; carrega quando o jogador fica pronto.
        this.collectionService = new CollectionService(database, new CollectionRepository(), catalog);
        CollectionTracker collectionTracker = new CollectionTracker(collectionService, plugins);
        plugins.registerEvents(new CollectionListener(collectionService, getLogger()), this);
        CollectionCommand collectionCommand =
                new CollectionCommand(getServer(), collectionService, collectionTracker, sessions::isReady, scheduler);
        PluginCommand collectionsCmd = getCommand("collections");
        collectionsCmd.setExecutor(collectionCommand);
        collectionsCmd.setTabCompleter(collectionCommand);
        scheduler.runSyncTimer(collectionService::flushAll, FLUSH_TICKS, FLUSH_TICKS);

        ItemFactory itemFactory = new ItemFactory(this);
        ItemCommand itemCommand = new ItemCommand(getServer(), content, itemFactory);
        PluginCommand greenskyCmd = getCommand("greensky");
        greenskyCmd.setExecutor(itemCommand);
        greenskyCmd.setTabCompleter(itemCommand);

        // Pesca (fase 8.2): troca só o peixe vanilla, só na ilha do jogador, em água aberta e sem pesca parada.
        plugins.registerEvents(new FishingListener(this, fishTable, config.fishing(), protection,
                worldManager.requireLoaded(), itemFactory, collectionTracker, sessions::isReady, new Random()), this);
        AncientPlants ancientPlants = new AncientPlants(this);
        CropMarkers fertilized = new CropMarkers(this, "fertilized_plants");
        FarmingCommand farmingCommand=new FarmingCommand(crops, ancientPlants, fertilized,
                worldManager.requireLoaded(), protection, collectionTracker, sessions::isReady);
        GameMenus menus=new GameMenus(this,scheduler,sessions::isReady,islandService,playerService,
                collectionService,crops,ancientPlants,fertilized,worldManager.requireLoaded(),protection,content);
        plugins.registerEvents(menus,this);
        getCommand("menu").setExecutor(menus);
        getCommand("guia").setExecutor((sender,cmd,label,args)-> {
            if (sender instanceof Player player) menus.guide(player);
            return true;
        });
        getCommand("pesca").setExecutor((sender,cmd,label,args)-> {
            if (sender instanceof Player player) menus.fishing(player);
            return true;
        });
        getCommand("agricultura").setExecutor((sender,cmd,label,args)-> {
            if (args.length==0 && sender instanceof Player player) { menus.farming(player); return true; }
            return farmingCommand.onCommand(sender,cmd,label,args);
        });
        collectionsCmd.setExecutor((sender,cmd,label,args)-> {
            if (args.length==0 && sender instanceof Player player) { menus.collectionList(player); return true; }
            return collectionCommand.onCommand(sender,cmd,label,
                    args.length>0 && args[0].equalsIgnoreCase("texto")?java.util.Arrays.copyOfRange(args,1,args.length):args);
        });
        greenskyCmd.setExecutor((sender,cmd,label,args)-> {
            if (args.length==0 && sender instanceof Player player) { menus.main(player); return true; }
            return itemCommand.onCommand(sender,cmd,label,args);
        });
        plugins.registerEvents(new FertilizerListener(crops, ancientPlants, fertilized, itemFactory, protection,
                worldManager.requireLoaded(), sessions::isReady, collectionTracker), this);
        plugins.registerEvents(new AncientPlantListener(ancientPlants, fertilized, itemFactory, protection,
                worldManager.requireLoaded(), sessions::isReady, collectionTracker), this);
        plugins.registerEvents(new FarmingListener(crops, protection, worldManager.requireLoaded(),
                collectionTracker, itemFactory, new CropItemFactory(this, itemFactory), ancientPlants, fertilized,
                sessions::isReady, new Random()), this);

        // Liga a autenticação por último: todos os ouvintes do "jogador pronto" já estão registrados.
        auth.start(this, sessions::authenticated);
        // Quem já estava online e autenticado (ex.: /reload) começa a sessão agora.
        for (Player online : getServer().getOnlinePlayers()) {
            if (auth.isAuthenticated(online)) {
                sessions.authenticated(online);
            }
        }

        PluginCommand command = getCommand("island");
        command.setExecutor((sender,cmd,label,args)-> {
            if (args.length==0 && sender instanceof Player player) { menus.island(player); return true; }
            return islandCommand.onCommand(sender,cmd,label,args);
        });
        command.setTabCompleter(islandCommand);

        getLogger().info("GreenSky " + getPluginMeta().getVersion() + " habilitado (ilhas: tamanho inicial "
                + config.islands().initialSize() + ", máx " + config.islands().maxSize()
                + ", espaçamento " + config.islands().spacing() + "; banco "
                + config.database().jdbcUrl() + ").");
    }

    @Override
    public void onDisable() {
        if (scheduler != null) {
            scheduler.cancelAll();
        }
        // O mundo é salvo e descarregado pelo próprio servidor ao desligar.
        if (collectionService != null && database != null) {
            try {
                // Último lote das coleções antes de fechar o banco (desligamento: pode esperar).
                collectionService.flushAll().get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Não foi possível gravar o último lote das coleções", e);
            }
        }
        if (database != null) {
            database.close();
        }
        getLogger().info("GreenSky desabilitado.");
    }

    /**
     * Escolhe a ponte de autenticação conforme {@code auth.provider}.
     *
     * @throws IllegalStateException se a configuração for insegura ou o plugin de login faltar
     */
    private AuthBridge createAuthBridge() {
        AuthSettings settings = config.auth();
        boolean onlineMode = getServer().getOnlineMode();
        settings.validateFor(onlineMode);
        if (settings.insecureDevMode(onlineMode)) {
            String line = "!".repeat(78);
            getLogger().warning(line);
            getLogger().warning("auth.dev-mode=true com auth.provider=none e online-mode=false:");
            getLogger().warning("QUALQUER UM PODE ENTRAR COM QUALQUER NICK, SEM SENHA.");
            getLogger().warning("Use SÓ em desenvolvimento/testes. NUNCA em produção.");
            getLogger().warning(line);
        }
        return switch (settings.provider()) {
            case NONE -> new NoAuthBridge();
            case NLOGIN -> {
                if (!getServer().getPluginManager().isPluginEnabled("nLogin")) {
                    throw new IllegalStateException(
                            "auth.provider=nlogin, mas o plugin nLogin não está instalado ou não iniciou.");
                }
                getLogger().info("Autenticação: nLogin. A lógica do GreenSky só roda depois do login.");
                yield new NLoginAuthBridge(scheduler, getLogger());
            }
        };
    }

    /** Lê um arquivo de conteúdo da pasta do plugin; na primeira vez, copia o padrão do jar. */
    private YamlConfiguration contentFile(String path) {
        File file = new File(getDataFolder(), path);
        if (!file.exists()) {
            saveResource(path, false);
        }
        return ContentYaml.load(file);
    }

    private void abort(String reason) {
        getLogger().severe(reason);
        getLogger().severe("GreenSky não pode iniciar. Corrija e reinicie.");
        getServer().getPluginManager().disablePlugin(this);
    }

    private static String rootMessage(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getMessage();
    }
}
