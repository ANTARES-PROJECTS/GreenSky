@echo off
rem Sobe o Paper de teste (server\) com o PostgreSQL. Digite "stop" no console para desligar.
setlocal
cd /d "%~dp0"

if not defined JAVA_HOME set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot"
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo [ERRO] Java 25 nao encontrado em "%JAVA_HOME%". Defina JAVA_HOME.
    exit /b 1
)

rem O plugin le a senha do banco de GREENSKY_DB_PASSWORD (nunca do config.yml).
if not exist ".env" (
    echo [ERRO] .env nao encontrado. Copie .env.example para .env e defina a senha.
    exit /b 1
)
for /f "usebackq eol=# tokens=1,* delims==" %%a in (".env") do set "%%a=%%b"

docker compose up -d --wait || (echo [ERRO] PostgreSQL nao subiu. O Docker esta aberto? & exit /b 1)

cd server
"%JAVA_HOME%\bin\java.exe" -Xms1G -Xmx2G -Dstdout.encoding=UTF-8 -jar paper-26.1.2-74.jar --nogui
