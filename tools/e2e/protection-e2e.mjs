// Teste de ponta a ponta do GreenSky: sobe o Paper de teste (run/), conecta bots reais
// (mineflayer) e confere no servidor, via console, o efeito de cada ação.
//
// Requisitos: run/ com o jar do Paper e o plugin em run/plugins, server.properties com
// online-mode=false e server-ip=127.0.0.1, PostgreSQL rodando e GREENSKY_DB_PASSWORD no ambiente.
// Uso:  JAVA=".../java.exe" node protection-e2e.mjs

import { spawn } from 'node:child_process';
import { createWriteStream } from 'node:fs';
import { resolve } from 'node:path';
import readline from 'node:readline';
import mineflayer from 'mineflayer';
import { Vec3 } from 'vec3';

const RUN_DIR = resolve(process.env.RUN_DIR ?? '../../run');
const JAR = process.env.PAPER_JAR ?? 'paper-26.1.2-74.jar';
const JAVA = process.env.JAVA ?? 'java';
const WORLD = 'minecraft:greensky_world';
const BASE_Y = 100; // islands.base-y
const LEVELS = [100, 150, 200, 300, 500]; // islands.expansion-levels
let HALF = 50; // metade do tamanho atual da ilha; lido do /island info

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const log = createWriteStream(resolve(RUN_DIR, 'e2e.log'));
const results = [];

