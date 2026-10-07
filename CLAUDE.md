# CLAUDE.md — GreenSky

Plugin Paper de um SkyBlock RPG. Visão completa do jogo: `README.md — GreenSky.md` (fora do repo, em Downloads). Este arquivo guia o trabalho técnico.

## Versões alvo (fixas)

| Item | Versão |
|---|---|
| Paper (servidor) | **26.1.2 build 74** (canal STABLE, `paper-26.1.2-74.jar`, SHA256 `1d70b1da…95e5f7`) |
| paper-api | `io.papermc.paper:paper-api:26.1.2.build.74-stable` (nunca `build.+`) |
| Java | **25** (Temurin 25.0.4.1, obrigatório desde o Paper 26.1) |
| Gradle | 9.8.0, **somente via wrapper** (`./gradlew`), Kotlin DSL |
| PostgreSQL | 18 (`postgres:18-alpine`, via docker-compose) |
| HikariCP / JDBC / Flyway | 7.1.0 / 42.7.13 / 13.9.0 |
| Shadow / JUnit | 9.6.1 / 6.1.3 |

**Decisão do usuário: usar a 26.1.2** (cliente mais leve que a 26.2/26.3, a confirmar num PC fraco).
- O Fill API marca a 26.1.2 como **UNSUPPORTED desde 2026-07-26**: não recebe mais builds (o último, 74, é de 2026-07-06). O servidor loga aviso "2 releases behind (26.2)". Aceito conscientemente; reavaliar periodicamente.
- Paper 26.2 build 132 (STABLE) é a opção suportada; o 26.3 só tem builds BETA.
- Esquema de versão novo: não existe mais `1.21.x-R0.1-SNAPSHOT` a partir da 26.1. A 1.21.11 (última 1.x) e a 1.20.6 foram descartadas: sem builds novos desde mai/2026 e out/2024.
- Ambiente de teste antigo (mundos da 26.2 não abrem na 26.1.2) ficou em `run-26.2/` (ignorado pelo git).

## Estado das fases (lista oficial = seções 154–172 do README)

- [x] Fase 1 — Core
- [x] Fase 2 — Database
- [x] Fase 3 — World
- [x] Fase 4 — Islands
- [x] Fase 5 — Protection
- [ ] Compat — ViaVersion/ViaBackwards (planejado abaixo, não implementado)
- [ ] Fase 6 — Expansion (próxima; **aguarda aprovação**)
- [ ] 7 First Playable · 8+ conteúdo

Regra: implementar **somente a fase aprovada**, uma por vez. Cada fase termina compilando, testada, rodando no Paper de teste, e com um commit.

## Comandos

```bash
# definir JAVA_HOME (o java não está no PATH)
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-25.0.4.101-hotspot"
docker compose up -d --wait          # PostgreSQL local
set -a; . ./.env; set +a              # carrega GREENSKY_DB_PASSWORD
./gradlew build --no-daemon           # compila, testa, gera build/libs/greensky-*.jar (shaded)
```

Servidor de teste: `run/` (ignorado pelo git). Copie o jar para `run/plugins/` e inicie com
`java -jar paper-26.1.2-74.jar --nogui`. O console do Windows corrompe o `stop` via PowerShell
(BOM); automatize com pipe do bash: `(sleep 45; echo stop) | java ... --nogui`.

## Arquitetura

Pacote raiz `com.greencodes.greensky`. **Criar pacotes só quando a fase deles chegar.**

```
GreenSkyPlugin          # onEnable: config -> senha -> Database -> GreenScheduler -> WorldManager
core/GreenScheduler     # único acesso a schedulers (global/async do Paper)
core/config/            # GreenSkyConfig + IslandSettings/DatabaseSettings/WorldSettings (records validados)
database/               # Database (Hikari + Flyway + executor async), SqlFunction, DatabaseException
generation/             # VoidGenerator, SingleBiomeProvider
world/                  # WorldManager, SkyWorld
player/                 # PlayerRepository (upsert em players)
island/                 # Island, IslandRegion (espiral de slots), IslandMember/Role/Permission,
                        # IslandRepository (SQL), IslandService (regras, async, cache dono->ilha),
                        # IslandBuilder (interface) + StarterIslandBuilder (ilha por código), IslandCommand
protection/             # IslandIndex (índice espacial em memória), IslandProtectionService (decisões, sem Bukkit),
                        # PlayerProtectionListener, WorldProtectionListener, ProtectionSessionListener, DenyNotifier
resources/db/migration/ # V1__players.sql, V2__islands.sql (Flyway)
tools/e2e/              # teste de ponta a ponta com bots reais (mineflayer); node_modules ignorado
```

Proteção (fase 5): regras em `IslandProtectionService`; listeners só traduzem eventos.
- Antes de carregar as ilhas, ou fora de qualquer ilha (spawn, vazio), **tudo é negado**.
- Dentro de uma ilha: só membros, conforme `IslandPermission` (dono tem tudo). Visitante não faz nada.
- Efeitos sem jogador (explosão, pistão, líquido, hopper, fogo, crescimento, dispenser) não cruzam a fronteira da região.
- Pérola/fruta do coro só levam a ilhas das quais o jogador é membro.
- Bypass: permissão `greensky.admin.bypass` (op).
- `IslandService` avisa a proteção por `IslandListener` depois de gravar no banco; participações carregam no join.

Teste com bots (servidor de teste precisa de `online-mode=false` e `server-ip=127.0.0.1` em `run/server.properties`; **nunca em produção**):
```bash
cd tools/e2e && npm install --ignore-scripts && JAVA="$JAVA_HOME/bin/java.exe" node protection-e2e.mjs
```

