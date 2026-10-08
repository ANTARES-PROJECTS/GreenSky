// Fase 8.2: pesca do GreenSky com bots reais (nLogin), incluindo os exploits:
// ilha alheia, poça rasa (sem água aberta), inventário cheio e pesca parada (AFK).
// Uso:  JAVA=".../java.exe" node fishing-e2e.mjs   (requisitos em lib.mjs)
//
// Cenário: a regra de "água aberta" do Minecraft olha 5x5 em volta da boia, da camada de baixo até 2
// acima. Por isso o lago aberto é grande (21x21) e o bot pesca de cima de um pilar no meio dele, longe
// de qualquer borda. A poça "não aberta" tem só 1 bloco de profundidade (o fundo é sólido).

import {
  BASE_Y, Vec3, check, cmd, connect, finish, inWorld, islandInfo, moveTo, say, sleep, startServer,
} from './lib.mjs';

const stamp = String(Date.now()).slice(-7);
const OWNER = 'Pesc' + stamp;
const VISITOR = 'Visi' + stamp;
const COMMON_CLEAR_DAY = new Set(['Bacalhau', 'Salmão', 'Baiacu']); // únicos possíveis com tempo limpo de dia

const itemCount = (bot) => bot.inventory.items().reduce((sum, i) => sum + i.count, 0);
const VANILLA_FISH = new Set(['cod', 'salmon', 'pufferfish', 'tropical_fish']);

/** Quantidade de cada tipo de item no inventário. */
function countsByName(bot) {
  const counts = new Map();
  for (const i of bot.inventory.items()) counts.set(i.name, (counts.get(i.name) ?? 0) + i.count);
  return counts;
}

/** Tipos de item que aumentaram entre duas contagens. */
function arrived(before, after) {
  return [...after.entries()].filter(([name, n]) => n > (before.get(name) ?? 0)).map(([name]) => name);
}

/**
 * Lança e espera. Só conta como fisgada se um item realmente chegou ao inventário (ou, com o
 * inventário cheio, se apareceu a mensagem). Devolve { caught, gsName, msgs } ou null se nada veio.
 */
async function fishOnce(bot, target, { inventoryFull = false } = {}) {
  await bot.lookAt(target, true);
  await sleep(300);
  const start = bot.messages.length;
  const before = itemCount(bot);
  const beforeByName = countsByName(bot);
  try {
    await Promise.race([bot.fish(), sleep(45000).then(() => { throw new Error('demorou'); })]);
  } catch {
    try { bot.activateItem(); } catch { /* recolhe a linha */ }
    await sleep(800);
    return null;
  }
  await sleep(2000);
  const msgs = bot.messages.slice(start);
  const line = msgs.find((m) => /Você pescou /.test(m));
  const gsName = line ? line.match(/Você pescou (.+?) — /)[1] : null;
  const caught = inventoryFull ? Boolean(gsName) : itemCount(bot) > before;
  // O que chegou: o peixe do GreenSky usa material de peixe vanilla, então só conta como "vanilla" sem a mensagem.
  const items = arrived(beforeByName, countsByName(bot));
  return caught ? { caught, gsName, msgs, items } : null;
}

/** Total de peixes do GreenSky na coleção (soma dos "xN" em /collections fishing). */
async function fishingTotal(bot) {
  const start = bot.messages.length;
  bot.chat('/collections fishing');
  await sleep(2000);
  return bot.messages.slice(start)
    .map((m) => m.match(/✔ .* x(\d+)$/))
    .filter(Boolean)
    .reduce((sum, m) => sum + Number(m[1]), 0);
}

async function giveRod(bot) {
  cmd(`give ${bot.username} minecraft:fishing_rod`);
  await sleep(1000);
  await bot.equip(bot.inventory.items().find((i) => i.name === 'fishing_rod'), 'hand');
  await sleep(300);
  cmd(`enchant ${bot.username} minecraft:lure 3`); // fisgadas mais rápidas
  await sleep(500);
}

/** Repete lances até ter {@code wanted} fisgadas reais (no máximo {@code tries} lances). */
async function fishMany(bot, wanted, tries, aim) {
  const catches = [];
  for (let i = 0; catches.length < wanted && i < tries; i++) {
    const result = await fishOnce(bot, aim(i));
    if (result) catches.push(result);
  }
  return catches;
}

