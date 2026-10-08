# INSTRUCOES — Histórico do que foi feito

Registro de cada alteração, por fase, com o motivo. Pasta do projeto: `C:\Users\geren\Desktop\green-sky`.

## Resumo

GreenSky é um plugin para **Paper 26.1.2 (build 74, STABLE)** com **Java 25**, **Gradle 9.8.0**
(wrapper) e **PostgreSQL 18**. Concluídas as fases 0 a 7 (seções 154 a 161 do README); as fases 1 a 3 foram
feitas na 26.2 e migradas para a 26.1.2 depois (ver "Troca de versão"). 74 testes automatizados (unidade +
PostgreSQL real) e dois roteiros com bots reais (30 e 19 verificações), tudo passando.

| Fase | Commit | O que entrega |
|---|---|---|
| 0 | — | Análise (nenhum arquivo criado) |
| 1 | `96336a7` | Plugin que sobe, config validada, `GreenScheduler` |
| 2 | `5b72a61` | PostgreSQL, Hikari, Flyway, SQL assíncrono |
| 3 | `16d7d7f` | Mundo void, `VoidGenerator`, `WorldManager` |
| — | `57155c7` | Troca para Paper 26.1.2 + CLAUDE.md e INSTRUCOES.md |
| 4 | `7141bba` | Ilhas, membros, permissões, `/island` |
| 5 | `75a2d9a` | Proteção + revisão das fases 1-4 |
| 6 | `3eeb11d` | Expansão por níveis + WorldBorder visual |
| 7 | `7c2a385` | Visitas, renascer na ilha, ciclo completo com restart |
| — | (sem commit) | Reorganização de pastas (abaixo) |

---

## Publicação privada no GitHub e estado de trabalho — 07/10/2026

Pedido: inspecionar primeiro, criar `ANTARES-PROJECTS/NOME_DO_PROJETO` privado, configurar origin,
commitar as alterações existentes e publicar main. Git já inicializado, main e histórico até a fase 7;
não reinicializar nem descartar histórico. Acesso administrativo à organização verificado. O nome foi
usado literalmente conforme o pedido. Verificação anterior à criação não encontrou o repositório.

**Alterações desta publicação:** registro deste procedimento; normalização de linhas vazias finais
nos documentos, fontes Java e roteiros E2E; remoção de espaços finais nos comentários dos dois YAMLs do Paper, sem alterar valores ou lógica; commit das alterações já existentes, incluindo a reorganização documentada para
`greensky/`, módulos API/core, fases 8.1–8.3, menus e modelos de ilha em desenvolvimento. Nenhuma
funcionalidade foi removida ou modificada para publicar. Repositório criado privado em https://github.com/ANTARES-PROJECTS/NOME_DO_PROJETO;
origin configurado por gh e verificado. Identidade Git configurada somente neste repositório com o nome da conta autenticada e seu e-mail privado noreply do GitHub, pois não havia identidade local configurada. Commit/push serão conferidos ao concluir.
Segredos, mundos, dados de jogadores, plugins e builds não entram no commit. Foram conferidos os
163 arquivos candidatos: nenhuma senha real do banco, token GitHub ou chave privada detectada.

## Menus, novas ilhas e OP — implementação em andamento — 07/10/2026

Pedidos anteriores: ações do GreenSky por menus, ilhas bonitas e variadas e OP total para blacknaut.
Esta seção registra todas as alterações já realizadas antes da solicitação de publicação.

- `greensky/greensky-core/src/main/java/com/greencodes/greensky/gui/GameMenus.java` (novo): inventários
  individuais de 54 slots, identidade visual, botões, paginação e navegação. Telas de início, criação/ilha,
  acesso público/privado, visitas, equipe/convites/permissões/remoção com confirmação, coleções,
  agricultura/inspeção, pesca, primeiros passos e administração (entrega de itens/expansão).
  Executa comandos como o jogador, sem elevar permissões. Revalida autenticação e pertencimento da
  tela; cancela clicks/drag no menu inteiro, incluindo inventário inferior. Só clicks esquerdo/direito
  executam botões; bloqueia dupla execução enquanto carrega dados. Ações de inventário no tick seguinte.
  Futures retornam à thread do servidor e só atualizam a mesma tela/sessão ainda aberta. Menu inicial
  abre após login, respeitando outra tela já aberta. Nenhum SQL/arquivo bloqueante por clique.
- `.../player/PlayerRepository.java` e `PlayerService.java`: consulta assíncrona por UUID para mostrar
  nomes registrados, inclusive membros e donos offline, sem confiar no cache de nomes do Minecraft.
- `.../GreenSkyPlugin.java`: registra menus e listeners; `/menu`, `/guia`, `/pesca`, `/gs` sem argumentos,
  `/is`, `/collections` e `/agricultura` abrem interfaces. Subcomandos anteriores continuam disponíveis;
  `/collections texto` e `/agricultura texto` preservam diagnóstico em chat e testes existentes.
- `.../island/StarterIslandPlan.java` (novo): blueprint puro e determinístico pela posição, com quatro
  estilos (bosque/carvalho, sakura/cerejeira, boreal/pinheiro, clareira/bétula), terreno orgânico e
  afunilado, pedra/musgo/flores, árvore própria, caminho, luz, rochas e folhagem inferior. Centro sempre
  tem piso de grama e dois blocos de ar. Com raio >=8, lago 5x5 com duas camadas de água e margem sólida.
  Estilo é automático, não uma seleção persistida no banco. Não remodela ilhas existentes.
- `.../island/StarterIslandBuilder.java`: carrega chunks async, aplica blueprint na thread do servidor
  e limita colocação à região da ilha. Folhas persistentes. Baú inicial em raio >=3 com sementes8,
  cenouras2, batatas2, enxada/vara1, pães12, farinha de osso12, baldes água/lava1, tábuas16, pedras24,
  e mudas do estilo2. Kit igual entre estilos, sem achados raros ou progresso de coleção automático.
  Neste estágio o kit fica no baú; não depende de quebrar a árvore nem de quest.
- `.../visit/HomeListener.java`: boas-vindas orientam a abrir o menu e clicar em Minha ilha.
- `greensky/greensky-core/src/main/resources/plugin.yml`: registra menu/jogar, guia e pesca.
- `.../resources/config.yml` e `server/plugins/GreenSky/config.yml` local: starter-radius passa de 4 a 8
  para novas ilhas; demais configurações preservadas. Ilhas existentes não são refeitas.
- `greensky/greensky-core/src/test/java/com/greencodes/greensky/island/StarterIslandPlanTest.java` (novo):
  dois testes verificam determinismo, quatro estilos/40 ilhas distintas, chegada segura em raios1–16,
  limite de chunks/altura, lago e preservação de baú/lanterna.
- `greensky/tools/e2e/menus-e2e.mjs` (novo): roteiro com bots para menus, anti-retirada por shift,
  criação/teleporte/chunks, baú/kit/lago, privacidade, coleções, agricultura, convite, visita de membro,
  permissões, confirmação/cancelamento/remoção offline, administração e guia. Aguarda chunks recebidos
  e botões de telas assíncronas antes de clicar.
- `greensky/tools/e2e/lib.mjs`: conexão de bots fecha menu de boas-vindas após carregar a sessão, para
  não interferir em roteiros legados; teste específico de autoabertura usa connectRaw/authenticate.
- `.../collections-e2e.mjs` e `farming-guide-e2e.mjs`: diagnóstico usa os subcomandos texto.
- `.../package.json`: adiciona menus-e2e na suíte e `test:menus`.
- `CLAUDE.md` e `INSTRUCOES.md`: registram o estado real, arquitetura e alterações.
- Operação local autorizada: UUID de blacknaut conferido no banco/nLogin; concedido OP em runtime
  pela conta administrativa de manutenção. `server/ops.json` confirmou OP nível4 persistido para
  `0070f5d5-98d9-33c6-b621-99bbf3f27898`. Arquivo não é versionado. Servidor foi parado normalmente
  (com salvamento) para deploy e testes; a intenção permanece deixá-lo online após o trabalho.

**Validação atual:** build/deploy final passou com 137 testes Java. Primeiro roteiro de menu parou
por chunks ainda não recebidos no cliente: corrigida espera. Segunda execução passou 14 verificações,
mas falhou ao procurar a ilha privada do membro na primeira página de uma lista com muitas ilhas de
outros testes; navegação/ordenação da lista e roteiro precisam de revisão. Menus não estão declarados
concluídos. Pedidos sobre /gm, quests iniciais, scoreboard, reset irreversível com confirmação,
dificuldade/recompensas e isolamento Easy/Hard ainda são propostas; não foram implementados.
Vault, ViaVersion, ViaBackwards e LuckPerms/fontes foram solicitados depois e ainda estão pendentes;
foram iniciadas apenas a pesquisa de fontes oficiais e a inspeção das dependências atuais.

---
## Fase 8.3 — Guia agrícola e servidor para teste manual — 07/10/2026

Pedido: continuar e deixar o servidor online para testar. Esta entrega facilita testar as mecânicas
agrícolas existentes; não implementa ainda mutações, estufas, contratos ou eventos.

**Todas as alterações:**

- `greensky/greensky-core/src/main/java/com/greencodes/greensky/farming/FarmingCommand.java` (novo):
  `/agricultura` (alias `/farming`) explica colheita, coleção, qualidade, sementes e fertilizante.
  Após autenticação e carregamento da coleção, inspeciona a cultura observada a até cinco blocos:
  nome, estágio/max e estado comum/fertilizado/ancestral. Inspeção exige SkyWorld e INTERACT.
  Não concede itens nem progresso; console recebe orientação para usar dentro do jogo.
- `.../content/ContentRegistry.java`: `withItems` acrescenta definições de atividades ao registro imutável;
  IDs duplicados são rejeitados com mensagem identificando o ID, sem sobrescrever conteúdo existente.
- `.../farming/CropCatalog.java`: expõe os achados raros para o registro administrativo e adiciona lore
  explicando como plantar Semente Ancestral e aplicar Trigo Dourado. Nomes/raridades continuam vindos
  das coleções; probabilidades e regras de colheita não foram alteradas.
- `.../content/ItemCommand.java`: mensagem de item inexistente passa a mencionar conteúdo carregado,
  pois o registro agora inclui os bônus agrícolas além de items.yml. Give e autocomplete existentes
  passam a reconhecer `crop.golden_wheat` e `crop.ancient_seed`; continua exigindo greensky.admin.
- `.../GreenSkyPlugin.java`: une itens agrícolas ao registro dentro da validação do boot e registra
  o comando agrícola com os serviços existentes de sessão, coleção, mundo, proteção e marcadores.
- `greensky/greensky-core/src/main/resources/plugin.yml`: registra `/agricultura` e alias `/farming`.
- `greensky/greensky-core/src/test/java/com/greencodes/greensky/content/ContentRegistryTest.java`:
  testa imutabilidade, adição de conteúdo e rejeição de colisões com itens existentes ou na própria lista.
- `greensky/tools/e2e/farming-guide-e2e.mjs` (novo): verifica guia e inspeção sem OP, negação de give
  ao jogador comum, entrega pelo console com identidade correta, fertilização/plantio com esses itens,
  leitura dos estados reais e ausência de progresso artificial.
