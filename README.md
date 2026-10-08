# GreenSky

> Um SkyBlock RPG vivo, explorável e social, onde a ilha é apenas o começo.

---

# 1. VISÃO

O GreenSky será um servidor Minecraft RPG baseado em SkyBlock, construído sobre Paper.

A proposta NÃO é criar mais um servidor onde o jogador:

```text
quebra bloco
↓
vende
↓
compra upgrade
↓
quebra mais bloco
↓
repete
```

Esse modelo é repetitivo e não deve ser o núcleo do servidor.

O GreenSky deve parecer um **mundo RPG dentro do Minecraft**, onde cada jogador possui uma ilha que evolui e funciona como sua base, mas existe um mundo muito maior para descobrir.

O jogador deverá ter motivos para:

- voltar amanhã;
- explorar;
- construir;
- colecionar;
- descobrir segredos;
- evoluir;
- jogar com amigos;
- competir;
- participar de eventos;
- completar objetivos;
- personalizar sua ilha;
- descobrir novas regiões;
- enfrentar bosses;
- pescar;
- cultivar;
- encontrar itens extremamente raros.

A ilha é a casa.

O mundo é a aventura.

---

# 2. OBJETIVO

Criar um servidor:

- bonito;
- diferente;
- extremamente otimizado;
- escalável;
- divertido;
- social;
- com progressão longa;
- com conteúdo para iniciantes e endgame;
- monetizável sem destruir o gameplay;
- tecnicamente profissional;
- preparado para múltiplos servidores.

---

# 3. PRINCÍPIO FUNDAMENTAL

## NÃO queremos um servidor de mineração.

Mineração pode existir.

Porém, ela será apenas uma atividade entre várias.

O jogador nunca deve sentir que existe apenas uma maneira de progredir.

O jogador poderá escolher seu caminho.

Exemplo:

```text
              GREEN SKY
                   │
       ┌───────────┼───────────┐
       │           │           │
    Farming     Fishing    Exploration
       │           │           │
       └───────────┼───────────┘
                   │
             Collections
                   │
            ┌──────┼──────┐
            │      │      │
          Bosses Quests Relics
            │      │      │
            └──────┼──────┘
                   │
             Island Level
                   │
             New Content
```

---

# 4. A EXPERIÊNCIA DO JOGADOR

O começo do servidor deve ser extremamente bem feito.

O jogador entra.

Recebe uma pequena ilha.

Existe um objetivo simples.

Mas rapidamente descobre que existe muito mais.

Exemplo:

```text
Primeiros minutos
↓
Criar ilha
↓
Conhecer sistema
↓
Construir primeira área
↓
Descobrir NPC
↓
Receber primeira missão
↓
Encontrar portal
↓
Descobrir primeira região
↓
Pescar
↓
Encontrar item raro
↓
Desbloquear coleção
↓
Voltar para ilha
↓
Melhorar ilha
↓
Explorar novamente
```

O tutorial não deve ser uma parede de texto.

O jogo deve ensinar jogando.

---

# 5. ILHA

A ilha é o centro da experiência.

Ela deverá possuir:

- casa;
- áreas agrícolas;
- construções;
- NPCs;
- armazenamento;
- decoração;
- máquinas;
- áreas de produção;
- museu;
- coleções;
- troféus;
- portais;
- áreas desbloqueáveis;
- espaços para amigos.

A ilha deverá evoluir visualmente.

Não deve ser apenas:

```text
100x100
200x200
300x300
```

A expansão deve desbloquear novas possibilidades.

---

# 6. EXPANSÃO DA ILHA

Exemplo:

```text
Nível 1
100x100
Starter

↓

Nível 2
150x150
Jardim

↓

Nível 3
200x200
Floresta

↓

Nível 4
300x300
Ruínas

↓

Nível 5
500x500
Área especial

↓

Endgame
Grande território personalizado
```

O tamanho deve permanecer configurável.

A ilha nunca deve ser movida.

Somente sua área desbloqueada aumenta.

---

# 7. ARQUITETURA DAS ILHAS

Não criar um mundo para cada jogador.

Não criar:

```text
world-player-001
world-player-002
world-player-003
```

Utilizar um ou poucos mundos SkyBlock.

Exemplo:

```text
SkyWorld
│
├── Island A
├── Island B
├── Island C
├── Island D
├── Island E
└── ...
```

As ilhas devem ocupar regiões físicas diferentes.

Cada ilha possui:

- centro;
- região;
- tamanho;
- chunks;
- owner;
- membros;
- permissões;
- nível;
- progressão.

---

# 8. WORLD BORDER

O Paper possui suporte a `WorldBorder` virtual e permite definir um border diferente para cada jogador. Isso deve ser avaliado para criar uma experiência de ilha individual mesmo com várias ilhas dentro do mesmo mundo.

Exemplo:

```text
Player A
Border → Ilha A

Player B
Border → Ilha B

Player C
Border → Ilha C
```

Porém:

**WorldBorder NÃO é o sistema de proteção.**

A proteção real será feita pelo GreenSky.

---

# 9. PROTEÇÃO

Criar:

```text
IslandProtectionService
```

Responsável por controlar:

- construção;
- quebra;
- interação;
- containers;
- redstone;
- líquidos;
- explosões;
- pistões;
- hoppers;
- entidades;
- veículos;
- projéteis;
- teleporte;
- mecanismos de fuga.

Deve existir uma camada robusta contra exploits.

---

# 10. WORLD GENERATION

O mundo SkyBlock será void/customizado.

Utilizar `ChunkGenerator` do Paper para controlar a geração. A API moderna de `ChunkGenerator` divide a geração de chunks em etapas, permitindo construir um gerador próprio em vez de depender da geração vanilla completa.

Objetivo:

```text
SkyWorld
↓
Void Generator
↓
Island Generator
↓
Island Templates
↓
Dynamic Expansion
```

Não gerar terreno vanilla desnecessário.

Não gerar o mundo inteiro antecipadamente.

---

# 11. PERFORMANCE

Performance é requisito fundamental.

