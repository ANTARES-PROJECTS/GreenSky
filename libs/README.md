# libs/

Jars de plugins externos (yPlugins, StormPlugins etc.) **só para compilar contra eles**.

- Coloque aqui o jar exato que roda no servidor, com a versão no nome (ex.: `StormClans-2.4.1.jar`).
- O GreenSky usa esses jars como `compileOnly`: eles nunca vão dentro do jar do GreenSky.
- Integração só depois que o jar estiver aqui e a API for lida nele (`javap`). Nunca supor a API.
- O GreenSky deve continuar funcionando sem eles (integração opcional, em `integration/`).
- Não versionar jars pagos/licenciados de terceiros sem permissão.

Hoje está vazio: nenhuma integração externa foi feita.
