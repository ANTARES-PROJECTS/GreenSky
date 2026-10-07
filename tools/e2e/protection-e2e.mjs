// Teste de ponta a ponta da proteção (fase 5) e da expansão (fase 6) com bots reais.
// Uso:  JAVA=".../java.exe" node protection-e2e.mjs   (requisitos em lib.mjs)

import {
  BASE_Y, LEVELS, Vec3, canOpen, check, cmd, connect, dig, finish, inWorld, isBlock, lastBorder,
  moveTo, place, say, sleep, startServer, waitLine,
} from './lib.mjs';

let HALF = 50; // metade do tamanho atual da ilha; lido do /island info

// ---------- roteiro ----------
async function main() {
  await startServer();

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

try {
  await main();
  await finish();
} catch (e) {
  await finish(e);
}