O GreenSky deve ser desenvolvido pensando em:

- baixo consumo de CPU;
- baixo consumo de RAM;
- poucos chunks ativos;
- baixo número de entidades;
- banco assíncrono;
- cache;
- operações não bloqueantes;
- controle de geração;
- controle de redstone;
- controle de hoppers;
- controle de mobs;
- controle de partículas;
- controle de loops.

Nunca fazer:

```text
SQL query por tick
```

Nunca fazer:

```text
scan gigantesco de blocos por tick
```

Nunca carregar todas as ilhas simultaneamente.

---

# 12. PAPER

Paper é a plataforma principal.

A implementação deve priorizar a API do Paper em vez de depender de NMS desnecessariamente.

Paper existe justamente como servidor Minecraft com melhorias de desempenho e uma API mais avançada para plugins.

---

# 13. PREPARAÇÃO PARA FOLIA

O servidor será desenvolvido primeiro para Paper.

Porém, a arquitetura deverá evitar decisões que impossibilitem uma futura adaptação para Folia.

Não marcar o plugin como compatível com Folia simplesmente para dizer que é compatível.

A própria documentação do Paper alerta que suporte real ao Folia exige desenvolvimento específico, principalmente por causa dos schedulers e do modelo de regiões.

Portanto:

```text
Paper primeiro
↓
arquitetura preparada
↓
Folia futuramente se fizer sentido
```

---

# 14. CHUNKS

O sistema deve trabalhar com chunks de forma inteligente.

Quando uma ilha aumenta:

```text
100x100
↓
200x200
```

não regenerar tudo.

Somente preparar os novos chunks necessários.

Quando uma ilha fica inativa:

- chunks podem descarregar;
- dados permanecem persistidos;
- ilha continua existindo.

---

# 15. BANCO DE DADOS

PostgreSQL será usado para metadados.

Não salvar blocos individuais no banco.

Banco:

```text
players
islands
island_members
island_permissions
island_settings
island_upgrades
island_statistics
quests
quest_progress
collections
player_progression
transactions
seasons
```

World/chunks:

```text
Minecraft/Paper
```

---

# 16. REDIS

Redis será adicionado quando o servidor estiver preparado para múltiplas instâncias.

Uso futuro:

- sincronização;
- eventos;
- presença;
- mensagens;
- transferência de ilha;
- comunicação entre servidores.

---

# 17. VELOCITY

Arquitetura futura:

```text
                    Velocity
                       │
       ┌───────────────┼───────────────┐
       │               │               │
     Lobby           Sky-01          Sky-02
                       │               │
                    Islands         Islands
```

Cada servidor possui autoridade sobre suas próprias ilhas.

Uma ilha não pode ser modificada simultaneamente por dois servidores.

---

# 18. EXPLORAÇÃO

Esta é uma das principais diferenças do GreenSky.

O mundo não deve ser apenas uma sequência de menus.

O jogador deve sair da ilha.

Existirão regiões exploráveis.

Exemplo:

```text
STARTER ISLAND
      │
      ▼
MEADOWS
      │
      ├── Forest
      │
      ├── Lake
      │
      └── Ruins
            │
            ▼
       ANCIENT FOREST
            │
            ▼
        LOST TEMPLE
            │
            ▼
        UNDERGROUND
            │
            ▼
        MYSTIC LANDS
```

---

# 19. REGIÕES

Cada região deverá possuir identidade própria.

Uma região não deve ser simplesmente:

```text
bioma diferente
+ mobs diferentes
```

Ela deve ter:

- música;
- ambientação;
- NPCs;
- história;
- recursos;
- quests;
- criaturas;
- coleções;
- segredos;
- bosses;
- eventos;
- itens exclusivos.

---

# 20. REGIÃO 1 — MEADOWS

Primeira região.

Visual:

- campos;
- árvores;
- pequenas montanhas;
- rios;
- vilarejo.

Atividades:

- agricultura;
- pesca;
- coleta;
- primeiras quests.

NPCs:

- agricultor;
- pescador;
- comerciante;
- explorador.

Objetivo:

ensinar o jogador sem parecer tutorial.

---

# 21. REGIÃO 2 — ENCHANTED FOREST

Floresta misteriosa.

Características:

- árvores gigantes;
- cogumelos;
- criaturas raras;
- plantas especiais;
- caminhos escondidos.

Mecânica:

Algumas áreas aparecem apenas durante determinadas condições.

Exemplo:

```text
Noite
+
Lua cheia
=
entrada secreta aparece
```

---

# 22. REGIÃO 3 — ANCIENT RUINS

Ruínas antigas espalhadas pelo mundo.

Possibilidades:

- puzzles;
- baús;
- inscrições;
- relíquias;
- mobs antigos;
- mini bosses.

O jogador deve ter motivos para voltar.

---

# 23. REGIÃO 4 — UNDERGROUND

Não tratar como uma simples mina.

A área subterrânea deverá ser uma zona de exploração.

Possibilidades:

- cavernas;
- rios subterrâneos;
- criaturas;
- cristais;
- ruínas;
- templos;
- passagens secretas.

Mineração existe.

Mas é apenas uma ferramenta para explorar.

---

# 24. REGIÃO 5 — MYSTIC LANDS

Endgame.

Região extremamente rara.

Possibilidades:

- criaturas únicas;
- bosses;
- recursos raríssimos;
- eventos;
- quests;
- artefatos;
- puzzles.

---

# 25. EXPLORAÇÃO DINÂMICA

O mundo deve possuir acontecimentos.

Exemplos:

```text
Meteor caiu!
```

O jogador recebe uma notificação.

Um meteoro aparece em uma região.

Jogadores vão até lá.

O evento possui:

- inimigos;
- recursos;
- recompensa;
- boss;
- ranking.

---

# 26. EVENTOS ALEATÓRIOS

Exemplos:

## Meteor Shower

Meteoros aparecem.

## Treasure Hunt

Tesouros são escondidos.

## Ancient Portal

Portal aparece temporariamente.

## Fishing Festival

Peixes raros aparecem.

