#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
sdk="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -z "$sdk" ]]; then echo 'Set ANDROID_SDK_ROOT to an SDK with platforms;android-35 and build-tools;35.0.0' >&2; exit 1; fi
bt="$sdk/build-tools/35.0.0"
jar="$sdk/platforms/android-35/android.jar"
rm -rf build
mkdir -p build/classes build/gen build/dex
"$bt/aapt2" compile --dir res -o build/resources.zip
"$bt/aapt2" link -o build/base.apk -I "$jar" --manifest AndroidManifest.xml --java build/gen --min-sdk-version 26 --target-sdk-version 35 -A assets build/resources.zip
find src build/gen -name '*.java' > build/sources.txt
compiler=(javac)
if ! command -v javac >/dev/null; then compiler=(java -m jdk.compiler/com.sun.tools.javac.Main); fi
"${compiler[@]}" -encoding UTF-8 -source 8 -target 8 -classpath "$jar" -d build/classes @build/sources.txt
find build/classes -name '*.class' > build/dex-inputs.txt
"$bt/d8" --min-api 26 --lib "$jar" --output build/dex @build/dex-inputs.txt
cp build/base.apk build/unsigned.apk
(cd build/dex && zip -q -u ../unsigned.apk classes*.dex)
"$bt/zipalign" -P 16 -f 4 build/unsigned.apk build/aligned.apk
# A local test key persists outside the public repository. No private key enters GitHub.
key="${TELUGU_TEST_KEYSTORE:-${HOME}/.local/share/telugu-matalu/test-signing.jks}"
mkdir -p "$(dirname "$key")"
if [[ ! -f "$key" ]]; then
 keytool -genkeypair -keystore "$key" -storepass android -keypass android -alias telugu-test -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=Telugu Matalu Test,O=AiTechApp,C=IN' >/dev/null 2>&1
fi
"$bt/apksigner" sign --ks "$key" --ks-key-alias telugu-test --ks-pass pass:android --key-pass pass:android --out build/TeluguMatalu-2.1.0-test.apk build/aligned.apk
"$bt/apksigner" verify --verbose --print-certs build/TeluguMatalu-2.1.0-test.apk
"$bt/zipalign" -c -P 16 -v 4 build/TeluguMatalu-2.1.0-test.apk > build/alignment.txt
sha256sum build/TeluguMatalu-2.1.0-test.apk > build/SHA256SUMS.txt
