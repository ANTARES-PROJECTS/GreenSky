import { BASE_Y, Vec3, check, cmd, connect, finish, inWorld, islandInfo, moveTo, say, sleep,
  startServer, stopServer } from './lib.mjs';
const stamp = String(Date.now()).slice(-7);
const OWNER = 'Seed' + stamp, VISITOR = 'Svis' + stamp;
const seedCount = bot => bot.inventory.items().filter(i => i.name === 'wheat_seeds').reduce((n, i) => n + i.count, 0);
async function seed(bot) {
  cmd(`clear ${bot.username}`);
  cmd(`give ${bot.username} minecraft:wheat_seeds[minecraft:custom_data={PublicBukkitValues:{"greensky:item":"crop.ancient_seed"}}] 1`);
  await sleep(1000);
  await bot.equip(bot.inventory.items().find(i => i.name === 'wheat_seeds'), 'hand');
}
async function main() {
  await startServer();
  let owner = await connect(OWNER);
  await say(owner, '/is create', /Ilha criada/);
  await sleep(1500);
  const { cx, cz } = await islandInfo(owner);
  await inWorld('time set day');
  await inWorld(`fill ${cx - 2} ${BASE_Y} ${cz - 2} ${cx + 4} ${BASE_Y} ${cz + 2} minecraft:stone`);
  await inWorld(`setblock ${cx + 2} ${BASE_Y} ${cz} minecraft:farmland[moisture=7]`);
  await inWorld(`setblock ${cx + 3} ${BASE_Y} ${cz} minecraft:water`);
  const soil = new Vec3(cx + 2, BASE_Y, cz), plant = soil.offset(0, 1, 0);
  await moveTo(owner, cx, BASE_Y + 1, cz);
  const visitor = await connect(VISITOR);
  await say(visitor, `/is visit ${OWNER}`, /Visitando/);
  await seed(visitor);
  await visitor.activateBlock(visitor.blockAt(soil), new Vec3(0, 1, 0));
  await sleep(1000);
  check('visitante não planta nem consome semente', seedCount(visitor) === 1 && owner.blockAt(plant).name === 'air');
  visitor.quit();
  await seed(owner);
  await owner.activateBlock(owner.blockAt(soil), new Vec3(0, 1, 0));
  await sleep(1000);
  check('plantio consome uma semente e cria trigo', seedCount(owner) === 0 && owner.blockAt(plant).name === 'wheat');
  await owner.dig(owner.blockAt(plant));
  await moveTo(owner, cx + 2, BASE_Y + 1, cz);
  await sleep(1200);
  check('quebra imatura devolve exatamente uma semente', seedCount(owner) === 1);
  const returned = owner.inventory.items().find(i => i.name === 'wheat_seeds');
  if (returned) await owner.equip(returned, 'hand');
  cmd(`op ${OWNER}`); await sleep(400);
  check('semente devolvida mantém identidade ancestral',
    /Item do GreenSky: crop.ancient_seed/.test(await say(owner, '/greensky item check', /Item do GreenSky|Não é item/)));
  cmd(`deop ${OWNER}`);
  await moveTo(owner, cx, BASE_Y + 1, cz);
  await owner.activateBlock(owner.blockAt(soil), new Vec3(0, 1, 0));
  await sleep(1000);
  await inWorld(`setblock ${cx + 1} ${BASE_Y + 1} ${cz} minecraft:water`);
  await sleep(1200);
  check('água não destrói a planta ancestral', owner.blockAt(plant).name === 'wheat');
  await inWorld(`setblock ${cx + 1} ${BASE_Y + 1} ${cz} minecraft:air`);
  await inWorld(`setblock ${cx} ${BASE_Y + 1} ${cz} minecraft:piston[facing=east]`);
  await inWorld(`setblock ${cx + 1} ${BASE_Y + 1} ${cz} minecraft:stone`);
  await inWorld(`setblock ${cx} ${BASE_Y + 2} ${cz} minecraft:redstone_block`);
  await sleep(800);
  check('pistão não empurra bloco sobre a planta ancestral', owner.blockAt(plant).name === 'wheat'
    && owner.blockAt(new Vec3(cx + 1, BASE_Y + 1, cz)).name === 'stone');
  await inWorld(`setblock ${cx} ${BASE_Y + 2} ${cz} minecraft:air`);
  await inWorld(`setblock ${cx} ${BASE_Y + 1} ${cz} minecraft:air`);
  await inWorld(`setblock ${cx + 1} ${BASE_Y + 1} ${cz} minecraft:air`);
  owner.quit(); await sleep(1000); await stopServer();
  await startServer(); owner = await connect(OWNER); await sleep(1500);
  check('planta permanece após restart', owner.blockAt(plant)?.name === 'wheat');
  await moveTo(owner, cx, BASE_Y + 1, cz);
  cmd(`give ${OWNER} minecraft:bone_meal 64`); await sleep(800);
  await owner.equip(owner.inventory.items().find(i => i.name === 'bone_meal'), 'hand');
  for (let i = 0; i < 12 && owner.blockAt(plant).metadata !== 7; i++) {
    await owner.activateBlock(owner.blockAt(plant)); await sleep(500);
  }
  check('planta especial amadurece com bone meal', owner.blockAt(plant).metadata === 7);
  await owner.dig(owner.blockAt(plant));
  await moveTo(owner, cx + 2, BASE_Y + 1, cz); await sleep(1200);
  const wheat = owner.inventory.items().filter(i => i.name === 'wheat');
  check('colheita madura entrega um trigo sem multiplicar sementes',
    wheat.reduce((n, i) => n + i.count, 0) === 1 && seedCount(owner) === 0);
  if (wheat.length) await owner.equip(wheat[0], 'hand');
  cmd(`op ${OWNER}`); await sleep(400);
  check('planta persistida entrega Trigo Dourado',
    /Item do GreenSky: crop.golden_wheat/.test(await say(owner, '/greensky item check', /Item do GreenSky|Não é item/)));
  cmd(`deop ${OWNER}`);
  const start = owner.messages.length; owner.chat('/collections farming'); await sleep(1000);
  check('colheita especial registra trigo e Trigo Dourado uma vez',
    /✔ Trigo \[Comum\] x1/.test(owner.messages.slice(start).join('|'))
    && /✔ Trigo Dourado \[Raro\] x1/.test(owner.messages.slice(start).join('|')));
  owner.quit();
}
try { await main(); await finish(); } catch (error) { await finish(error); }
