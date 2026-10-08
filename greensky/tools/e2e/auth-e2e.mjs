// Autenticação e identidade com o nLogin real (servidor de teste com online-mode=false).
// Exige o nLogin em server/plugins, já configurado (assistente concluído), e auth.provider=nlogin.
// Uso:  JAVA=".../java.exe" node auth-e2e.mjs   (requisitos em lib.mjs)

import { execFileSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import {
  AUTH_PASSWORD, check, connect, connectRaw, finish, heard, say, sleep, startServer,
} from './lib.mjs';

/** UUID "offline" do Minecraft: v3 de MD5("OfflinePlayer:" + nick exato). */
function offlineUuid(name) {
  const h = createHash('md5').update('OfflinePlayer:' + name, 'utf8').digest();
  h[6] = (h[6] & 0x0f) | 0x30;
  h[8] = (h[8] & 0x3f) | 0x80;
  const x = h.toString('hex');
  return `${x.slice(0, 8)}-${x.slice(8, 12)}-${x.slice(12, 16)}-${x.slice(16, 20)}-${x.slice(20)}`;
}

/** UUID gravado pelo GreenSky para o nick (consulta só leitura no Postgres do docker-compose). */
function storedUuid(name) {
  const out = execFileSync('docker', ['exec', 'greensky-postgres', 'psql', '-U', 'greensky', '-d', 'greensky',
    '-t', '-A', '-c', `select uuid from players where lower(name) = lower('${name.replace(/'/g, "''")}')`]);
  return out.toString().trim();
}

const stamp = String(Date.now()).slice(-6);
const NICK = 'Auth' + stamp; // ex.: Auth123456
const OTHER_CASE = NICK.toLowerCase(); // ex.: auth123456

async function main() {
  await startServer();

  // --- antes do login: o GreenSky não atende ---
  const raw = await connectRaw(NICK);
  await sleep(3000);
  check('nLogin pede registro', raw.messages.some((m) => /\/register /.test(m)));
  const before = raw.messages.length;
  raw.chat('/island create');
  await sleep(3000);
  check('antes do login: sem dica do GreenSky', !raw.messages.some((m) => /Bem-vindo ao GreenSky/.test(m)));
  check('antes do login: /island create não cria ilha', !raw.messages.slice(before).some((m) => /Ilha criada/.test(m)));
  check('antes do login: nick não é registrado no GreenSky', storedUuid(NICK) === '');

  // --- registro: libera o GreenSky ---
  const start = raw.messages.length;
  raw.chat(`/register ${AUTH_PASSWORD} ${AUTH_PASSWORD}`);
  check('depois do /register: GreenSky dá as boas-vindas', await heard(raw, /Bem-vindo ao GreenSky/, start));
  check('UUID no cliente = UUID offline do nick', raw.player.uuid === offlineUuid(NICK), raw.player.uuid);
  check('UUID gravado pelo GreenSky = UUID offline do nick', storedUuid(NICK) === offlineUuid(NICK), storedUuid(NICK));
  check('depois do login: /island create funciona',
    /Ilha criada/.test(await say(raw, '/island create', /Ilha criada|já tem|login/)));
  const home = await say(raw, '/island info', /centro/);
  raw.quit();
  await sleep(4000); // o servidor precisa processar a saída antes de a mesma conta entrar de novo

  // --- mesmo nick com outras maiúsculas: o nLogin trata como a MESMA conta (exige a senha dela) ---
  const variant = await connectRaw(OTHER_CASE);
  await sleep(3000);
  check(`"${OTHER_CASE}" recebe o UUID da conta "${NICK}" (não um novo)`, variant.player.uuid === offlineUuid(NICK),
    variant.player.uuid);
  check('a outra grafia precisa de /login (senha da conta), não de /register',
    variant.messages.some((m) => /\/login /.test(m)) && !variant.messages.some((m) => /\/register /.test(m)));
  const wrong = variant.messages.length;
  variant.chat('/login SenhaErrada999');
  await sleep(3000);
  variant.chat('/island info');
  await sleep(2000);
  check('senha errada: GreenSky continua sem atender',
    !variant.messages.slice(wrong).some((m) => /centro|Bem-vindo ao GreenSky/.test(m)));
  variant.quit();
  await sleep(4000);

  const variant2 = await connectRaw(OTHER_CASE);
  await sleep(2000);
  const start2 = variant2.messages.length;
  variant2.chat(`/login ${AUTH_PASSWORD}`);
  await heard(variant2, /successfully logged/i, start2);
  await sleep(1500);
  const info = await say(variant2, '/island info', /centro|ainda não tem/);
  check('com a senha certa: mesma conta, mesma ilha (não cria outra)', info === home, info);
  check('continua existindo uma única conta para o nick no GreenSky', storedUuid(OTHER_CASE) === offlineUuid(NICK));
  variant2.quit();
  await sleep(4000);

  // --- novo login com a grafia original ---
  const again = await connect(NICK);
  const info2 = await say(again, '/island info', /centro|ainda não tem/);
  check('relogin: mesma conta continua com a ilha', info2 === home, info2);
  check('relogin: UUID continua o offline do nick registrado', again.player.uuid === offlineUuid(NICK));
  again.quit();
}

try {
  await main();
  await finish();
} catch (e) {
  await finish(e);
}
