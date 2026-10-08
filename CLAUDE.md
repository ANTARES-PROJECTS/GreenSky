# CLAUDE.md — GreenSky

Plugin Paper de um SkyBlock RPG. A visão completa do jogo está no [README.md](README.md); o histórico de cada
alteração, no [INSTRUCOES.md](INSTRUCOES.md). Este arquivo tem as regras de trabalho.

## Estrutura

```
green-sky/
├── README.md            # visão do jogo (fonte das fases e regras)
├── CLAUDE.md            # este arquivo
├── INSTRUCOES.md        # histórico do que foi feito, fase a fase
├── greensky/            # projeto Gradle (wrapper: greensky/gradlew)
│   ├── greensky-api/    # contratos públicos (vazio por enquanto)
│   ├── greensky-core/   # o plugin (todo o código)
│   └── tools/e2e/       # testes com bots reais (mineflayer)
├── server/              # Paper de teste: jar, plugins/, configs, mundo
├── libs/                # jars externos, só para compilar contra eles
├── docker-compose.yml   # PostgreSQL (lê .env)
├── .env / .env.example  # GREENSKY_DB_PASSWORD (o .env nunca vai para o git)
├── build.bat            # compila, testa e copia o jar para server/plugins
└── start-server.bat     # sobe PostgreSQL + Paper de teste
```

## Versões (fixas)

| Item | Versão |
|---|---|
| Paper | **26.1.2 build 74** (STABLE), `server/paper-26.1.2-74.jar` |
| paper-api | `io.papermc.paper:paper-api:26.1.2.build.74-stable` (nunca `build.+`) |
| Java | **25** (Temurin 25.0.4.1 em `C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot`; não está no PATH) |
| Gradle | 9.8.0, **só pelo wrapper** `greensky/gradlew` |
| PostgreSQL | 18 (`postgres:18-alpine`) |
| HikariCP / JDBC / Flyway / Shadow / JUnit | 7.1.0 / 42.7.13 / 13.9.0 / 9.6.1 / 6.1.3 |

A 26.1.2 está **UNSUPPORTED** no Paper desde 2026-07-26 (sem builds novos). Foi escolha do usuário; reavaliar.

## Comandos

```bat
build.bat            :: compila, roda os testes (sobe o Postgres) e copia o jar para server\plugins
start-server.bat     :: sobe Postgres + Paper; "stop" no console para desligar
```
No Gradle direto: `greensky\gradlew.bat -p greensky build deploy` (`deploy` copia o jar para `server/plugins`).
Testes com bots (servidor de teste com `online-mode=false` e `server-ip=127.0.0.1`):
`cd greensky\tools\e2e && npm install --ignore-scripts && npm test` (com `JAVA` e `GREENSKY_DB_PASSWORD` no ambiente).
Os bots fazem `/register`/`/login` sozinhos quando o nLogin pede (senha só do banco local do nLogin).
Roteiros: `auth-e2e` (login/identidade), `protection-e2e` (fases 5-6), `first-playable-e2e` (fase 7, com restart).
No Git Bash, use `MSYS_NO_PATHCONV=1` e caminho `C:/...` no `JAVA` (senão `/nlogin` e `/c/...` viram caminhos).

## Regras do projeto

1. **Uma fase por vez.** A lista oficial é a das seções **154 a 172** do README. Implementar só a fase
   aprovada; ao terminar, compilar, testar, subir no servidor de teste, documentar no INSTRUCOES.md e
   **parar e esperar aprovação** antes da próxima.
2. **Nunca inventar API.** Antes de usar uma classe/método, confirmar na jar (`javap`) ou na documentação
   oficial do Paper da versão fixada. Se for deprecated/removida, usar o substituto indicado na Javadoc.
3. **Dependências externas (yPlugins, StormPlugins etc.):** só integrar com o jar real em `libs/`, lido com
   `javap`. **Nunca supor a API** a partir do nome do plugin ou de memória. Sempre `compileOnly`, nunca
   embutido; o GreenSky precisa funcionar sem eles (camada `integration/`, criada só quando houver o jar).