## Boss Invasion

Boss invade determinada região.

## Harvest Festival

Agricultura recebe bônus.

## Blood Moon

Criaturas especiais aparecem.

---

# 27. SEGREDOS

O mundo deve possuir segredos.

Exemplo:

```text
placa escondida
↓
puzzle
↓
porta secreta
↓
sala
↓
relíquia
```

Isso gera exploração orgânica.

---

# 28. RELÍQUIAS

Itens extremamente raros.

Cada relíquia possui:

- nome;
- lore;
- raridade;
- origem;
- história;
- coleção;
- possibilidade de exposição.

Exemplo:

```text
Relic:
Heart of the Ancient Forest
```

---

# 29. MUSEU

Cada ilha pode possuir um museu.

O jogador coloca:

- relíquias;
- peixes raros;
- troféus;
- itens de boss;
- descobertas.

O museu mostra progresso.

---

# 30. COLEÇÕES

Coleções serão uma das principais formas de progressão.

Exemplo:

```text
FISHING
├── Common Fish
├── Rare Fish
├── Legendary Fish
└── Mythical Fish
```

Outra:

```text
EXPLORATION
├── Meadows
├── Forest
├── Ruins
├── Underground
└── Mystic Lands
```

Outra:

```text
BOSSES
├── Forest Guardian
├── Ancient Golem
├── Abyssal Beast
└── ...
```

---

# 31. BESTIÁRIO

O jogador descobre criaturas.

Cada criatura possui:

- nome;
- raridade;
- habitat;
- drops;
- lore;
- quantidade derrotada.

Isso cria progressão adicional.

---

# 32. PESCA

Pesca será uma atividade completa.

Peixes terão:

- raridade;
- tamanho;
- qualidade;
- região;
- horário;
- clima;
- chance;
- coleção.

Alguns peixes poderão aparecer apenas:

```text
durante tempestade
```

ou:

```text
durante lua cheia
```

---

# 33. AGRICULTURA

Agricultura terá progressão própria.

Possibilidades:

- sementes raras;
- qualidade;
- mutações;
- plantas especiais;
- estufas;
- fertilizantes;
- contratos;
- eventos.

---

# 34. QUESTS

As quests devem contar pequenas histórias.

Evitar:

```text
mate 10 mobs
```

como única estrutura.

Exemplo:

```text
NPC encontra uma carta antiga
↓
manda investigar ruínas
↓
jogador encontra pista
↓
encontra entrada secreta
↓
resolve puzzle
↓
encontra relíquia
↓
volta ao NPC
↓
desbloqueia nova região
```

---

# 35. QUEST CHAINS

As quests podem formar cadeias.

```text
Quest 1
↓
Quest 2
↓
Quest 3
↓
Quest 4
↓
Boss
↓
Nova região
```

---

# 36. BOSSES

Bosses não devem ser mobs com HP absurdo.

Cada boss deve possuir mecânicas.

Exemplo:

```text
FOREST GUARDIAN
```

Fase 1:

- ataques básicos.

Fase 2:

- raízes surgem.

Fase 3:

- arena começa a mudar.

Fase 4:

- invoca criaturas.

Fase 5:

- ataque especial.

O jogador deve precisar reagir.

---

# 37. DUNGEONS

Futuro sistema de instâncias.

Cada dungeon pode possuir:

- entrada;
- salas;
- puzzles;
- mobs;
- mini bosses;
- boss final;
- recompensas.

As instâncias podem ser temporárias.

Não criar uma ilha permanente para cada grupo.

---

# 38. RAID

Conteúdo para grupos.

Exemplo:

```text
4-8 jogadores
↓
entrada
↓
salas
↓
eventos
↓
boss
↓
loot
```

---

# 39. CLANS

Clans podem possuir objetivos coletivos.

Exemplos:

- ranking;
- eventos;
- quests;
- bosses;
- competições.

---

# 40. COMPETIÇÕES

Eventos entre jogadores:

- melhor ilha;
- melhor museu;
- maior coleção;
- maior nível;
- melhor decoração;
- pesca;
- agricultura;
- boss damage.

---

# 41. TEMPORADAS

Temporadas devem introduzir conteúdo novo.

Exemplo:

```text
SEASON 1
The Lost Civilization
```

Conteúdo:

- quests;
- região;
- boss;
- coleção;
- cosméticos;
- eventos.

A temporada termina.

Mas parte do conteúdo pode permanecer.

---

# 42. NÃO RESETAR O PROGRESSO SEM MOTIVO

Temporadas não devem significar automaticamente:

```text
apagar tudo
```

O jogador deve sentir que seu progresso possui valor.

---

# 43. ECONOMIA

Criar uma economia que tenha múltiplos usos.

Moeda principal:

```text
Coins
```

Moedas secundárias somente quando realmente necessárias.

Exemplo:

```text
Coins
→ mercado
→ upgrades
→ construção
→ NPCs
```

---

# 44. MERCADO

Futuramente:

```text
Player
↓
Market
↓
Player
```

Jogadores podem comercializar itens.

Criar proteção contra:

- duplicação;
- exploits;
- transações inconsistentes.

---

# 45. PROGRESSÃO

A progressão deve ser horizontal e vertical.

Vertical:

```text
level
```

Horizontal:

```text
collections
exploration
fishing
bosses
museum
achievements
```

Isso permite jogadores diferentes terem objetivos diferentes.

---

# 46. ENDGAME

Endgame não deve significar apenas:

```text
número maior
```

Deve possuir:

- coleções extremamente raras;
- bosses difíceis;
- regiões secretas;
- puzzles;
- achievements;
- decoração;
- ranking;
- eventos;
- conteúdo cooperativo.

---

# 47. COSMÉTICOS

Monetização principal.

Exemplos:

- partículas;
- pets;
- trails;
- títulos;
- tags;
- animações;
- skins da ilha;
- temas;
- céu da ilha;
- músicas;
- efeitos de entrada;
- efeitos de teleporte;
- efeitos de morte;
- emotes.

---

# 48. TEMAS DE ILHA

