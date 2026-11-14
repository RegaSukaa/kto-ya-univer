@echo off
chcp 65001 >nul
cd /d "%~dp0server"
if not exist node_modules (
  echo Устанавливаю зависимости...
  call npm install || goto :err
)
echo.
echo Сервер комнат запущен. Возьми адрес LAN из списка ниже, например:
echo   ws://192.168.1.163:8080/ws
echo Впиши его в приложении в поле «Сервер». Окно не закрывай.
echo.
node index.mjs
pause
exit /b 0
:err
echo Не удалось установить зависимости. Проверь, что установлен Node.js 20+.
pause
