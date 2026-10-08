// Agricultura: plantas maduras, permissões, automação e persistência usando nLogin real.
import { BASE_Y, check, cmd, connect, finish, inWorld, islandInfo, moveTo, say, sleep, startServer,
  stopServer, waitLine, Vec3 } from './lib.mjs';
const stamp = String(Date.now()).slice(-7);
const OWNER = 'Farm' + stamp;
const VISITOR = 'Fvis' + stamp;

async function totals(bot) {
  const start = bot.messages.length;
  bot.chat('/collections farming');
  await sleep(1200);
  return bot.messages.slice(start).join(' | ');
}

async function main() {
  await startServer();
  let owner = await connect(OWNER);
  await say(owner, '/is create', /Ilha criada/);
  await sleep(2000);
  const { cx, cz } = await islandInfo(owner);
  cmd(`gamemode survival ${OWNER}`);
  await inWorld(`fill ${cx - 2} ${BASE_Y} ${cz - 2} ${cx + 5} ${BASE_Y} ${cz + 2} minecraft:stone`);
  await inWorld(`setblock ${cx + 2} ${BASE_Y} ${cz} minecraft:farmland[moisture=7]`);
  await moveTo(owner, cx, BASE_Y + 1, cz);
  async function plant(block, age) {
    await inWorld(`setblock ${cx + 2} ${BASE_Y + 1} ${cz} minecraft:${block}[age=${age}]`);
    await sleep(300);
  }
  async function harvest(bot) {
    await bot.dig(bot.blockAt(new Vec3(cx + 2, BASE_Y + 1, cz)));
    await sleep(800);
  }
  await plant('wheat', 0);
  await harvest(owner);
  check('planta imatura não conta', !(await totals(owner)).includes('✔ Trigo'));
  await plant('wheat', 7);
  await harvest(owner);
  check('trigo maduro conta uma planta', /✔ Trigo \[Comum\] x1/.test(await totals(owner)));
  await moveTo(owner, cx + 2, BASE_Y + 1, cz);
  await sleep(1200);
  await moveTo(owner, cx, BASE_Y + 1, cz);
  const wheat = owner.inventory.items().find(item => item.name === 'wheat');
  check('produto colhido chega ao inventário', Boolean(wheat));
  if (wheat) await owner.equip(wheat, 'hand');
  cmd(`op ${OWNER}`);
  await sleep(500);
  check('produto agrícola tem identidade do GreenSky',
    /Item do GreenSky: crop.wheat/.test(await say(owner, '/greensky item check', /Item do GreenSky|Não é item/)));
  cmd(`deop ${OWNER}`);
  const qualityLine = waitLine(/has the following entity data:.*crop_quality/);
  cmd(`data get entity ${OWNER} Inventory`);
  const qualityData = await qualityLine;
  const originalQuality = qualityData.match(/greensky:crop_quality[^\d]*([123])/);
  check('qualidade de 1..3 estrelas gravada no item', Boolean(originalQuality));
  await plant('carrots', 7);
  await harvest(owner);
  check('cenoura madura conta uma planta', /✔ Cenoura \[Comum\] x1/.test(await totals(owner)));
  await plant('potatoes', 7);
  await harvest(owner);
  check('batata madura conta uma planta', /✔ Batata \[Comum\] x1/.test(await totals(owner)));
  const visitor = await connect(VISITOR);
  await say(visitor, `/is visit ${OWNER}`, /Visitando/);
  await plant('wheat', 7);
  try { await harvest(visitor); } catch { /* quebra negada pela proteção */ }
  check('visitante não ganha progresso', !(await totals(visitor)).includes('✔ Trigo'));
  check('visitante não destrói a planta', owner.blockAt(new Vec3(cx + 2, BASE_Y + 1, cz)).name === 'wheat');
  visitor.quit();
  await inWorld(`setblock ${cx + 2} ${BASE_Y + 1} ${cz} minecraft:air destroy`);
  await sleep(500);
  check('quebra automática não conta', /✔ Trigo \[Comum\] x1/.test(await totals(owner)));
  owner.quit();
  await sleep(1000);
  await stopServer();
  await startServer();
  owner = await connect(OWNER);
  await sleep(1500);
  check('progresso agrícola persiste após restart', /✔ Trigo \[Comum\] x1/.test(await totals(owner)));
  const persistedLine = waitLine(/has the following entity data:.*crop_quality/);
  cmd(`data get entity ${OWNER} Inventory`);
  const persisted = await persistedLine;
  check('qualidade do produto persiste após restart',
    originalQuality && persisted.match(/greensky:crop_quality[^\d]*([123])/)?.[1] === originalQuality[1]);
  owner.quit();
}
try { await main(); await finish(); } catch (error) { await finish(error); }