4. **Git:** o projeto mantém o git (`.git/` na raiz), mas o Claude **nunca** faz commit, push, add, reset,
   checkout, merge ou qualquer comando que altere o repositório **por conta própria**: só quando o usuário
   pedir explicitamente, e só o que foi pedido. Comandos só de leitura (`status`, `diff`, `log`,
   `check-ignore`, `ls-files`) podem ser usados para conferir. Fase concluída **não** gera commit automático.
   Não mexer em `.gitignore`/`.gitattributes` sem pedido.
5. **Nunca bloquear a thread do servidor** com SQL, arquivo, HTTP ou Redis. Só o boot é bloqueante.
6. **Segredos fora do código e do git:** senha só em `GREENSKY_DB_PASSWORD` (`.env`).
7. **Config inválida => plugin não sobe**, com mensagem dizendo a chave.
8. **WorldBorder por jogador é só visual.** A proteção real é do `IslandProtectionService`.
9. **Folia:** não declarar `folia-supported`; manter schedulers atrás do `GreenScheduler`.
10. Ações irreversíveis ou externas (instalar software, aceitar termos, apagar dados, baixar arquivos) só com autorização.
11. **Lógica de entrada só depois da autenticação** (ver "Identidade e autenticação"). Nada do GreenSky
    (carregar participações/acesso, dica de início, borda, comandos `/is`) roda para um jogador que ainda
    não fez login no plugin de login.

## Identidade e autenticação (decisão de projeto)

- **`online-mode=false` também em produção:** o servidor aceita contas originais e piratas. A
  autenticação é de um plugin de login (provavelmente **nLogin**), não da Mojang.
- **Identidade = UUID.** Todo o banco e os serviços usam só o UUID (`players.uuid`, `islands.owner_uuid`,
  `island_members.player_uuid`, caches e proteção). O nome é só exibição e busca em comandos.
- **Nick único sem diferenciar maiúsculas** (migration V4: índice único em `lower(name)`). Em
  `online-mode=false` puro o UUID vem do nick **exato**: `Gustavo` e `gustavo` seriam duas contas e duas
  ilhas (comprovado sem nLogin). Defesas do GreenSky:
  - pré-login assíncrono (`PlayerSessionListener`): recusa um nick cuja outra grafia já é de outra conta;
  - a conta só é registrada (nick reivindicado) **depois** da autenticação;
  - comandos resolvem nomes pelo banco do GreenSky (`PlayerService.findByName`), nunca pelo cache do servidor.
- **Com o nLogin 2.0.24 (testado):** a outra grafia do nick recebe o **mesmo UUID** da conta registrada e
  precisa da senha dela (`/login`). O UUID de uma conta autenticada é o **UUID offline do nick registrado**
  (`MD5("OfflinePlayer:"+nick)`), igual no cliente, no servidor e no banco. O pré-login do GreenSky continua
  como defesa se não houver nLogin ou se o comportamento dele mudar.
- **Autenticação (`integration/auth`):** `AuthBridge` com `NLoginAuthBridge` (ouve
  `com.nickuc.login.api.event.bukkit.auth.AuthenticateEvent`, que dispara no `/register` e no `/login`;
  `nLoginAPI.isAuthenticated` para `/reload`) e `NoAuthBridge` (entrar = autenticado). Ao autenticar,
  `PlayerSessionService` registra a conta e dispara `GreenSkyPlayerReadyEvent` (módulo `greensky-api`);
  participações, borda, dica de início e comandos `/is` só existem a partir daí.
- **Config `auth`:** `provider: nlogin|none` e `dev-mode`. `nlogin` sem o plugin => não sobe.
  `none` + `online-mode=false` => **não sobe**, a menos que `dev-mode: true` (aviso forte no log; só para
  desenvolvimento/testes com bots). O padrão do jar é `nlogin` + `dev-mode: false`.
