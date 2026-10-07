# INSTRUCOES — Histórico do que foi feito

Registro de cada alteração, por fase, com o motivo. Pasta do projeto: `C:\Users\geren\Desktop\green-sky`.

## Resumo

GreenSky é um plugin para **Paper 26.1.2 (build 74, STABLE)** com **Java 25**, **Gradle 9.8.0**
(wrapper) e **PostgreSQL 18**. Concluídas as fases 1 (Core), 2 (Database) e 3 (World), feitas
na 26.2 e migradas para a 26.1.2 depois (ver "Troca de versão").
Três commits, 21 testes automatizados, tudo validado rodando num Paper de teste.

| Fase | Commit | O que entrega |
|---|---|---|
| 0 | — | Análise (nenhum arquivo criado) |
| 1 | `96336a7` | Plugin que sobe, config validada, `GreenScheduler` |
| 2 | `5b72a61` | PostgreSQL, Hikari, Flyway, SQL assíncrono |
| 3 | `16d7d7f` | Mundo void, `VoidGenerator`, `WorldManager` |

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

```bash
docker compose up -d --wait
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-25.0.4.101-hotspot"
set -a; . ./.env; set +a
./gradlew build --no-daemon
cp build/libs/greensky-0.1.0-SNAPSHOT.jar run/plugins/
cd run && java -jar paper-26.1.2-74.jar --nogui
```

## Pendências

- **Fase 5 (Protection)** aguardando sua aprovação.
- Testar `/is create|home|info|add|remove` com um cliente real.
- `verifyVoid()` avisa se o chunk 0,0 tem blocos (no `run/` há um bloco de ouro de teste); em produção o slot 0 é reservado, então o chunk fica vazio.
- Decidir se o mundo `world` padrão usará o gerador void via `bukkit.yml`.
- Paper 26.1.2 está sem suporte no Paper: reavaliar 26.2/26.3.
- Implementar ViaVersion/ViaBackwards (planejado) e testar com clientes reais.
