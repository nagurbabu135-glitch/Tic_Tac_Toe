@echo off
echo ===================================================
echo     Building Tic-Tac-Toe Pro (Universal App)
echo ===================================================

if not exist bin mkdir bin

"C:\Program Files\Java\jdk-21.0.12\bin\javac.exe" --add-modules jdk.httpserver --add-reads com.formdev.flatlaf=jdk.httpserver -d bin -cp "lib/flatlaf.jar" src/com/tictactoe/model/*.java src/com/tictactoe/service/*.java src/com/tictactoe/web/*.java src/com/tictactoe/ui/*.java src/com/tictactoe/*.java

if %ERRORLEVEL% EQU 0 (
    echo Unpacking FlatLaf into bin folder...
    cd bin
    "C:\Program Files\Java\jdk-21.0.12\bin\jar.exe" -xf "../lib/flatlaf.jar"
    cd ..
    
    echo Creating Manifest...
    echo Main-Class: com.tictactoe.Main> MANIFEST.MF
    
    echo Packaging TicTacToe.jar...
    "C:\Program Files\Java\jdk-21.0.12\bin\jar.exe" cfm TicTacToe.jar MANIFEST.MF -C bin .
    copy /Y TicTacToe.jar ..\TicTacToe.jar
    
    echo.
    echo ===================================================
    echo [SUCCESS] TicTacToe.jar created successfully!
    echo Web Dashboard ready at http://localhost:8096
    echo ===================================================
) else (
    echo.
    echo [ERROR] Compilation failed with error code %ERRORLEVEL%
)