- `greensky/tools/e2e/package.json`: inclui o roteiro na suíte e adiciona `test:farming-guide`.
- `CLAUDE.md`: atualiza o estado/arquitetura da agricultura.
- `INSTRUCOES.md`: registra cada alteração e o roteiro de teste manual.

**Validação:** build/deploy offline concluído; 135 testes Java, zero falhas/ignorados.
`farming-guide-e2e`: 11/11 verificações no Paper com nLogin. Depois do roteiro, Paper foi iniciado
em segundo plano (janela oculta), com PostgreSQL saudável, sem encerrar ao terminar esta entrega.
Boot confirmou GreenSky habilitado e Done; listener 127.0.0.1:25565/PID 32764 e conexão TCP local
foram verificados. Logs desta execução: `server/manual-stdout.log` e `server/manual-stderr.log`.
O jar atualizado está em `server/plugins/greensky-0.1.0-SNAPSHOT.jar`.

**Teste manual:** Minecraft Java 26.1.2, endereço `127.0.0.1:25565` no mesmo computador. Faça login pelo
nLogin, use `/is create` se ainda não tiver ilha, `/is home`, `/agricultura` e `/collections farming`.
Com OP/greensky.admin: `/greensky item give SEU_NICK crop.ancient_seed 1` e
`/greensky item give SEU_NICK crop.golden_wheat 8`. Plante em solo arado e use o Trigo Dourado em uma
cultura comum imatura; consulte `/agricultura` olhando para ela. As entregas administrativas não contam
na coleção: o progresso é obtido colhendo plantas maduras. Para desligar o servidor, um administrador
pode usar `/stop` dentro do jogo. O endereço/configuração de rede permanecem locais.

---

## Fase 8.3 — Trigo Dourado como fertilizante — 07/10/2026

Pedido: continuar a agricultura e registrar todas as alterações. Fertilizantes são uma possibilidade
prevista na seção 33 do README. Esta entrega define uma primeira regra de jogo: clicar com Trigo Dourado
em trigo, cenoura ou batata imaturos consome uma unidade, avança dois estágios (limitados ao máximo)
e garante qualidade três na colheita madura. A fase 8.3 continua em andamento.

**Todas as alterações:**

- `greensky/greensky-core/src/main/java/com/greencodes/greensky/farming/CropMarkers.java` (novo):
  extrai o armazenamento de posições em LONG_ARRAY no PDC do chunk, com chave recebida no construtor.
  Mantém coordenadas locais/altura, inserção idempotente e remoção da chave vazia. Protege os três
  tipos de cultura suportados e o solo abaixo. Sem scanner, SQL ou arquivo síncrono por aplicação.
- `.../farming/AncientPlants.java`: passa a especializar CropMarkers, preservando exatamente a chave
  `greensky:ancient_plants`; plantas ancestrais existentes não precisam de migração.
- `.../farming/FertilizerListener.java` (novo): reconhece `crop.golden_wheat` pelo PDC; exige sobrevivência,
  autenticação pronta, coleção carregada, SkyWorld e permissões BUILD + INTERACT. Respeita negação
  explícita da interação com o bloco, tratando a ausência de ação vanilla do trigo separadamente.
  Consome o item da mão indicada no evento; segunda aplicação, planta madura ou ancestral não consomem.
  Grava `greensky:fertilized_plants` no chunk e informa a garantia de três estrelas ao jogador.
- `.../farming/FarmingListener.java`: produtos comuns de uma planta fertilizada recebem qualidade três;
  quantidades vanilla e progressão continuam iguais. Remove o efeito na colheita, inclusive imatura:
  quebrar antes da maturidade perde o tratamento sem devolver o fertilizante.
- `.../farming/AncientPlantListener.java`: estende proteções contra água, física, pisoteio, pistões e
  explosões às culturas fertilizadas e seu solo. Limpa ambos os marcadores nas quebras sem drops e
  drops cancelados; novo plantio limpa tratamento anterior. Quebra protegida exige sessão/coleção prontas.
- `.../GreenSkyPlugin.java`: cria o armazenamento de fertilizantes e registra/injeta o novo listener.
- `greensky/greensky-core/src/main/resources/content/crops.yml`: documenta consumo, avanço, qualidade,
  aplicação única, exclusão ancestral e perda na quebra imatura. Não altera probabilidades ou configurações
  existentes do servidor: os comentários do recurso são para novas instalações.
- `greensky/greensky-core/src/test/java/com/greencodes/greensky/farming/AncientPlantsTest.java`: verifica
  que remover um tratamento recarregado não apaga a identidade ancestral na mesma posição.
- `greensky/tools/e2e/fertilizer-e2e.mjs` (novo): bots com nLogin verificam permissões, identidade do item,
  consumo, reaplicação, água, restart, maturidade, qualidade e limpeza após colheitas maduras/imaturas.
  Também verifica exclusão de criativo/plantas ancestrais; seleção da semente por inventário isolado,
  sem depender de NBT exposto pelo Mineflayer, e coleta dos drops anteriores antes de medir consumo.
  Desliga crescimento aleatório durante o roteiro e restaura randomTickSpeed=3 ao encerrar.
- `greensky/tools/e2e/package.json`: inclui roteiro na suíte e adiciona `test:fertilizer`.
- `CLAUDE.md`: atualiza o estado da fase 8.3.
- `INSTRUCOES.md`: registra esta entrega e suas regras, arquivos e validação.

**Validação:** build/deploy offline concluído; 134 testes Java (incluindo PostgreSQL real), zero falhas
e zero testes ignorados. No jar final: fertilizer-e2e 13/13, ancient-plant-e2e 11/11 e farming-e2e
12/12 — 36 verificações com bots reais no Paper/nLogin, incluindo reinícios. Plugin copiado para
`server/plugins/greensky-0.1.0-SNAPSHOT.jar`; servidor de teste encerrado ao final.

Mutações, estufas, contratos e eventos agrícolas permanecem pendentes. Não foi feito commit.

---

## Fase 8.3 — Plantio da Semente Ancestral — 07/10/2026

Pedido: continuar a agricultura. A Semente Ancestral deixa de ser apenas colecionável: em sobrevivência,
planta trigo especial na ilha autorizada. Quebrar imatura devolve uma semente ancestral; madura rende
exatamente um Trigo Dourado de qualidade três, sem reproduzir outra semente ancestral.

**Todas as alterações:**
- `greensky/greensky-core/src/main/java/com/greencodes/greensky/farming/AncientPlants.java` (novo):
  - Guarda posições das plantas no PDC do chunk, chave `greensky:ancient_plants`/LONG_ARRAY.
  - Coordenadas x/z locais ao chunk + altura, incluindo valores negativos; inserção idempotente,
    remoção individual e limpeza da chave quando fica vazia. Não escreve uma chave vazia ao remover
    marcador inexistente de um plantio comum.
  - Persistência acompanha o save dos chunks e blocos. Não usa SQL por plantio, nem mapa de todos os
    mundos carregado no boot, nem scanner por tick. Corrige a proposta inicial de usar banco, evitando
    separar a identidade da planta da gravação do mundo.
- `greensky/greensky-core/src/main/java/com/greencodes/greensky/farming/AncientPlantListener.java` (novo):
  - Identifica semente pelo PDC `crop.ancient_seed`; nome/lore não são usados como identidade.
  - Plantio exige sobrevivência, WHEAT colocado, SkyWorld, jogador pronto, coleção carregada, BUILD
    e canBuild; plantio recusado não consome semente. Consumo é feito pelo Minecraft, sem desconto manual.
  - Marca somente BlockPlaceEvent autorizado no MONITOR; plantio comum limpa marcador antigo da posição.
  - Protege planta/solo contra fluidos, física, alterações por entidades, pistões e explosões.
    Pistões verificam blocos movidos e destinos, inclusive bloco empurrado sobre trigo frágil.
  - Cancela quebra do solo enquanto a planta existe; exige BREAK e coleções carregadas para quebrar a planta.
  - Limpa marcador em quebra criativa/sem drops e em BlockDropItemEvent cancelado, pois cancelar drops
    não restaura o bloco removido. Eventos cancelados não geram recompensa nem progresso.
- `greensky/greensky-core/src/main/java/com/greencodes/greensky/farming/FarmingListener.java`:
  - Recebe AncientPlants. Antes da qualidade comum, consulta identidade e snapshot WHEAT autorizado.
  - Imatura: substitui drops por uma Semente Ancestral; madura: substitui por um Trigo Dourado ★★★.
    Reutiliza a primeira entidade de drop e remove excedentes da lista, sem inserir entidades nela.
  - Na colheita autorizada remove marcador. Só madura conta +1 trigo e +1 Trigo Dourado; imatura não
    gera descobertas. Não roda sorteio de outro bônus no ramo ancestral, evitando replicação de sementes.
  - Extraída checagem de permissão/disponibilidade independente da maturidade para devolução imatura.
- `greensky/greensky-core/src/main/java/com/greencodes/greensky/farming/CropCatalog.java`:
  - Valida no boot a base exigida pelo plantio: WHEAT/crop.wheat e definições dos bônus
    crop.golden_wheat/WHEAT e crop.ancient_seed/WHEAT_SEEDS. Config incompleta falha com mensagem de arquivo.
- `greensky/greensky-core/src/main/java/com/greencodes/greensky/GreenSkyPlugin.java`:
  - Cria AncientPlants, registra AncientPlantListener e injeta o armazenamento no FarmingListener.
- `greensky/greensky-core/src/main/resources/content/crops.yml`: comentário atualizado com o ciclo de
  plantio ancestral; chances existentes continuam 2%/0,2%, pesos de qualidade comuns preservados.
- `greensky/greensky-core/src/test/java/com/greencodes/greensky/farming/AncientPlantsTest.java` (novo):
  - Duas regressões: posições negativas/alturas distintas; idempotência, remoção sem afetar outras plantas
    e leitura da mesma identidade com nova instância do serviço sobre os dados persistidos.
- `greensky/tools/e2e/ancient-plant-e2e.mjs` (novo):
  - Semente provisionada no teste pelo console com PDC, sem alterar chances dos achados em produção.
  - Onze verificações: visitante não planta/consome; consumo e bloco; devolução imatura sem multiplicação;
    identidade da semente; proteção contra água/pistão; planta após restart; crescimento com bone meal;
    um trigo sem sementes; identidade Trigo Dourado e progresso +1 nas duas entradas.
  - OP temporário só no diagnóstico do item, retirado antes do restante do roteiro.
- `greensky/tools/e2e/package.json`: test:ancient e ancient-plant-e2e no final da suíte completa.
- `CLAUDE.md`: estado de 8.3 e mapa de arquitetura atualizados.
- `INSTRUCOES.md`: este registro completo.

**APIs:** Chunk/PersistentDataHolder, BlockPlaceEvent, BlockState/Ageable, BlockPhysicsEvent e
BlockBreakEvent/isDropItems conferidos com javap na API fixada. Acesso a chunk/entidades somente na
thread do servidor. Nenhuma migration/dependência nova.

