#!/usr/bin/env bash
set -euo pipefail
src_dir="$(cd "$(dirname "$0")" && pwd)"
sdk_dir="${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT}"
build_dir="${VOICE_BUILD_DIR:?Set VOICE_BUILD_DIR outside source}"
deps="${VOICE_DEPS_DIR:?Set VOICE_DEPS_DIR}"
assets="${VOICE_ASSETS_DIR:?Set VOICE_ASSETS_DIR}"
bt="$sdk_dir/build-tools/36.1.0"
jarfile="$sdk_dir/platforms/android-36/android.jar"
mkdir -p "$build_dir/classes" "$build_dir/generated" "$build_dir/dex" "$build_dir/native/lib/arm64-v8a"
"$bt/aapt2" compile --dir "$src_dir/res" -o "$build_dir/resources.zip"
"$bt/aapt2" link -o "$build_dir/unsigned.apk" --manifest "$src_dir/AndroidManifest.xml" -I "$jarfile" --java "$build_dir/generated" -A "$assets" -0 onnx "$build_dir/resources.zip"
find "$src_dir/src" "$build_dir/generated" -name '*.java' > "$build_dir/sources.txt"
javac --release 8 -classpath "$jarfile:$deps/sherpa.jar:$deps/kotlin.jar" -d "$build_dir/classes" @"$build_dir/sources.txt"
jar cf "$build_dir/app.jar" -C "$build_dir/classes" .
"$bt/d8" --min-api 29 --lib "$jarfile" --output "$build_dir/dex" "$build_dir/app.jar" "$deps/sherpa.jar" "$deps/kotlin.jar"
cp "$deps/native/jni/arm64-v8a/"*.so "$build_dir/native/lib/arm64-v8a/"
(cd "$build_dir/dex" && zip -q "$build_dir/unsigned.apk" classes*.dex)
(cd "$build_dir/native" && zip -qr "$build_dir/unsigned.apk" lib)
"$bt/zipalign" -P 16 -f 4 "$build_dir/unsigned.apk" "$build_dir/aligned.apk"
"$bt/apksigner" sign --ks "${VOICE_KEYSTORE:?}" --ks-key-alias dolswe-voice --ks-pass env:VOICE_STOREPASS --key-pass env:VOICE_STOREPASS --out "$build_dir/dolswe-voice.apk" "$build_dir/aligned.apk"
"$bt/apksigner" verify --verbose "$build_dir/dolswe-voice.apk"