async function main() {
  await startServer();
  const owner = await connect(OWNER);
  await say(owner, '/island create', /Ilha criada/);
  await sleep(3000);
  const { cx, cz } = await islandInfo(owner);
  console.log(`Ilha de ${OWNER}: ${cx}, ${cz}`);

  await inWorld('weather clear');
  await inWorld('time set day');

  // Lago ABERTO 21x21x3 com pilar de vidro no meio (onde o bot fica).
  const lx = cx + 15; // centro do lago
  await inWorld(`fill ${lx - 11} ${BASE_Y - 4} ${cz - 11} ${lx + 11} ${BASE_Y} ${cz + 11} minecraft:glass`);
  await inWorld(`fill ${lx - 10} ${BASE_Y - 3} ${cz - 10} ${lx + 10} ${BASE_Y - 1} ${cz + 10} minecraft:water`);
  await inWorld(`fill ${lx - 10} ${BASE_Y} ${cz - 10} ${lx + 10} ${BASE_Y} ${cz + 10} minecraft:air`);
  await inWorld(`fill ${lx} ${BASE_Y - 3} ${cz} ${lx} ${BASE_Y} ${cz} minecraft:glass`);
  // Mira: 5 a 6 blocos a leste do pilar, longe do pilar e das bordas (>= 4 blocos).
  const aim = (i) => new Vec3(lx + 5 + (i % 2), BASE_Y - 0.6, cz + (i % 3) - 1);

  await moveTo(owner, lx, BASE_Y + 1, cz);
  await giveRod(owner);

  // --- 1) Dono em água aberta: peixes do GreenSky e coleção +1 por peixe ---
  const before = await fishingTotal(owner);
  const ownerCatches = await fishMany(owner, 5, 15, aim);
  const names = ownerCatches.map((c) => c.gsName).filter(Boolean);
  check('dono: lances com fisgada real', ownerCatches.length >= 4, `${ownerCatches.length}`);
  // Propriedade exata: em água aberta na própria ilha, NENHUM peixe vanilla passa sem troca. O que não vira
  // peixe do GreenSky tem de ser lixo/tesouro (~15% das fisgadas no Minecraft, mantidos vanilla de propósito).
  const notGs = ownerCatches.filter((c) => !c.gsName);
  check('dono em água aberta: nenhum peixe vanilla escapa da troca',
    notGs.every((c) => !c.items.some((name) => VANILLA_FISH.has(name))),
    `do GreenSky: ${names.length}; lixo/tesouro: ${notGs.map((c) => c.items.join('+')).join(', ') || 'nenhum'}`);
  check('dono em água aberta: há peixes do GreenSky', names.length >= 2, `${names.length} de ${ownerCatches.length}`);
  check('tempo limpo de dia: só peixes comuns', names.every((n) => COMMON_CLEAR_DAY.has(n)), names.join(', '));
  check('coleção soma exatamente 1 por peixe do GreenSky', (await fishingTotal(owner)) - before === names.length);

  // --- 2) Visitante pescando no mesmo lago (ilha alheia): só vanilla ---
  const visitor = await connect(VISITOR);
  await inWorld(`setblock ${lx} ${BASE_Y} ${cz + 3} minecraft:glass`); // pilar do visitante
  await inWorld(`fill ${lx} ${BASE_Y - 3} ${cz + 3} ${lx} ${BASE_Y - 1} ${cz + 3} minecraft:glass`);
  await moveTo(visitor, lx, BASE_Y + 1, cz + 3);
  await giveRod(visitor);
  const visitorCatches = await fishMany(visitor, 3, 12, (i) => new Vec3(lx - 5 - (i % 2), BASE_Y - 0.6, cz + 3));
  check('visitante: lances com fisgada real', visitorCatches.length >= 2, `${visitorCatches.length}`);
  check('visitante na ilha alheia: nenhum peixe do GreenSky', visitorCatches.every((c) => !c.gsName));
  check('visitante: coleção continua zerada', (await fishingTotal(visitor)) === 0);
  visitor.quit();

  // --- 3) Poça rasa (1 bloco de fundo, sem água aberta): só vanilla ---
  const px = cx - 12; // poça 9x9 a oeste da ilha
  await inWorld(`fill ${px - 5} ${BASE_Y - 2} ${cz - 5} ${px + 5} ${BASE_Y} ${cz + 5} minecraft:glass`);
  await inWorld(`fill ${px - 4} ${BASE_Y - 1} ${cz - 4} ${px + 4} ${BASE_Y - 1} ${cz + 4} minecraft:water`);
  await inWorld(`fill ${px - 4} ${BASE_Y} ${cz - 4} ${px + 4} ${BASE_Y} ${cz + 4} minecraft:air`);
  await moveTo(owner, px + 5, BASE_Y + 1, cz); // em cima da borda de vidro, a 4-5 blocos da mira
  const puddleCatches = await fishMany(owner, 3, 12, (i) => new Vec3(px + (i % 2), BASE_Y - 1.6, cz + (i % 3) - 1));
  check('poça rasa: lances com fisgada real', puddleCatches.length >= 2, `${puddleCatches.length}`);
  check('poça sem água aberta: nenhum peixe do GreenSky', puddleCatches.every((c) => !c.gsName));

  // --- 4) Inventário cheio: o peixe cai no chão, a coleção conta 1 vez e não conta de novo ao pegar ---
  await moveTo(owner, lx, BASE_Y + 1, cz);
  const free = owner.inventory.emptySlotCount();
  cmd(`give ${OWNER} minecraft:stone ${free * 64}`);
  await sleep(1500);
  check('inventário cheio de fato', owner.inventory.emptySlotCount() === 0, `${owner.inventory.emptySlotCount()} vazios`);
  const full = await fishingTotal(owner);
  let fullCatch = null;
  for (let i = 0; !fullCatch && i < 12; i++) {
    fullCatch = await fishOnce(owner, aim(i), { inventoryFull: true });
  }
  check('inventário cheio: ainda pesca peixe do GreenSky', Boolean(fullCatch), fullCatch?.gsName ?? '');
  const afterCatch = await fishingTotal(owner);
  check('inventário cheio: coleção +1 na fisgada', afterCatch - full === 1);
  cmd(`clear ${OWNER} minecraft:stone`);
  await sleep(4000); // abre espaço: o peixe que caiu é recolhido
  check('pegar o peixe do chão não conta de novo', (await fishingTotal(owner)) === afterCatch);

  // --- 5) Pesca parada: mesma posição e mesma mira, mais de 10 fisgadas seguidas ---
  const fixed = aim(0);
  let idleWarned = false;
  const afterIdle = [];
  for (let i = 0, catches = 0; catches < 14 && i < 35; i++) {
    const result = await fishOnce(owner, fixed);
    if (!result) continue;
    catches++;
    if (result.msgs.some((m) => /Pesca parada detectada/.test(m))) idleWarned = true;
    else if (idleWarned) afterIdle.push(result);
  }
  check('pesca parada é detectada (aviso ao jogador)', idleWarned);
  check('pesca parada: sem peixes do GreenSky depois do aviso', afterIdle.length >= 2 && afterIdle.every((c) => !c.gsName),
    `${afterIdle.filter((c) => c.gsName).length} de ${afterIdle.length}`);
  // Lixo/tesouro permanecem vanilla: duas fisgadas sem peixe não comprovam falha de retomada.
  // Para ao obter um peixe; peixe vanilla sem substituição é falha, lixo/tesouro permite outro lance.
  const back = [];
  for (let i = 0; back.length < 6 && i < 18; i++) {
    const result = await fishOnce(owner, aim(i + 1)); // mira segura dentro do lago, diferente da fixa
    if (!result) continue;
    back.push(result);
    if (result.gsName || result.items.some((name) => VANILLA_FISH.has(name))) break;
  }
  check('mudando a mira, volta a pescar peixes do GreenSky',
    back.some((c) => c.gsName) && back.every((c) => c.gsName || !c.items.some((name) => VANILLA_FISH.has(name))),
    back.map((c) => c.gsName ?? c.items.join('+')).join(', '));
  owner.quit();
}

try {
  await main();
  await finish();
} catch (e) {
  await finish(e);
}