Comando: `/island` (alias `/is`): `create`, `home`, `info`, `add <online>`, `remove <nome>`, e
`admin create <nick>` (permissão `greensky.admin`, funciona no console).

Ilhas: slot vindo da sequência `island_slot_seq` (começa em 1; slot 0 = origem/spawn, reservado),
posição em espiral quadrada com `islands.spacing`. Centro e tamanho ficam gravados no banco (mudar
`spacing` não move ilhas existentes). `state` PENDING/READY: criação grava a linha, gera os blocos e
só então marca READY; PENDING é refeito por `ensureReady` (usado no `/is home`).

Camadas: `Listener/Command -> Service -> Repository`. Listeners e comandos finos. Sem singleton, sem `static` de estado.

## Regras que não podem ser quebradas

1. **Não inventar API.** Confirmar na jar (`javap`) ou na doc oficial do Paper antes de usar.
2. **Nunca bloquear a thread principal** com SQL, arquivo, HTTP ou Redis. Só o boot (`Database.open`) é bloqueante.
3. **Segredos fora do git.** Senha só em `GREENSKY_DB_PASSWORD` (`.env` ignorado). Nada no `config.yml`.
4. **Config inválida => plugin não sobe** (loga erro e se desabilita). Já vale para `spacing > max-size + spacing-margin`.
5. **WorldBorder por jogador é só visual.** A proteção real é do `IslandProtectionService` (fase 5).
6. **Sem integração externa agora.** Não criar `integration/` (Storm, yPlugins) até haver as APIs reais.
7. **Folia:** não declarar `folia-supported`. Só manter tudo atrás do `GreenScheduler`.
8. Não salvar blocos no banco; mundo/chunks ficam no Paper.
9. Ações irreversíveis ou externas (instalar software, aceitar termos, commit/push) só com pedido ou autorização do usuário.

## Armadilhas já encontradas

- Mundos de plugin ficam em `world/dimensions/minecraft/<nome>/` no Paper 26 (backup deve cobrir `world/`).
- `WorldCreator.keepSpawnLoaded` está removido na prática (sem chunks de spawn desde a 1.21.9). Não usar.
- Chamar `World.save()` à mão gera WARN; o servidor já salva ao desligar.
- Flyway 13: `FluentConfiguration(ClassLoader)`, sem `.classLoader()`. Usar `failOnMissingLocations(true)`.
- Shadow: `duplicatesStrategy = INCLUDE` + `mergeServiceFiles()`, senão some o suporte a PostgreSQL do Flyway. Hikari precisa de `setDriverClassName` (DriverManager não vê drivers de plugin).
- `verifyVoid()` olha o chunk 0,0; na Fase 4 isso vira falso alarme quando a ilha inicial ocupar esse chunk.
- Paper 26.2 tem NPE ao receber `stop` no console no exato instante do "Done" (bug do servidor).
- `gradlew` deve ficar com LF (`.gitattributes`).
- `World.generateTree(Location, TreeType)` é deprecated; usar `generateTree(Location, Random, TreeType)`.
- Os testes `*IT` consomem a sequência de slots: ilhas de teste ficam em posições altas, com buracos. Normal.
- Comandos de console no teste: `execute in minecraft:greensky_world run forceload add X Z` antes de `execute ... if block`; `if block` não leva `run`.
- `TeleportCause.CHORUS_FRUIT` está deprecated para remoção; usar `CONSUMABLE_EFFECT`.
- `getOfflinePlayer(String)` pode consultar a Mojang (rede); em comandos use `getOfflinePlayerIfCached`.
- Config: toda chave é obrigatória (`requireInt`/`requireString`); chave nova sem valor no config.yml do usuário impede o boot com mensagem clara.
- Água no Minecraft corre só na direção da queda mais próxima (até 4 blocos): testes de fluido precisam de piso largo.
- Lacunas conhecidas da proteção (não cobertas): pegar/dropar itens por visitantes, PvP, dano de mobs a entidades, barcos colocados em água, projéteis acionando botões/alvos, laço/vara de pesca puxando entidades, endermen dentro da ilha. Visitantes não podem nem abrir portas (decisão: configurável na fase social).
- Corrida no join: se um membro for adicionado enquanto as participações carregam, ele fica sem acesso até reentrar (erra para negar).

## Plano ViaVersion (clientes 1.21.x entrarem no servidor 26.1.2)

Não implementado. Só plugins na pasta `plugins/` do servidor, sem código do GreenSky.
- **ViaVersion** (cliente mais novo que o servidor) e **ViaBackwards** (cliente mais antigo que o servidor) rodam juntos, na mesma versão. Para 1.21.x entrando em servidor 26.1.2 o necessário é o **ViaBackwards**.
- Versão pesquisada: **5.12.0** (Hangar, set/2026; declara suporte a Paper até 26.3). O changelog cita correções "26.1 -> 1.21.11", ou seja, clientes 1.21.11 são tratados. **Não confirmado:** até que 1.21.x mais antigo ele aceita.
- Passos: baixar do Hangar com checksum, fixar a versão, testar com cliente real 1.21.11 (e 1.21.x mais antigos), medir CPU/RAM com spark, documentar em `INSTRUCOES.md`.
- Riscos: custo de CPU da tradução; itens, GUIs e resource pack futuros podem renderizar diferente em cliente antigo; é plugin externo (não depender dele no código).

## Decisões em aberto

- Ilha inicial hoje é por código (disco + árvore). Trocar por template/schematic implementando `IslandBuilder`.
- Apontar o mundo `world` padrão para o gerador void via `bukkit.yml` (não testado).
- Versão do Paper para produção: a 26.1.2 está sem suporte no Paper; reavaliar (26.2 STABLE, ou 26.3 quando STABLE).
