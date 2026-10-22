@echo off
setlocal

rem Always compile the current sources so Main.class and its dependencies are present.
cd /d "%~dp0"
if not exist out mkdir out

javac -cp "lib\mysql-connector-j-9.4.0.jar" -d out src\Bird.java src\Database.java src\Game.java src\GamePanel.java src\GameState.java src\Main.java src\Pipe.java src\ScoreManager.java src\UiButton.java
if errorlevel 1 (
    echo.
    echo The game could not be compiled. Fix the errors above and run this file again.
    pause
    exit /b 1
)

java -cp "out;lib\mysql-connector-j-9.4.0.jar" Main
if errorlevel 1 (
    echo.
    echo The game stopped unexpectedly.
    pause
)
