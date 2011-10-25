; only for Windows Vista or up (need mklink)

set workingDirectory=%CD%


;;;;;;;;;;;;;;;;;;;;;;;
;  Software Launcher  ;
;;;;;;;;;;;;;;;;;;;;;;;

mkdir ..\SoftwareLauncher\src\org\apache\commons\
mklink /J ..\SoftwareLauncher\src\org\apache\commons\codec\ %workingDirectory%\src\org\apache\commons\codec\

mklink /J ..\SoftwareLauncher\src\watne\ %workingDirectory%\src\watne\

mkdir ..\SoftwareLauncher\src\updater\gui\
mklink ..\SoftwareLauncher\src\updater\gui\JTitledPanel.java %workingDirectory%\src\updater\gui\JTitledPanel.java
mklink ..\SoftwareLauncher\src\updater\gui\UpdaterWindow.java %workingDirectory%\src\updater\gui\UpdaterWindow.java

mkdir ..\SoftwareLauncher\src\updater\script\
mklink ..\SoftwareLauncher\src\updater\script\Client.java %workingDirectory%\src\updater\script\Client.java
mklink ..\SoftwareLauncher\src\updater\script\InvalidFormatException.java %workingDirectory%\src\updater\script\InvalidFormatException.java
mklink ..\SoftwareLauncher\src\updater\script\Patch.java %workingDirectory%\src\updater\script\Patch.java

mkdir ..\SoftwareLauncher\src\updater\patch\
mklink ..\SoftwareLauncher\src\updater\patch\PatchReadUtil.java %workingDirectory%\src\updater\patch\PatchReadUtil.java
mklink ..\SoftwareLauncher\src\updater\patch\PatchLogReader.java %workingDirectory%\src\updater\patch\PatchLogReader.java
mklink ..\SoftwareLauncher\src\updater\patch\PatchLogWriter.java %workingDirectory%\src\updater\patch\PatchLogWriter.java
mklink ..\SoftwareLauncher\src\updater\patch\Patcher.java %workingDirectory%\src\updater\patch\Patcher.java
mklink ..\SoftwareLauncher\src\updater\patch\PatcherListener.java %workingDirectory%\src\updater\patch\PatcherListener.java

mkdir ..\SoftwareLauncher\src\updater\util\
mklink ..\SoftwareLauncher\src\updater\util\CommonUtil.java %workingDirectory%\src\updater\util\CommonUtil.java
mklink ..\SoftwareLauncher\src\updater\util\InterruptibleInputStream.java %workingDirectory%\src\updater\util\InterruptibleInputStream.java
mklink ..\SoftwareLauncher\src\updater\util\InterruptibleOutputStream.java %workingDirectory%\src\updater\util\InterruptibleOutputStream.java
mklink ..\SoftwareLauncher\src\updater\util\XMLUtil.java %workingDirectory%\src\updater\util\XMLUtil.java
mklink ..\SoftwareLauncher\src\updater\util\SeekableFile.java %workingDirectory%\src\updater\util\SeekableFile.java

mkdir ..\SoftwareLauncher\test\updater\
mklink ..\SoftwareLauncher\test\updater\TestCommon.java %workingDirectory%\test\updater\TestCommon.java


;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;  Software Patch Builder  ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;

mkdir ..\SoftwarePatchBuilder\src\org\apache\commons\
mklink /J ..\SoftwarePatchBuilder\src\org\apache\commons\codec\ %workingDirectory%\src\org\apache\commons\codec\

mklink /J ..\SoftwarePatchBuilder\src\watne\ %workingDirectory%\src\watne\

mkdir ..\SoftwarePatchBuilder\src\updater\script\
mklink ..\SoftwarePatchBuilder\src\updater\script\Catalog.java %workingDirectory%\src\updater\script\Catalog.java
mklink ..\SoftwarePatchBuilder\src\updater\script\Client.java %workingDirectory%\src\updater\script\Client.java
mklink ..\SoftwarePatchBuilder\src\updater\script\InvalidFormatException.java %workingDirectory%\src\updater\script\InvalidFormatException.java
mklink ..\SoftwarePatchBuilder\src\updater\script\Patch.java %workingDirectory%\src\updater\script\Patch.java

