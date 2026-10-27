#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")/.."
root=$PWD
clone="$root/artifacts/j2me-loader-build"
revision=7e978c5ddee51a13d78ac59fa059d773d9de6b48
[[ -f artifacts/Avatar258-private.jar && -f .secrets/avatar-signing.properties ]] || { echo 'Prepare private JAR and private signing properties first'; exit 1; }
if [[ ! -d "$clone/.git" ]]; then
  git clone https://github.com/nikita36078/J2ME-Loader.git "$clone"
  git -C "$clone" checkout "$revision"
  git -C "$clone" apply "$root/android/j2me-loader.patch"
fi
[[ $(git -C "$clone" rev-parse HEAD) == "$revision" ]]
cp android/WebSocketConnection.java "$clone/app/src/main/java/org/microemu/cldc/socket/"
cp .secrets/avatar-signing.properties "$clone/keystore.properties"
chmod 600 "$clone/keystore.properties"
# Use the same instrumentation as the upstream JAR converter before D8.
asm="$root/artifacts/asm-9.7.jar"
[[ -f "$asm" ]] || curl -fL https://repo.maven.apache.org/maven2/org/ow2/asm/asm/9.7/asm-9.7.jar -o "$asm"
mkdir -p artifacts/instrument-classes "$clone/app/libs"
javac -cp "$asm" -d artifacts/instrument-classes android/InstrumentJar.java "$clone"/dexlib/src/main/java/org/microemu/android/asm/*.java
java -cp "artifacts/instrument-classes:$asm" InstrumentJar artifacts/Avatar258-private.jar "$clone/app/libs/avatar.jar"
if [[ -n ${AVATAR_GRADLE:-} ]]; then
  "$AVATAR_GRADLE" -p "$clone" assembleMidletRelease
else
  (cd "$clone" && ./gradlew assembleMidletRelease)
fi
cp "$clone/app/build/outputs/apk/midlet/release/Avatar_Homelab-2.5.8-release.apk" artifacts/Avatar-Homelab.apk
