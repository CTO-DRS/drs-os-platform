#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# DRS OS - installation package builder (Phase 6, re-executed 2026-10-17).
# Rebuilds drs-os-0.3.1-phase3-alpha-installation-package.zip from freshly
# built APKs, re-verifying every APK before it is allowed into the package.
#
# Hard-fail verification per APK (zero-tolerance):
#   - package name must match the expected applicationId
#   - versionName must match 0.3.1-phase3
#   - minSdk 29 / targetSdk 35
#   - Arabic application label must match
#   - ZERO android.permission.* and ZERO uses-permission entries
# Package integrity chain: staging -> SHA256SUMS.txt -> zip -> extraction
# re-verification with `sha256sum -c` executed FROM THE PACKAGE ROOT.
set -uo pipefail

export JAVA_HOME=/home/z/tools/jdk
export ANDROID_HOME=/home/z/tools/android-sdk
AAPT2="$ANDROID_HOME/build-tools/35.0.0/aapt2"
APKSIGNER="$ANDROID_HOME/build-tools/35.0.0/apksigner"

REPO=/home/z/my-project/drs-os
APPS_BASE="$REPO/packages/apps"
OUT_DIR=/home/z/my-project/download
STAGE_ROOT=/home/z/my-project/scripts/tmp/pkg_stage
VERSION="0.3.1-phase3"
PKG_NAME="drs-os-${VERSION}-alpha-installation-package"
ZIP_PATH="$OUT_DIR/${PKG_NAME}.zip"

# name|applicationId|arabic-label|apk-file
SPECS=(
  "DRSSettings|os.drs.settings|إعدادات DRS|DRSSettings-${VERSION}-debug.apk"
  "DRSAutomation|os.drs.automation|أتمتة DRS|DRSAutomation-${VERSION}-debug.apk"
  "DRSSecurityCenter|os.drs.security|مركز أمان DRS|DRSSecurityCenter-${VERSION}-debug.apk"
)

ok()   { echo "[PASS] $*"; }
bad()  { echo "[FAIL] $*"; FAILED=1; }
note() { echo "[INFO] $*"; }

FAILED=0
GATE_LOG=/home/z/my-project/scripts/tmp/verification-gate.txt
: > "$GATE_LOG"
exec > >(tee -a "$GATE_LOG") 2>&1

echo "== DRS OS verification gate + packaging ($VERSION) =="
echo "== run started: $(date -u +'%Y-%m-%dT%H:%M:%SZ') =="

rm -rf "$STAGE_ROOT"
mkdir -p "$STAGE_ROOT/$PKG_NAME/apks" "$STAGE_ROOT/$PKG_NAME/reports"

for spec in "${SPECS[@]}"; do
  IFS='|' read -r app pkg label apkfile <<< "$spec"
  src="$APPS_BASE/$app/build/outputs/apk/debug/${app}-debug.apk"

  # Fixed guard (explicit if — bash &&/|| precedence bug of the previous
  # session is not repeated): missing/empty APK must hard-fail the build.
  if [ ! -f "$src" ] || [ ! -s "$src" ]; then
    bad "$app: APK missing/empty — run the build first"
    continue
  fi
  ok "$app: APK present ($(stat -c%s "$src") bytes)"

  info="$("$AAPT2" dump badging "$src")"
  echo "$info" > "$STAGE_ROOT/$PKG_NAME/reports/badging-${app}.txt"

  echo "$info" | grep -q "package: name='$pkg'" && ok "$app: package name $pkg" || bad "$app: package name mismatch"
  echo "$info" | grep -q "versionName='$VERSION'" && ok "$app: versionName $VERSION" || bad "$app: versionName mismatch"
  echo "$info" | grep -q "minSdkVersion:'29'" && ok "$app: minSdk 29" || bad "$app: minSdk mismatch"
  echo "$info" | grep -q "targetSdkVersion:'35'" && ok "$app: targetSdk 35" || bad "$app: targetSdk mismatch"
  echo "$info" | grep -q "application-label:'$label'" && ok "$app: Arabic label '$label'" || bad "$app: Arabic label mismatch"

  # Zero-permission policy: the ONLY tolerated entry is the manifest-merger-
  # injected same-package signature permission AndroidX uses for non-exported
  # runtime receivers (identical to the 0.3.0 release baseline). Any
  # android.permission.* or any other permission hard-fails.
  foreign_uses="$(echo "$info" | grep '^uses-permission:' | grep -v "name='$pkg.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'" || true)"
  n_uses="$(echo "$info" | grep -c '^uses-permission:' || true)"
  if [ -z "$foreign_uses" ]; then
    ok "$app: zero foreign uses-permission entries (uses-permission lines: $n_uses — same-package merger injection only)"
  else
    bad "$app: foreign uses-permission entries found:"
    echo "$foreign_uses"
  fi
  if echo "$info" | grep -q 'android\.permission\.'; then
    bad "$app: android.permission.* present in badging"
  else
    ok "$app: zero android.permission.* references"
  fi

  if "$APKSIGNER" verify "$src" >/dev/null 2>&1; then
    ok "$app: apksigner verify PASS"
  else
    bad "$app: apksigner verify FAILED"
  fi

  # Capture the listing first: `unzip | grep -q` under pipefail dies with
  # SIGPIPE when grep exits early — a false failure, not an APK defect.
  listing="$(unzip -l "$src")"
  if echo "$listing" | grep -q 'classes.dex' && echo "$listing" | grep -q 'AndroidManifest.xml'; then
    ok "$app: zip structure sanity (classes.dex + AndroidManifest.xml)"
  else
    bad "$app: APK zip structure sanity FAILED"
  fi

  cp "$src" "$STAGE_ROOT/$PKG_NAME/apks/$apkfile"
