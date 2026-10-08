import { BASE_Y, Vec3, check, cmd, connect, finish, inWorld, islandInfo, moveTo, say, sleep,
  startServer, stopServer, waitLine } from './lib.mjs';
const stamp = String(Date.now()).slice(-7);
const OWNER = 'Fert' + stamp, VISITOR = 'Vfert' + stamp;
const count = bot => bot.inventory.items().filter(i => i.name === 'wheat').reduce((n, i) => n + i.count, 0);
async function give(bot, special = true) {
  cmd(`clear ${bot.username}`);
  cmd(`give ${bot.username} minecraft:wheat${special ? '[minecraft:custom_data={PublicBukkitValues:{"greensky:item":"crop.golden_wheat"}}]' : ''} 2`);
  await sleep(700);
  await bot.equip(bot.inventory.items().find(i => i.name === 'wheat'), 'hand');
}
async function main() {
  await startServer();
  let owner = await connect(OWNER);
  await say(owner, '/is create', /Ilha criada/); await sleep(1500);
  const { cx, cz } = await islandInfo(owner);
  await inWorld('gamerule randomTickSpeed 0');
  await inWorld('time set day');
  await inWorld(`fill ${cx - 2} ${BASE_Y} ${cz - 2} ${cx + 4} ${BASE_Y} ${cz + 2} minecraft:stone`);
  const plant = new Vec3(cx + 2, BASE_Y + 1, cz), soil = plant.offset(0, -1, 0);
  await inWorld(`setblock ${soil.x} ${soil.y} ${soil.z} minecraft:farmland[moisture=7]`);
  await inWorld(`setblock ${plant.x} ${plant.y} ${plant.z} minecraft:carrots[age=0]`);
  await moveTo(owner, cx, BASE_Y + 1, cz);
  const visitor = await connect(VISITOR);
  await say(visitor, `/is visit ${OWNER}`, /Visitando/); await give(visitor);
  await visitor.activateBlock(visitor.blockAt(plant)); await sleep(700);
  check('visitante não fertiliza nem perde item', count(visitor) === 2 && owner.blockAt(plant).metadata === 0);
  visitor.quit();
  await give(owner, false);
  await owner.activateBlock(owner.blockAt(plant)); await sleep(700);
  check('trigo comum não fertiliza', count(owner) === 2 && owner.blockAt(plant).metadata === 0);
  await give(owner);
  await owner.activateBlock(owner.blockAt(plant)); await sleep(700);
  check('fertilizante cresce dois estágios e consome um', count(owner) === 1 && owner.blockAt(plant).metadata === 2);
  await owner.activateBlock(owner.blockAt(plant)); await sleep(700);
  check('segunda aplicação não cresce nem consome', count(owner) === 1 && owner.blockAt(plant).metadata === 2);
  await inWorld(`setblock ${cx + 1} ${BASE_Y + 1} ${cz} minecraft:water`); await sleep(900);
  check('água preserva cultura fertilizada', owner.blockAt(plant).name === 'carrots');
  await inWorld(`setblock ${cx + 1} ${BASE_Y + 1} ${cz} minecraft:air`);
  owner.quit(); await sleep(700); await stopServer();
  await startServer(); owner = await connect(OWNER); await sleep(1500);
  await moveTo(owner, cx, BASE_Y + 1, cz);
  await owner.equip(owner.inventory.items().find(i => i.name === 'wheat'), 'hand');
  await owner.activateBlock(owner.blockAt(plant)); await sleep(700);
  check('tratamento persiste e impede reaplicação após restart', count(owner) === 1 && owner.blockAt(plant).metadata === 2);
  cmd(`give ${OWNER} minecraft:bone_meal 64`); await sleep(700);
  await owner.equip(owner.inventory.items().find(i => i.name === 'bone_meal'), 'hand');
  for (let i = 0; i < 12 && owner.blockAt(plant).metadata !== 7; i++) {
    await owner.activateBlock(owner.blockAt(plant)); await sleep(400);
  }
  check('cultura fertilizada amadurece', owner.blockAt(plant).metadata === 7);
  await owner.equip(owner.inventory.items().find(i => i.name === 'wheat'), 'hand');
  await owner.activateBlock(owner.blockAt(plant)); await sleep(500);
  check('planta madura não consome fertilizante', count(owner) === 1);
  await owner.dig(owner.blockAt(plant));
  await moveTo(owner, cx + 2, BASE_Y + 1, cz); await sleep(1100);
  const result = waitLine(/has the following entity data:.*crop_quality/);
  cmd(`data get entity ${OWNER} Inventory`);
  const inventory = await result;
  check('cenoura colhida tem qualidade três', /greensky:crop_quality[^\d]*3/.test(inventory) && /crop.carrot/.test(inventory));
  await moveTo(owner, cx, BASE_Y + 1, cz);
  await inWorld(`setblock ${plant.x} ${plant.y} ${plant.z} minecraft:potatoes[age=0]`);
  await owner.activateBlock(owner.blockAt(plant)); await sleep(600);
  check('colheita limpa tratamento e permite fertilizar nova planta', count(owner) === 0 && owner.blockAt(plant).metadata === 2);
  await owner.dig(owner.blockAt(plant)); await sleep(500);
  await inWorld(`setblock ${plant.x} ${plant.y} ${plant.z} minecraft:wheat[age=6]`);
  await give(owner);
  await owner.activateBlock(owner.blockAt(plant)); await sleep(600);
  check('quebra imatura limpa efeito e avanço respeita máximo', count(owner) === 1 && owner.blockAt(plant).metadata === 7);
  await owner.dig(owner.blockAt(plant)); await sleep(500);
  await moveTo(owner, cx + 2, BASE_Y + 1, cz); await sleep(1000);
  await moveTo(owner, cx, BASE_Y + 1, cz);
  await give(owner);
  await inWorld(`setblock ${plant.x} ${plant.y} ${plant.z} minecraft:wheat[age=0]`);
  cmd(`gamemode creative ${OWNER}`); await sleep(500);
  await owner.activateBlock(owner.blockAt(plant)); await sleep(500);
  check('criativo não aplica nem consome', count(owner) === 2 && owner.blockAt(plant).metadata === 0);
  cmd(`gamemode survival ${OWNER}`); await sleep(500);
  await owner.dig(owner.blockAt(plant)); await sleep(500);
  cmd(`clear ${OWNER}`);
  cmd(`give ${OWNER} minecraft:wheat_seeds[minecraft:custom_data={PublicBukkitValues:{"greensky:item":"crop.ancient_seed"}}] 1`);
  await sleep(700);
  await owner.equip(owner.inventory.items().find(i => i.name === 'wheat_seeds'), 'hand');
  await owner.activateBlock(owner.blockAt(soil), new Vec3(0, 1, 0)); await sleep(600);
  await give(owner);
  await owner.activateBlock(owner.blockAt(plant)); await sleep(600);
  check('planta ancestral não aceita fertilizante', count(owner) === 2
    && owner.blockAt(plant).name === 'wheat' && owner.blockAt(plant).metadata === 0);
  await inWorld('gamerule randomTickSpeed 3');
  owner.quit();
}
try { await main(); await finish(); } catch (error) {
  try { await inWorld('gamerule randomTickSpeed 3'); } catch {}
  await finish(error);
}
