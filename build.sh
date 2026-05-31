#!/bin/bash
set -e

echo "Compiling Mirror Duel..."
mkdir -p out/assets
cp -r assets/* out/assets/

javac -d out -sourcepath src src/mirrorduel/Main.java

echo "Creating JAR..."
jar cfm MirrorDuel.jar MANIFEST.MF -C out .

echo "Done! Running game..."
java -jar MirrorDuel.jar
