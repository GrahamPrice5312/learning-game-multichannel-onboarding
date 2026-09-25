#!/bin/sh
set -eu

BUILD_DIR="${TMPDIR:-/tmp}/learning-game-onboarding-classes"
mkdir -p "$BUILD_DIR"
javac -d "$BUILD_DIR" $(find src/main/java -name '*.java')
java -cp "$BUILD_DIR" learnarcade.onboarding.GameOnboardingApplication
