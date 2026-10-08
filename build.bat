@echo off
rem Compila o GreenSky, roda os testes e copia o jar para server\plugins.
rem Os testes de integracao usam o PostgreSQL do docker-compose (sobe se estiver parado).
setlocal
cd /d "%~dp0"

if not defined JAVA_HOME set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot"
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo [ERRO] Java 25 nao encontrado em "%JAVA_HOME%". Defina JAVA_HOME.
    exit /b 1
)

rem Carrega GREENSKY_DB_PASSWORD e afins do .env (sem esse arquivo, os testes de banco sao pulados).
if exist ".env" for /f "usebackq eol=# tokens=1,* delims==" %%a in (".env") do set "%%a=%%b"

docker compose up -d --wait >nul 2>&1 || echo [AVISO] Nao consegui subir o PostgreSQL; testes de banco podem falhar.

call "%~dp0greensky\gradlew.bat" -p "%~dp0greensky" build deploy %*
set "RESULT=%ERRORLEVEL%"
if "%RESULT%"=="0" (echo. & echo [OK] Plugin copiado para server\plugins.) else (echo. & echo [ERRO] Build falhou.)
exit /b %RESULT%
