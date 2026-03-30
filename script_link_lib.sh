#!/bin/bash

# Find your Android Studio DerivedData directory
find /Library/Caches -name "iosApp-*" -type d 2>/dev/null

# For each directory found, create the symlinks:
DERIVED_DIR="/Library/Caches/Google/AndroidStudio2025.1.2/DerivedData/iosApp-cressajurkchroaydvivdkdrkwvn"

# Create necessary directories
mkdir -p "$DERIVED_DIR/Build/Products/Debug-iphoneos"
mkdir -p "$DERIVED_DIR/Build/Products/Debug-iphonesimulator"

# Link TensorFlow frameworks
ln -sf ~/YourProject/iosApp/Pods/TensorFlowLiteObjC/Frameworks \ "$DERIVED_DIR/Build/Products/Debug-iphoneos/TensorFlowLiteObjC"