Exemplos:

```text
Floating Castle
Cyberpunk
Japanese
Fantasy
Volcano
Pirate
Underwater
Ancient Temple
Space
Fairy
Steampunk
```

Temas podem alterar:

- estruturas;
- partículas;
- ambiente;
- decoração;
- música.

---

# 49. SKYBOX / CÉU

Personalização visual.

Exemplos:

- noite estrelada;
- aurora;
- espaço;
- pôr do sol;
- céu mágico.

---

# 50. PETS

Pets cosméticos.

Eles podem:

- acompanhar;
- possuir animações;
- emitir partículas;
- ter sons.

Não devem fornecer vantagem absurda.

---

# 51. MONETIZAÇÃO

Monetização deve ser desenhada desde o início, mas sem destruir a economia.

Prioridade:

```text
COSMÉTICOS
↓
PERSONALIZAÇÃO
↓
CONVENIÊNCIA
↓
CONTEÚDO OPCIONAL
```

Evitar:

```text
pague para ser impossível competir
```

---

# 52. LOJA

A loja poderá possuir:

```text
GreenSky Store
```

Categorias:

```text
Ranks
Cosmetics
Island Themes
Pets
Particles
Titles
Bundles
Season Pass
Convenience
```

---

# 53. RANKS

Ranks podem fornecer:

- cosméticos;
- comandos convenientes;
- slots;
- efeitos;
- personalização.

Evitar vantagens econômicas absurdas.

---

# 54. SEASON PASS

Cada temporada poderá possuir:

```text
Free Track
Premium Track
```

Free:

- recompensas normais;
- cosméticos;
- itens.

Premium:

- cosméticos adicionais;
- temas;
- pets;
- efeitos;
- títulos.

---

# 55. BUNDLES

Exemplo:

```text
Explorer Bundle
```

Inclui:

- título;
- pet;
- partículas;
- tema de ilha;
- cosmético.

---

# 56. MONETIZAÇÃO INTELIGENTE

O jogador deve comprar porque:

> "Isso é muito bonito."

e não:

> "Se eu não comprar, não consigo jogar."

---

# 57. LOOT BOXES / CRATES

Se existirem:

- transparência;
- chances visíveis;
- itens principalmente cosméticos;
- proteção contra abuso;
- não transformar o servidor em cassino.

Crates não devem ser o centro da economia.

---

# 58. ANTI PAY-TO-WIN

Itens pagos não devem destruir:

- ranking;
- competição;
- economia;
- progressão.

Se existir conveniência paga, ela deve economizar tempo de forma moderada, não criar uma diferença impossível.

---

# 59. GUI

Menus devem parecer parte do mesmo jogo.

Não utilizar dezenas de menus genéricos.

Criar identidade visual.

---

# 60. RESOURCE PACK

Futuro resource pack próprio.

Pode fornecer:

- ícones;
- sons;
- modelos;
- texturas;
- interface;
- partículas;
- itens customizados.

---

# 61. SOM

Áudio deve ser parte da experiência.

Regiões diferentes podem possuir:

- músicas;
- sons ambientes;
- efeitos especiais.

Bosses devem possuir identidade sonora.

---

# 62. VISUAL

O servidor deve possuir uma identidade.

Não depender somente de:

```text
menu com 9 itens
```

O mundo deve parecer vivo.

---

# 63. NPCS

NPCs devem ter função.

Exemplos:

```text
Guide
Farmer
Fisherman
Explorer
Archaeologist
Merchant
Blacksmith
Boss Hunter
Collector
```

---

# 64. HUB

O lobby deve ser simples, bonito e funcional.

Não transformar o lobby em uma cidade gigante que o jogador nunca usa.

O foco deve ser entrar rapidamente no gameplay.

---

# 65. SISTEMA DE TELEPORTE

Teleportes devem evitar carregamento síncrono de chunks sempre que possível.

A documentação do Paper recomenda `teleportAsync` quando o destino pode estar em chunks descarregados, evitando carga síncrona pesada na thread principal.

---

# 66. PERFORMANCE — BANCO

Nunca bloquear o thread principal com:

- queries;
- arquivos;
- HTTP;
- Redis.

Utilizar operações assíncronas onde apropriado.

---

# 67. PERFORMANCE — CONFIGURAÇÕES

Avaliar cuidadosamente:

- view distance;
- simulation distance;
- entity limits;
- chunk loading;
- autosave;
- unload;
- mob spawning;
- redstone;
- hoppers.

O Paper possui configurações específicas de mundo para autosave, unload de chunks, limites de entidades e otimizações de explosão, entre outras.

---

# 68. PERFORMANCE — PROFILING

Não otimizar por achismo.

Utilizar profiling.

O Paper 1.21+ inclui spark como profiler recomendado.

Durante problemas reais:

```text
/spark profiler start --timeout 600
```

Usar os resultados para identificar:

- plugins lentos;
- eventos caros;
- loops;
- GC;
- chunk loading;
- entidades.

---

# 69. OTIMIZAÇÃO DE ENTIDADES

Não permitir milhares de entidades inúteis.

Sistemas como:

- farms;
- mobs;
- itens dropados;
- pets;
- NPCs

devem ser controlados.

---

# 70. REDSTONE

Redstone pode existir.

Porém, proteger o servidor contra máquinas abusivas.

Criar limites/configurações para:

- pistões;
- hoppers;
- clocks;
- observers;
- farms extremamente grandes.

---

# 71. HOPPERS

Hoppers são uma fonte potencial de carga.

Implementar monitoramento e limites quando necessário.

Não simplesmente desabilitar.

---

# 72. ITENS DROPADOS

Limitar itens abandonados.

Evitar milhares de entidades item em uma ilha.

---

# 73. PARTICLES

Partículas devem ser controladas.

Cosméticos nunca devem permitir spam capaz de prejudicar o servidor.

---

# 74. CACHE

Utilizar cache para:

- ilhas;
- jogadores;
- configurações;
- coleções;
- dados frequentemente acessados.

Sempre implementar invalidação.

---

