package com.greencodes.greensky.gui;

import com.greencodes.greensky.api.event.GreenSkyPlayerReadyEvent;
import com.greencodes.greensky.collection.CollectionService;
import com.greencodes.greensky.content.*;
import com.greencodes.greensky.core.GreenScheduler;
import com.greencodes.greensky.farming.*;
import com.greencodes.greensky.island.*;
import com.greencodes.greensky.player.PlayerService;
import com.greencodes.greensky.protection.IslandProtectionService;
import com.greencodes.greensky.world.SkyWorld;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.block.data.Ageable;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.plugin.Plugin;

/** Inventários por jogador; dados assíncronos só atualizam a tela que iniciou a consulta. */
public final class GameMenus implements Listener, CommandExecutor {
    private static final int[] SLOTS = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43};
    private final Plugin plugin;
    private final GreenScheduler scheduler;
    private final Predicate<Player> ready;
    private final IslandService islands;
    private final PlayerService players;
    private final CollectionService collections;
    private final CropCatalog crops;
    private final AncientPlants ancient;
    private final CropMarkers fertilized;
    private final SkyWorld world;
    private final IslandProtectionService protection;
    private final ContentRegistry content;

    private static final class Screen implements InventoryHolder {
        final UUID owner;
        final Map<Integer, Runnable> actions = new HashMap<>();
        Inventory inventory;
        boolean busy;
        boolean waiting;
        Screen(UUID owner) { this.owner = owner; }
        @Override public Inventory getInventory() { return inventory; }
    }
    private record Button(Material material, String name, List<String> lore, Runnable action) {}
    private record NamedIsland(Island island, String name, IslandVisibility visibility) {}
    private record NamedMember(IslandMember member, String name) {}

    public GameMenus(Plugin plugin, GreenScheduler scheduler, Predicate<Player> ready, IslandService islands,
            PlayerService players, CollectionService collections, CropCatalog crops, AncientPlants ancient,
            CropMarkers fertilized, SkyWorld world, IslandProtectionService protection, ContentRegistry content) {
        this.plugin=plugin; this.scheduler=scheduler; this.ready=ready; this.islands=islands;
        this.players=players; this.collections=collections; this.crops=crops; this.ancient=ancient;
        this.fertilized=fertilized; this.world=world; this.protection=protection; this.content=content;
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) main(player);
        else sender.sendMessage(Component.text("Abra o menu dentro do jogo."));
        return true;
    }

    private boolean allowed(Player player) {
        if (ready.test(player)) return true;
        player.sendMessage(Component.text("Faça login primeiro.", NamedTextColor.RED));
        return false;
    }

    private Screen screen(Player player, String title) {
        Screen view = new Screen(player.getUniqueId());
        view.inventory = plugin.getServer().createInventory(view, 54, Component.text("GreenSky • " + title, NamedTextColor.DARK_GREEN));
        for (int slot=0; slot<54; slot++) {
            if (slot<9 || slot>=45 || slot%9==0 || slot%9==8)
                view.inventory.setItem(slot, icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of()));
        }
        button(view, 4, Material.OAK_SAPLING, title, List.of("Sua aventura entre as nuvens."), () -> {});
        button(view, 45, Material.ARROW, "Início", List.of("Voltar ao menu principal."), () -> main(player));
        button(view, 53, Material.BARRIER, "Fechar", List.of("Voltar para o mundo."), player::closeInventory);
        player.openInventory(view.inventory);
        return view;
    }

    private static ItemStack icon(Material material, String title, List<String> lore) {
        ItemStack icon = ItemStack.of(material);
        icon.editMeta(meta -> {
            meta.itemName(Component.text(title, NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore.stream().map(line -> Component.text(line, NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)).toList());
        });
        return icon;
    }
    private void button(Screen screen, int slot, Material material, String title, List<String> lore, Runnable action) {
        screen.inventory.setItem(slot, icon(material, title, lore));
        screen.actions.put(slot, action);
    }
    private boolean active(Player player, Screen screen) {
        return player.isOnline() && ready.test(player)
                && player.getOpenInventory().getTopInventory() == screen.inventory;
    }
    private <T> void await(Player player, Screen screen, CompletableFuture<T> future, Consumer<T> render) {
        screen.waiting=true; screen.busy=true;
        future.whenComplete((value,error) -> scheduler.runSync(() -> {
            if (!active(player,screen)) return;
            screen.waiting=false; screen.busy=false;
            if (error != null) {
                button(screen,22,Material.BARRIER,"Não foi possível concluir",List.of("A ação foi recusada ou os dados estão indisponíveis.",
                        "Volte e tente novamente."),()->main(player));
                plugin.getLogger().fine("Menu: " + error.getMessage());
            } else render.accept(value);
        }));
    }
    private void execute(Player player, String command) {
        player.closeInventory();
        player.performCommand(command); // executa com as permissões do próprio jogador
    }
    private void page(Player player, String title, List<Button> entries, int number, Runnable back) {
        Screen view=screen(player,title);
        int page=Math.max(0,Math.min(number,Math.max(0,(entries.size()-1)/SLOTS.length)));
        for (int i=0; i<SLOTS.length && page*SLOTS.length+i<entries.size(); i++) {
            var entry=entries.get(page*SLOTS.length+i);
            button(view,SLOTS[i],entry.material(),entry.name(),entry.lore(),entry.action());
        }
        if (entries.isEmpty()) button(view,22,Material.PAPER,"Nada por aqui ainda",List.of("Esta lista aparecerá conforme você jogar."),()->{});
        button(view,45,Material.ARROW,"Voltar",List.of(),back);
        if (page>0) button(view,48,Material.ARROW,"Página anterior",List.of(),()->page(player,title,entries,page-1,back));
        if ((page+1)*SLOTS.length<entries.size()) button(view,50,Material.ARROW,"Próxima página",List.of(),()->page(player,title,entries,page+1,back));
    }
    public void main(Player player) {
        if (!allowed(player)) return;
        Screen view=screen(player,"Início");
        button(view,10,Material.GRASS_BLOCK,"Minha ilha",List.of("Criar sua casa ou ver seus dados.","Gerenciar acesso e voltar para casa."),()->island(player));
        button(view,12,Material.ENDER_PEARL,"Voltar para casa",List.of("Teleportar para o centro da sua ilha."),()->execute(player,"is home"));
        button(view,14,Material.COMPASS,"Visitar ilhas",List.of("Conhecer ilhas públicas, inclusive de jogadores offline."),()->visits(player));
        button(view,16,Material.PLAYER_HEAD,"Equipe",List.of("Convidar, remover e ajustar permissões."),()->members(player));
        button(view,28,Material.BOOK,"Coleções",List.of("Veja descobertas e progresso real."),()->collectionList(player));
        button(view,30,Material.WHEAT,"Agricultura",List.of("Plantio, qualidade, achados raros e fertilizantes."),()->farming(player));
        button(view,32,Material.FISHING_ROD,"Pesca",List.of("Como pescar e onde encontrar seus peixes."),()->fishing(player));
        button(view,34,Material.WRITABLE_BOOK,"Como jogar",List.of("Entenda o que já funciona no servidor.","Comece sua aventura por aqui."),()->guide(player));
        if (player.hasPermission("greensky.admin"))
            button(view,49,Material.COMMAND_BLOCK,"Administração",List.of("Itens para testar e expansão de ilhas."),()->admin(player));
    }
    public void island(Player player) {
        if (!allowed(player)) return;
        Screen view=screen(player,"Minha ilha");
        button(view,22,Material.CLOCK,"Carregando sua ilha",List.of(),()->{});
        await(player,view,islands.findByOwner(player.getUniqueId()),found -> {
            if (found.isEmpty()) { creation(player); return; }
            Island island=found.get();
            button(view,13,Material.GRASS_BLOCK,"Sua casa",List.of("Área liberada: "+island.region().size()+" × "+island.region().size(),
                    "Nível: "+islands.levelOf(island),"Centro: "+island.region().centerX()+", "+island.region().centerZ()),()->{});
            button(view,22,Material.ENDER_PEARL,"Ir para a ilha",List.of(),()->execute(player,"is home"));
            button(view,29,Material.OAK_DOOR,"Ilha pública",List.of("Permitir visitas de outros jogadores.","Visitantes não podem construir."),()->execute(player,"is public"));
            button(view,31,Material.IRON_DOOR,"Ilha privada",List.of("Somente você e membros podem entrar."),()->execute(player,"is private"));
            button(view,33,Material.PLAYER_HEAD,"Gerenciar equipe",List.of(),()->members(player));
            button(view,40,Material.MAP,"Expansão",List.of("Libera espaço para construir; não refaz o terreno.",
                    "Nesta fase, a expansão é administrativa.","A economia ainda não está implementada."),()->{
                if (player.hasPermission("greensky.admin")) execute(player,"is admin expand "+player.getName());
            });
            await(player,view,islands.visibility(island),visibility ->
                    button(view,15,visibility==IslandVisibility.PUBLIC?Material.OAK_DOOR:Material.IRON_DOOR,"Acesso atual",List.of(
                            visibility==IslandVisibility.PUBLIC?"Pública: permite visitas.":"Privada: apenas membros."),()->{}));
        });
    }
    private void creation(Player player) {
        Screen view=screen(player,"Criar ilha");
        button(view,10,Material.OAK_SAPLING,"Bosque verde",List.of("Carvalhos, musgo, pedras e flores."),()->{});
        button(view,12,Material.CHERRY_SAPLING,"Jardim sakura",List.of("Cerejeiras e flores em tons suaves."),()->{});
        button(view,14,Material.SPRUCE_SAPLING,"Refúgio boreal",List.of("Pinheiros e rochas cobertas de musgo."),()->{});
        button(view,16,Material.BIRCH_SAPLING,"Clareira dourada",List.of("Bétulas, margaridas e flores douradas."),()->{});
        button(view,31,Material.EMERALD,"Criar minha ilha",List.of("Cada ilha recebe um estilo e detalhes próprios.",
                "O estilo é definido automaticamente.","Você chega no centro seguro da sua nova casa."),()->execute(player,"is create"));
        button(view,40,Material.BOOK,"Primeiros passos",List.of("1. Colete madeira e replante a árvore.","2. Construa e comece sua plantação.",
                "3. Colha culturas maduras para avançar nas coleções.","4. Prepare uma área de água aberta para pescar."),()->{});
    }
    private <T> CompletableFuture<List<T>> sequence(List<CompletableFuture<T>> futures) {
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenApply(ignored -> futures.stream().map(future -> future.getNow(null)).toList());
    }
    private void visits(Player player) {
        Screen view=screen(player,"Visitar ilhas");
        var future=islands.loadAll().thenCompose(all -> sequence(all.stream()
                .filter(island -> !island.owner().equals(player.getUniqueId()))
                .map(island -> players.findByUuid(island.owner()).thenCombine(islands.visibility(island),
                        (owner,visibility)->new NamedIsland(island,owner.map(record->record.name()).orElse("Ilha #"+island.slot()),visibility)))
                .toList())).thenCombine(islands.membershipsOf(player.getUniqueId()),(entries,memberships)-> {
                    Set<UUID> accessible=new HashSet<>();
                    memberships.forEach(member->accessible.add(member.islandId()));
                    return entries.stream().filter(entry->entry.visibility()==IslandVisibility.PUBLIC
                            || accessible.contains(entry.island().id())).toList();
                });
        await(player,view,future,entries -> page(player,"Ilhas públicas",entries.stream()
                .map(entry->new Button(Material.GRASS_BLOCK,entry.name(),List.of("Área: "+entry.island().region().size()+" × "+entry.island().region().size(),
                        "Clique para visitar."),()->visit(player,entry.island())))
                .toList(),0,()->main(player)));
    }
    private void visit(Player player, Island target) {
        Screen view=screen(player,"Preparando visita");
        await(player,view,islands.authorizeVisit(player.getUniqueId(),target.owner()),island -> {
            player.closeInventory();
            // O comando existente cuida do teleporte para jogadores conhecidos, mesmo offline.
            var waiting=screen(player,"Destino autorizado");
            await(player,waiting,players.findByUuid(island.owner()),owner -> {
                if (owner.isPresent()) execute(player,"is visit "+owner.get().name());
            });
        });
    }
    private void members(Player player) {
        Screen view=screen(player,"Equipe");
        var future=islands.findByOwner(player.getUniqueId()).thenCompose(found -> found.isEmpty()
                ? CompletableFuture.<List<NamedMember>>failedFuture(new IllegalStateException("Crie sua ilha primeiro"))
                : islands.members(found.get().id()).thenCompose(all -> sequence(all.stream().map(member ->
                        players.findByUuid(member.playerId()).thenApply(record -> new NamedMember(member,
                                record.map(value->value.name()).orElse(member.playerId().toString())))).toList())));
        await(player,view,future,entries -> {
            List<Button> buttons=new ArrayList<>();
            buttons.add(new Button(Material.EMERALD,"Convidar jogador",List.of("Escolha um jogador online."),()->invite(player)));
            for (var entry:entries) buttons.add(new Button(Material.PLAYER_HEAD,entry.name(),List.of("Cargo: "+entry.member().role(),
                    "Clique para ver permissões e remover."),()->member(player,entry)));
            page(player,"Equipe",buttons,0,()->island(player));
        });
    }
    private void invite(Player player) {
        page(player,"Convidar jogador",plugin.getServer().getOnlinePlayers().stream()
                .filter(target->ready.test(target)&&!target.getUniqueId().equals(player.getUniqueId()))
                .map(target->new Button(Material.PLAYER_HEAD,target.getName(),List.of("Adicionar à equipe da sua ilha."),
                        ()->execute(player,"is add "+target.getName()))).toList(),0,()->members(player));
    }
    private void member(Player player, NamedMember target) {
        Screen view=screen(player,target.name());
        button(view,45,Material.ARROW,"Voltar à equipe",List.of(),()->members(player));
        if (target.member().role()==IslandRole.OWNER) {
            button(view,22,Material.GOLDEN_HELMET,"Dono da ilha",List.of("O dono sempre tem todas as permissões."),()->{}); return;
        }
        int slot=10;
        for (IslandPermission permission:IslandPermission.values()) {
            boolean enabled=target.member().can(permission);
            button(view,slot++,enabled?Material.LIME_DYE:Material.GRAY_DYE,permissionName(permission),
                    List.of(enabled?"Ativa; clique para desativar.":"Desativada; clique para ativar."),()->{
                        Set<IslandPermission> permissions=EnumSet.noneOf(IslandPermission.class);
                        permissions.addAll(target.member().permissions());
                        if (!permissions.remove(permission)) permissions.add(permission);
                        var action=islands.findByOwner(player.getUniqueId()).thenCompose(found -> found.isPresent()
                                ? islands.setPermissions(found.get(),player.getUniqueId(),target.member().playerId(),permissions)
                                : CompletableFuture.<Void>failedFuture(new IllegalStateException("Sem ilha")));
                        await(player,view,action,ignored->members(player));
                    });
        }
        button(view,31,Material.TNT,"Remover da equipe",List.of("Abre uma confirmação antes de remover."),()->confirm(player,
                "Remover "+target.name(),()->execute(player,"is remove "+target.name()),()->members(player)));
    }
    private static String permissionName(IslandPermission permission) {
        return switch(permission) {
            case BUILD -> "Construir"; case BREAK -> "Quebrar"; case INTERACT -> "Interagir";
            case CONTAINERS -> "Abrir baús"; case INVITE -> "Convidar membros";
            case KICK -> "Remover membros"; case MANAGE_PERMISSIONS -> "Gerenciar permissões";
        };
    }
    private void confirm(Player player,String title,Runnable action,Runnable cancel) {
        Screen view=screen(player,title);
        button(view,20,Material.LIME_CONCRETE,"Confirmar",List.of("Aplicar esta ação."),action);
        button(view,24,Material.RED_CONCRETE,"Cancelar",List.of("Voltar sem aplicar."),cancel);
    }
    public void collectionList(Player player) {
        if (!allowed(player)) return;
        if (!collections.isLoaded(player.getUniqueId())) { screen(player,"Coleções carregando"); return; }
        var progress=collections.snapshot(player.getUniqueId());
        page(player,"Coleções",collections.catalog().collections().stream().map(collection -> new Button(
                collection.id().equals("farming")?Material.WHEAT:collection.id().equals("fishing")?Material.COD:Material.BOOK,
                collection.name(),List.of("Descobertas: "+collection.discoveredCount(progress.keySet())+"/"+collection.entries().size(),
                        "Clique para ver cada entrada."),()->collection(player,collection.id()))).toList(),0,()->main(player));
    }
    private void collection(Player player,String id) {
        if (!collections.isLoaded(player.getUniqueId())) { collectionList(player); return; }
        var definition=collections.catalog().collection(id).orElseThrow();
        var progress=collections.snapshot(player.getUniqueId());
        page(player,definition.name(),definition.entries().stream().map(entry -> {
            long amount=progress.getOrDefault(entry.key(),0L);
            boolean hidden=entry.secret()&&amount==0;
            return new Button(amount>0?Material.LIME_DYE:Material.GRAY_DYE,hidden?"???":entry.name(),
                    hidden?List.of("Descoberta secreta."):List.of("Raridade: "+entry.rarity().label(),
                            amount>0?"Descoberta! Total: "+amount:"Ainda não descoberta."),()->{});
        }).toList(),0,()->collectionList(player));
    }
    public void farming(Player player) {
        if (!allowed(player)) return;
        Screen view=screen(player,"Agricultura");
        button(view,10,Material.WHEAT,"Colheita",List.of("Trigo, cenoura e batata maduros: +1 planta.",
                "Fortuna não multiplica o progresso.","Quebra automática não conta nas coleções."),()->collection(player,"farming"));
        button(view,12,Material.NETHER_STAR,"Qualidade",List.of("Produtos colhidos recebem de ★ a ★★★.","As estrelas ficam no item após reiniciar."),()->{});
        button(view,14,Material.WHEAT_SEEDS,"Semente Ancestral",List.of("Plante em solo arado, na sua ilha.",
                "Imatura: devolve uma semente.","Madura: entrega 1 Trigo Dourado ★★★."),()->{});
        button(view,16,Material.GLOWSTONE_DUST,"Trigo Dourado",List.of("Fertilizante: clique direito em cultura imatura.",
                "Consome 1; avança 2 estágios; garante ★★★.","Uma aplicação por planta comum.","Quebrar imatura perde o tratamento."),()->{});
        if (world.contains(player.getWorld())) {
            var block=player.getTargetBlockExact(5);
            if (block!=null && crops.crop(block.getType()).isPresent() && block.getBlockData() instanceof Ageable age
                    && protection.can(player.getUniqueId(),IslandPermission.INTERACT,block.getX(),block.getZ())) {
                button(view,31,Material.SPYGLASS,"Planta observada",List.of(crops.crop(block.getType()).orElseThrow().product().name(),
                        "Estágio: "+age.getAge()+"/"+age.getMaximumAge(),ancient.contains(block)?"Ancestral: rende Trigo Dourado ★★★.":
                        fertilized.contains(block)?"Fertilizada: qualidade ★★★ garantida.":"Comum: qualidade sorteada."),()->farming(player));
            } else button(view,31,Material.SPYGLASS,"Inspecionar planta",List.of("Olhe para uma cultura a até 5 blocos.","Abra este menu novamente para ver o estado."),()->farming(player));
        }
        button(view,40,Material.BOOK,"Coleção Agricultura",List.of("Seu progresso salvo."),()->collection(player,"farming"));
    }
    public void fishing(Player player) {
        if (!allowed(player)) return;
        Screen view=screen(player,"Pesca");
        button(view,13,Material.FISHING_ROD,"Prepare seu lugar de pesca",List.of("Pesque na ilha de que você é dono ou membro.",
                "Use água aberta: sem pequenos buracos ou máquinas.","Peixes podem depender de hora, clima e lua.","Pescar parado repetidamente para de dar progresso."),()->{});
        button(view,31,Material.COD,"Coleção Pesca",List.of("Veja seus peixes e descobertas."),()->collection(player,"fishing"));
    }
    public void guide(Player player) {
        if (!allowed(player)) return;
        Screen view=screen(player,"Como jogar");
        button(view,10,Material.GRASS_BLOCK,"1 • Crie sua casa",List.of("Abra Minha ilha e clique em Criar.","A área inicial liberada é maior que o terreno.","Use essa área para expandir suas construções."),()->island(player));
        button(view,12,Material.WHEAT,"2 • Comece sua plantação",List.of("Use terra arada, sementes e água.","Colha plantas maduras manualmente.","Achados raros desbloqueiam usos especiais."),()->farming(player));
        button(view,14,Material.FISHING_ROD,"3 • Prepare água para pesca",List.of("Construa um local com água aberta.","Cada peixe conta na coleção ao ser pescado."),()->fishing(player));
        button(view,16,Material.BOOK,"4 • Descubra e compartilhe",List.of("Acompanhe as coleções.","Convide amigos ou visite ilhas públicas."),()->collectionList(player));
        button(view,31,Material.CLOCK,"O servidor está em desenvolvimento",List.of("Já funciona: ilhas, proteção, equipe e visitas;",
                "coleções, pesca e parte da agricultura.","Quests, exploração, combate e economia",
                "ainda não estão disponíveis."),()->{});
    }
    private void admin(Player player) {
        if (!player.hasPermission("greensky.admin")) return;
        Screen view=screen(player,"Administração");
        button(view,20,Material.CHEST,"Entregar itens",List.of("Escolha um item e o jogador de destino."),()->adminItems(player));
        button(view,24,Material.MAP,"Expandir ilha",List.of("Escolha o dono; expansão gratuita de teste.","Aumenta um nível por ação."),()->page(player,"Expandir ilha",
                plugin.getServer().getOnlinePlayers().stream().filter(ready).map(target->new Button(Material.PLAYER_HEAD,target.getName(),
                        List.of("Expandir a ilha deste jogador."),()->confirm(player,"Expandir "+target.getName(),
                                ()->execute(player,"is admin expand "+target.getName()),()->admin(player)))).toList(),0,()->admin(player)));
    }
    private void adminItems(Player player) {
        if (!player.hasPermission("greensky.admin")) return;
        page(player,"Itens de teste",content.items().stream().map(item->new Button(item.material(),item.name(),
                List.of(item.id(),"Escolher quem recebe."),()->page(player,"Destino do item",
                        plugin.getServer().getOnlinePlayers().stream().filter(ready).map(target -> new Button(Material.PLAYER_HEAD,target.getName(),
                                List.of("Entregar 1 unidade de "+item.name()+"."),()->execute(player,
                                        "greensky item give "+target.getName()+" "+item.id()+" 1"))).toList(),0,()->adminItems(player)))).toList(),0,()->admin(player));
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Screen view)) return;
        event.setCancelled(true); // inclusive shift, números, duplo clique e inventário inferior
        if (!(event.getWhoClicked() instanceof Player player) || !view.owner.equals(player.getUniqueId())
                || !ready.test(player) || view.busy || event.getRawSlot()<0 || event.getRawSlot()>=54
                || (event.getClick()!=ClickType.LEFT && event.getClick()!=ClickType.RIGHT)) return;
        Runnable action=view.actions.get(event.getRawSlot());
        if (action==null) return;
        view.busy=true;
        scheduler.runSyncLater(() -> {
            if (!active(player,view)) return;
            try { action.run(); }
            finally { if (!view.waiting) view.busy=false; }
        },1);
    }
    @EventHandler(priority=EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Screen) event.setCancelled(true);
    }
    @EventHandler
    public void onReady(GreenSkyPlayerReadyEvent event) {
        Player player=event.getPlayer();
        scheduler.runSyncLater(() -> {
            if (player.isOnline() && ready.test(player) && player.getOpenInventory().getTopInventory().getType()==InventoryType.CRAFTING)
                main(player);
        },10);
    }
}
