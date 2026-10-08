// Fase 7 (First Playable): o ciclo mínimo completo do README, com bots reais e restart no meio.
// entrar -> criar ilha -> construir -> expandir -> visitar -> voltar -> persistir -> restart -> continuar
// Uso:  JAVA=".../java.exe" node first-playable-e2e.mjs   (requisitos em lib.mjs)

import {
  BASE_Y, check, cmd, connect, finish, heard, inWorld, isBlock, islandInfo, lastBorder, moveTo, place, say,
  sleep, startServer, stopServer, waitLine,
} from './lib.mjs';

// Jogador novo a cada execução: o teste começa sempre do zero, como alguém entrando pela primeira vez.
const NEWBIE = 'Novo' + String(Date.now()).slice(-8);

const near = (bot, cx, cz, d = 3) =>
  Math.abs(bot.entity.position.x - cx) < d && Math.abs(bot.entity.position.z - cz) < d;

async function main() {
  await startServer();

  // --- entrar ---
  let newbie = await connect(NEWBIE);
  check('entrar: jogador novo recebe a dica /is create', await heard(newbie, /Bem-vindo ao GreenSky/, 0));

  // --- criar ilha ---
  await say(newbie, '/island create', /Ilha criada/);
  await sleep(3000);
  const { cx, cz, size } = await islandInfo(newbie);
  console.log(`Ilha de ${NEWBIE}: centro ${cx}, ${cz}, tamanho ${size}`);
  check('criar: ilha nova no nível 1 e jogador sobre ela', size === 100 && near(newbie, cx, cz) && newbie.entity.position.y > BASE_Y);

  // --- construir ---
  cmd(`give ${NEWBIE} dirt 32`);
  await sleep(800);
  await place(newbie, cx - 1, BASE_Y + 1, cz - 2);
  check('construir: na própria ilha', await isBlock(cx - 1, BASE_Y + 1, cz - 2, 'dirt'));

  // --- expandir ---
  const grown = waitLine(/expandida para/);
  cmd(`island admin expand ${NEWBIE}`);
  check('expandir: 100 -> 150', (await grown).includes('expandida para 150x150'));
  const spot = cx + 53; // fora dos 100x100, dentro dos 150x150
  await inWorld(`fill ${spot - 2} ${BASE_Y} ${cz - 2} ${spot + 2} ${BASE_Y} ${cz + 2} minecraft:stone`);
  await moveTo(newbie, spot, BASE_Y + 1, cz);
  await place(newbie, spot + 1, BASE_Y + 1, cz);
  check('expandir: constrói na área nova', await isBlock(spot + 1, BASE_Y + 1, cz, 'dirt'));
  check('expandir: borda visual de 150', lastBorder(newbie)?.size === 150, JSON.stringify(lastBorder(newbie)));

  // --- visitar ---
  const carol = await connect('Carol');
  await say(carol, `/island visit ${NEWBIE}`, /Visitando/);
  await sleep(2500);
  check('visitar: ilha pública recebe visitante', near(carol, cx, cz));
  cmd('give Carol dirt 8');
  await sleep(800);
  await place(carol, cx + 1, BASE_Y + 1, cz - 1);
  check('visitar: visitante não constrói', await isBlock(cx + 1, BASE_Y + 1, cz - 1, 'air'));

  const before = carol.messages.length;
  await say(newbie, '/island private', /agora é privada/);
  await sleep(2500);
  check('privada: visitante é avisado e levado ao spawn',
    (await heard(carol, /agora é privada/, before)) && !near(carol, cx, cz, 200));
  check('privada: nova visita recusada', /é privada/.test(await say(carol, `/island visit ${NEWBIE}`, /privada|Visitando/)));

  const erin = await connect('Erin');
  await say(erin, '/island remove Erin', /./, 2000).catch(() => {}); // ruído inofensivo
  await say(newbie, '/island add Erin', /agora é membro/);
  await say(erin, `/island visit ${NEWBIE}`, /Visitando/);
  await sleep(2500);
  check('privada: membro visita mesmo assim', near(erin, cx, cz));

  // --- voltar (morrer e renascer na ilha) ---
  await moveTo(newbie, cx + 20, BASE_Y + 30, cz + 20);
  cmd(`kill ${NEWBIE}`);
  await sleep(4000);
  check('voltar: renasce na própria ilha', near(newbie, cx, cz) && newbie.entity.position.y > BASE_Y,
    `pos ${newbie.entity.position.x.toFixed(1)},${newbie.entity.position.y.toFixed(1)},${newbie.entity.position.z.toFixed(1)}`);

  // --- persistir + restart ---
  for (const bot of [newbie, carol, erin]) bot.quit();
  await sleep(1000);
  await stopServer();
  console.log('-- servidor reiniciado --');
  await startServer();

  // --- continuar ---
  newbie = await connect(NEWBIE);
  const again = await islandInfo(newbie);
  check('restart: mesma ilha, mesmo centro, tamanho 150', again.cx === cx && again.cz === cz && again.size === 150, again.text);
  check('restart: sem dica de jogador novo (já tem ilha)', !(await heard(newbie, /Bem-vindo ao GreenSky/, 0, 4000)));
  // Terra sobre grama ao ar livre vira grama com o tempo (espalhamento vanilla): os dois contam como "continua".
  check('restart: blocos construídos continuam',
    (await isBlock(cx - 1, BASE_Y + 1, cz - 2, 'dirt') || await isBlock(cx - 1, BASE_Y + 1, cz - 2, 'grass_block'))
    && await isBlock(spot + 1, BASE_Y + 1, cz, 'dirt'));

  await say(newbie, '/island home', /./, 3000).catch(() => {});
  await sleep(2500);
  check('restart: /is home funciona e a borda é a de 150', near(newbie, cx, cz) && lastBorder(newbie)?.size === 150,
    JSON.stringify(lastBorder(newbie)));
  cmd(`give ${NEWBIE} dirt 8`);
  await sleep(800);
  await place(newbie, cx - 2, BASE_Y + 1, cz - 3);
  check('continuar: dono constrói depois do restart', await isBlock(cx - 2, BASE_Y + 1, cz - 3, 'dirt'));

  const carol2 = await connect('Carol');
  check('restart: ilha continua privada', /é privada/.test(await say(carol2, `/island visit ${NEWBIE}`, /privada|Visitando/)));

  const erin2 = await connect('Erin');
  await sleep(1500);
  await say(erin2, `/island visit ${NEWBIE}`, /Visitando/);
  await sleep(2500);
  cmd('give Erin dirt 8');
  await sleep(800);
  await place(erin2, cx + 1, BASE_Y + 1, cz + 1);
  check('restart: membro continua membro e constrói', await isBlock(cx + 1, BASE_Y + 1, cz + 1, 'dirt'));

  for (const bot of [newbie, carol2, erin2]) bot.quit();
}

try {
  await main();
  await finish();
} catch (e) {
  await finish(e);
}