# 75. CONCORRÊNCIA

Operações críticas devem impedir race conditions.

Exemplo:

Dois jogadores tentando expandir a mesma ilha.

Resultado:

```text
apenas uma operação válida
```

---

# 76. TRANSAÇÕES

Economia e operações críticas devem possuir consistência transacional.

Nunca:

```text
remove dinheiro
↓
crash
↓
item não entregue
```

---

# 77. DUPLICAÇÃO

Auditar cuidadosamente:

- inventários;
- containers;
- shops;
- trades;
- rewards;
- crates;
- quests;
- bosses.

---

# 78. BACKUP

Criar sistema de backup.

Backup deve incluir:

- banco;
- configurações;
- mundos;
- dados importantes.

Testar restauração.

Backup que nunca foi restaurado não deve ser considerado confiável.

---

# 79. MONITORAMENTO

Futuramente:

- TPS;
- MSPT;
- RAM;
- CPU;
- players;
- chunks;
- entidades;
- banco;
- Redis.

---

# 80. ADMIN PANEL

Futuramente criar painel web.

Possibilidades:

```text
Players
Islands
Economy
Logs
Reports
Bans
Statistics
Performance
```

---

# 81. REPORTS

Sistema para jogadores denunciarem:

- bugs;
- exploits;
- jogadores;
- problemas.

---

# 82. ACHIEVEMENTS

Achievements devem incentivar exploração.

Exemplo:

```text
First Fish
First Boss
First Relic
Find Secret Room
Discover Ancient Ruins
Catch Mythical Fish
Complete Museum
Defeat Boss Without Damage
```

---

# 83. ACHIEVEMENTS SECRETOS

Alguns achievements devem ser secretos.

Exemplo:

```text
???
```

Quando desbloqueado:

```text
Achievement discovered!
```

Isso cria curiosidade.

---

# 84. SISTEMA DE DESCOBERTA

O servidor deve registrar descobertas.

Exemplo:

```text
Discovered:
Ancient Forest
```

```text
Discovered:
Secret Temple
```

```text
Discovered:
Mythical Fish
```

---

# 85. LORE

O mundo deve possuir uma história.

Não precisa ser uma novela enorme.

Pequenas pistas:

- livros;
- NPCs;
- ruínas;
- itens;
- diálogos;
- ambientes.

O jogador monta a história.

---

# 86. HISTÓRIA

Premissa sugerida:

O mundo original foi fragmentado.

As ilhas são fragmentos sobreviventes.

Regiões antigas possuem vestígios da civilização que existia antes da fragmentação.

O jogador descobre aos poucos:

```text
Ilhas
↓
Ruínas
↓
Relíquias
↓
Civilização
↓
Catástrofe
↓
Mistério
```

Não revelar tudo no começo.

---

# 87. ENDGAME LORE

Jogadores avançados descobrem que algumas regiões possuem relação com a origem das ilhas.

Isso pode alimentar temporadas futuras.

---

# 88. EVENTO MUNDIAL

Futuramente:

```text
THE SHATTERING
```

Um evento global.

Todos os jogadores participam.

Isso pode introduzir uma nova região.

---

# 89. SISTEMA DE MUNDO VIVO

O mundo não deve ficar exatamente igual para sempre.

Eventos podem alterar:

- NPCs;
- regiões;
- mobs;
- recursos;
- clima;
- quests.

---

# 90. RANDOM EVENTS

Exemplo:

```text
[!] Um meteorito caiu na Ancient Forest.
```

Jogadores podem correr para lá.

---

# 91. SOCIAL

Incentivar jogadores a interagir.

Exemplos:

- visitas;
- colaboração;
- mercado;
- clans;
- bosses;
- dungeons;
- eventos.

---

# 92. VISITAS

Jogadores podem visitar ilhas públicas.

Sistema:

```text
/is visit player
```

Rankings:

```text
Top Islands
Most Visited
Best Museum
Best Builder
```

---

# 93. ILHAS PÚBLICAS

Jogadores podem marcar:

```text
Public
Private
Friends
```

---

# 94. BUILDING

Construção deve ser uma atividade importante.

Possibilidades:

- concursos;
- ranking;
- temas;
- achievements;
- visitantes.

---

# 95. ILHA COMO PERFIL

A ilha deve funcionar como identidade do jogador.

Quando alguém visita:

```text
Nome
Rank
Island Level
Collections
Museum
Achievements
Theme
```

---

# 96. PERSONALIZAÇÃO

Cada jogador poderá criar uma ilha muito diferente.

Não queremos 500 ilhas visualmente iguais.

---

# 97. TEMPLATES

Templates ajudam iniciantes.

Mas jogadores avançados podem construir tudo manualmente.

---

# 98. SISTEMA DE UPGRADES

Upgrades não devem ser apenas:

```text
+10%
+20%
+30%
```

Alguns devem desbloquear novas mecânicas.

Exemplo:

```text
Greenhouse
```

desbloqueia agricultura avançada.

---

# 99. SKILLS

Futuramente:

```text
Farming
Fishing
Exploration
Combat
Collection
```

Cada uma evolui independentemente.

---

# 100. PLAYER IDENTITY

Cada jogador terá uma trajetória.

Exemplo:

```text
Gustavo
Explorer
Fishing Level 32
Island Level 17
98/150 Fish
12 Relics
3 Bosses
```

---

# 101. NÃO OBRIGAR UM CAMINHO

Um jogador pode ser:

```text
Fisherman
```

Outro:

```text
Builder
```

Outro:

```text
Explorer
```

Outro:

```text
Collector
```

Outro:

```text
Boss Hunter
```

Todos conseguem progredir.

---

# 102. RECOMPENSAS

Recompensas podem incluir:

- coins;
- cosmetics;
- titles;
- relics;
- collection unlocks;
- access;
- decorations;
- pets.

---

# 103. RARIDADE

Utilizar raridades com significado.

Exemplo:

```text
COMMON
UNCOMMON
RARE
EPIC
LEGENDARY
MYTHIC
SECRET
```