**Validação:** build/deploy offline, wrapper e Java 25; 134 testes Java, zero falhas/erros, zero pulados,
incluindo PostgreSQL. Ciclo ancestral com nLogin/Paper: 11/11 no jar final. Regressão agrícola comum:
12/12 antes das últimas salvaguardas restritas a plantas ancestrais. Jar instalado em server/plugins;
roteiros desligaram o servidor. Sem commit.

**Limites:** dados especiais acompanham a última gravação do mundo; backup deve incluir os chunks.
Comandos administrativos/setblock e ferramentas que editam blocos sem eventos não são reconciliados
automaticamente. Água/pistão e ciclo de plantio foram validados com bots; explosões/física/cancelamento
por outro plugin não têm cenário dedicado neste roteiro. Mutação, fertilizantes/estufas, contratos e
eventos agrícolas continuam pendentes; fase 8.3 ainda em andamento.

---

## Fase 8.3 — Qualidade dos produtos agrícolas — 07/10/2026

Pedido: continuar a agricultura após a base validada. Nesta entrega, qualidade persistente de uma a três
estrelas nos produtos das colheitas manuais; não implementa ainda efeitos de plantio/mutações/fertilizantes.

**Todas as alterações:**
- `greensky/greensky-core/src/main/java/com/greencodes/greensky/farming/CropQuality.java` (novo):
  - Pesos configuráveis para 1/2/3 estrelas, padrão [80,18,2]; uma amostragem por planta.
  - Valida três inteiros não negativos, soma positiva sem overflow. Zero desativa uma faixa.
  - Formata estrelas e recusa valores fora de 1..3. Sorteio em memória, sem banco.
- `greensky/greensky-core/src/main/java/com/greencodes/greensky/farming/CropItemFactory.java` (novo):
  - Reutiliza ItemFactory para id, nome e raridade do produto. Adiciona lore de qualidade e
    `greensky:crop_quality` como inteiro no PDC, preservado no próprio item ao sair/reiniciar.
- `greensky/greensky-core/src/main/java/com/greencodes/greensky/farming/CropCatalog.java`:
  - Crop agora contém o ContentItem do produto e expõe a mesma chave de progresso por entry().
  - Materiais de produto: trigo WHEAT, cenoura CARROT e batata POTATO; nomes/raridade vêm de farming.
  - Lê quality-weights na raiz de crops.yml; arquivos anteriores recebem o padrão sem precisar sobrescrever
    o conteúdo existente. Chave presente com valor inválido impede o boot.
- `greensky/greensky-core/src/main/java/com/greencodes/greensky/farming/FarmingListener.java`:
  - Handler HIGH qualifica somente drops do produto principal de plantas maduras autorizadas.
  - Mantém quantidade original e usa a mesma qualidade para todos os stacks da planta. Não qualifica
    sementes vanilla/batata venenosa nem substitui itens já marcados como GreenSky.
  - Extraída elegibilidade comum: SkyWorld, sessão pronta, coleções carregadas, planta madura, drops e BREAK.
  - Contagem e achados raros continuam no MONITOR, sem registrar evento cancelado; qualidade não multiplica
    a contagem da planta. O handler de qualidade não cancela o evento nem insere/remove entidades da lista.
- `greensky/greensky-core/src/main/java/com/greencodes/greensky/GreenSkyPlugin.java`:
  - Injeta CropItemFactory no FarmingListener usando o plugin e ItemFactory já existentes.
- `greensky/greensky-core/src/main/resources/content/crops.yml`: adicionado quality-weights [80,18,2]
  com comentário de compatibilidade com arquivos anteriores.
- `greensky/greensky-core/src/test/java/com/greencodes/greensky/farming/CropQualityTest.java` (novo):
  - Três testes: distribuição em 100 mil sorteios, faixa única/faixas desativadas, pesos inválidos e overflow.
- `greensky/tools/e2e/farming-e2e.mjs`: quatro novas verificações de produto recolhido, identidade GreenSky,
  qualidade válida no inventário (consulta autoritativa ao servidor) e mesmo valor após restart.
  O bot recebe OP somente durante o comando de diagnóstico e é deopado antes das verificações de proteção.
- `CLAUDE.md`: estado de 8.3 e mapa farming atualizados.
- `INSTRUCOES.md`: este registro completo.

**Validação:** build/deploy offline pelo wrapper com Java 25; 132 testes Java, zero falhas/erros e zero
pulados, incluindo PostgreSQL. Roteiro agrícola com nLogin real: 12/12, incluindo qualidade/progresso após
restart. Jar atualizado no servidor de teste; roteiro desligou o servidor. Sem commit.

**Escopo:** qualidade diferencia itens/estrelas, ainda sem bônus de produção ou preço (Coins seguem para
fase 11). Crafting/plantio vanilla não transfere metadados ao resultado; plantio especial está pendente.
Achados raros mantêm sua identidade e raridade próprias. Mutação, sementes de plantio especial e
fertilizantes/estufas ainda não implementados. Fase 8.3 permanece em andamento.

---

## Fase 8.3 — Base da agricultura nativa — 07/10/2026

O pedido de continuar após a regressão foi tratado como avanço para a agricultura. Esta entrega é a base
de colheita/progresso e achados raros; não declara todas as possibilidades agrícolas do README concluídas.

**Todas as alterações:**
- `greensky/greensky-core/src/main/java/com/greencodes/greensky/farming/CropCatalog.java` (novo):
  - Lê `content/crops.yml` no boot, vincula culturas/bônus à coleção farming e reutiliza nome/raridade.
  - Suporta WHEAT/CARROTS/POTATOES; rejeita seção vazia, cultura/entrada inválida, bônus inválidos,
    material que não é item, chances não finitas/fora de (0,1] e soma das chances acima de 1.
  - Sorteio em memória, no máximo um achado por planta, sem SQL e sem dependência externa.
- `greensky/greensky-core/src/main/java/com/greencodes/greensky/farming/FarmingListener.java` (novo):
  - Observa BlockDropItemEvent não cancelado, com snapshot da planta madura e drops reais.
  - Exige SkyWorld, autenticação, coleção carregada e permissão BREAK na ilha da planta.
  - Soma uma planta, não o número de drops: Fortuna não multiplica a coleção.
  - Colheita imatura, visitante, quebra automática e recolhimento de itens não geram progresso.
  - Mantém drops vanilla; achado raro recebe identidade PDC via ItemFactory e conta na coleção antes
    de ser largado no mundo, inclusive com inventário cheio. Nenhuma recompensa em Coins.
- `greensky/greensky-core/src/main/resources/content/crops.yml` (novo):
  - Trigo/cenoura/batata e chaves já existentes, sem renomear progresso salvo.
  - No trigo: Trigo Dourado 2%, Semente Ancestral 0,2%. Chances ajustáveis por conteúdo.
  - Achados são colecionáveis, sem efeito de fertilização/plantio especial nesta entrega.
- `greensky/greensky-core/src/main/java/com/greencodes/greensky/GreenSkyPlugin.java`:
  - Carrega/valida CropCatalog junto ao conteúdo no boot; arquivo inválido aborta o plugin.
  - Registra FarmingListener com os serviços existentes, sem nova migration/config/dependência.
- `greensky/greensky-core/src/test/java/com/greencodes/greensky/farming/CropCatalogTest.java` (novo):
  - Arquivos reais, três culturas, ausência de bônus na cenoura, distribuição de 100 mil sorteios
    e rejeição de definições inválidas/chances cuja soma excede 1.
- `greensky/tools/e2e/farming-e2e.mjs` (novo):
  - Bots com nLogin, ilha própria, plataforma de teste e plantas em estágios controlados.
  - Oito verificações: planta imatura; trigo/cenoura/batata maduros; visitante sem progresso e sem
    destruir planta; quebra por comando sem progresso; persistência agrícola após restart.
- `greensky/tools/e2e/package.json`: adicionados test:farming e farming-e2e ao final da suíte completa.
- `CLAUDE.md`: estado 8.3 atualizado como base implementada, mecânicas avançadas pendentes; farming no mapa.
- `INSTRUCOES.md`: este registro de escopo, alterações e testes.

**APIs:** construtor/métodos de BlockDropItemEvent, snapshot BlockState e Ageable conferidos com javap
na paper-api fixada. Nenhum NMS, scanner por tick ou gravação por colheita.

**Validação:** `build deploy --offline` pelo wrapper, Java 25; 129 testes Java, zero falhas/erros,
zero pulados, incluindo PostgreSQL. Agricultura com bots no Paper/nLogin: 8/8, incluindo restart.
Jar instalado em server/plugins e `content/crops.yml` criado pelo plugin na primeira inicialização.
Roteiro encerrou o servidor. Não reexecutada a suíte completa de bots nesta entrega; a regressão completa
anterior está abaixo. Sem commit.

**Pendências de 8.3:** qualidade de produtos, mutações/plantas especiais, sementes com plantio especial,
estufas/fertilizantes e demais mecânicas agrícolas precisam de definição e implementação antes de declarar
a etapa inteira concluída. Quests/exploração não iniciadas. Bônus raros foram validados por sorteio em teste
Java; o roteiro com bots não força essas probabilidades nem comprova a entrega rara no servidor.

---

## Deploy e validação com bots após P1/P2 — 07/10/2026

Pedido: continuar a validação após as correções, antes de iniciar uma nova fase.

**Todas as alterações e ações:**
- Executado `gradlew deploy --offline`, com Java 25: instalado o jar corrigido em
  `server/plugins/greensky-0.1.0-SNAPSHOT.jar`. SHA-256 conferido: idêntico ao jar em build/libs.
- Executados os cinco roteiros com Paper local e nLogin real. Os roteiros iniciaram/desligaram o servidor,
  criaram contas/ilhas de bots e alteraram seus cenários no banco e no mundo de teste.
- `greensky/tools/e2e/collections-e2e.mjs`: após conectar, somente o bot desse roteiro recebe modo criativo.
  Motivo: na primeira execução um Husk matou o bot logo após a entrega da relíquia, causando duas falhas
  de inventário; o log confirmou a entrega antes da morte. Nenhuma regra de produção foi alterada.
- `greensky/tools/e2e/fishing-e2e.mjs`: a retomada após mudar a mira agora distingue peixes de lixo/tesouro.
  Repete até um peixe, com limite de seis fisgadas reais/18 lances; peixe vanilla sem substituição reprova.
  Usa os pontos de mira já válidos no lago e imprime os materiais dos itens vanilla no diagnóstico.
  Motivo: a primeira execução teve 16/17; o critério anterior exigia peixe personalizado em apenas duas
  fisgadas, embora lixo/tesouro vanilla seja permitido. A repetição completa com o novo critério passou.
- `INSTRUCOES.md`: registro completo das ações, alterações nos testes e resultados.

**Resultados finais:** autenticação 15/15; proteção 23/23; ciclo jogável com restart 19/19;
coleções 17/17 após ajuste do cenário; pesca 17/17 após ajuste do critério: 91 verificações aprovadas.
A expansão foi pulada no roteiro de proteção porque Alice já estava no nível máximo, mas o roteiro
first-playable validou 100 -> 150 numa ilha nova e persistência dos blocos, tamanho e borda após restart.
Pesca confirmou bloqueio AFK e retorno com Salmão após mudar a mira; inventário cheio conta na fisgada,
sem duplicação ao recolher o item. Coleções confirmou identidade da relíquia e recusa de cópia pelo nome.

