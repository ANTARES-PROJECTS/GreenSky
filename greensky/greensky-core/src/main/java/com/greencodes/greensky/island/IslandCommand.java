package com.greencodes.greensky.island;

import com.greencodes.greensky.core.GreenScheduler;
import com.greencodes.greensky.core.config.IslandSettings;
import com.greencodes.greensky.player.PlayerRecord;
import com.greencodes.greensky.player.PlayerService;
import com.greencodes.greensky.world.SkyWorld;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/**
 * {@code /island} (alias {@code /is}). Apenas traduz comandos em chamadas ao
 * {@link IslandService}; não contém regra de negócio.
 */
public final class IslandCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS =
            List.of("create", "home", "info", "visit", "public", "private", "add", "remove", "admin");

    private final Server server;
    private final Logger logger;
    private final IslandService service;
    private final SkyWorld skyWorld;
    private final IslandSettings settings;
    private final GreenScheduler scheduler;
    private final PlayerService players;
    private final Predicate<Player> ready;

    /**
     * @param ready o jogador já autenticou e a sessão no GreenSky começou?
     */
    public IslandCommand(
            Server server,
            Logger logger,
            IslandService service,
            SkyWorld skyWorld,
            IslandSettings settings,
            GreenScheduler scheduler,
            PlayerService players,
            Predicate<Player> ready) {
        this.server = server;
        this.logger = logger;
        this.service = service;
        this.skyWorld = skyWorld;
        this.settings = settings;
        this.scheduler = scheduler;
        this.players = players;
        this.ready = ready;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "home" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "create" -> withPlayer(sender, player -> create(sender, player.getUniqueId(), player.getName(), player));
            case "home" -> withPlayer(sender, player -> home(player));
            case "info" -> withPlayer(sender, player -> info(player));
            case "visit" -> withPlayer(sender, player -> visit(player, args));
            case "public" -> withPlayer(sender, player -> setVisibility(player, IslandVisibility.PUBLIC));
            case "private" -> withPlayer(sender, player -> setVisibility(player, IslandVisibility.PRIVATE));
            case "add" -> withPlayer(sender, player -> add(player, args));
            case "remove" -> withPlayer(sender, player -> remove(player, args));
            case "admin" -> admin(sender, args);
            default -> send(sender, "Uso: /" + label + " <create|home|info|visit|public|private|add|remove>",
                    NamedTextColor.YELLOW);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream().filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        // null = o servidor sugere nomes de jogadores online (útil em add/remove).
        return args.length == 2 ? null : List.of();
    }

    private void create(CommandSender reply, UUID uuid, String name, Player teleportTo) {
        send(reply, "Criando a ilha...", NamedTextColor.GRAY);
        service.create(uuid, name).whenComplete((island, error) -> {
            if (error != null) {
                fail(reply, error);
                return;
            }
            send(reply, "Ilha criada!", NamedTextColor.GREEN);
            if (teleportTo != null) {
                teleportHome(teleportTo, island);
            }
        });
    }

    private void home(Player player) {
        service.findByOwner(player.getUniqueId())
                .thenCompose(found -> found.isPresent()
                        ? service.ensureReady(found.get())
                        : java.util.concurrent.CompletableFuture.<Island>failedFuture(
                                new IslandException(IslandException.Reason.NO_ISLAND)))
                .whenComplete((island, error) -> {
                    if (error != null) {
                        fail(player, error);
                    } else {
                        teleportHome(player, island);
                    }
                });
    }

    private void info(Player player) {
        service.findByOwner(player.getUniqueId()).whenComplete((found, error) -> {
            if (error != null) {
                fail(player, error);
            } else if (found.isEmpty()) {
                fail(player, new IslandException(IslandException.Reason.NO_ISLAND));
            } else {
                Island island = found.get();
                IslandRegion r = island.region();
                send(player, "Ilha #" + island.slot() + " | centro " + r.centerX() + ", " + r.centerZ()
                        + " | tamanho " + r.size() + " | nível " + service.levelOf(island) + " | " + island.state(),
                        NamedTextColor.AQUA);
            }
        });
    }

    private void visit(Player visitor, String[] args) {
        if (args.length < 2) {
            send(visitor, "Uso: /is visit <jogador>", NamedTextColor.YELLOW);
            return;
        }
        withKnownPlayer(visitor, args[1], target ->
                service.authorizeVisit(visitor.getUniqueId(), target.uuid()).whenComplete((island, error) -> {
                    if (error != null) {
                        fail(visitor, error);
                    } else {
                        send(visitor, "Visitando a ilha de " + target.name() + ".", NamedTextColor.GREEN);
                        teleportHome(visitor, island);
                    }
                }));
    }

    private void setVisibility(Player owner, IslandVisibility visibility) {
        service.findByOwner(owner.getUniqueId())
                .thenCompose(found -> found.isPresent()
                        ? service.setVisibility(found.get(), owner.getUniqueId(), visibility)
                        : java.util.concurrent.CompletableFuture.<Void>failedFuture(
                                new IslandException(IslandException.Reason.NO_ISLAND)))
                .whenComplete((ignored, error) -> {
                    if (error != null) {
                        fail(owner, error);
                    } else if (visibility == IslandVisibility.PUBLIC) {
                        send(owner, "Sua ilha agora é pública: qualquer um pode visitar.", NamedTextColor.GREEN);
                    } else {
                        send(owner, "Sua ilha agora é privada: só membros podem visitar.", NamedTextColor.GREEN);
                    }
                });
    }

    private void add(Player owner, String[] args) {
        if (args.length < 2) {
            send(owner, "Uso: /is add <jogador online>", NamedTextColor.YELLOW);
            return;
        }
        Player target = server.getPlayerExact(args[1]);
        if (target == null) {
            send(owner, "Jogador '" + args[1] + "' não está online.", NamedTextColor.RED);
            return;
        }
        service.findByOwner(owner.getUniqueId())
                .thenCompose(found -> found.isPresent()
                        ? service.addMember(found.get(), owner.getUniqueId(), target.getUniqueId(), target.getName())
                        : java.util.concurrent.CompletableFuture.<IslandMember>failedFuture(
                                new IslandException(IslandException.Reason.NO_ISLAND)))
                .whenComplete((member, error) -> {
                    if (error != null) {
                        fail(owner, error);
                    } else {
                        send(owner, target.getName() + " agora é membro da sua ilha.", NamedTextColor.GREEN);
                    }
                });
    }

    private void remove(Player owner, String[] args) {
        if (args.length < 2) {
            send(owner, "Uso: /is remove <jogador>", NamedTextColor.YELLOW);
            return;
        }
        withKnownPlayer(owner, args[1], target -> service.findByOwner(owner.getUniqueId())
                .thenCompose(found -> found.isPresent()
                        ? service.removeMember(found.get(), owner.getUniqueId(), target.uuid())
                        : java.util.concurrent.CompletableFuture.<Void>failedFuture(
                                new IslandException(IslandException.Reason.NO_ISLAND)))
                .whenComplete((ignored, error) -> {
                    if (error != null) {
                        fail(owner, error);
                    } else {
                        send(owner, target.name() + " foi removido da sua ilha.", NamedTextColor.GREEN);
                    }
                }));
    }

    /**
     * {@code /is admin create <nick>}: cria a ilha de um jogador (online ou não).
     * {@code /is admin expand <nick>}: sobe a ilha do jogador um nível (sem custo; a economia vem depois).
     */
    private void admin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("greensky.admin")) {
            send(sender, "Sem permissão.", NamedTextColor.RED);
            return;
        }
        String action = args.length >= 3 ? args[1].toLowerCase(Locale.ROOT) : "";
        if (!action.equals("create") && !action.equals("expand")) {
            send(sender, "Uso: /is admin <create|expand> <nick>", NamedTextColor.YELLOW);
            return;
        }
        withKnownPlayer(sender, args[2], target -> {
            if (action.equals("create")) {
                create(sender, target.uuid(), target.name(), null);
                return;
            }
            service.findByOwner(target.uuid())
                    .thenCompose(found -> found.isPresent()
                            ? service.expand(found.get())
                            : java.util.concurrent.CompletableFuture.<Island>failedFuture(
                                    new IslandException(IslandException.Reason.NO_ISLAND)))
                    .whenComplete((island, error) -> {
                        if (error != null) {
                            fail(sender, error);
                        } else {
                            send(sender, "Ilha de " + target.name() + " expandida para " + island.region().size() + "x"
                                    + island.region().size() + " (nível " + service.levelOf(island) + ").",
                                    NamedTextColor.GREEN);
                        }
                    });
        });
    }

    /**
     * Resolve um nick para a conta registrada no GreenSky, sem diferenciar maiúsculas (o nick é
     * único no banco). Não usa o cache do servidor, que pode apontar para outra grafia.
     */
    private void withKnownPlayer(CommandSender reply, String name, java.util.function.Consumer<PlayerRecord> action) {
        players.findByName(name).whenComplete((found, error) -> {
            if (error != null) {
                fail(reply, error);
            } else if (found.isEmpty()) {
                send(reply, "Jogador '" + name + "' nunca entrou no servidor.", NamedTextColor.RED);
            } else {
                action.accept(found.get());
            }
        });
    }

    private void teleportHome(Player player, Island island) {
        // teleportAsync carrega o chunk de destino sem travar o servidor.
        scheduler.runSync(() -> player.teleportAsync(island.home(skyWorld.bukkit(), settings.baseY())));
    }

    private void withPlayer(CommandSender sender, java.util.function.Consumer<Player> action) {
        if (sender instanceof Player player) {
            if (!ready.test(player)) {
                send(player, "Faça login primeiro.", NamedTextColor.RED);
                return;
            }
            action.accept(player);
        } else {
            send(sender, "Este comando só pode ser usado por jogadores.", NamedTextColor.RED);
        }
    }

    private void fail(CommandSender sender, Throwable error) {
        Throwable cause = unwrap(error);
        if (cause instanceof IslandException e) {
            send(sender, message(e.reason()), NamedTextColor.RED);
        } else {
            logger.log(Level.SEVERE, "Falha inesperada em /island", cause);
            send(sender, "Erro interno. Avise a administração.", NamedTextColor.RED);
        }
    }

    private static String message(IslandException.Reason reason) {
        return switch (reason) {
            case ALREADY_HAS_ISLAND -> "Você já tem uma ilha. Use /is home.";
            case NO_ISLAND -> "Você ainda não tem uma ilha. Use /is create.";
            case NOT_OWNER -> "Apenas o dono da ilha pode fazer isso.";
            case ALREADY_MEMBER -> "Esse jogador já é membro da ilha.";
            case NOT_A_MEMBER -> "Esse jogador não é membro da ilha.";
            case CANNOT_REMOVE_OWNER -> "O dono não pode ser removido da própria ilha.";
            case MAX_SIZE_REACHED -> "A ilha já está no tamanho máximo.";
            case EXPANSION_CONFLICT -> "A ilha acabou de ser expandida por outra ação. Tente de novo.";
            case TARGET_HAS_NO_ISLAND -> "Esse jogador não tem ilha.";
            case ISLAND_PRIVATE -> "Essa ilha é privada.";
        };
    }

    private static Throwable unwrap(Throwable t) {
        while ((t instanceof CompletionException || t instanceof ExecutionException) && t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }

    /** Envia na thread do servidor, pois os callbacks chegam de threads do banco. */
    private void send(CommandSender to, String text, NamedTextColor color) {
        scheduler.runSync(() -> to.sendMessage(Component.text(text, color)));
    }
}
