// Apoio aos testes de ponta a ponta: sobe/derruba o Paper de teste (run/), fala com o console,
// conecta bots reais (mineflayer) e confere blocos pelo servidor (a autoridade).
//
// Requisitos: run/ com o jar do Paper e o plugin em run/plugins, server.properties com
// online-mode=false e server-ip=127.0.0.1 (SÓ no servidor de teste), PostgreSQL rodando e
// GREENSKY_DB_PASSWORD no ambiente.

import { spawn } from 'node:child_process';
import { createWriteStream } from 'node:fs';
import { resolve } from 'node:path';
import readline from 'node:readline';
import mineflayer from 'mineflayer';
import { Vec3 } from 'vec3';

export { Vec3 };

export const RUN_DIR = resolve(process.env.RUN_DIR ?? '../../run');
const JAR = process.env.PAPER_JAR ?? 'paper-26.1.2-74.jar';
const JAVA = process.env.JAVA ?? 'java';
export const WORLD = 'minecraft:greensky_world';
export const BASE_Y = 100; // islands.base-y
export const LEVELS = [100, 150, 200, 300, 500]; // islands.expansion-levels

export const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
export const log = createWriteStream(resolve(RUN_DIR, process.env.E2E_LOG ?? 'e2e.log'));
export const results = [];

export function check(name, ok, detail = '') {
  results.push({ name, ok });
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${detail ? '  (' + detail + ')' : ''}`);
}

// ---------- servidor (pode ser reiniciado no meio do teste) ----------
let proc = null;
const waiters = [];

export async function startServer() {
  proc = spawn(JAVA, ['-Xmx1G', '-Dstdout.encoding=UTF-8', '-jar', JAR, '--nogui'], { cwd: RUN_DIR, env: process.env });
  readline.createInterface({ input: proc.stdout }).on('line', (raw) => {
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
  proc.stderr.on('data', (d) => log.write(d));
  const done = waitLine(/Done \(/, 180000);
  const protection = waitLine(/Prote.+ativa/, 180000);
  await done;
  await protection.catch(() => {});
  await sleep(2000);
}

export async function stopServer() {
  if (!proc) return;
  const exited = new Promise((r) => proc.once('exit', r));
  cmd('stop');
  await Promise.race([exited, sleep(60000)]);
  proc = null;
}

export function waitLine(re, ms = 20000) {
  return new Promise((resolveFn, reject) => {
    const w = { re, resolve: resolveFn };
    w.timer = setTimeout(() => {
      waiters.splice(waiters.indexOf(w), 1);
      reject(new Error('timeout esperando ' + re));
    }, ms);
    waiters.push(w);
  });
}

export const cmd = (c) => proc.stdin.write(c + '\n');

/** Pergunta ao servidor (autoridade) qual bloco existe na posição. */
export async function isBlock(x, y, z, block) {
  const answer = waitLine(/Test (passed|failed)|not loaded|Incorrect|Unknown/);
  cmd(`execute in ${WORLD} if block ${x} ${y} ${z} minecraft:${block}`);
  return (await answer).includes('Test passed');
}

export async function inWorld(c) {
  cmd(`execute in ${WORLD} run ${c}`);
  await sleep(300);
}

// ---------- bots ----------
export function connect(name) {
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

export async function say(bot, text, expect, ms = 15000) {
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

/** Espera uma mensagem que já pode ter chegado depois de {@code fromIndex}. */
export async function heard(bot, expect, fromIndex, ms = 10000) {
  const until = Date.now() + ms;
  while (Date.now() < until) {
    if (bot.messages.slice(fromIndex).some((m) => expect.test(m))) return true;
    await sleep(100);
  }
  return false;
}

export async function moveTo(bot, x, y, z) {
  await inWorld(`tp ${bot.username} ${x + 0.5} ${y} ${z + 0.5}`);
  await sleep(1500);
  await bot.waitForChunksToLoad();
}

export async function place(bot, x, y, z, item = 'dirt') {
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

export async function dig(bot, x, y, z) {
  try {
    await bot.dig(bot.blockAt(new Vec3(x, y, z)), true);
  } catch {
    // idem
  }
  await sleep(800);
}

export async function canOpen(bot, x, y, z) {
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
export function lastBorder(bot) {
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

/** Centro e tamanho da ilha, lidos do /island info do dono. */
export async function islandInfo(owner) {
  const info = await say(owner, '/island info', /centro/);
  const [, cx, cz] = info.match(/centro (-?\d+), (-?\d+)/);
  return { cx: Number(cx), cz: Number(cz), size: Number(info.match(/tamanho (\d+)/)[1]), text: info };
}

/** Termina o processo com o resumo; código 0 só se tudo passou. */
export async function finish(error) {
  if (error) console.error('ERRO:', error.message);
  const passed = results.filter((r) => r.ok).length;
  console.log(`\n${passed}/${results.length} verificações passaram.`);
  await sleep(1000);
  await stopServer();
  log.end();
  process.exit(!error && results.every((r) => r.ok) ? 0 : 1);
}
