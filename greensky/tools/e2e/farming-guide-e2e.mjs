import { BASE_Y, Vec3, check, cmd, connect, finish, inWorld, islandInfo, moveTo, say, sleep,
  startServer } from './lib.mjs';
const OWNER = 'Guide' + String(Date.now()).slice(-7);
async function main() {
  await startServer();
  const owner = await connect(OWNER);
  await say(owner, '/is create', /Ilha criada/); await sleep(1300);
  const { cx, cz } = await islandInfo(owner);
  await inWorld('time set day');
  await inWorld(`fill ${cx - 2} ${BASE_Y} ${cz - 2} ${cx + 4} ${BASE_Y} ${cz + 2} minecraft:stone`);
  const soil = new Vec3(cx + 2, BASE_Y, cz), plant = soil.offset(0, 1, 0);
  await inWorld(`setblock ${soil.x} ${soil.y} ${soil.z} minecraft:farmland[moisture=7]`);
  await inWorld(`setblock ${plant.x} ${plant.y} ${plant.z} minecraft:carrots[age=0]`);
  await moveTo(owner, cx, BASE_Y + 1, cz);
  async function inspect(expect) {
    await owner.lookAt(plant.offset(0.5, 0.1, 0.5), true); await sleep(300);
    return say(owner, '/agricultura texto', expect);
  }
  check('guia disponível para jogador sem OP', /Agricultura/.test(await say(owner, '/agricultura texto', /=== Agricultura ===/)));
  check('inspeção da cultura comum', /Sem fertilizante/.test(await inspect(/Sem fertilizante/)));
  check('jogador comum não consegue gerar itens', /Sem permissão/.test(
    await say(owner, `/greensky item give ${OWNER} crop.golden_wheat 2`, /Sem permissão/)));
  cmd(`greensky item give ${OWNER} crop.golden_wheat 2`); await sleep(800);
  const wheat = owner.inventory.items().find(i => i.name === 'wheat');
  check('console entrega Trigo Dourado pelo catálogo agrícola', wheat?.count === 2);
  await owner.equip(wheat, 'hand');
  cmd(`op ${OWNER}`); await sleep(500);
  check('item administrativo tem identidade correta', /crop.golden_wheat/.test(
    await say(owner, '/greensky item check', /Item do GreenSky/)));
  cmd(`deop ${OWNER}`); await sleep(500);
  await owner.activateBlock(owner.blockAt(plant)); await sleep(600);
  check('produto entregue fertiliza sem OP', owner.blockAt(plant).metadata >= 2
    && owner.inventory.items().find(i => i.name === 'wheat')?.count === 1);
  check('inspeção mostra tratamento real', /Fertilizada: qualidade/.test(await inspect(/Fertilizada: qualidade/)));
  await owner.dig(owner.blockAt(plant)); await sleep(500);
  cmd(`clear ${OWNER}`);
  cmd(`greensky item give ${OWNER} crop.ancient_seed 1`); await sleep(800);
  const seed = owner.inventory.items().find(i => i.name === 'wheat_seeds');
  check('console entrega Semente Ancestral', seed?.count === 1);
  await owner.equip(seed, 'hand');
  await owner.activateBlock(owner.blockAt(soil), new Vec3(0, 1, 0)); await sleep(600);
  check('semente administrativa planta trigo especial', owner.blockAt(plant).name === 'wheat');
  check('inspeção reconhece planta ancestral', /Ancestral: a colheita/.test(await inspect(/Ancestral: a colheita/)));
  const start = owner.messages.length; owner.chat('/collections farming'); await sleep(800);
  check('guia e entrega administrativa não concedem progresso', !owner.messages.slice(start).join('|').includes('✔'));
  owner.quit();
}
try { await main(); await finish(); } catch (error) { await finish(error); }
