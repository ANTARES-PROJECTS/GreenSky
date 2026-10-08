import { BASE_Y,Vec3,authenticate,check,cmd,connect,connectRaw,finish,heard,islandInfo,say,sleep,startServer } from './lib.mjs';
const stamp=String(Date.now()).slice(-7), OWNER='Gui'+stamp, FRIEND='Gfriend'+stamp;
const text=item=>JSON.stringify(item,(_,value)=>typeof value==='bigint'?String(value):value);
const title=bot=>JSON.stringify(bot.currentWindow?.title??'');
async function waitMenu(bot,regex,ms=12000) {
  const end=Date.now()+ms;
  while(Date.now()<end) { if(bot.currentWindow&&regex.test(title(bot))) return; await sleep(100); }
  throw new Error('Menu não abriu: '+regex+'; atual='+title(bot));
}
async function open(bot,command='/menu',regex=/Início/) { bot.chat(command); await waitMenu(bot,regex); await sleep(200); }
async function click(bot,slot,regex) { await bot.clickWindow(slot,0,0); await sleep(350); if(regex) await waitMenu(bot,regex); }
async function pick(bot,name) { const end=Date.now()+15000; while(Date.now()<end) { const slot=find(bot,name); if(slot>=0)return slot; await sleep(100); } throw new Error('Botão não apareceu: '+name); }
function find(bot,name) { return bot.currentWindow.slots.slice(0,54).findIndex(item=>item&&text(item).includes(name)); }
async function main() {
  await startServer();
  const owner=await connectRaw(OWNER); await authenticate(owner); await sleep(500);
  check('menu principal abre após autenticar', /Início/.test(title(owner)));
  if(!owner.currentWindow) await open(owner);
  const initial=owner.inventory.items().reduce((sum,item)=>sum+item.count,0);
  await owner.clickWindow(10,0,1); await sleep(400);
  check('shift não transfere ícones nem executa botões', /Início/.test(title(owner))
    && owner.inventory.items().reduce((sum,item)=>sum+item.count,0)===initial);
  await click(owner,10,/Criar ilha/);
  check('criação mostra quatro estilos e primeiros passos', [10,12,14,16,40].every(slot=>Boolean(owner.currentWindow.slots[slot])));
  const created=owner.messages.length;
  await click(owner,31); await heard(owner,/Ilha criada/,created,20000); await sleep(1200);
  const {cx,cz}=await islandInfo(owner);
  await owner.waitForChunksToLoad(); await sleep(700);
  check('ilha criada por menu e jogador teleportado', Math.abs(owner.entity.position.x-cx)<2&&Math.abs(owner.entity.position.z-cz)<2);
  check('chegada segura com espaço livre', owner.blockAt(new Vec3(cx,BASE_Y,cz)).name==='grass_block'
    && owner.blockAt(new Vec3(cx,BASE_Y+1,cz)).name==='air');
  check('ilha nova contém baú e iluminação', owner.blockAt(new Vec3(cx-2,BASE_Y+1,cz+2)).name==='chest'
    && owner.blockAt(new Vec3(cx+2,BASE_Y+2,cz-2)).name==='lantern');
  check('lago inicial tem duas camadas de água', owner.blockAt(new Vec3(cx-4,BASE_Y,cz-3)).name==='water'
    && owner.blockAt(new Vec3(cx-4,BASE_Y-1,cz-3)).name==='water');
  const chest=await owner.openContainer(owner.blockAt(new Vec3(cx-2,BASE_Y+1,cz+2)));
  check('kit inicial tem enxada, vara e sementes', ['wooden_hoe','fishing_rod','wheat_seeds'].every(name=>chest.containerItems().some(item=>item.name===name)));
  chest.close(); await sleep(250);
  await open(owner,'/is',/Minha ilha/);
  await click(owner,31); await sleep(600);
  check('acesso privado configurado por clique', await heard(owner,/agora é privada/,created));
  await open(owner,'/menu'); await click(owner,28,/Coleções/);
  check('coleções aparecem em inventário', owner.currentWindow.slots.some(item=>item&&text(item).includes('Agricultura')));
  const farming=find(owner,'Agricultura'); await click(owner,farming,/Agricultura/);
  check('entradas da coleção visíveis sem conceder progresso', owner.currentWindow.slots.some(item=>item&&text(item).includes('Ainda não descoberta')));
  await open(owner,'/agricultura',/Agricultura/);
  check('guia agrícola disponível por menu', [10,12,14,16,40].every(slot=>Boolean(owner.currentWindow.slots[slot])));
  await open(owner,'/menu');
  check('jogador sem OP não vê administração', owner.currentWindow.slots[49]?.name!=='command_block');
  const friend=await connect(FRIEND);
  await open(owner); await click(owner,16,/Equipe/); await click(owner,await pick(owner,'Convidar jogador'),/Convidar jogador/);
  await click(owner,await pick(owner,FRIEND)); await sleep(800);
  check('convite de membro por seleção de jogador', owner.messages.some(message=>message.includes(FRIEND+' agora é membro')));
  await open(friend); await click(friend,14,/Ilhas públicas/);
  check('membro vê ilha privada acessível na lista de visitas', find(friend,OWNER)>=0);
  await click(friend,await pick(friend,OWNER)); await sleep(1000);
  check('visita por menu respeita autorização de membro', Math.abs(friend.entity.position.x-cx)<3);
  friend.quit(); await sleep(400);
  await open(owner); await click(owner,16,/Equipe/); await click(owner,await pick(owner,FRIEND),new RegExp(FRIEND));
  await click(owner,10); await sleep(800); // BUILD: desativar
  await open(owner); await click(owner,16,/Equipe/); await click(owner,await pick(owner,FRIEND),new RegExp(FRIEND));
  check('permissão de construir alterada pelo menu', owner.currentWindow.slots[10].name==='gray_dye');
  await click(owner,31,/Remover/);
  check('remoção de membro exige confirmação', owner.currentWindow.slots[20].name==='lime_concrete');
  await click(owner,24,/Equipe/);
  check('cancelar mantém o membro', find(owner,FRIEND)>=0);
  await click(owner,await pick(owner,FRIEND),new RegExp(FRIEND)); await click(owner,31,/Remover/); await click(owner,20); await sleep(700);
  check('remoção confirmada funciona mesmo com jogador offline', owner.messages.some(message=>message.includes(FRIEND+' foi removido')));
  cmd(`op ${OWNER}`); await sleep(500);
  await open(owner); await click(owner,49,/Administração/); await click(owner,20,/Itens de teste/);
  const golden=find(owner,'Trigo Dourado'); await click(owner,golden,/Destino do item/);
  await click(owner,await pick(owner,OWNER)); await sleep(600);
  check('administrador entrega Trigo Dourado só com cliques', owner.inventory.items().some(item=>item.name==='wheat'));
  cmd(`deop ${OWNER}`); await sleep(400);
  await open(owner); await click(owner,34,/Como jogar/);
  check('guia explica sistemas disponíveis e pendentes', text(owner.currentWindow.slots[31]).includes('economia'));
  owner.closeWindow(owner.currentWindow); owner.quit();
}
try { await main(); await finish(); } catch(error) { await finish(error); }