function check(name, ok, detail = '') {
  results.push({ name, ok });
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${detail ? '  (' + detail + ')' : ''}`);
}

// ---------- servidor ----------
const server = spawn(JAVA, ['-Xmx1G', '-Dstdout.encoding=UTF-8', '-jar', JAR, '--nogui'], {
  cwd: RUN_DIR,
  env: process.env,
});
const waiters = [];
readline.createInterface({ input: server.stdout }).on('line', (raw) => {
  const line = raw.replace(/\x1b\[[0-9;]*m/g, '');
  log.write(line + '\n');
  for (const w of [...waiters]) {
    if (w.re.test(line)) {
      waiters.splice(waiters.indexOf(w), 1);
      clearTimeout(w.timer);
      w.resolve(line);
    }
  }
});
server.stderr.on('data', (d) => log.write(d));

function waitLine(re, ms = 20000) {
  return new Promise((resolveFn, reject) => {
    const w = { re, resolve: resolveFn };
    w.timer = setTimeout(() => {
      waiters.splice(waiters.indexOf(w), 1);
      reject(new Error('timeout esperando ' + re));
    }, ms);
    waiters.push(w);
  });
}
const cmd = (c) => server.stdin.write(c + '\n');

/** Pergunta ao servidor (autoridade) qual bloco existe na posição. */
async function isBlock(x, y, z, block) {
  const answer = waitLine(/Test (passed|failed)|not loaded|Incorrect|Unknown/);
  cmd(`execute in ${WORLD} if block ${x} ${y} ${z} minecraft:${block}`);
  return (await answer).includes('Test passed');
}
async function inWorld(c) {
  cmd(`execute in ${WORLD} run ${c}`);
  await sleep(300);
}

// ---------- bots ----------
function connect(name) {
  return new Promise((resolveFn, reject) => {
    const bot = mineflayer.createBot({ host: '127.0.0.1', port: 25565, username: name, version: '26.1.2', auth: 'offline' });
    bot.messages = [];
    bot.borders = [];
    bot.on('messagestr', (m) => {
      bot.messages.push(m);
      log.write(`[${name}] ${m}\n`);
    });
    // Pacotes de borda do mundo que chegam ao cliente (a borda por jogador é só visual).
    bot._client.on('packet', (data, meta) => {
      if (meta.name.includes('border')) {
        bot.borders.push({ name: meta.name, data });
        log.write(`[${name}] pacote ${meta.name} ${JSON.stringify(data)}\n`);
      }
    });
    bot.once('spawn', () => resolveFn(bot));
    bot.once('error', reject);
    bot.once('kicked', (r) => reject(new Error(`${name} expulso: ${JSON.stringify(r)}`)));
  });
}
async function say(bot, text, expect, ms = 15000) {
  const start = bot.messages.length;
  bot.chat(text);
  const until = Date.now() + ms;
  while (Date.now() < until) {
    const found = bot.messages.slice(start).find((m) => expect.test(m));
    if (found) return found;
    await sleep(100);
  }
  throw new Error(`${bot.username}: sem resposta ${expect} para "${text}". Recebido: ${bot.messages.slice(start)}`);
}
async function moveTo(bot, x, y, z) {
  await inWorld(`tp ${bot.username} ${x + 0.5} ${y} ${z + 0.5}`);
  await sleep(1500);
  await bot.waitForChunksToLoad();
}
async function place(bot, x, y, z, item = 'dirt') {
  const held = bot.inventory.items().find((i) => i.name === item);
  if (!held) throw new Error(`${bot.username} sem ${item}`);
  await bot.equip(held, 'hand');
  const below = bot.blockAt(new Vec3(x, y - 1, z));
  try {
    await bot.placeBlock(below, new Vec3(0, 1, 0));
  } catch {
    // Negado pelo servidor: o mineflayer desiste. O resultado real é conferido no console.
  }
  await sleep(800);
}
async function dig(bot, x, y, z) {
  try {
    await bot.dig(bot.blockAt(new Vec3(x, y, z)), true);
  } catch {
    // idem
  }
  await sleep(800);
}
async function canOpen(bot, x, y, z) {
  try {
    const win = await Promise.race([
      bot.openContainer(bot.blockAt(new Vec3(x, y, z))),
      sleep(4000).then(() => null),
    ]);
    if (win) {
      win.close();
      await sleep(300);
      return true;
    }
  } catch {
    // negado
  }
  return false;
}

/** Última borda recebida pelo cliente: centro e diâmetro, seja qual for o pacote que os trouxe. */
function lastBorder(bot) {
  let x;
  let z;
  let size;
  for (const { data } of bot.borders) {
    if (data.x !== undefined) x = data.x;
    if (data.z !== undefined) z = data.z;
    const d = data.newDiameter ?? data.diameter ?? data.size;
    if (d !== undefined) size = d;
  }
  return size === undefined ? null : { x, z, size };
}

// ---------- roteiro ----------
async function main() {
  await waitLine(/Done \(/, 180000);
  await waitLine(/Prote.+ativa/, 30000).catch(() => {});
  await sleep(3000);

  const alice = await connect('Alice');
  const bob = await connect('Bob');
  await sleep(2000);

  // --- Fase 4 com jogador real: create / home / info ---
  const created = await say(alice, '/island create', /Ilha criada|já tem uma ilha/);
  if (created.includes('já tem')) await say(alice, '/island home', /./, 3000).catch(() => {});
  await sleep(3000);
  const info = await say(alice, '/island info', /centro/);
  const [, cxs, czs] = info.match(/centro (-?\d+), (-?\d+)/);
  const cx = Number(cxs);
  const cz = Number(czs);
  const size = Number(info.match(/tamanho (\d+)/)[1]);
  HALF = size / 2;
  console.log(`Ilha da Alice: centro ${cx}, ${cz}, tamanho ${size}`);
  const p = alice.entity.position;
  check('create/home: Alice está sobre a própria ilha', Math.abs(p.x - cx) < 3 && Math.abs(p.z - cz) < 3 && p.y > BASE_Y,
    `pos ${p.x.toFixed(1)},${p.y.toFixed(1)},${p.z.toFixed(1)}`);
  check('create: segunda ilha é recusada',
    /já tem uma ilha/.test(await say(alice, '/island create', /já tem uma ilha|Ilha criada/)));

  // Estado conhecido: desfaz o que execuções anteriores deixaram na ilha (console ignora a proteção).
  for (const [x, z] of [[-3, 1], [-2, -1], [-1, -3]]) await inWorld(`setblock ${cx + x} ${BASE_Y + 1} ${cz + z} minecraft:air`);
  for (const [x, z] of [[-1, 1], [1, -3]]) await inWorld(`setblock ${cx + x} ${BASE_Y} ${cz + z} minecraft:grass_block`);
  await inWorld(`fill ${cx + HALF - 9} ${BASE_Y} ${cz - 45} ${cx + HALF + 8} ${BASE_Y + 2} ${cz - 15} minecraft:air`);
  await inWorld(`fill ${cx + 28} ${BASE_Y + 1} ${cz - 40} ${cx + 33} ${BASE_Y + 1} ${cz - 40} minecraft:air`);
  await inWorld('kill @e[type=minecraft:item]');
  cmd('clear Alice');
  cmd('clear Bob');

  cmd('give Alice dirt 32');
  cmd('give Bob dirt 32');
  await sleep(1000);

  // --- Dono pode construir e quebrar ---
  await moveTo(alice, cx, BASE_Y + 1, cz - 2);
  await place(alice, cx - 1, BASE_Y + 1, cz - 3);
  check('dono: construir na própria ilha', await isBlock(cx - 1, BASE_Y + 1, cz - 3, 'dirt'));
  await dig(alice, cx + 1, BASE_Y, cz - 3);
  check('dono: quebrar na própria ilha', await isBlock(cx + 1, BASE_Y, cz - 3, 'air'));

  // --- Visitante (Bob) é bloqueado ---
  await inWorld(`setblock ${cx - 2} ${BASE_Y + 1} ${cz + 3} minecraft:chest`);
  await moveTo(bob, cx - 2, BASE_Y + 1, cz);
  await place(bob, cx - 3, BASE_Y + 1, cz + 1);
  check('visitante: construir negado', await isBlock(cx - 3, BASE_Y + 1, cz + 1, 'air'));
  await dig(bob, cx - 1, BASE_Y, cz + 1);
  check('visitante: quebrar negado', await isBlock(cx - 1, BASE_Y, cz + 1, 'grass_block'));
  check('visitante: abrir baú negado', !(await canOpen(bob, cx - 2, BASE_Y + 1, cz + 3)));

  // --- Ender pearl do visitante não teleporta dentro da ilha alheia ---
  cmd('give Bob ender_pearl 1');
  await sleep(800);
  const before = bob.entity.position.clone();
  await bob.equip(bob.inventory.items().find((i) => i.name === 'ender_pearl'), 'hand');
  await bob.lookAt(new Vec3(cx + 2, BASE_Y, cz - 2));
  bob.activateItem();
  await sleep(3500);
  const moved = bob.entity.position.distanceTo(before);
  check('visitante: pérola não teleporta para ilha alheia', moved < 1.5, `moveu ${moved.toFixed(2)}`);

  // --- Bob vira membro: passa a poder ---
  await say(alice, '/island add Bob', /agora é membro/);
  await sleep(1000);
  await place(bob, cx - 3, BASE_Y + 1, cz + 1);
  check('membro: construir permitido', await isBlock(cx - 3, BASE_Y + 1, cz + 1, 'dirt'));
  await dig(bob, cx - 1, BASE_Y, cz + 1);
  check('membro: quebrar permitido', await isBlock(cx - 1, BASE_Y, cz + 1, 'air'));
  check('membro: abrir baú permitido', await canOpen(bob, cx - 2, BASE_Y + 1, cz + 3));

  // --- Bob removido: volta a ser bloqueado ---
  await say(alice, '/island remove Bob', /removido/);
  await sleep(1000);
  await place(bob, cx - 2, BASE_Y + 1, cz - 1);
  check('ex-membro: construir negado de novo', await isBlock(cx - 2, BASE_Y + 1, cz - 1, 'air'));

  // --- /island home traz de volta ---
  cmd('effect give Alice minecraft:slow_falling 60 0 true');
  await moveTo(alice, cx + 20, BASE_Y + 20, cz + 20);
  await say(alice, '/island home', /./, 2000).catch(() => {});
  await sleep(3000);
  const h = alice.entity.position;
  check('home: volta ao centro da ilha', Math.abs(h.x - cx) < 3 && Math.abs(h.z - cz) < 3, `pos ${h.x.toFixed(1)},${h.z.toFixed(1)}`);

  // --- Fronteira da região (último bloco dentro = cx+49, fora = cx+50) ---
  const edgeIn = cx + HALF - 1;
  const edgeOut = cx + HALF;

  // Explosão fora da ilha não destrói nada; dentro, não passa da fronteira.
  await inWorld(`setblock ${edgeIn} ${BASE_Y + 1} ${cz - 20} minecraft:dirt`);
  await inWorld(`setblock ${edgeOut} ${BASE_Y + 1} ${cz - 20} minecraft:dirt`);
  await inWorld(`summon minecraft:tnt ${edgeOut + 1} ${BASE_Y + 1} ${cz - 20} {fuse:1}`);
  await sleep(2000);
  check('explosão fora: não quebra dentro', await isBlock(edgeIn, BASE_Y + 1, cz - 20, 'dirt'));
  check('explosão fora: não quebra fora', await isBlock(edgeOut, BASE_Y + 1, cz - 20, 'dirt'));
  await inWorld(`setblock ${edgeIn - 1} ${BASE_Y + 1} ${cz - 26} minecraft:dirt`);
  await inWorld(`setblock ${edgeOut} ${BASE_Y + 1} ${cz - 26} minecraft:dirt`);
  await inWorld(`summon minecraft:tnt ${edgeIn} ${BASE_Y + 1} ${cz - 26} {fuse:1}`);
  await sleep(2000);
  check('explosão dentro: quebra dentro', await isBlock(edgeIn - 1, BASE_Y + 1, cz - 26, 'air'));
  check('explosão dentro: não cruza a fronteira', await isBlock(edgeOut, BASE_Y + 1, cz - 26, 'dirt'));

  // Água não escorre para fora da região. O piso precisa ser largo: a água só corre na direção
  // da queda mais próxima (até 4 blocos); sem quedas por perto ela se espalha para todos os lados.
  await inWorld(`fill ${edgeIn - 8} ${BASE_Y} ${cz - 43} ${edgeOut + 8} ${BASE_Y} ${cz - 27} minecraft:stone`);
  await inWorld(`setblock ${edgeIn} ${BASE_Y + 1} ${cz - 35} minecraft:water`);
  await sleep(5000);
  check('água: espalha dentro', await isBlock(edgeIn - 1, BASE_Y + 1, cz - 35, 'water'));
  check('água: espalha dentro, ao longo da borda', await isBlock(edgeIn, BASE_Y + 1, cz - 33, 'water'));
  check('água: não sai da região', await isBlock(edgeOut, BASE_Y + 1, cz - 35, 'air')
    && await isBlock(edgeOut, BASE_Y + 1, cz - 33, 'air'));

  // Pistão: controle dentro funciona; empurrar para fora é cancelado.
  await inWorld(`setblock ${cx + 30} ${BASE_Y + 1} ${cz - 40} minecraft:piston[facing=east]`);
  await inWorld(`setblock ${cx + 31} ${BASE_Y + 1} ${cz - 40} minecraft:dirt`);
  await inWorld(`setblock ${cx + 29} ${BASE_Y + 1} ${cz - 40} minecraft:redstone_block`);
  await sleep(1500);
  check('pistão dentro: empurra (controle)', await isBlock(cx + 32, BASE_Y + 1, cz - 40, 'dirt'));
  await inWorld(`setblock ${edgeIn - 1} ${BASE_Y + 1} ${cz - 45} minecraft:piston[facing=east]`);
  await inWorld(`setblock ${edgeIn} ${BASE_Y + 1} ${cz - 45} minecraft:dirt`);
  await inWorld(`setblock ${edgeIn - 2} ${BASE_Y + 1} ${cz - 45} minecraft:redstone_block`);
  await sleep(1500);
  check('pistão: não empurra para fora', await isBlock(edgeIn, BASE_Y + 1, cz - 45, 'dirt')
    && await isBlock(edgeOut, BASE_Y + 1, cz - 45, 'air'));

  // --- Fase 6: expansão ---
  // Borda visual: dentro da ilha, Alice recebe uma borda do tamanho da região, centrada na ilha.
  await moveTo(alice, cx, BASE_Y + 1, cz);
  await sleep(1000);
  const border = lastBorder(alice);
  check('borda: tamanho e centro da região', border && border.size === size && border.x === cx && border.z === cz,
    JSON.stringify(border));

  const next = LEVELS.find((l) => l > size);
  if (!next) {
    console.log(`(expansão não testada: a ilha já está no nível máximo ${size}; apague a ilha da Alice para repetir)`);
  } else {
    const spot = cx + HALF + 3; // 3 blocos além da borda atual, dentro do próximo nível
    await inWorld(`fill ${spot - 3} ${BASE_Y} ${cz - 3} ${spot + 3} ${BASE_Y} ${cz + 3} minecraft:stone`);
    await inWorld(`fill ${spot - 3} ${BASE_Y + 1} ${cz - 3} ${spot + 3} ${BASE_Y + 2} ${cz + 3} minecraft:air`);
    await moveTo(alice, spot, BASE_Y + 1, cz);
    await place(alice, spot + 1, BASE_Y + 1, cz);
    check('expansão: antes, fora da região é negado até ao dono', await isBlock(spot + 1, BASE_Y + 1, cz, 'air'));
    check('borda: fora de ilha volta à borda normal', (lastBorder(alice)?.size ?? 0) > next,
      JSON.stringify(lastBorder(alice)));

    const expanded = waitLine(/expandida para|tamanho máximo|expandida por outra/);
    cmd('island admin expand Alice');
    check('expansão: comando de admin', (await expanded).includes(`expandida para ${next}x${next}`));
    await sleep(1500);
    await place(alice, spot + 1, BASE_Y + 1, cz);
    check('expansão: área nova liberada para o dono', await isBlock(spot + 1, BASE_Y + 1, cz, 'dirt'));
    const grown = lastBorder(alice);
    check('expansão: borda cresce na hora, mesmo centro', grown && grown.size === next && grown.x === cx && grown.z === cz,
      JSON.stringify(grown));
    const info2 = await say(alice, '/island info', /centro/);
    check('expansão: ilha não se moveu', info2.includes(`centro ${cx}, ${cz}`) && info2.includes(`tamanho ${next}`), info2);

    await place(bob, cx - 2, BASE_Y + 1, cz - 1);
    check('expansão: visitante continua bloqueado', await isBlock(cx - 2, BASE_Y + 1, cz - 1, 'air'));
  }

  alice.quit();
  bob.quit();
}

let exitCode = 1;
try {
  await main();
  exitCode = results.every((r) => r.ok) ? 0 : 1;
} catch (e) {
  console.error('ERRO:', e.message);
} finally {
  const passed = results.filter((r) => r.ok).length;
  console.log(`\n${passed}/${results.length} verificações passaram.`);
  await sleep(1000);
  cmd('stop');
  await Promise.race([new Promise((r) => server.once('exit', r)), sleep(60000)]);
  log.end();
  process.exit(exitCode);
}