- **nLogin:** jar `libs/nLogin-2.0.24.jar` (versão grátis; `compileOnly`; também em `server/plugins/`). A API
  vem embutida no jar do plugin; no Maven (`repo.nickuc.com`) só existe `com.nickuc.login:api:2.0` (a
  documentação cita "10.4", que não existe). Código fechado; login automático de contas originais só na
  versão paga. Ao iniciar, **baixa dependências da internet**; o jar se identifica como "2.0.24 DEV".
  Assistente feito no servidor de teste: idioma inglês, canal estável, **atualização com confirmação manual**,
  senha "Safer", **sem diálogos** (comandos; diálogos exigem cliente 1.21.6+), nAntiBot recusado. O nLogin
  avisa que **as coordenadas não são protegidas antes do login** (configurar `/nlogin spawn set join` em produção).

## Estado das fases (seções 154–172)

| Fase | Status |
|---|---|
| 0 Análise · 1 Core · 2 Database · 3 World · 4 Islands · 5 Protection · 6 Expansion · 7 First Playable | concluídas |
| **8 Gameplay** — 8.1 Base de conteúdo + Coleções | **concluída** |
| 8.2 Pesca (nativa) | **concluída** |
| 8.3 Agricultura (nativa) | **colheita, achados raros, qualidade, plantio ancestral, fertilizante e guia agrícola implementados/testados; demais mecânicas avançadas pendentes** |
| 8.4 Quests · 8.5 Exploração | pendentes |
| 9 Combat · 10 Social · 11 Economy · 12 Content · 13 Cosmetics · 14 Monetization · 15 Integrations · 16 Scale · 17 Hardening · 18 Performance | pendentes |

Pendente fora das fases: ViaVersion/ViaBackwards 5.12.0 para clientes 1.21.x (planejado, não implementado).

## Arquitetura (greensky-core, pacote `com.greencodes.greensky`)

```
GreenSkyPlugin   # liga tudo no onEnable: config -> banco -> mundo -> ilhas -> proteção -> borda -> visitas
core/            # GreenScheduler; config/ (GreenSkyConfig e records validados)
database/        # Database (Hikari + Flyway + executor async)
generation/      # VoidGenerator, SingleBiomeProvider
world/           # WorldManager, SkyWorld
player/          # PlayerService (nick único), PlayerSessionService (sessão após login), PlayerSessionListener (pré-login)
integration/     # auth/: AuthBridge, NLoginAuthBridge, NoAuthBridge (plugins externos ficam só aqui)
content/         # Rarity, ContentItem, ContentRegistry (items.yml), ItemFactory (id escondido no item), ContentYaml, ItemCommand
collection/      # CollectionCatalog (collections.yml), CollectionService (memória + lote), CollectionTracker
                 # (porta das atividades; dispara CollectionDiscoverEvent), CollectionListener, CollectionCommand
fishing/         # FishTable (sorteio em memória), FishConditions/FishingEnvironment (hora, clima, lua), FishLoader
                 # (content/fish.yml), AfkFishingGuard (pesca parada), FishingListener (só troca o peixe vanilla)
farming/         # CropCatalog (content/crops.yml), FarmingListener (planta madura quebrada manualmente)
                 # CropQuality (pesos 1..3 estrelas), CropItemFactory (PDC crop_quality)
                 # CropMarkers/AncientPlants (PDC do chunk), AncientPlantListener (proteção ambiental)
                 # FertilizerListener (Trigo Dourado), FarmingCommand (/agricultura: guia e inspeção)
island/          # domínio, IslandRepository (SQL), IslandService (regras, async, cache), IslandCommand
protection/      # IslandIndex (índice espacial), IslandProtectionService (decisões), listeners
border/          # WorldBorder visual por jogador
visit/           # expulsão ao ficar privada, renascer na ilha, dica de início
resources/db/migration/  # V1 players, V2 islands, V3 island_settings, V4 nick único, V5 collection_progress
resources/content/       # items.yml, collections.yml (copiados para plugins/GreenSky/content na 1ª vez; PROVISÓRIOS)
greensky-api/    # api/event/GreenSkyPlayerReadyEvent, CollectionDiscoverEvent (contratos públicos)
```
- Camadas: `Listener/Command -> Service -> Repository`. Listeners e comandos finos. Sem singleton nem `static` de estado.
- Pacotes: `island` não depende de `protection`/`border`/`visit` (eles dependem de `island`, via `IslandListener`).
- Criar pacotes só quando a fase deles chegar.