Não colocar tudo como Legendary.

---

# 104. LOOT

Loot deve ter propósito.

Evitar excesso de lixo.

---

# 105. MARKET SINKS

A economia precisa remover dinheiro.

Exemplos:

- decoração;
- expansão;
- cosméticos;
- upgrades;
- teleportes;
- crafting;
- NPC services.

---

# 106. ECONOMIA SAUDÁVEL

Evitar inflação absurda.

Monitorar:

```text
money created
money destroyed
average balance
top balances
transaction volume
```

---

# 107. ANTI-INFLAÇÃO

A economia deve possuir sinks permanentes.

---

# 108. SHOP ROTATION

Algumas lojas podem ter rotação.

Exemplo:

```text
Weekly Merchant
```

Isso cria motivo para voltar.

---

# 109. NPC ROTATION

NPCs podem viajar entre regiões.

Exemplo:

```text
Wandering Merchant
```

Aparece durante algumas horas.

---

# 110. DAILY CONTENT

Não transformar o jogo em obrigação diária.

Daily quests devem ser opcionais.

---

# 111. WEEKLY CONTENT

Conteúdo semanal deve ser mais interessante:

```text
Boss
Event
Tournament
Fishing Festival
Treasure Hunt
```

---

# 112. ANTI-GRIND

Sempre que uma mecânica estiver baseada apenas em repetição:

perguntar:

> "Isso é realmente divertido?"

Se não for, adicionar:

- decisão;
- exploração;
- risco;
- descoberta;
- estratégia;
- interação social.

---

# 113. PRINCÍPIO DE DESIGN

Cada sistema deve responder:

> Por que o jogador faria isso?

Se a resposta for apenas:

> para ganhar números maiores

reavaliar.

---

# 114. FASES DE DESENVOLVIMENTO

## FASE 0

Análise do projeto.

Não implementar.

---

## FASE 1

Core.

---

## FASE 2

Banco.

---

## FASE 3

SkyWorld.

---

## FASE 4

Island System.

---

## FASE 5

Protection.

---

## FASE 6

Expansion.

---

## FASE 7

Progression.

---

## FASE 8

Farming.

---

## FASE 9

Fishing.

---

## FASE 10

Exploration.

---

## FASE 11

Quests.

---

## FASE 12

Collections.

---

## FASE 13

Bosses.

---

## FASE 14

Dungeons.

---

## FASE 15

Economy.

---

## FASE 16

Social.

---

## FASE 17

GUI.

---

## FASE 18

Cosmetics.

---

## FASE 19

Events.

---

## FASE 20

Seasons.

---

## FASE 21

External Integrations.

---

## FASE 22

Redis.

---

## FASE 23

Velocity.

---

## FASE 24

Scaling.

---

## FASE 25

Security audit.

---

## FASE 26

Performance audit.

---

# 115. PLUGINS EXTERNOS

O servidor poderá possuir plugins externos como:

```text
yPlugins
yCampo
yPesca
yBosses
yArmazem

StormPlugins
StormClans
StormCrates
StormEconomiaSecundaria
StormLobbyV2
StormLooting
StormMinas
StormMinasPrivadas
StormPescaria
StormRankUP
StormSpawners
StormSpawnersShopV2
```

Porém:

## NÃO criar dependência arquitetural deles.

O GreenSky deve continuar funcionando sempre que possível sem eles.

---

# 116. GREENBRIDGE

Criar camada:

```text
integration/
```

Exemplo:

```text
StormBridge
YPluginBridge
EconomyBridge
PermissionBridge
PlaceholderBridge
```

---

# 117. PRINCÍPIO DOS PLUGINS

Plugin externo deve ser uma extensão.

Não deve ser o cérebro do servidor.

O cérebro é:

```text
GreenSky
```

---

# 118. CÓDIGO

Evitar:

- classes gigantes;
- Singleton global;
- static abuse;
- SQL espalhado;
- lógica de negócio em listeners;
- lógica em comandos;
- código duplicado.

---

# 119. LISTENERS

Listeners devem ser finos.

Exemplo:

```text
Event
↓
Service
↓
Business Logic
```

Não:

```text
Listener
↓
1000 linhas
```

---

# 120. COMMANDS

Commands devem chamar serviços.

Não colocar toda a lógica dentro do executor.

---

# 121. SERVIÇOS

Serviços representam regras de negócio.

---

# 122. REPOSITORIES

Repositories representam persistência.

---

# 123. EVENTOS INTERNOS

Criar eventos internos quando útil.

Exemplos:

```text
IslandCreateEvent
IslandExpandEvent
IslandLevelUpEvent
QuestCompleteEvent
BossDefeatEvent
CollectionUnlockEvent
```

---

# 124. CONFIGURAÇÃO

Tudo que pode ser balanceado deve ser configurável.

Exemplo:

```yaml
islands:
  initial-size: 100
  spacing: 512
  max-size: 1000

performance:
  max-entities-per-island: 500

economy:
  starting-coins: 100
```

Valores são exemplos.

---

# 125. SEGURANÇA

Nunca colocar:

- senhas;
- tokens;
- API keys

no Git.

Utilizar:

```text
.env
environment variables
secrets
```

---

# 126. LOGGING

Logs importantes.

Não spam.

---

# 127. TESTES

Testar:

- regiões;
- expansão;
- proteção;
- economia;
- repositories;
- quests;
- progressão;
- rewards;
- serialização.

---

# 128. RESTART

Após restart:

- ilhas continuam;
- progresso continua;
- economia continua;
- coleções continuam;
- quests continuam;
- membros continuam;
- configurações continuam.

---

# 129. CRASH RECOVERY

Operações críticas devem ser recuperáveis.

Principalmente:

- expansão;
- reset;
- transações;
- rewards;
- migração.

---

# 130. BACKUP

Backup automático.

Testar restauração.

---

# 131. PERFORMANCE TEST

Simular:

```text
10 players
50 players
100 players
200 players
500 players
```

Monitorar:

- TPS;
- MSPT;
- CPU;
- RAM;
- chunk loading;
- entidades;
- banco.

