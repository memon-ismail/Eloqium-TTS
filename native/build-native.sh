#!/usr/bin/env bash
# Builds libeloqiumjni.so out of native/openevv
set -euo pipefail

here=$(cd "$(dirname "$0")" && pwd)
root=$(cd "$here/.." && pwd)
engine="$here/openevv"

ABIS=${ABIS:-"arm64-v8a"}
API=${API:-21}
RULES=${RULES:-bytecode}
LANGS=${LANGS:-lang/enus}
OUT=${OUT:-"$root/app/build/native/jniLibs"}
JOBS=${JOBS:-$(nproc 2>/dev/null || echo 4)}
# Apply architecture and multi-language patches to upstream openevv
for patch in "$here"/patches/*.patch; do
    [ -e "$patch" ] || continue
    if git -C "$engine" apply --reverse --check "$patch" >/dev/null 2>&1; then
        continue
    fi
    if ! git -C "$engine" apply --whitespace=nowarn "$patch"; then
        if git -C "$engine" apply --reverse --check "$patch" >/dev/null 2>&1; then
            continue
        fi
        echo "could not apply $(basename "$patch") to native/openevv" >&2
        exit 1
    fi
    echo "applied $(basename "$patch")"
done

# If Termux native build (no NDK set)
if [ -z "${ANDROID_NDK_HOME:-${ANDROID_NDK_ROOT:-}}" ]; then
    echo "Building natively with Termux clang..."
    abi="arm64-v8a"
    mkdir -p "$OUT/$abi"
    build="build/android-$abi"
    
    incs="-Iinclude"
    for l in $LANGS; do
        incs="$incs -I$l -Irom/${l##*/}"
    done
    for d in "$engine"/src "$engine"/src/*/ "$engine"/src/*/*/; do
        [ -d "$d" ] || continue
        rel=${d#"$engine"/}
        incs="$incs -I${rel%/}"
    done
    
    make -C "$engine" -f Makefile -f "$here/android.mk" \
        RULES="$RULES" LANGS="$LANGS" BUILD="$build" \
        CC="clang" CFLAGS="-O2 -fPIC -fvisibility=hidden -fsigned-char" \
        -j"$JOBS" android-objects

    objdir=$(make -C "$engine" -f Makefile -f "$here/android.mk" RULES="$RULES" LANGS="$LANGS" BUILD="$build" -s android-objdir)
    
    rsp="$engine/$build/objects.rsp"
    : > "$rsp"
    for o in "$engine/$objdir"/*.o; do
        echo "$o" >> "$rsp"
    done
    
    ( cd "$engine" && clang -O2 -std=gnu99 -fPIC -fvisibility=hidden -fsigned-char \
        -DEVV_ARENA=1 -DECI_STATIC $incs \
        -shared \
        "$root/app/src/main/cpp/eloqium_jni.c" \
        lib/eci_api.c \
        "@$build/objects.rsp" \
        -lm -llog \
        -o "$OUT/$abi/libeloqiumjni.so" )
    
    echo "Built $OUT/$abi/libeloqiumjni.so successfully!"
    ls -lh "$OUT/$abi/libeloqiumjni.so"
    exit 0
fi

# NDK cross-compilation flow
NDK=${ANDROID_NDK_HOME:-${ANDROID_NDK_ROOT}}
case "$(uname -s)" in
    Linux*)  hosttag=linux-x86_64 ;;
    Darwin*) hosttag=darwin-x86_64 ;;
    *)       hosttag=windows-x86_64 ;;
esac
bin="$NDK/toolchains/llvm/prebuilt/$hosttag/bin"
clang="$bin/clang"
strip="$bin/llvm-strip"

incs="-Iinclude"
for l in $LANGS; do
    incs="$incs -I$l -Irom/${l##*/}"
done
for d in "$engine"/src "$engine"/src/*/ "$engine"/src/*/*/; do
    [ -d "$d" ] || continue
    rel=${d#"$engine"/}
    incs="$incs -I${rel%/}"
done

triple_for() {
    case "$1" in
        arm64-v8a)   echo "aarch64-linux-android$API" ;;
        armeabi-v7a) echo "armv7a-linux-androideabi$API" ;;
        x86_64)      echo "x86_64-linux-android$API" ;;
        x86)         echo "i686-linux-android$API" ;;
        *)           echo "unknown ABI $1" >&2; exit 1 ;;
    esac
}

for abi in $ABIS; do
    triple=$(triple_for "$abi")
    build="build/android-$abi"
    echo "==> $abi ($triple)"
    make -C "$engine" -f Makefile -f "$here/android.mk" \
        RULES="$RULES" LANGS="$LANGS" BUILD="$build" \
        CC="$clang --target=$triple" \
        CFLAGS="-fPIC -fvisibility=hidden -fsigned-char" \
        -j"$JOBS" android-objects
    objdir=$(make -C "$engine" -f Makefile -f "$here/android.mk" RULES="$RULES" LANGS="$LANGS" BUILD="$build" -s android-objdir)
    rsp="$engine/$build/objects.rsp"
    : > "$rsp"
    for o in "$engine/$objdir"/*.o; do
        echo "$o" >> "$rsp"
    done
    arena=""
    case "$abi" in
        arm64-v8a|x86_64) arena="-DEVV_ARENA=1" ;;
    esac
    mkdir -p "$OUT/$abi"
    ( cd "$engine" && "$clang" --target="$triple" \
        -O2 -std=gnu99 -w -fPIC -fvisibility=hidden -fsigned-char \
        -Werror=int-conversion -Werror=incompatible-pointer-types \
        $arena -DECI_STATIC $incs \
        -shared \
        "$root/app/src/main/cpp/eloqium_jni.c" \
        lib/eci_api.c \
        "@$build/objects.rsp" \
        -Wl,-z,max-page-size=16384 \
        -lm -llog \
        -o "$OUT/$abi/libeloqiumjni.so" )
    "$strip" "$OUT/$abi/libeloqiumjni.so"
    ls -lh "$OUT/$abi/libeloqiumjni.so"
done