## Menus e modelos de ilha (pedido adicional)

Implementados em desenvolvimento, com 137 testes Java passando. Roteiro de menus ainda possui pendência
na lista paginada de visitas; ver INSTRUCOES.md. Não declarar esta entrega concluída.
Novos modelos são determinísticos por posição; ilhas existentes não são reescritas.
Dependências Vault/ViaVersion/ViaBackwards/LuckPerms e fontes foram solicitadas, mas ainda não instaladas.

## Plugins externos no servidor de teste

Ver INSTRUCOES.md ("Plugins externos: StormPlugins e yPlugins"). Hoje: só GreenSky + nLogin + o carregador
StormPlugins (com todos os plugins Storm em `plugins_nao_carregar`); `yPlugins-3.7.0.jar` guardado em `libs/`.
Os carregadores baixam os plugins para a memória (sem jar em disco => **não dá para integrar**, regra 3) e
têm licença presa ao IP:porta. **Fase 8: pesca e agricultura nativas, sem integração com Storm/y.**

## Conteúdo e coleções (8.1)

- Itens do GreenSky: identidade no `PersistentDataContainer` (`greensky:item`), nunca nome/lore.
- Conteúdo em `plugins/GreenSky/content/*.yml`, lido por `ContentYaml` (separador `/`: chaves com ponto
  como `fish.cod` ficam inteiras). Erro no arquivo => plugin não sobe, dizendo arquivo e chave.
- Chaves de entrada de coleção são gravadas no banco: renomear uma chave apaga o progresso dela.
- Atividades registram progresso por `CollectionTracker.record(player, chave, qtd)` na thread do servidor.
  Progresso em memória, gravado em lote a cada 10 s, ao sair e ao desligar (crash perde no máximo ~10 s).

## Pesca (8.2)

Peixe do GreenSky (e +1 na coleção, contado na fisgada) só quando: SkyWorld, jogador pronto, ilha de que é
dono/membro, água aberta (`FishHook#isInOpenWater`, `fishing.require-open-water`) e sem pesca parada
(`fishing.afk-max-catches-same-spot` fisgadas seguidas na mesma posição+mira). Senão: pesca vanilla.
Lixo e tesouro vanilla nunca são trocados. Lua: `MoonPhase.getPhase(world.getFullTime() / 24000)`.
`fish.yml`: cada id precisa existir na coleção `fishing`; nome e raridade vêm de `collections.yml`.

## Armadilhas já encontradas

- YAML do Bukkit: o ponto é separador de caminho; `Material.isItem()` exige o servidor (não usar em teste
  de unidade sem injetar).
- Ilha muito grande: a borda passa da distância de simulação do jogador; testes de fronteira precisam de `forceload`.

- Mundos de plugin ficam em `server/world/dimensions/minecraft/<nome>/` (backup deve cobrir `server/world/`).
- `keepSpawnLoaded`, `generateTree(Location, TreeType)`, `TeleportCause.CHORUS_FRUIT`: deprecated; ver substitutos no INSTRUCOES.md.
- `World.save()` manual gera WARN. `getOfflinePlayer(String)` pode ir à rede: usar `getOfflinePlayerIfCached`.
- Flyway 13: `new FluentConfiguration(loader)`; Shadow: `duplicatesStrategy = INCLUDE` + `mergeServiceFiles()`.
- `getConfig()` usa o config.yml do jar como padrão: chave ausente recebe o padrão; tipo errado é rejeitado.
- Arquivos `.bat` precisam de CRLF; e chamar programas pelo caminho completo (`%~dp0...`): com
  `NoDefaultCurrentDirectoryInExePath` definido, o `cmd` não procura na pasta atual.
- Água corre só para a queda mais próxima (testes de fluido precisam de piso largo).
- Lacunas conhecidas da proteção: pegar/dropar itens por visitantes, PvP, barcos, projéteis em botões, endermen.