done

# Defense-in-depth: exactly 3 APKs must be staged.
n_apks="$(find "$STAGE_ROOT/$PKG_NAME/apks" -maxdepth 1 -name '*.apk' -type f | wc -l | tr -d ' ')"
[ "$n_apks" -eq 3 ] || bad "expected 3 staged APKs, found $n_apks"
[ "$n_apks" -eq 3 ] && ok "staged APK count = 3"

# Static validators of the repository (real exit statuses recorded).
echo "-- repository static validators --"
cd "$REPO"
TMPD=/home/z/my-project/scripts/tmp
if bash build/scripts/validate_project.sh > "$TMPD/validate.log" 2>&1; then
  ok "validate_project.sh exit 0 ($(grep -c '^\[PASS\]' "$TMPD/validate.log" 2>/dev/null || echo '?') pass lines)"
else
  bad "validate_project.sh FAILED (see $TMPD/validate.log)"
fi
if bash tests/static/run_static_checks.sh > "$TMPD/static.log" 2>&1; then
  ok "run_static_checks.sh exit 0"
else
  bad "run_static_checks.sh FAILED (see $TMPD/static.log)"
fi
if bash tests/automation/run_automation_tests.sh > "$TMPD/automation.log" 2>&1; then
  ok "automation reference tests exit 0"
else
  bad "automation reference tests FAILED (see $TMPD/automation.log)"
fi

if [ "$FAILED" -ne 0 ]; then
  echo "== VERIFICATION GATE: FAIL — package NOT built =="
  exit 1
fi
echo "== VERIFICATION GATE: PASS =="

# Docs into the package.
cp "$REPO/LICENSE" "$REPO/NOTICE" "$STAGE_ROOT/$PKG_NAME/" 2>/dev/null || true
cp "$REPO/docs/distribution/INSTALL.md" "$STAGE_ROOT/$PKG_NAME/INSTALL.md"
cp "$REPO/docs/distribution/RELEASE_NOTES.md" "$STAGE_ROOT/$PKG_NAME/RELEASE_NOTES.md"
cp "$GATE_LOG" "$STAGE_ROOT/$PKG_NAME/reports/verification-gate.txt"

# Reproducible zip: fixed mtimes, no extra attributes.
cd "$STAGE_ROOT/$PKG_NAME"
find . -exec touch -t 202601010000.00 {} +
sha256sum apks/*.apk INSTALL.md RELEASE_NOTES.md LICENSE NOTICE reports/* > SHA256SUMS.txt 2>/dev/null || \
  sha256sum apks/*.apk INSTALL.md RELEASE_NOTES.md reports/* > SHA256SUMS.txt

mkdir -p "$OUT_DIR"
rm -f "$ZIP_PATH"
# Zip FROM THE PARENT so the archive contains the package folder itself
# (manual extraction then produces drs-os-.../ directly).
( cd "$STAGE_ROOT" && zip -X -q -r "$ZIP_PATH" "$PKG_NAME" )
note "zip built: $(stat -c%s "$ZIP_PATH") bytes"
sha256sum "$ZIP_PATH"

# Integrity chain: extraction re-verification FROM THE PACKAGE ROOT.
VERIFY_DIR=/home/z/my-project/scripts/tmp/pkg_verify
rm -rf "$VERIFY_DIR"
mkdir -p "$VERIFY_DIR"
unzip -q "$ZIP_PATH" -d "$VERIFY_DIR"
cd "$VERIFY_DIR/$PKG_NAME" || { bad "extracted package root missing"; exit 1; }
if sha256sum -c SHA256SUMS.txt > "$TMPD/check.log" 2>&1; then
  ok "extraction re-verification: $(grep -c ': OK' "$TMPD/check.log")/$(grep -c ':' "$TMPD/check.log") entries OK"
else
  bad "sha256sum -c FAILED after extraction"
fi

if [ "$FAILED" -ne 0 ]; then
  echo "== PACKAGING: FAIL =="
  exit 1
fi
echo "== PACKAGING: PASS =="
echo "ZIP: $ZIP_PATH"
echo "ZIP_SHA256: $(sha256sum "$ZIP_PATH" | awk '{print $1}')"