Não houve nova alteração no código Java; a suíte Java anteriormente validada continua com 127 testes
aprovados. Sem commit. Servidor de teste desligado normalmente pelo roteiro ao terminar.
Estado: correções P1/P2 instaladas e regressão com bots concluída; agricultura (8.3) ainda não iniciada.

---

## Correções P2 da revisão — 07/10/2026

Pedido: corrigir os quatro P2 restantes e registrar todas as alterações.

**Todas as alterações:**
- `greensky/greensky-core/src/main/java/com/greencodes/greensky/collection/CollectionService.java`:
  - `load()` agora compartilha consultas em andamento por UUID e não recarrega uma sessão já carregada.
  - Cada resposta só publica os totais se seu future ainda for o carregamento atual. `unload()` invalida
    carregamentos pendentes; resposta antiga não restaura jogador offline nem sobrescreve uma reconexão.
  - Quando há sessão encerrada com lotes pendentes/em andamento, a reconexão reutiliza os totais locais,
    em vez de consultar um banco que ainda não recebeu essas somas. A fila de retry do P1 é preservada.
  - Falha de leitura remove o carregamento pendente e permite nova tentativa, sem publicar totais vazios.
  - Início/fim de sessão e registro são sincronizados somente para operações de memória; não há espera
    bloqueante por banco na thread do servidor.
- `greensky/greensky-core/src/main/java/com/greencodes/greensky/collection/CollectionTracker.java`:
  - Adicionado `isLoaded(Player)` para as atividades verificarem se as coleções estão disponíveis.
- `greensky/greensky-core/src/main/java/com/greencodes/greensky/fishing/FishingListener.java`:
  - Fisgada sem coleções carregadas permanece vanilla, sem sorteio/entrega de peixe personalizado.
  - Cria a recompensa, registra a coleção e verifica o resultado: só DISCOVERED/PROGRESSED permitem
    substituir o item e anunciar o peixe. NOT_LOADED/UNKNOWN_ENTRY não entregam recompensa personalizada.
- `greensky/greensky-core/src/main/java/com/greencodes/greensky/island/IslandService.java`:
  - Mapa `generating` compartilha um future por ilha desde antes da consulta até confirmar READY no banco.
  - Criação, recuperação por home e visitas usam a mesma operação. Ao concluir/falhar, a entrada é removida.
  - Antes de gerar, relê o estado persistido por id. Um objeto PENDING atrasado não regenera ilha já READY.
  - Atualiza o cache somente após sucesso; falhas continuam propagadas para o chamador.
- `greensky/greensky-core/src/main/java/com/greencodes/greensky/protection/IslandIndex.java`:
  - `put`, `remove`, `at`, `get` e `size` sincronizados no mesmo monitor: substituição de região e
    atualização dos baldes não se intercalam com outra escrita ou leitura parcial. Continua sem SQL.
- `greensky/greensky-core/src/test/java/com/greencodes/greensky/collection/CollectionSessionTest.java` (novo):
  - Quatro regressões: leitura antiga após reconexão/saída; chamadas de load duplicadas; reconexão durante
    gravação com totais preservados/sem redescoberta; falha de leitura seguida de nova tentativa.
- `greensky/greensky-core/src/test/java/com/greencodes/greensky/protection/IslandIndexTest.java`:
  - Regressão com oito workers, 250 substituições cada, verificando ausência de regiões antigas após
    substituição final e remoção.
- `greensky/greensky-core/src/test/java/com/greencodes/greensky/island/IslandServiceIT.java`:
  - Regressão contra PostgreSQL com geração controlada: 20 chamadas concorrentes e objeto PENDING antigo
    produzem exatamente uma construção; resultado é READY.
- `greensky/greensky-core/src/test/java/com/greencodes/greensky/world/FishingReadinessTest.java` (novo):
  - Evento real `PlayerFishEvent` com interfaces simuladas por proxies, sem dependência adicional.
    Jogador autenticado sem coleção carregada não lê/substitui o item vanilla nem cancela a fisgada.
  - Construtor do evento conferido com javap. A primeira execução falhou por falta de `Plugin.namespace()`
    no proxy; o proxy foi ajustado à API instalada, e a suíte completa passou.
- `INSTRUCOES.md`: este registro completo. Nenhuma alteração de config, schema, dependências ou fases.

**Validação:** Gradle wrapper, Java 25, build offline; 127 testes, zero falhas/erros e zero pulados,
incluindo PostgreSQL real. Sete regressões novas; testes P1 continuam passando. Jar atualizado em
`greensky/greensky-core/build/libs/greensky-0.1.0-SNAPSHOT.jar`.
Não realizado deploy, restart do servidor nem testes com bots. Sem commit.

**Limites:** fila de retry continua em memória; nenhuma alegação de Folia ou validação de carga em produção.
Os quatro P2 listados na revisão foram tratados; o desenvolvimento continua na fase 8.2.

---

## Correções P1 da revisão — 07/10/2026

Pedido: corrigir primeiro os dois P1, sem iniciar a agricultura ou os demais P2.

**Todas as alterações:**
- `greensky/greensky-core/src/main/java/com/greencodes/greensky/collection/CollectionService.java`:
  - Ao sair, a sessão é removida do acesso online e passa para a fila `retiring`, independente das
    sessões novas. Uma falha de gravação devolve o lote à sessão encerrada e não descarta suas somas.
  - `flushAll()` também tenta os lotes de jogadores offline, aproveitando o timer já existente.
  - Uma gravação por sessão por vez (`inFlight`); chamadas simultâneas compartilham o future.
  - Depois de confirmar um lote de uma sessão encerrada, grava também o restante acumulado durante
    aquela operação. O future aguardado no desligamento inclui esse restante.
  - Sessões encerradas saem da fila somente quando não há lote pendente nem gravação em andamento.
  - O construtor público mantém PostgreSQL e transações; um construtor interno com funções de
    leitura/gravação permite simular falhas e atrasos determinísticos nos testes, sem dependência nova.
  - Corrigida a documentação: lotes não confirmados ficam em memória; durante indisponibilidade do
    banco, um crash/desligamento pode perder mais que os 10 segundos do intervalo normal.
- `greensky/greensky-core/src/main/java/com/greencodes/greensky/protection/IslandProtectionService.java`:
  - `beginSession()` gera uma identidade por sessão autenticada e limpa as permissões anteriores.
  - `completeSession()` só aplica participações se a identidade ainda for a atual.
  - Saída invalida a identidade e remove as participações. Início, conclusão e saída são sincronizados
    para que validar a identidade e aplicar o resultado não concorram com o encerramento.
  - `playerJoined()` continua disponível para os testes/clientes existentes, usando o mesmo ciclo.
- `greensky/greensky-core/src/main/java/com/greencodes/greensky/protection/ProtectionSessionListener.java`:
  - Cria a identidade antes da consulta e usa essa identidade na resposta; respostas de sessões
    encerradas ou substituídas por reconexão são ignoradas.
  - Captura o nome antes da consulta, evitando obter esse dado do Player no callback do banco.
- `greensky/greensky-core/src/test/java/com/greencodes/greensky/collection/CollectionRetryTest.java` (novo):
  - Falha ao sair + reconexão + reenvio exato sem duplicar lote confirmado.
  - Saída enquanto há gravação em andamento: falha reúne lote anterior e novas somas para retry.
  - Sucesso da gravação em andamento: lote restante também é aguardado no flush de desligamento.
- `greensky/greensky-core/src/test/java/com/greencodes/greensky/protection/IslandProtectionServiceTest.java`:
  - Três regressões: resposta após saída, resposta da sessão anterior durante reconexão e liberação
    de acesso somente pela resposta da sessão atual.
- `INSTRUCOES.md`: este registro completo das mudanças e validação.

**Validação:** build pelo wrapper, offline, Java 25; 120 testes, zero falhas/erros e zero pulados,
incluindo integração com PostgreSQL. Jar gerado em `greensky/greensky-core/build/libs/`.
Shadow mantém os avisos de LICENSE duplicadas já existentes; build concluído com sucesso.
Não feito deploy, restart do servidor nem testes com bots nesta correção. Sem commit.

**Limites:** retry é em memória, não um journal em disco. Os P2 da revisão continuam pendentes,
inclusive carregamento/reconexão das coleções, pesca, geração de ilha e índice espacial.

---

## Fase 8.2 — Pesca (nativa, sem plugin externo e sem Coins)

**APIs conferidas na jar 26.1.2:** `PlayerFishEvent` (`getState`, `getCaught`, `getHook`; estado
`CAUGHT_FISH`), `FishHook#isInOpenWater()`, `Item#setItemStack`, `World#isDayTime/hasStorm/isThundering/
getFullTime`, `io.papermc.paper.world.MoonPhase.getPhase(long)`. Não existe `World#getMoonPhase`; o bytecode
mostra que `getPhase` recebe o **número do dia** (`dia % 8`), então a lua é `getPhase(getFullTime() / 24000)`.

**O que foi feito (`fishing/`):**
- `content/fish.yml`: peixes com material, peso no sorteio, faixa de tamanho e condições (horário, clima,
  lua). Cada id precisa existir na coleção `fishing`; nome e raridade vêm de `collections.yml`. Conteúdo
  ainda provisório no visual (materiais vanilla; o resource pack vem depois): Bacalhau, Salmão, Baiacu
  (sempre), Peixe Tropical (chuva), Enguia da Tempestade (tempestade), Carpa da Lua Cheia (noite de lua
  cheia), Sussurro Abissal (secreto: noite + tempestade + lua nova).
- `FishTable`: sorteio só em memória entre os peixes possíveis no momento; tamanho puxado para baixo
  (peixes grandes são raros) e qualidade em estrelas (★★★ no topo da faixa).
- `FishingListener`: na fisgada de um **peixe** vanilla troca o item por um peixe do GreenSky (id escondido +
  tamanho e estrelas no item) e soma 1 na coleção. **Lixo e tesouro vanilla nunca são trocados.**
- Config `fishing.require-open-water` (true) e `fishing.afk-max-catches-same-spot` (10).

**Anti-exploit (todos testados com bots):**
- **Ilha alheia:** só na ilha de que o jogador é dono/membro; visitante pesca vanilla e não alimenta coleção.
- **Balde/poça:** só em água aberta (regra do Minecraft: 5x5 em volta da boia, de 1 abaixo até 2 acima).
- **Pesca parada (AFK/fazenda):** mais de 10 fisgadas seguidas na mesma posição **e** mira => aviso e pesca
  vanilla até mudar de lugar/mira. Não ouve o movimento do jogador (só compara a cada fisgada).
- **Inventário cheio:** a coleção conta **na fisgada**, nunca ao pegar o item; o peixe cai no chão e, ao ser
  recolhido, não conta de novo.

**Performance:** nenhuma consulta ao banco por fisgada (a coleção grava em lote); sorteio em memória; sem
listener de movimento.

