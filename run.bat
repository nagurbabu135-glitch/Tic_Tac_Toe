@echo off
echo Starting Tic-Tac-Toe Pro Application...
"C:\Program Files\Java\jdk-21.0.12\bin\java.exe" -jar TicTacToe.jar
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Running directly from compiled classes...
    "C:\Program Files\Java\jdk-21.0.12\bin\java.exe" -cp "bin;lib/*" com.tictactoe.Main
)
pause
