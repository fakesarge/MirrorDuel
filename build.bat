@echo off
echo Compiling Mirror Duel...

if not exist out mkdir out
xcopy /E /I /Y assets out\assets >nul

"C:\Program Files\Java\jdk-26.0.1\bin\javac.exe" -d out -sourcepath src src\mirrorduel\Main.java

if errorlevel 1 (
    echo Compilation failed.
    pause
    exit /b 1
)

echo Creating JAR...

"C:\Program Files\Java\jdk-26.0.1\bin\jar.exe" cfm MirrorDuel.jar MANIFEST.MF -C out .

echo Done! Running game...

"C:\Program Files\Java\jdk-26.0.1\bin\java.exe" -jar MirrorDuel.jar