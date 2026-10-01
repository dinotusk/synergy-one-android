#!/bin/sh

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P) || exit
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ -n "$JAVA_HOME" ]; then
    JAVA_EXE=$JAVA_HOME/bin/java
else
    JAVA_EXE=java
fi

exec "$JAVA_EXE" -Xmx64m -Xms64m ${JAVA_OPTS:-} ${GRADLE_OPTS:-} \
    "-Dorg.gradle.appname=$(basename "$0")" -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain "$@"