mkdir ..\SoftwarePatchBuilder\src\updater\patch\
mklink ..\SoftwarePatchBuilder\src\updater\patch\PatchReadUtil.java %workingDirectory%\src\updater\patch\PatchReadUtil.java
mklink ..\SoftwarePatchBuilder\src\updater\patch\PatchWriteUtil.java %workingDirectory%\src\updater\patch\PatchWriteUtil.java
mklink ..\SoftwarePatchBuilder\src\updater\patch\PatchLogReader.java %workingDirectory%\src\updater\patch\PatchLogReader.java
mklink ..\SoftwarePatchBuilder\src\updater\patch\PatchLogWriter.java %workingDirectory%\src\updater\patch\PatchLogWriter.java
mklink ..\SoftwarePatchBuilder\src\updater\patch\Patcher.java %workingDirectory%\src\updater\patch\Patcher.java
mklink ..\SoftwarePatchBuilder\src\updater\patch\PatcherListener.java %workingDirectory%\src\updater\patch\PatcherListener.java

mkdir ..\SoftwarePatchBuilder\src\updater\util\
mklink ..\SoftwarePatchBuilder\src\updater\util\CommonUtil.java %workingDirectory%\src\updater\util\CommonUtil.java
mklink ..\SoftwarePatchBuilder\src\updater\util\InterruptibleInputStream.java %workingDirectory%\src\updater\util\InterruptibleInputStream.java
mklink ..\SoftwarePatchBuilder\src\updater\util\InterruptibleOutputStream.java %workingDirectory%\src\updater\util\InterruptibleOutputStream.java
mklink ..\SoftwarePatchBuilder\src\updater\util\XMLUtil.java %workingDirectory%\src\updater\util\XMLUtil.java
mklink ..\SoftwarePatchBuilder\src\updater\util\SeekableFile.java %workingDirectory%\src\updater\util\SeekableFile.java

mkdir ..\SoftwarePatchBuilder\test\updater\
mklink ..\SoftwarePatchBuilder\test\updater\TestCommon.java %workingDirectory%\test\updater\TestCommon.java


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;  Software Patch Downloader  ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

mkdir ..\SoftwarePatchDownloader\src\updater\gui\
mklink ..\SoftwarePatchDownloader\src\updater\gui\JTitledPanel.java %workingDirectory%\src\updater\gui\JTitledPanel.java
mklink ..\SoftwarePatchDownloader\src\updater\gui\UpdaterWindow.java %workingDirectory%\src\updater\gui\UpdaterWindow.java

mkdir ..\SoftwarePatchDownloader\src\updater\script\
mklink ..\SoftwarePatchDownloader\src\updater\script\Catalog.java %workingDirectory%\src\updater\script\Catalog.java
mklink ..\SoftwarePatchDownloader\src\updater\script\Client.java %workingDirectory%\src\updater\script\Client.java
mklink ..\SoftwarePatchDownloader\src\updater\script\InvalidFormatException.java %workingDirectory%\src\updater\script\InvalidFormatException.java
mklink ..\SoftwarePatchDownloader\src\updater\script\Patch.java %workingDirectory%\src\updater\script\Patch.java

mkdir ..\SoftwarePatchDownloader\src\updater\util\
mklink ..\SoftwarePatchDownloader\src\updater\util\CommonUtil.java %workingDirectory%\src\updater\util\CommonUtil.java
mklink ..\SoftwarePatchDownloader\src\updater\util\XMLUtil.java %workingDirectory%\src\updater\util\XMLUtil.java

mkdir ..\SoftwarePatchDownloader\test\updater\
mklink ..\SoftwarePatchDownloader\test\updater\TestCommon.java %workingDirectory%\test\updater\TestCommon.java