**Testes:** 114 no total. Novos: `FishTableTest` (6: condições de clima/horário/lua com os arquivos reais,
distribuição de 100 mil sorteios conforme os pesos, tamanho na faixa, estrelas), `FishLoaderTest` (3),
`AfkFishingGuardTest` (3), 1 em `GreenSkyConfigTest`.

**Teste com bots (`fishing-e2e.mjs`, nLogin real): 17/17** — dono em água aberta pesca peixes do GreenSky e
**nenhum peixe vanilla escapa da troca** (o que não virou peixe do GreenSky era lixo: couro e poção); tempo
limpo de dia só dá peixes comuns; coleção +1 por peixe; visitante na ilha alheia e poça rasa: só vanilla;
inventário cheio: pesca, conta 1 vez e não conta de novo ao recolher; pesca parada detectada com aviso e sem
peixes do GreenSky depois; mudando a mira, volta ao normal.

**Erros meus encontrados durante o teste e corrigidos:**
1. Cenário: a piscina tinha 5x5, o mesmo tamanho da área que o Minecraft checa; boia fora do centro "via" a
   borda e a pesca virava vanilla (comportamento correto do plugin, cenário errado). Agora o lago é 21x21 e o
   bot pesca de um pilar no meio.
2. Teste contava lances sem fisgada (o bot às vezes recolhe a linha cedo); com isso "visitante" e "poça"
   passavam sem provar nada. Agora só conta lance em que o item chegou de fato.
3. **No plugin:** a mensagem usava o emoji 🎣, que a fonte do Minecraft não desenha (aparecia como
   quadrados). Trocado por "»".
4. Critério estatístico frágil ("no máximo 1 de 5 sem ser do GreenSky"): ~15% das fisgadas são lixo/tesouro.
   Substituído pela propriedade exata (nenhum peixe vanilla passa sem troca).
5. Erros de rede do bot depois de conectado derrubavam o teste; agora só são registrados.

**Regressão final (todos com nLogin real):** `auth` 15/15, `protection` 23/23 (expansão pulada: ilha da
Alice no máximo), `first-playable` 19/19, `collections` 17/17, `fishing` 17/17; 114 testes sem cache.
Numa rodada anterior duas falhas foram **dos testes, não do plugin** (banco e mundo íntegros, conferidos):
- Fase 7: a terra colocada sobre grama ao ar livre **virou grama** (espalhamento vanilla) antes do restart;
  conferido no mundo salvo. O teste agora aceita terra ou grama nesse ponto.
- Coleções: o nLogin demorou mais de 8 s para pedir o login logo após o boot; o apoio agora espera até 20 s
  e reconhece também a contagem ("seconds to register/login").
- Ao investigar, achei outro problema do apoio: `isBlock` respondia "false" para chunk não carregado (um
  falso "bloco diferente"). Agora isso é erro explícito.

**Limite conhecido:** o detector de pesca parada pega posição+mira idênticas; macros que mexem a mira de
leve passam. Endurecer isso (ex.: limite de peixes por hora) fica para a fase de hardening.

---

## Decisão: WorldEdit/FAWE (levantamento antes da Fase 12)

**Decisão sua:** usar o **FAWE 2.16.0 só no servidor de construção, na Fase 12**. Nada baixado nem instalado
agora; **nada de WorldEdit/FAWE no servidor do jogo**.

**Levantamento (fontes oficiais):** WorldEdit 7.4.5 (release; Paper 1.21.4–26.2) e FAWE 2.16.0 (release;
lista 26.1.2) declaram suporte ao Paper 26.1.2 (Modrinth/Hangar). Os dois não convivem (o FAWE substitui o
WorldEdit). Documentação do FAWE: "We suggest that FAWE operations are completed asynchronously";
`Operations#complete` e `EditSession#close` bloqueiam; não reaproveitar `EditSession`. Documentação do
WorldEdit: `ClipboardFormats` → `ClipboardHolder.createPaste(...).to(...).build()` → `Operations.complete`;
fechar a `EditSession`; `newEditSessionBuilder().maxBlocks(...)`. Não testado no nosso servidor.

**Fluxo recomendado (a seguir na Fase 12):**
1. **Regiões grandes e fixas** (Meadows, Ancient Ruins...): montadas no **servidor de construção** com FAWE e
   levadas para o SkyWorld como **arquivos de região do mundo** (`world/dimensions/minecraft/greensky_world/`).
   Custo zero em jogo: o servidor carrega sob demanda, como qualquer terreno.
2. **Colagens pequenas em tempo de jogo** (templates de ilha, estruturas de evento), sempre:
   - leitura do arquivo fora da thread do servidor;
   - cálculo prévio dos chunks atingidos; **recusa** acima de um limite de tamanho ou fora da região permitida;
   - chunks pré-carregados de forma assíncrona e presos só durante a cola;
   - **uma colagem por vez, em fila**, com limite de blocos;
   - **estado no banco** (PENDENTE → PRONTO, como a criação de ilha) para refazer após crash;
   - medição com spark antes de liberar em produção.

**A confirmar quando o jar chegar (em `libs/`, lido com `javap`):** se o FAWE 2.16.0 sobe e cola sem erro no
Paper 26.1.2; os nomes exatos das opções de efeitos colaterais (iluminação, vizinhos; a busca citou
`setSideEffectApplier`, mas a página oficial deu 404); como o FAWE carrega chunks e quais limites aplica
(memória, fila); se a leitura do schematic fora da thread do servidor é segura; e o formato/versão dos
arquivos de região entre o servidor de construção e o do jogo (mesma versão do Paper).

---

## Fase 8.1 — Base de conteúdo + Coleções

**Ordem aprovada da Fase 8:** 8.1 base + coleções → 8.2 pesca → 8.3 agricultura → 8.4 quests → 8.5
exploração. Pesca e agricultura nativas (sem Storm/y). Recompensas sem Coins até a economia (fase 11).

**APIs conferidas na jar 26.1.2:** `ItemStack.of`, `ItemStack#editMeta`, `ItemMeta#itemName/lore`,
`PersistentDataContainer`/`PersistentDataContainerView` (`set`/`get`), `PersistentDataType.STRING`,
`NamespacedKey(Plugin, String)`, `Material.matchMaterial`/`isItem`, `ConfigurationOptions#pathSeparator`;
Adventure 4.26.1: `Audience#showTitle/playSound`, `Title.title`.

**O que foi feito:**
- `V5__collection_progress.sql`: progresso por jogador e entrada (a definição das coleções fica em YAML).
- `content/`: `Rarity` (COMMON..SECRET, seção 103, com cor), `ContentItem`, `ContentRegistry` (lê
  `content/items.yml`), `ItemFactory` (cria o item e grava a identidade escondida `greensky:item`; reconhece
  pelo id, não pelo nome), `ContentYaml`, `ItemCommand` (`/greensky item give|check`, admin).
- `collection/`: `CollectionCatalog` (lê `content/collections.yml`; chaves únicas entre coleções),
  `CollectionService` (progresso em memória, gravação em lote a cada 10 s, ao sair e ao desligar; se o banco
  falhar, o lote volta para a fila), `CollectionTracker` (porta das atividades; dispara
  `CollectionDiscoverEvent` na 1ª vez), `CollectionListener` (carrega após o login; "✦ Descoberto: X" +
  título + som), `CollectionCommand` (`/collections [id]` com barra "██████░░░░ 60%", secretas como "???";
  `/collections admin grant <nick> <entrada> [qtd]`).
- `greensky-api`: `CollectionDiscoverEvent` (para quests e conquistas usarem depois).
- Conteúdo inicial **provisório**: coleções Pesca (7), Agricultura (5), Exploração (5) e 1 relíquia de
  exemplo; 8.2, 8.3 e 8.5 definem o conteúdo de verdade.

**Erros encontrados pelos testes e corrigidos:**
1. No YAML do Bukkit o ponto é separador de caminho: `fish.cod` virava `fish -> cod`. Agora o conteúdo é lido
   com separador `/` (`ContentYaml`); erro de sintaxe passa a impedir o boot com mensagem (o Bukkit devolvia
   um arquivo vazio em silêncio).
2. `Material.isItem()` exige o servidor; a validação recebe essa checagem de fora (produção usa a real).
3. Corrida no `unload`: sair e entrar rápido podia apagar a sessão nova; agora só remove a mesma sessão.

**Testes:** 101 no total. Novos: `CollectionCatalogTest` (4, inclui o arquivo real do jar), `ContentRegistryTest`
(5), `ProgressBarTest` (3) e `CollectionServiceIT` (4, Postgres: descoberta única, recusa de entrada inválida,
persistência após "restart", e 8 threads x 250 registros com lotes no meio: 1 descoberta e soma exata 2000).

**Teste com bot (`collections-e2e.mjs`, nLogin real): 17/17** — coleções zeradas no início; 1ª entrada
"Descoberto" uma única vez; segunda só soma; entrada inexistente e quantidade inválida recusadas; detalhe com
x5, secreta "???" e não descobertas apagadas; item real reconhecido e **cópia com o mesmo nome não**; depois de
reiniciar o servidor o progresso continua e não há "redescoberta".

**Regressão com o jar da 8.1:** `protection-e2e` 23/23 (expansão pulada: ilha da Alice no máximo),
`first-playable-e2e` 19/19, `auth-e2e` 15/15. Numa das execuções o `auth-e2e` parou porque o servidor respondeu
"Chat disabled in client options" (o comando do bot chegou antes de ele mandar as opções de chat; corrida do
bot, não do GreenSky); repetido, passou. O `say` do `lib.mjs` agora reenvia uma vez nesse caso.

**Riscos já tratados na base:** sem consulta ao banco por evento (lote); identidade de item não falsificável
por nome/lore; descoberta disparada uma única vez mesmo com registros simultâneos. **Para a 8.2:** contar
coleção só no evento de origem (pescar), nunca por pegar item do chão.

---

## Plugins externos: StormPlugins e yPlugins (antes da Fase 8)

**Instalados por pedido seu (carregadores):** `StormPlugins` 2.6.6 e `yPlugins` 3.7.0, que baixam da loja os
plugins comprados e os carregam **direto na memória** (nenhum jar dos plugins fica em disco). Ambos têm a
**licença presa ao IP e à porta** (`104.28.202.67:25565`, cadastrado por você). Na primeira tentativa, sem o IP
cadastrado, o **yPlugins desligou o servidor sozinho** ("Falha na autenticação ... desligando servidor"): em
produção, uma falha do servidor de licenças deles ou troca de IP derruba o servidor. `104.28.x.x` parece ser
faixa da Cloudflare (WARP/VPN); se for, o IP pode mudar.

**Carregados com a licença certa (18):** Storm: Minas 2.9.9.7, MinasPrivadas 1.0.5, RankUP 1.7.1, Spawners
1.5.7, SpawnersShopV2 1.2.3, LobbyV2 2.4.7, Clans 1.2.5, EconomiaSecundaria 1.2.9, Crates 1.1.1, Vip 1.6.0,
Looting 1.1.7, Pescaria 1.5.8. y: yPesca 1.7.9, yCampo 1.5.2, yBosses 1.4.5, yArmazem 3.9.0 (+ yCore e
yPlotCore 1.0.0). yPesca, yCampo, yBosses, yArmazem e StormSpawners deram erro por **falta do Vault**.

