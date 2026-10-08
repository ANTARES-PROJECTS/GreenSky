// Fase 8.1: coleções e itens do GreenSky, com bot real (nLogin) e restart no meio.
// Uso:  JAVA=".../java.exe" node collections-e2e.mjs   (requisitos em lib.mjs)

import { check, cmd, connect, finish, heard, say, sleep, startServer, stopServer, waitLine } from './lib.mjs';

const NICK = 'Col' + String(Date.now()).slice(-8); // jogador novo: progresso começa vazio

/** Pede ao console uma concessão de coleção e devolve o resultado (DISCOVERED, PROGRESSED...). */
async function grant(key, amount) {
  const line = waitLine(/Coleção: .* -> [A-Z_]+/);
  cmd(`collections admin grant ${NICK} ${key} ${amount}`);
  return (await line).match(/-> ([A-Z_]+)/)[1];
}

/** Manda o comando e junta as linhas que chegarem no chat por alguns segundos. */
async function chatLines(bot, text, ms = 2500) {
  const start = bot.messages.length;
  bot.chat(text);
  await sleep(ms);
  return bot.messages.slice(start);
}

async function main() {
  await startServer();
  let bot = await connect(NICK);
  // Este roteiro valida coleções/inventário; mobs do spawn não podem matar o bot entre as conferências.
  cmd(`gamemode creative ${NICK}`);
  await sleep(1500);

  const overview = await chatLines(bot, '/collections texto');
  check('início: coleções zeradas', overview.some((m) => /Pesca \(fishing\) .*0%  0\/7/.test(m)), overview.join(' | '));

  // --- descoberta: uma única vez ---
  let mark = bot.messages.length;
  check('primeiro bacalhau: DISCOVERED', (await grant('fish.cod', 3)) === 'DISCOVERED');
  check('jogador vê "Descoberto: Bacalhau" com o progresso da coleção',
    await heard(bot, /Descoberto: Bacalhau \[Comum\].*Pesca 1\/7/, mark, 5000));
  mark = bot.messages.length;
  check('segundo registro: PROGRESSED', (await grant('fish.cod', 2)) === 'PROGRESSED');
  await sleep(1500);
  check('sem nova mensagem de descoberta', !bot.messages.slice(mark).some((m) => /Descoberto/.test(m)));
  check('entrada inexistente: UNKNOWN_ENTRY', (await grant('fish.nao_existe', 1)) === 'UNKNOWN_ENTRY');
  check('quantidade inválida é recusada', await (async () => {
    const line = waitLine(/Quantidade inválida|Coleção:/);
    cmd(`collections admin grant ${NICK} fish.cod -4`);
    return /Quantidade inválida/.test(await line);
  })());

  const fishing = await chatLines(bot, '/collections fishing');
  check('detalhe: bacalhau x5', fishing.some((m) => /✔ Bacalhau \[Comum\] x5/.test(m)), fishing.join(' | '));
  check('detalhe: secreta aparece como ???', fishing.some((m) => /✘ \?\?\?/.test(m)));
  check('detalhe: não descoberta aparece apagada', fishing.some((m) => /✘ Salmão/.test(m)));

  // --- itens do GreenSky: identidade escondida, não o nome ---
  cmd(`op ${NICK}`);
  await sleep(1000);
  cmd(`greensky item give ${NICK} relic.heart_of_ancient_forest`);
  await sleep(1500);
  const relic = bot.inventory.items().find((i) => i.name === 'heart_of_the_sea');
  check('item entregue ao jogador', Boolean(relic));
  if (relic) await bot.equip(relic, 'hand');
  await sleep(500);
  check('item real é reconhecido', (await chatLines(bot, '/greensky item check')).some((m) => /Item do GreenSky: relic\.heart_of_ancient_forest/.test(m)));

  // Falsificação: item comum com o MESMO nome. Fica num slot separado para segurar só ele.
  cmd(`clear ${NICK} minecraft:heart_of_the_sea`);
  await sleep(800);
  cmd(`give ${NICK} minecraft:heart_of_the_sea[minecraft:item_name="Heart of the Ancient Forest",minecraft:custom_name="Heart of the Ancient Forest"]`);
  await sleep(1500);
  const fake = bot.inventory.items().find((i) => i.name === 'heart_of_the_sea');
  check('cópia falsificada entregue', Boolean(fake));
  if (fake) await bot.equip(fake, 'hand');
  await sleep(500);
  check('cópia com o mesmo nome NÃO é item do GreenSky',
    (await chatLines(bot, '/greensky item check')).some((m) => /Não é item do GreenSky/.test(m)));
  cmd(`deop ${NICK}`);
  await sleep(500);

  // --- restart: o progresso persiste ---
  bot.quit();
  await sleep(1500);
  await stopServer();
  console.log('-- servidor reiniciado --');
  await startServer();
  bot = await connect(NICK);
  await sleep(1500);
  const after = await chatLines(bot, '/collections fishing');
  check('depois do restart: bacalhau continua x5', after.some((m) => /✔ Bacalhau \[Comum\] x5/.test(m)), after.join(' | '));
  mark = bot.messages.length;
  check('depois do restart: não "redescobre"', (await grant('fish.cod', 1)) === 'PROGRESSED');
  await sleep(1000);
  check('sem mensagem de descoberta repetida', !bot.messages.slice(mark).some((m) => /Descoberto/.test(m)));
  bot.quit();
}

try {
  await main();
  await finish();
} catch (e) {
  await finish(e);
}
