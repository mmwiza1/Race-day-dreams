#!/bin/sh
# Re-sign the verified API35 test APK with the Race Day Dreams release key for distribution.
#
# Usage: sign-distribution-apk.sh VERIFIED_APK OUTPUT_APK
# Prints "signed" or "test-signed" on the last line.
#
# The key never enters the repository. It comes only from GitHub Actions secrets, which are passed
# to the publish job on pushes to main or version tags (never to pull-request builds):
#   RDD_ANDROID_KEYSTORE_BASE64  base64 of the release keystore (.jks / PKCS12)
#   RDD_ANDROID_KEYSTORE_PASSWORD, RDD_ANDROID_KEY_ALIAS, RDD_ANDROID_KEY_PASSWORD
# The public certificate fingerprint is committed in config/android-signing-certificate.sha256.
#
# States:
#   no secrets and no fingerprint  -> the verified test-signed APK is published unchanged, with
#                                     no update manifest (no in-place updates), exactly as before.
#   only one of them configured    -> refuse: publication stops rather than guessing.
#   both configured                -> sign, then require the fingerprint and identical contents.
set -eu

verified=$1
output=$2
root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
pin="$root/config/android-signing-certificate.sha256"
apksigner=${APKSIGNER:-$(ls -d "${ANDROID_HOME:?ANDROID_HOME is required}"/build-tools/*/ | sort -V | tail -n 1)apksigner}

have_secrets=false
if [ -n "${RDD_ANDROID_KEYSTORE_BASE64:-}" ]; then have_secrets=true; fi
have_pin=false
if [ -f "$pin" ]; then have_pin=true; fi

if [ "$have_secrets" = false ] && [ "$have_pin" = false ]; then
    echo "Stable release signing is not configured; publishing the verified test-signed APK without an update manifest."
    cp "$verified" "$output"
    echo test-signed
    exit 0
fi
if [ "$have_secrets" != "$have_pin" ]; then
    echo "Release signing is only partly configured (secrets: $have_secrets, committed fingerprint: $have_pin)." >&2
    echo "Refusing to publish: see docs/ANDROID_UPDATES.md, 'One-time signing setup'." >&2
    exit 1
fi
for name in RDD_ANDROID_KEYSTORE_PASSWORD RDD_ANDROID_KEY_ALIAS RDD_ANDROID_KEY_PASSWORD; do
    eval "value=\${$name:-}"
    if [ -z "$value" ]; then echo "Missing secret $name." >&2; exit 1; fi
done

work=$(mktemp -d "${RUNNER_TEMP:-${TMPDIR:-/tmp}}/rdd-sign.XXXXXX")
trap 'rm -rf "$work"' EXIT INT TERM
umask 077
printf '%s' "$RDD_ANDROID_KEYSTORE_BASE64" | base64 -d > "$work/release.keystore"
"$apksigner" sign \
    --ks "$work/release.keystore" \
    --ks-key-alias "$RDD_ANDROID_KEY_ALIAS" \
    --ks-pass env:RDD_ANDROID_KEYSTORE_PASSWORD \
    --key-pass env:RDD_ANDROID_KEY_PASSWORD \
    --out "$work/signed.apk" "$verified"
rm -f "$work/release.keystore"
"$apksigner" verify --print-certs -v "$work/signed.apk" > "$work/verify.txt"
python3 "$root/scripts/development-assets.py" --version 0.0.0-test0 --directory "$work" \
    --signed-check "$work/signed.apk" --verified-apk "$verified" \
    --apksigner-output "$work/verify.txt" --pin "$pin"
cp "$work/signed.apk" "$output"
echo signed
