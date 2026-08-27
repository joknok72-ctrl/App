#!/bin/bash
# ============================================
# FF Booster Pro - Direct APK build script
# (aapt2 + javac + d8 + zipalign + apksigner)
# ============================================
set -e

SDK=/home/user/android-sdk
BT=$SDK/build-tools/35.0.0
PLATFORM=$SDK/platforms/android-34/android.jar
ROOT=/home/user/webapp
SRC=$ROOT/app/src/main
OUT=$ROOT/build-output
KEYSTORE=$ROOT/ffbooster.keystore

rm -rf $OUT/tmp
mkdir -p $OUT/tmp/{compiled,gen,classes}

echo "[1/6] Compiling resources (aapt2)..."
$BT/aapt2 compile --dir $SRC/res -o $OUT/tmp/compiled/res.zip

echo "[2/6] Linking resources + generating R.java..."
$BT/aapt2 link \
  -I $PLATFORM \
  --manifest $SRC/AndroidManifest.xml \
  --java $OUT/tmp/gen \
  -o $OUT/tmp/base.apk \
  $OUT/tmp/compiled/res.zip \
  --auto-add-overlay

echo "[3/6] Compiling Java sources..."
javac -source 17 -target 17 \
  -classpath $PLATFORM \
  -nowarn -Xlint:none \
  -d $OUT/tmp/classes \
  $OUT/tmp/gen/com/ffbooster/pro/R.java \
  $SRC/java/com/ffbooster/pro/*.java

echo "[4/6] Dexing (d8)..."
$BT/d8 --release \
  --lib $PLATFORM \
  --min-api 24 \
  --output $OUT/tmp \
  $(find $OUT/tmp/classes -name '*.class')

cd $OUT/tmp
zip -qj base.apk classes.dex

echo "[5/6] Aligning..."
$BT/zipalign -f 4 base.apk aligned.apk

echo "[6/6] Signing..."
if [ ! -f $KEYSTORE ]; then
  keytool -genkeypair -v -keystore $KEYSTORE -alias ffbooster \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass ffbooster123 -keypass ffbooster123 \
    -dname "CN=FF Booster Pro, OU=Dev, O=FFBooster, L=Cairo, C=EG" 2>/dev/null
fi

$BT/apksigner sign --ks $KEYSTORE \
  --ks-pass pass:ffbooster123 --key-pass pass:ffbooster123 \
  --out $OUT/FFBoosterPro.apk aligned.apk

$BT/apksigner verify $OUT/FFBoosterPro.apk && echo "SIGNATURE OK"

rm -rf $OUT/tmp
ls -lh $OUT/FFBoosterPro.apk
echo "✅ BUILD SUCCESSFUL: $OUT/FFBoosterPro.apk"
