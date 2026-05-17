#!/usr/bin/env sh
# Minimal Gradle wrapper launcher (requires gradle/wrapper/gradle-wrapper.jar)
DIR="$(cd "$(dirname "$0")" && pwd)"
JAVA_CMD="${JAVA_HOME:+$JAVA_HOME/bin/}java"
if [ ! -x "$JAVA_CMD" ]; then
  JAVA_CMD=java
fi
exec "$JAVA_CMD" -jar "$DIR/gradle/wrapper/gradle-wrapper.jar" "$@"