---

# 132. PROFILING

Utilizar spark quando investigar problemas reais.

Não otimizar aleatoriamente.

Paper recomenda spark como profiler a partir da linha 1.21.

---

# 133. MULTI-SERVER PERFORMANCE

Quando necessário:

```text
Sky-01
Sky-02
Sky-03
```

distribuir ilhas.

---

# 134. MIGRAÇÃO

Uma ilha pode ser migrada.

Fluxo:

```text
lock
↓
save
↓
export
↓
transfer
↓
import
↓
validate
↓
unlock
```

---

# 135. INSTÂNCIAS

Dungeons/bosses podem usar regiões ou mundos temporários.

Criar:

```text
InstanceManager
```

---

# 136. DESIGN DE CONTEÚDO

Cada nova região deve adicionar pelo menos alguns destes elementos:

- nova mecânica;
- novo recurso;
- nova criatura;
- nova coleção;
- nova quest;
- novo segredo;
- novo boss;
- novo item;
- nova história.

---

# 137. NÃO FAZER REGIÕES VAZIAS

Uma região sem conteúdo é apenas decoração.

---

# 138. SEGREDOS

Sempre deixar conteúdo opcional.

Jogadores curiosos devem ser recompensados.

---

# 139. EXPLORAÇÃO RECOMPENSADA

Não entregar tudo via menu.

Algumas coisas devem ser encontradas no mundo.

---

# 140. DISCOVERY LOG

Criar registro de descobertas.

Exemplo:

```text
██████████ 100%
```

---

# 141. MAPA

Futuramente criar mapa do mundo.

Pode mostrar:

- regiões;
- descobertas;
- bosses;
- eventos;
- pontos de interesse.

---

# 142. POIs

Points of Interest:

```text
Ancient Ruin
Fishing Spot
Boss Arena
Secret Cave
Merchant
Quest NPC
Treasure
Portal
```

---

# 143. EVENTOS REGIONAIS

Cada região pode ter eventos próprios.

---

# 144. CLIMA

Futuro:

- chuva;
- tempestade;
- neve;
- eventos especiais.

Clima pode alterar gameplay.

---

# 145. CICLO DIA/NOITE

Alguns conteúdos dependem de:

- dia;
- noite;
- lua;
- clima.

---

# 146. PESCA DINÂMICA

Exemplo:

```text
Normal Day
→ Common fish

Rain
→ Rare fish

Storm
→ Electric fish

Full Moon
→ Mythical fish
```

---

# 147. FARMING DINÂMICO

Exemplo:

```text
Normal
→ normal crops

Special weather
→ mutations

Rare event
→ unique crops
```

---

# 148. BOSS DINÂMICO

Bosses podem ter condições de spawn.

---

# 149. MUNDO COM CICLOS

O mundo deve parecer vivo.

---

# 150. PRINCÍPIO FINAL DE GAMEPLAY

O jogador deve pensar:

> "O que será que tem ali?"

e não:

> "Qual comando preciso digitar para ganhar dinheiro?"

---

# 151. ARQUITETURA

Estrutura inicial sugerida:

```text
com.greencodes.greensky
│
├── core
├── database
├── player
├── island
├── world
├── generation
├── protection
├── progression
├── farming
├── fishing
├── exploration
├── quest
├── collection
├── bestiary
├── relic
├── boss
├── dungeon
├── economy
├── shop
├── clan
├── event
├── season
├── cosmetic
├── gui
├── npc
├── integration
├── redis
└── velocity
```

A arquitetura pode ser alterada quando existir justificativa técnica.

---

# 152. REGRA PARA CLAUDE CODE

Antes de codificar qualquer sistema:

1. Leia o README.
2. Inspecione o projeto.
3. Entenda o código existente.
4. Verifique a versão do Paper.
5. Verifique APIs disponíveis.
6. Verifique dependências.
7. Planeje.
8. Implemente somente a fase solicitada.
9. Compile.
10. Teste.
11. Corrija.
12. Revise.

---

# 153. NÃO IMPLEMENTAR TUDO DE UMA VEZ

Nunca receber a ordem:

```text
"faça o GreenSky inteiro"
```

e sair implementando tudo.

Trabalhar fase por fase.

---

# 154. FASE 0 — ANÁLISE

Primeira execução:

NÃO modificar código.

Entregar:

- análise;
- arquitetura;
- dependências;
- riscos;
- problemas;
- plano.

---

# 155. FASE 1 — CORE

Implementar somente a fundação.

---

# 156. FASE 2 — DATABASE

Implementar PostgreSQL.

---

# 157. FASE 3 — WORLD

Implementar:

```text
SkyWorld
VoidGenerator
ChunkGenerator
WorldManager
```

---

# 158. FASE 4 — ISLANDS

Implementar:

```text
Island
IslandRegion
IslandService
IslandRepository
IslandMember
IslandPermission
```

---

# 159. FASE 5 — PROTECTION

Implementar proteção.

---

# 160. FASE 6 — EXPANSION

Implementar expansão.

---

# 161. FASE 7 — FIRST PLAYABLE

Criar uma experiência mínima completa:

```text
entrar
↓
criar ilha
↓
construir
↓
expandir
↓
visitar
↓
voltar
↓
persistir
↓
restart
↓
continuar
```

Somente depois começar a adicionar conteúdo.

---

# 162. FASE 8 — GAMEPLAY

Adicionar:

- farming;
- fishing;
- exploration;
- quests;
- collections.

---

# 163. FASE 9 — COMBAT

Adicionar:

- mobs;
- bosses;
- dungeons;
- raids.

---

# 164. FASE 10 — SOCIAL

Adicionar:

- clans;
- visits;
- rankings;
- competitions.

---

# 165. FASE 11 — ECONOMY

Adicionar:

- shops;
- market;
- transactions;
- sinks.

---

# 166. FASE 12 — CONTENT

Adicionar:

- regions;
- lore;
- secrets;
- events;
- seasons.

---

# 167. FASE 13 — COSMETICS

Adicionar:

- pets;
- particles;
- themes;
- titles;
- effects.

---

# 168. FASE 14 — MONETIZATION

Adicionar:

- ranks;
- store;
- bundles;
- season pass;
- cosmetics.

---

# 169. FASE 15 — INTEGRATIONS

Adicionar plugins externos quando necessário.

---

# 170. FASE 16 — SCALE

Adicionar:

- Redis;
- Velocity;
- multiple Sky servers.

---

# 171. FASE 17 — HARDENING

Auditar:

- exploits;
- duplication;
- economy;
- permissions;
- chunks;
- crashes.

---

# 172. FASE 18 — PERFORMANCE

Realizar profiling real.

---

# 173. DEFINITION OF DONE

Uma fase só está concluída quando:

- compila;
- inicia;
- funciona;
- persiste;
- sobrevive a restart quando aplicável;
- não apresenta erros relevantes;
- possui testes quando necessário;
- não introduz regressões;
- respeita a arquitetura.

---

# 174. NÃO INVENTAR API

Se não souber como uma API funciona:

Pesquisar.

Se houver dúvida:

Inspecionar a API.

Se houver incompatibilidade:

Adaptar.

Nunca criar código baseado em uma API imaginária.

---

# 175. DOCUMENTAÇÃO OFICIAL

Priorizar documentação oficial do Paper.

---

# 176. DECISÕES TÉCNICAS

Se houver duas soluções:

```text
Opção A
Opção B
```

comparar:

- performance;
- complexidade;
- manutenção;
- compatibilidade;
- escalabilidade.

Escolher conscientemente.

---

# 177. CÓDIGO LIMPO

Preferir:

```text
baixo acoplamento
alta coesão
serviços pequenos
repositories
interfaces úteis
eventos
configuração
testes
```

Evitar:

```text
God classes
static abuse
Singleton abuse
listeners gigantes
commands gigantes
SQL espalhado
```

---

# 178. PRINCÍPIO DE PERFORMANCE

Não otimizar sacrificando gameplay sem necessidade.

O objetivo é:

```text
GAMEPLAY
+
PERFORMANCE
+
ESCALABILIDADE
```

e não:

```text
performance
sem jogo
```

---

# 179. PRINCÍPIO DE MONETIZAÇÃO

Monetização deve aumentar o valor percebido do servidor.

Não vender:

```text
"poder jogar"
```

Vender:

```text
"quero deixar minha ilha incrível"
```

---

# 180. PRINCÍPIO DE RETENÇÃO

O jogador deve ter:

### Objetivo imediato

"Quero melhorar minha ilha."

### Objetivo de médio prazo

"Quero desbloquear aquela região."

### Objetivo de longo prazo

"Quero descobrir aquele segredo/boss."

### Objetivo de endgame

"Quero completar minha coleção e ficar no ranking."

---

# 181. PRINCÍPIO DE DESCOBERTA

Nem todo conteúdo deve estar em menus.

O mundo precisa esconder coisas.

---

# 182. PRINCÍPIO SOCIAL

Algumas experiências devem ser melhores com amigos.

---

# 183. PRINCÍPIO DE COMPETIÇÃO

Competição deve existir sem obrigar pay-to-win.

---

# 184. PRINCÍPIO DE PERSONALIZAÇÃO

Cada ilha deve poder parecer única.

---

# 185. PRINCÍPIO DE TEMPORADAS

Conteúdo novo deve chegar regularmente.

---

# 186. PRINCÍPIO DE ESCALA

O GreenSky deve começar simples.

Mas a arquitetura deve permitir:

```text
1 servidor
↓
2 servidores
↓
5 servidores
↓
10+ servidores
```

sem reescrever o sistema inteiro.

---

# 187. RESULTADO ESPERADO

Ao final, o GreenSky deve ser percebido como:

> "Um RPG de ilhas dentro do Minecraft."

e não:

> "Mais um SkyBlock."

---

# 188. PRIMEIRO COMANDO PARA A CLAUDE

Ao receber este README pela primeira vez, executar SOMENTE:

```text
FASE 0 — ANÁLISE
```

Não escrever código.

Não criar arquivos.

Não modificar o projeto.

Analisar tudo e apresentar:

1. Estado atual.
2. Arquitetura proposta.
3. Problemas encontrados.
4. Dependências.
5. Riscos.
6. Plano de implementação.
7. Primeira etapa recomendada.

Após aprovação humana, implementar somente a próxima fase.

---

# 189. REGRA ABSOLUTA

Não sacrificar a qualidade para terminar rápido.

Não sacrificar a performance por conveniência.

Não sacrificar a experiência do jogador por monetização.

Não sacrificar a arquitetura por uma implementação rápida.

Não criar um servidor genérico.

O GreenSky deve ser:

```text
DIFERENTE
BONITO
DIVERTIDO
ESCALÁVEL
OTIMIZADO
SOCIAL
EXPLORÁVEL
MONETIZÁVEL
```

---

# 190. GREEN SKY

```text
                    GREEN SKY
                       │
           ┌───────────┴───────────┐
           │                       │
         ILHA                    MUNDO
           │                       │
     ┌─────┼─────┐          ┌──────┼──────┐
     │     │     │          │      │      │
   Build Farm Museum      Explore Bosses Events
     │     │     │          │      │      │
     └─────┼─────┘          └──────┼──────┘
           │                       │
           └───────────┬───────────┘
                       │
                   PROGRESSÃO
                       │
          ┌────────────┼────────────┐
          │            │            │
      Collections    Quests      Achievements
          │            │            │
          └────────────┼────────────┘
                       │
                    ENDGAME
                       │
          ┌────────────┼────────────┐
          │            │            │
       Dungeons      Raids       Seasons
          │            │            │
          └────────────┼────────────┘
                       │
                  PERSONALIZAÇÃO
                       │
            Cosmetics / Themes / Pets
                       │
                  MONETIZAÇÃO
```

**A regra mais importante de todas:**

> O jogador deve entrar por causa da ilha, continuar por causa da progressão e permanecer por causa do mundo.