**Desativado (decisão sua) e por quê:**
- `server/plugins/StormPlugins/config.yml`: `plugins_nao_carregar` com os 12 plugins Storm e
  `atualizar_automatico: false` (não trocar código sozinho).
  - Minas, MinasPrivadas, RankUP: o README não quer um servidor de mineração/rankup; a ilha é a base.
  - Spawners, SpawnersShopV2: carga de entidades e economia de grind.
  - LobbyV2: é para um servidor de lobby separado (futuro, com Velocity); no servidor das ilhas brigaria
    com a entrada (nLogin + GreenSky).
  - Clans (fase 10), EconomiaSecundaria (11), Crates e Vip (13-14), Looting (não avaliado): ficam para as
    fases deles.
  - Pescaria: a pesca será nativa no GreenSky (fase 8).
- `yPlugins-3.7.0.jar` **saiu de `server/plugins/` e foi guardado em `libs/`** (nada apagado; as pastas de
  dados dos plugins y continuam em `server/plugins/`). yPesca e yCampo: pesca e agricultura serão nativas.
- **Fase 8: pesca e agricultura nativas no GreenSky, sem integração com nenhum plugin Storm ou y.** Além da
  decisão de design, não há como integrar: os jars não ficam em disco, então não há API para ler (regra 3).
- O carregador `StormPlugins-2.6.6.jar` continua em `server/plugins/` (com a lista acima ele não carrega
  nenhum plugin Storm, mas ainda valida a licença e baixa as próprias bibliotecas ao iniciar).

---

## Identidade e autenticação (online-mode=false + nLogin) — depois da reorganização

**Decisão sua:** o servidor aceita originais e piratas, então `online-mode=false` também em produção, com
plugin de login (nLogin). Sem commit (regra de git).

**Auditoria (antes de mudar):** o GreenSky já usava só UUID como identidade. Com `online-mode=false` **sem**
plugin de login, comprovado com bots: `Caso845254` e `caso845254` tiveram UUIDs diferentes, entraram ao mesmo
tempo e criaram **duas ilhas**; e `/is visit`/`/is admin expand` por nome iam para a conta errada (o cache
do servidor ignora maiúsculas e devolvia a última conta vista).

**nLogin (pesquisa + jar real):**
- Fonte oficial: docs.nickuc.com, jd.nickuc.com/nlogin, nickuc.com. Grátis; versão paga BRL 9,90/mês,
  19,90/3 meses, 29,90/6 meses ou 59,90 permanente. Código fechado (sem licença publicada encontrada).
  Login automático de contas originais: **só na versão paga**.
- Jar: nLogin **2.0.24** (SHA-256 `e3266376…dc448`), em `libs/` e `server/plugins/`. Baixado por engano
  numa consulta de leitura e depois autorizado por você só para o servidor local.
- Inconsistência de versão: a documentação manda usar `com.nickuc.login:api:10.4`, mas o repositório Maven
  oficial só tem **`2.0`** (a Javadoc "api 2.0" está certa). A API vem embutida no jar do plugin, então o
  GreenSky compila contra `libs/nLogin-2.0.24.jar`.
- `javap`: `AuthenticateEvent` (Bukkit) tem só `getPlayer()`; `LoginEvent`/`RegisterEvent` são eventos
  separados (canceláveis), não subclasses. `nLoginAPI.getApi()`, `isAvailable()`, `isAuthenticated(String)`.
- **Funciona no Paper 26.1.2** (testado; a documentação não lista versões). Ao iniciar, baixa dependências
  da internet; o jar se identifica como "2.0.24 DEV"; avisa sobre `perform-username-validation` do Paper.
- Assistente de configuração (no servidor de teste, com um bot op): inglês, canal estável, atualização
  com **confirmação manual** (nunca automática), senha "Safer", **sem diálogos** (comandos), nAntiBot
  **recusado** (não baixar outro plugin). Avisa que as coordenadas não são protegidas antes do login.

**Comportamento real do nLogin (com bots):**
- O UUID de uma conta autenticada **é o UUID offline do nick registrado** (`MD5("OfflinePlayer:"+nick)`),
  igual no cliente, no servidor e no banco do GreenSky.
- A outra grafia do nick (`auth…` vs `Auth…`) recebe **o mesmo UUID** e precisa da senha da conta (`/login`);
  com senha errada nada do GreenSky é liberado; com a certa, é a mesma ilha. Ou seja: **com o nLogin, não há
  duas ilhas por maiúsculas**. A auditoria anterior (duas contas) vale para o servidor **sem** nLogin.
- `AuthenticateEvent` dispara tanto no `/register` quanto no `/login`.

**O que foi implementado:**
- `V4__players_name_unique.sql`: índice único em `lower(name)`. Antes, apaguei do banco de DEV (com seu OK)
  só as contas `Caso845254`/`caso845254` e as duas ilhas delas (`Sonda3` nunca foi gravado). 7 -> 5 ilhas.
- `player/`: `PlayerService` (nick único, busca sem maiúsculas), `PlayerSessionService` (registra a conta
  **depois** do login e dispara `GreenSkyPlayerReadyEvent`), `PlayerSessionListener` (pré-login
  **assíncrono** recusa outra grafia de um nick já registrado; consulta ao banco fora da thread do servidor;
  se o banco falhar, recusa a entrada).
- `integration/auth/`: `AuthBridge`, `NLoginAuthBridge` (`AuthenticateEvent`; troca para a thread do servidor
  se vier assíncrono), `NoAuthBridge`.
- `greensky-api`: `GreenSkyPlayerReadyEvent` (primeiro contrato público).
- Config `auth.provider` (nlogin|none) e `auth.dev-mode`. `none` + `online-mode=false` **recusa subir** sem
  `dev-mode: true`; com ele, sobe com aviso forte. `nlogin` sem o plugin => não sobe.
- Proteção (participações), dica de início, borda e comandos `/is` passaram a reagir ao "jogador pronto" em
  vez do join; `/is` antes do login responde "Faça login primeiro."; `visit`/`remove`/`admin` buscam o nome no
  banco do GreenSky. `plugin.yml`: `softdepend: [nLogin]`. `.gitignore`: comentário de `online-mode` corrigido.

**Bug encontrado e corrigido durante a validação:** com o índice único do nome, duas inserções simultâneas da
**mesma** conta (ex.: `/is create` repetido) eram confundidas com "nick de outra conta". Só apareceu ao forçar
os testes sem cache. O `upsert` agora usa `INSERT ... ON CONFLICT DO NOTHING` + `UPDATE` pelo UUID; dois
testes de regressão (12 inserções simultâneas da mesma conta; 6 contas disputando o mesmo nick: só 1 vence).

