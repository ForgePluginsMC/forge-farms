#!/usr/bin/env bash
# ForgeFarms direct-javac build (Gradle daemon cannot run in this sandbox).
#
# JDBC drivers (SQLite + MySQL) are SHADED into the jar because Bukkit cannot
# load nested jars from lib/. PlaceholderAPI is compile-only (softdepend) —
# it must NOT be bundled.
set -euo pipefail
ROOT="$HOME/workspace/forge-farms"
DEPS="$HOME/workspace/.toolchains/paper-deps"
JDBC="$HOME/workspace/.toolchains/jdbc"
JAVAC="$HOME/workspace/.toolchains/jdk-25.0.4.1+1/bin/javac"
JAR="$HOME/workspace/.toolchains/jdk-25.0.4.1+1/bin/jar"
CP=$(ls "$DEPS"/*.jar "$JDBC"/*.jar 2>/dev/null | tr '\n' ':')

rm -rf "$ROOT/build" && mkdir -p "$ROOT/build/classes" "$ROOT/build/stage"
find "$ROOT/src/main/java" -name '*.java' > "$ROOT/build/sources.txt"
$JAVAC -Werror -Xlint:all -Xlint:-classfile -parameters -d "$ROOT/build/classes" -cp "$CP" @"$ROOT/build/sources.txt"
cp -r "$ROOT/build/classes"/. "$ROOT/build/stage"/
# Shade JDBC drivers (skip signatures/manifests to avoid jar conflicts).
for j in "$JDBC"/*.jar; do
  unzip -o -q "$j" -d "$ROOT/build/stage" \
    -x 'META-INF/*.SF' 'META-INF/*.DSA' 'META-INF/*.RSA' 'META-INF/MANIFEST.MF' 'META-INF/INDEX.LIST'
done
cp -r "$ROOT/src/main/resources/." "$ROOT/build/stage"/
( cd "$ROOT/build/stage" && $JAR --create --file "$ROOT/ForgeFarms-1.0.0.jar" . )
echo "built $ROOT/ForgeFarms-1.0.0.jar"
ls -la "$ROOT/ForgeFarms-1.0.0.jar"
