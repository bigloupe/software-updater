#!/bin/bash

WORKING_DIRECTORY=$(pwd)


linkFile() {
  rm $SOFTWARE_DIRECTORY"/$1"
  ln -s -f "$WORKING_DIRECTORY/SoftwareUpdaterCommon/$1" $SOFTWARE_DIRECTORY"/$1"
}

linkDir() {
  rm -r -f $SOFTWARE_DIRECTORY"/$1"
  ln -s -d "$WORKING_DIRECTORY/SoftwareUpdaterCommon/$1" $SOFTWARE_DIRECTORY"/$1"
}


SOFTWARE_DIRECTORY="SoftwareLauncher"

mkdir -p "$SOFTWARE_DIRECTORY/src/org/apache/commons"
linkDir src/org/apache/commons/codec
mkdir -p "$SOFTWARE_DIRECTORY/src"
linkDir src/watne
mkdir -p "$SOFTWARE_DIRECTORY/src/updater/crypto"
linkFile src/updater/crypto/AESKey.java
mkdir -p "$SOFTWARE_DIRECTORY/src/updater/gui"
linkFile src/updater/gui/JTitledPanel.java
linkFile src/updater/gui/UpdaterWindow.java
mkdir -p "$SOFTWARE_DIRECTORY/src/updater/script"
linkFile src/updater/script/Client.java
linkFile src/updater/script/InvalidFormatException.java
linkFile src/updater/script/Patch.java
mkdir -p "$SOFTWARE_DIRECTORY/src/updater/patch"
linkFile src/updater/patch/Compression.java
linkFile src/updater/patch/PatchReadUtil.java
linkFile src/updater/patch/PatchLogReader.java
linkFile src/updater/patch/PatchLogWriter.java
linkFile src/updater/patch/Patcher.java
linkFile src/updater/patch/PatcherListener.java
mkdir -p "$SOFTWARE_DIRECTORY/src/updater/util"
linkFile src/updater/util/CommonUtil.java
linkFile src/updater/util/InterruptibleInputStream.java
linkFile src/updater/util/InterruptibleOutputStream.java
linkFile src/updater/util/XMLUtil.java
linkFile src/updater/util/SeekableFile.java
mkdir -p "$SOFTWARE_DIRECTORY/test/updater"
linkFile test/updater/TestCommon.java


SOFTWARE_DIRECTORY="SoftwarePatchBuilder"

mkdir -p "$SOFTWARE_DIRECTORY/src/org/apache/commons"
linkDir src/org/apache/commons/codec
mkdir -p "$SOFTWARE_DIRECTORY/src"
linkDir src/watne
mkdir -p "$SOFTWARE_DIRECTORY/src/updater/crypto"
linkFile src/updater/crypto/AESKey.java
linkFile src/updater/crypto/KeyGenerator.java
linkFile src/updater/crypto/RSAKey.java
mkdir -p "$SOFTWARE_DIRECTORY/src/updater/script"
linkFile src/updater/script/Catalog.java
linkFile src/updater/script/Client.java
linkFile src/updater/script/InvalidFormatException.java
linkFile src/updater/script/Patch.java
mkdir -p "$SOFTWARE_DIRECTORY/src/updater/patch"
linkFile src/updater/patch/Compression.java
linkFile src/updater/patch/PatchCreator.java
linkFile src/updater/patch/PatchExtractor.java
linkFile src/updater/patch/PatchReadUtil.java
linkFile src/updater/patch/PatchWriteUtil.java
linkFile src/updater/patch/PatchLogReader.java
linkFile src/updater/patch/PatchLogWriter.java
linkFile src/updater/patch/PatchPacker.java
linkFile src/updater/patch/Patcher.java
linkFile src/updater/patch/PatcherListener.java
mkdir -p "$SOFTWARE_DIRECTORY/src/updater/util"
linkFile src/updater/util/CommonUtil.java
linkFile src/updater/util/InterruptibleInputStream.java
linkFile src/updater/util/InterruptibleOutputStream.java
linkFile src/updater/util/XMLUtil.java
linkFile src/updater/util/SeekableFile.java
mkdir -p "$SOFTWARE_DIRECTORY/test/updater"
linkFile test/updater/TestCommon.java


SOFTWARE_DIRECTORY="SoftwarePatchDownloader"

mkdir -p "$SOFTWARE_DIRECTORY/src/updater/gui"
linkFile src/updater/gui/JTitledPanel.java
linkFile src/updater/gui/UpdaterWindow.java
mkdir -p "$SOFTWARE_DIRECTORY/src/updater/script"
linkFile src/updater/script/Catalog.java
linkFile src/updater/script/Client.java
linkFile src/updater/script/InvalidFormatException.java
linkFile src/updater/script/Patch.java
mkdir -p "$SOFTWARE_DIRECTORY/src/updater/util"
linkFile src/updater/util/CommonUtil.java
linkFile src/updater/util/DownloadProgressUtil.java
linkFile src/updater/util/XMLUtil.java
mkdir -p "$SOFTWARE_DIRECTORY/test/updater"
linkFile test/updater/TestCommon.java
