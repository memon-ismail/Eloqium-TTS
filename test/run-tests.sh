#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

SUITE_ARGS=()
ABI=""

for arg in "$@"; do
    case "$arg" in
        arm64-v8a|armeabi-v7a|x86_64|x86)
            ABI="$arg"
            ;;
        *)
            SUITE_ARGS+=("$arg")
            ;;
    esac
done

if [ -z "$ABI" ]; then
    ARCH="$(uname -m)"
    case "$ARCH" in
        aarch64|arm64) ABI="arm64-v8a" ;;
        armv7l|arm)   ABI="armeabi-v7a" ;;
        x86_64)       ABI="x86_64" ;;
        *)            ABI="arm64-v8a" ;;
    esac
fi

LIB_PATH="$ROOT_DIR/app/build/native/jniLibs/$ABI"
if [ ! -f "$LIB_PATH/libeloqiumjni.so" ]; then
    echo "Native library not found at $LIB_PATH/libeloqiumjni.so"
    echo "Please ensure native libraries are compiled first (e.g. via ./native/build-native.sh)"
    exit 1
fi

BUILD_DIR="$ROOT_DIR/test_build"
mkdir -p "$BUILD_DIR"

echo "=== Compiling Android Stubs ==="
javac -d "$BUILD_DIR" \
    "$ROOT_DIR"/test/stubs/android/content/*.java \
    "$ROOT_DIR"/test/stubs/android/content/res/*.java \
    "$ROOT_DIR"/test/stubs/android/media/*.java \
    "$ROOT_DIR"/test/stubs/android/net/*.java \
    "$ROOT_DIR"/test/stubs/android/os/*.java \
    "$ROOT_DIR"/test/stubs/android/speech/tts/*.java \
    "$ROOT_DIR"/test/stubs/android/util/*.java

echo "=== Compiling Test Runner ==="
kotlinc -cp "$BUILD_DIR:$LIB_PATH" -include-runtime -d "$BUILD_DIR/TestRunner.jar" \
    "$ROOT_DIR"/test/MockSharedPreferences.kt \
    "$ROOT_DIR"/test/TestRunner.kt \
    "$ROOT_DIR"/app/src/main/kotlin/org/eloqium/tts/engine/*.kt \
    "$ROOT_DIR"/app/src/main/kotlin/org/eloqium/tts/pipeline/*.kt \
    "$ROOT_DIR"/app/src/main/kotlin/org/eloqium/tts/service/*.kt \
    "$ROOT_DIR"/app/src/main/kotlin/org/eloqium/tts/ui/AboutNavigation.kt

echo "=== Executing Eloqium Test Suite on $ABI ==="
java -Djava.library.path="$LIB_PATH" -cp "$BUILD_DIR:$BUILD_DIR/TestRunner.jar" test.TestRunnerKt "${SUITE_ARGS[@]}"