**Validado:** 85 testes (duas execuções completas, sem cache); `auth-e2e` 15/15, `protection-e2e` 30/30 e
`first-playable-e2e` 19/19, todos com o nLogin real; recusa de boot com `none` sem `dev-mode` (servidor
real); com `dev-mode` e **sem** nLogin, o pré-login do GreenSky recusou a outra grafia ("O nick 'Guard…' já
está registrado…") e o aviso forte apareceu no log. Depois desse teste, nLogin e config foram restaurados.

**Dados de teste no banco de DEV:** contas de bots (`Auth…`, `Login…`, `Guard…`, `Novo…`, `SetupAdmin`, Alice,
Bob, Carol, Erin) e as ilhas delas. A ilha da Alice chegou ao nível máximo (500); a parte de expansão do
`protection-e2e` passa a ser pulada (com aviso) até a ilha dela ser apagada.

---

## Reorganização de pastas (depois da Fase 7)

Pedido seu: nova estrutura, **sem alterar lógica, classes nem comportamento**, e sem nenhum comando git
durante a mudança. Plano mostrado e aprovado antes de mover.

**Movido:**
| Antes | Depois |
|---|---|
| `src/`, `build.gradle.kts` | `greensky/greensky-core/` |
| `settings.gradle.kts`, `gradle.properties`, `gradlew(.bat)`, `gradle/wrapper/` | `greensky/` |
| `tools/e2e/` | `greensky/tools/e2e/` |
| `run/` (servidor de teste, mundo, configs) | `server/` |
| `README.md — GreenSky.md` (Downloads) | `README.md` (cópia) |

**Apagado (com seu OK):** `run-26.2/` (ambiente antigo da 26.2), `.gradle/` e `build/` (cache e saída, regenerados).
**Intocados:** `.git/`, `.gitignore`, `.gitattributes`, `docker-compose.yml`, `.env`, `.env.example`, `INSTRUCOES.md`.

**Ajustes:**
- `greensky/settings.gradle.kts` inclui `greensky-api` (novo, vazio, ligado no Gradle) e `greensky-core`
  (todo o código atual, sem nenhuma alteração em `.java`). `greensky/build.gradle.kts` com o comum aos módulos.
- `greensky-core` depende de `greensky-api`; o jar continua `greensky-0.1.0-SNAPSHOT.jar`.
- Nova task `deploy`: copia o jar para `server/plugins/`, trocando a versão anterior.
- O wrapper não precisou de ajuste (só aponta para a URL do Gradle).
- `greensky/tools/e2e/lib.mjs`: o caminho do servidor (`server/`) agora é resolvido a partir do próprio arquivo.
- Novos: `build.bat`, `start-server.bat`, `libs/README.md`; `CLAUDE.md` reescrito com as regras do projeto.

**Problemas encontrados nos .bat, corrigidos:** (1) gravei com LF; `.bat` precisa de CRLF. (2) O ambiente
tinha `NoDefaultCurrentDirectoryInExePath=1`, então o `cmd` não achava o `gradlew.bat` na pasta atual; agora
o `build.bat` chama pelo caminho completo (`%~dp0greensky\gradlew.bat -p ...`).

**Validado depois da mudança:** `build.bat` compila os dois módulos, roda os 74 testes (nenhum pulado: o
`.env` foi lido e o banco usado) e copia o jar; `start-server.bat` sobe o Postgres e o Paper, o plugin carrega
e indexa as 4 ilhas que já existiam (mundo e banco preservados); `first-playable-e2e.mjs` rodado do novo
local: **19/19** (criar ilha, construir, expandir, visitar, privada, renascer, restart, persistir, continuar).

**Git (pedido seu, depois da reorganização):**
- Regra no `CLAUDE.md`: o Claude nunca faz commit, push ou qualquer comando que altere o repositório por
  conta própria; só quando você pedir. A reorganização **não** foi commitada.
- `.gitignore` atualizado: ignora `.env`, caches de build (`.gradle/`, `build/`), `node_modules/`,
  `libs/*.jar` e todo o `server/`, **exceto** `bukkit.yml`, `spigot.yml`, `commands.yml` e
  `config/paper-global.yml`/`paper-world-defaults.yml` (sem segredos preenchidos). O `server.properties`
  **não** é versionado: tem um `management-server-secret` gerado pelo servidor (e `online-mode=false` de teste).
- Conferido só com comandos de leitura: `.env` é ignorado (`git check-ignore`) e não está no índice
  (`git ls-files` vazio); `.env.example` continua versionável; mundo, jars, logs, `eula.txt`, `ops.json` e o
  config gerado do plugin ficam fora; o maior arquivo visível ao git tem 48 KB.

---

## Fase 0 — Análise

Sem código. Li o README, inspecionei a pasta e pesquisei a documentação oficial do Paper.
Entregue: estado atual, arquitetura, problemas do README, dependências, riscos, plano e primeira etapa.

Principais achados:
- O Paper mudou o esquema de versão para `26.x`; a 26.1 em diante exige Java 25.
- O exemplo `spacing: 512` conflita com ilhas de até 1000 blocos (sobreposição).
- O README tinha duas listas de fases conflitantes; ficou a das seções 154–172.
- A API de `ChunkGenerator`, `Player#setWorldBorder` e `Entity#teleportAsync` foi conferida na Javadoc.

Decisões suas: usar a última versão STABLE do Paper com build fixo, Gradle só pelo wrapper,
PostgreSQL via Docker, validar `spacing > max-size + margem`, adiar as Bridges de plugins
externos, criar pacotes só quando a fase chegar.

---

## Fase 1 — Core (`96336a7`)

**Escolha da versão (histórico).** Na época o Fill API mostrava o 26.3 só BETA e o 26.2 build 132
como último STABLE; foi o que usei nas fases 1 a 3. Depois foi trocado (ver "Troca de versão").

**Instalações (com seu OK):**
- JDK 25: `winget install EclipseAdoptium.Temurin.25.JDK` (Temurin 25.0.4.1).
- Gradle: nada global. Usei uma cópia temporária (checksum SHA-256 verificado) só para gerar o
  wrapper 9.8.0; o projeto roda com `./gradlew`.
- Jar do Paper 26.2-132 baixado para `run/` com SHA-256 conferido. Você aceitou o EULA da Mojang.

**Arquivos criados:**
- `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, wrapper: Java 25, paper-api, JUnit.
- `.gitignore` (ignora `build/`, `run/`, `.env`), `.gitattributes` (mantém `gradlew` com LF).
- `plugin.yml` clássico, `api-version: '26.2'`. Sem `folia-supported`.
- `GreenSkyPlugin`: carrega a config e se desabilita se ela for inválida.
- `core/config/`: `GreenSkyConfig`, `IslandSettings`, `ConfigException`. `IslandSettings` garante
  `spacing > max-size + spacing-margin` (com `long`, sem estouro de inteiro).
- `core/GreenScheduler`: wrapper dos schedulers global e async do Paper.
- `IslandSettingsTest` (6 testes).

**Desvios do plano:** módulo único (não há API para separar ainda); `plugin.yml` clássico em vez
do `paper-plugin.yml` experimental.

**Validado:** compila contra a API real, sobe e desliga limpo, e com `spacing: 512` o plugin se
recusa a iniciar.

---

## Fase 2 — Database (`5b72a61`)

**Versões pesquisadas no Maven Central:** HikariCP 7.1.0, PostgreSQL JDBC 42.7.13, Flyway 13.9.0,
Shadow 9.6.1; imagem `postgres:18-alpine`.

**Arquivos criados/alterados:**
- `docker-compose.yml`: Postgres só em `127.0.0.1:5432`, healthcheck, volume em `/var/lib/postgresql` (Postgres 18).
- `.env.example` (versionado) e `.env` (ignorado, senha aleatória local, não exibida).
- `build.gradle.kts`: dependências embutidas com Shadow e relocadas para `com.greencodes.greensky.libs.*`.
- `database/Database`: pool Hikari, migrations Flyway no boot, `query()` e `transaction()`
  assíncronos (commit/rollback), `ping()`, `close()`.
- `database/SqlFunction`, `database/DatabaseException`.
- `core/config/DatabaseSettings` e seção `database` no `config.yml`. A senha vem **só** de
  `GREENSKY_DB_PASSWORD`.
- `db/migration/V1__players.sql`: tabela `players`.
- `GreenSkyPlugin`: sem senha ou sem banco, o plugin se desabilita com mensagem clara.
- Testes: `DatabaseSettingsTest` (3) e `DatabaseIT` (6, contra Postgres real; ignorados sem a senha).

**Por que embutir as libs:** `libraries:` no `plugin.yml` baixa do Maven Central em runtime, o que
a doc do Paper descreve como contra os termos do Maven Central e deixa o boot dependente de internet.

**Problemas resolvidos:**
- Flyway 13 removeu `.classLoader()`; confirmei na jar e usei `new FluentConfiguration(loader)`.
- O Shadow podia descartar service files duplicados, perdendo o suporte a PostgreSQL do Flyway.
  Corrigi com `DuplicatesStrategy.INCLUDE` e inspecionei o jar final.

**Validado:** boot feliz (migration aplicada, `flyway_schema_history` com `success = t`),
segundo boot sem reaplicar, sem senha, e com o banco parado.

---

## Fase 3 — World (`16d7d7f`)

**APIs conferidas na jar com `javap`:** `WorldCreator`, `Server.createWorld`, `ChunkGenerator`,
`BiomeProvider`, `World`.

**Arquivos criados/alterados:**
- `generation/VoidGenerator`: não sobrescreve nenhuma etapa; todos os `shouldGenerate*` ficam
  `false`. Spawn fixo e `canSpawn` sempre `false`.
- `generation/SingleBiomeProvider`: bioma único (planície).
- `world/WorldManager`: cria/carrega o mundo a cada boot e confirma que o chunk 0,0 é void.
- `world/SkyWorld`: encapsula o `World` e o spawn.
- `core/config/WorldSettings` e seção `world` no `config.yml`: nome validado (`[a-z0-9_-]`,
  até 32; `world`, `world_nether`, `world_the_end` reservados) e `spawn-y` entre -63 e 318.
- `GreenSkyPlugin`: carrega o mundo depois do banco; falha desabilita o plugin.
- `build.gradle.kts`: paper-api também no classpath de teste.
- Testes: `WorldSettingsTest` (4) e `VoidGeneratorTest` (2).

**Descobertas:**
- Mundos de plugin ficam em `world/dimensions/minecraft/<nome>/` no Paper 26.
- `keepSpawnLoaded` não funciona mais (sem chunks de spawn desde a 1.21.9); removi a chamada.
- `World.save()` manual dispara WARN; removi.

**Validado:** mundo criado, chunk 0,0 com 24/24 seções vazias, e um bloco colocado persistiu
após restart (positivo em y=100, negativo em y=99).

---

## Fase 4 — Islands

Feita já na Paper 26.1.2. Decisão minha (você não escolheu): a ilha inicial é **gerada por código**
atrás da interface `IslandBuilder`, para trocar por template/schematic depois sem mexer no resto.

**APIs conferidas na jar:** `World.getChunkAtAsync`, `Entity.teleportAsync`, `Block.setType(Material, boolean)`,
`generateTree` (a versão sem `Random` é deprecated; troquei por `generateTree(Location, Random, TreeType)`).

**Banco (`V2__islands.sql`):** sequência `island_slot_seq`, tabelas `islands` (com `state` PENDING/READY e
índice único por dono), `island_members` e `island_permissions` (cascade). O centro e o tamanho ficam
gravados, então mudar `spacing` no config não move ilhas existentes.

**Código novo:**
- `island/IslandRegion`: área da ilha e espiral quadrada de slots (slot 0 = spawn, reservado).
- `island/Island`, `IslandMember`, `IslandRole` (OWNER/MEMBER), `IslandPermission` (BUILD, BREAK, INTERACT,
  CONTAINERS, INVITE, KICK, MANAGE_PERMISSIONS), `IslandState`, `IslandException`.
- `island/IslandRepository`: só SQL, com `Connection` para compor transações.
- `island/IslandService`: regras, tudo assíncrono, cache dono -> ilha. Criação = transação (slot + linha +
  dono) -> gera blocos -> READY. Falha no meio deixa PENDING e `ensureReady` refaz.
- `island/IslandBuilder` + `StarterIslandBuilder`: disco de grama sobre terra afunilando, bedrock no centro,
  árvore com semente fixa (refazer gera a mesma árvore). Carrega só os chunks do disco.
- `island/IslandCommand`: `/island` (`/is`): create, home, info, add, remove e `admin create <nick>`.
- `player/PlayerRepository`: upsert em `players`.
- Config: `islands.base-y` e `islands.starter-radius` (validados: o diâmetro cabe no tamanho inicial).
- `plugin.yml`: comando `island` (alias `is`) e permissão `greensky.admin` (op).
- `build.gradle.kts`: `-Xlint:deprecation` ligado.

**Testes:** 37 no total. Novos: `IslandRegionTest` (8: espiral única e completa, nenhuma sobreposição no
tamanho máximo, spawn reservado) e `IslandServiceIT` (7, no Postgres real: criação, dono único, 8 criações
simultâneas do mesmo dono = 1 ilha, 24 donos simultâneos = slots distintos, recuperação de ilha PENDING,
membros/permissões e regras de autorização).

**Validado no Paper 26.1.2:** `admin create` cria a ilha, a segunda tentativa do mesmo jogador é recusada,
um segundo jogador ganha outra ilha; depois de um restart a grama, terra, bedrock, árvore (oak_log) e o
vazio ao redor estão corretos, e o banco mostra as ilhas READY.

**Não testado (precisa de cliente real):** `/is create`, `/is home` (teleportAsync), `/is info`, `/is add|remove`.

**Notas:** os testes de integração consomem números da sequência, por isso as ilhas de teste ficaram em slots
altos (77 e 79, buracos são normais). As ilhas de teste (`tester1`, `Tester2`) continuam no banco e no mundo
de teste (`run/`). O nome gravado de `tester1` saiu em minúsculas; só afeta exibição.

---

## Fase 7 — First Playable

Objetivo do README (seção 161): entrar -> criar ilha -> construir -> expandir -> visitar -> voltar -> persistir
-> restart -> continuar. O que faltava era **visitar** e **voltar**; o resto já existia e foi validado junto.

**APIs conferidas na jar 26.1.2:** `PlayerRespawnEvent` (`setRespawnLocation`, `isBedSpawn`, `isAnchorSpawn`,
`getRespawnReason`), `World#getSpawnLocation`.

**O que foi feito:**
- `V3__island_settings.sql`: tabela `island_settings` (prevista no README) com a visibilidade; ilhas que já
  existiam ficam públicas.
- `islands.default-visibility: public` no config (validado: public/private).
- `IslandService`: `visibility`, `setVisibility` (só o dono) e `authorizeVisit` (pública: qualquer um;
  privada: só membros; quem não tem ilha não pode ser visitado). Ilha nova grava a visibilidade padrão.
- Comandos `/is visit <nick>`, `/is public`, `/is private`.
- `visit/VisitorExpeller`: ao ficar privada, visitantes não-membros que estão na ilha vão ao spawn do servidor.
- `visit/HomeListener`: quem morre renasce na própria ilha (cama/âncora têm prioridade), sem consultar o banco
  (usa as participações já carregadas); quem entra sem ilha recebe "Use /is create".
- Corrigi um desenho meu durante a fase: o listener de respawn estava no pacote `island`, o que criaria
  dependência circular com `protection`. Movido para `visit`.
- "Amigos" (README seção 93) ficou de fora: ainda não há sistema de amigos (fase social).

**Testes:** 74 no total. Novos: 3 em `IslandServiceIT` (regras de visita e persistência da visibilidade,
notificação, padrão configurado), 1 em `IslandProtectionServiceTest`, 1 em `GreenSkyConfigTest`.

**Teste com bots (`first-playable-e2e.mjs`, 19/19), com um jogador novo a cada execução:** recebe a dica,
cria a ilha, constrói, a ilha é expandida e ele constrói na área nova (borda de 150), Carol visita a ilha
pública e não constrói, a ilha fica privada e Carol é avisada e levada ao spawn, nova visita é recusada,
Erin (membro) visita mesmo privada, o dono morre e renasce na ilha. **Servidor reiniciado** e então: mesma
ilha (centro e tamanho), sem a dica de novato, blocos continuam, `/is home` e a borda de 150 funcionam, o
dono constrói, a ilha continua privada e a Erin continua membro e constrói.

As funções de apoio dos testes foram para `tools/e2e/lib.mjs` (subir/reiniciar servidor, bots, conferência
de blocos); o roteiro de proteção continua passando (30/30) depois da mudança.

---

## Fase 6 — Expansion

**APIs conferidas na jar 26.1.2:** `Server#createWorldBorder`, `WorldBorder#setCenter/setSize/setWarningDistance`,
`Player#setWorldBorder/getWorldBorder`, eventos de join, teleporte, troca de mundo e respawn.

**O que foi feito:**
- Config `islands.expansion-levels: [100, 150, 200, 300, 500]` (`ExpansionSettings`), validada: começa em
  `initial-size`, estritamente crescente, último nível até `max-size` (preserva a garantia de não sobreposição).
- `IslandService.expand`: sobe um nível mantendo o centro. Não regenera nada (mundo void). Uma `UPDATE`
  condicional no tamanho antigo garante que só uma de duas expansões simultâneas vale; a outra recebe
  `EXPANSION_CONFLICT` e o cache é descartado. Não há o que recuperar após crash (ou a linha mudou, ou não).
- `IslandListener.onIslandExpanded`: a proteção troca a região no índice na hora; a borda de quem está na ilha cresce.
- `border/IslandBorderService` + `IslandBorderListener`: WorldBorder por jogador, só visual, do tamanho da região;
  fora de ilhas o jogador vê a borda normal. Atualiza em join, teleporte, troca de mundo, respawn e expansão.
- Comando `/is admin expand <nick>`; `/is info` mostra o nível. Sem custo e sem comando de jogador ainda
  (precisa da economia, fase 15).

**Testes:** 69 no total. Novos: `ExpansionSettingsTest` (3), 1 em `GreenSkyConfigTest`, 1 em
`IslandProtectionServiceTest` (área nova liberada na hora) e 3 em `IslandServiceIT` (todos os níveis até o
máximo mantendo o centro; 8 expansões simultâneas = 1 aplicada; objeto antigo não pula nível).

**Teste com bots:** 30/30 em duas execuções (100 -> 150 e 150 -> 200). Novos checks: borda recebida pelo
cliente (pacotes reais `world_border_size`/`world_border_center`) com tamanho e centro da região; fora da ilha
volta à borda normal; antes da expansão o dono não constrói 3 blocos além da borda, depois constrói; a borda
cresce na hora com o mesmo centro; a ilha não se move; visitante continua bloqueado.

**Correção de uma afirmação minha (Fase 5):** eu disse que uma chave ausente no config.yml "virava 0 em
silêncio". Testando a atualização no servidor real, o plugin subiu sem `expansion-levels` no config.yml antigo:
o `getConfig()` do Bukkit usa o config.yml de dentro do jar como padrão. Então chave ausente recebe o padrão.
O `requireInt` continua útil para **tipo errado**, que antes caía no padrão em silêncio.

---

## Fase 5 — Protection

**APIs conferidas na jar 26.1.2:** as 27 classes de evento usadas existem. `TeleportCause.CHORUS_FRUIT` está
marcado para remoção; usei `CONSUMABLE_EFFECT` (substituto indicado na Javadoc).

**Código novo (`protection/`):**
- `IslandIndex`: índice espacial em memória (baldes de 512x512); acha a ilha de um bloco sem consultar o banco
  e não depende do `spacing` do config. Pronto para a expansão (substitui a região da ilha).
- `IslandProtectionService`: decide tudo sem Bukkit (testável). Nega tudo antes de carregar e fora de ilhas;
  dentro, só membros com a permissão; `sameIsland` para efeitos de mundo.
- `PlayerProtectionListener`: quebrar, construir, baldes, interagir com blocos, abrir containers, entidades
  (animais, aldeões, quadros, armor stands, veículos), isqueiro, e fuga por pérola/fruta do coro.
- `WorldProtectionListener`: explosões, pistões, líquidos, hoppers, fogo natural, crescimento de árvores,
  dispensers e entidades que mudam blocos não cruzam a fronteira da ilha.
- `ProtectionSessionListener`: carrega participações no join, limpa no quit. `DenyNotifier`: aviso na action bar (1/s).
- `IslandService`: `loadAll()`, `membershipsOf()` e `IslandListener` (notifica só depois de gravar; listener com
  defeito não derruba a operação). `IslandRepository`: `findAll` e `membershipsOf`.
- `plugin.yml`: permissão `greensky.admin.bypass` (op).

**Testes:** `IslandIndexTest` (6), `IslandProtectionServiceTest` (10) e 3 novos em `IslandServiceIT`.

**Teste com bots reais (`tools/e2e/protection-e2e.mjs`, mineflayer 4.39.0):** sobe o Paper 26.1.2, conecta
Alice e Bob e confere cada efeito no console do servidor. **22/22, em duas execuções seguidas:**
create/home/info com jogador real, segunda ilha recusada, dono constrói e quebra, visitante não constrói,
não quebra, não abre baú e a pérola não o teleporta; depois de `/is add` o Bob pode tudo isso; depois de
`/is remove` volta a ser bloqueado; `/is home` traz de volta; explosão fora da ilha não destrói nada e
dentro não cruza a fronteira; água não sai da região; pistão não empura para fora (com controle dentro).
Para os bots entrarem, o `run/server.properties` de teste usa `online-mode=false` e `server-ip=127.0.0.1`.

**Erros meus no teste, corrigidos:** o check da segunda ilha pegava a mensagem "Criando a ilha..."; o teste
da água tinha piso de 1 bloco (a água caía pelos lados e nunca andava no eixo testado, o que deixava o check
de "não sai" vazio); e o teste não era repetível (estado da execução anterior). Nenhum era bug do plugin.

**Não testado de ponta a ponta:** hoppers, fogo, crescimento de árvore, dispenser, entidades (quadros, armor
stands, animais, veículos), fruta do coro e containers além do baú. A lógica de decisão deles está nos testes
unitários; a ligação evento -> decisão só foi exercitada nos casos acima.

**Lacunas conhecidas:** ver "Armadilhas" no `CLAUDE.md`.

## Revisão das fases anteriores (junto com a Fase 5)

Reli o código das fases 1 a 4. Corrigido:
1. **Config com chave ausente virava 0 em silêncio** (`getInt`). Um config.yml antigo sem `islands.base-y`
   colocaria ilhas em y=0. Agora toda chave é obrigatória e o boot falha dizendo qual falta.
   Novo `GreenSkyConfigTest` (5) lê o `config.yml` real do jar.
2. **`/is remove` e `/is admin create` usavam `getOfflinePlayer(String)`**, que pode consultar a Mojang pela
   rede na thread do servidor (proibido pelo README). Trocado por `getOfflinePlayerIfCached`; nome nunca
   visto gera mensagem clara. Isso também resolve o nome `tester1` gravado em minúsculas.
3. **Tab completion** de `/is add|remove` agora sugere jogadores online.

Revisado sem mudança: transações e rollback do `Database`, ordem de fechamento no disable, espiral e
regiões, recuperação de ilha PENDING, relocação do Shadow, `.env` fora do git.

---

## Troca de versão: 26.2 -> 26.1.2 (depois da Fase 3)

**Motivo:** você quis um servidor mais leve para o PC de quem joga. Pesquisei a 1.20.6 (último
build em out/2024, sem correções há ~2 anos) e a 1.21.11 (último build em mai/2026). Comecei a
migrar para a 1.21.11, mas você interrompeu e pediu a **26.1.2**.

**Verificado no Fill API:** a 26.1.2 existe, os builds 53 a 74 são STABLE e o mais novo é o **74**
(`paper-26.1.2-74.jar`, SHA256 `1d70b1dab9cf4a6de615209a536f3a45a2186240253c428213ce2188ab95e5f7`,
conferido). O Fill marca a versão como **UNSUPPORTED desde 2026-07-26**: não recebe builds novos.
Você confirmou usar mesmo assim.

**Alterações:**
- `build.gradle.kts`: `paper-api:26.1.2.build.74-stable` (compileOnly e teste); `release` 25.
- `plugin.yml`: `api-version: '26.1'`.
- `.gitignore`: `run-*/`.
- Ambiente de teste: o `run/` da 26.2 foi movido para `run-26.2/` (mundos novos não abrem em
  servidor mais antigo); novo `run/` com `paper-26.1.2-74.jar`.
- Nenhum código Java mudou: compilou direto contra a API 26.1.2.

**Validado na 26.1.2:** compila, 21 testes passam, o servidor se identifica como `26.1.2-74`,
mundo void (24/24 seções vazias), bloco persistido após restart. O Paper avisa que há 2 releases
mais novas (26.2).

**Plano ViaVersion (não implementado):** ver `CLAUDE.md`, seção "Plano ViaVersion". Resumo:
ViaVersion + ViaBackwards 5.12.0 (mesma versão) em `plugins/`, testar com clientes 1.21.x reais e
medir com spark. Não confirmado: até que 1.21.x antigo o ViaBackwards 5.12.0 aceita.

## Como reproduzir

Na raiz `green-sky\`:
```bat
build.bat
start-server.bat
```
`build.bat` sobe o Postgres, compila, testa e copia o jar para `server\plugins`; `start-server.bat` sobe o
Postgres e o Paper de teste. Testes com bots: `cd greensky\tools\e2e && npm install --ignore-scripts && npm test`.

## Pendências

- **Fase 8 (Gameplay)** aguardando sua aprovação.
- Expansão por jogador (com custo) depende da economia.
- Teste com bots cobre create/home/info/add/remove; falta um teste manual com cliente Minecraft de verdade.
- `verifyVoid()` avisa se o chunk 0,0 tem blocos (no `server/` há um bloco de ouro de teste); em produção o slot 0 é reservado, então o chunk fica vazio.
- A reorganização e a autenticação ainda não foram commitadas (só quando você pedir).
- Produção com nLogin: definir `/nlogin spawn set join` (coordenadas antes do login) e decidir diálogos vs comandos.
- Decidir se o mundo `world` padrão usará o gerador void via `bukkit.yml`.
- Paper 26.1.2 está sem suporte no Paper: reavaliar 26.2/26.3.
- Implementar ViaVersion/ViaBackwards (planejado) e testar com clientes reais.
