; only for Windows Vista or up (need mklink)

set workingDirectory=%CD%

;;;;;;;;;;;;;;;;;;;;;;;
;  Software Launcher  ;
;;;;;;;;;;;;;;;;;;;;;;;

set softwareDirectory=SoftwareLauncher

mkdir %softwareDirectory%\src\org\apache\commons\
set file=src\org\apache\commons\codec\
call:linkDir

set file=src\watne\
call:linkDir

mkdir %softwareDirectory%\src\updater\crypto\
set file=src\updater\crypto\AESKey.java
call:linkFile

mkdir %softwareDirectory%\src\updater\gui\
set file=src\updater\gui\JTitledPanel.java
call:linkFile
set file=src\updater\gui\UpdaterWindow.java
call:linkFile

mkdir %softwareDirectory%\src\updater\script\
set file=src\updater\script\Client.java
call:linkFile
set file=src\updater\script\InvalidFormatException.java
call:linkFile
set file=src\updater\script\Patch.java
call:linkFile

mkdir %softwareDirectory%\src\updater\patch\
set file=src\updater\patch\PatchReadUtil.java
call:linkFile
set file=src\updater\patch\PatchLogReader.java
call:linkFile
set file=src\updater\patch\PatchLogWriter.java
call:linkFile
set file=src\updater\patch\Patcher.java
call:linkFile
set file=src\updater\patch\PatcherListener.java
call:linkFile

mkdir %softwareDirectory%\src\updater\util\
set file=src\updater\util\CommonUtil.java
call:linkFile
set file=src\updater\util\InterruptibleInputStream.java
call:linkFile
set file=src\updater\util\InterruptibleOutputStream.java
call:linkFile
set file=src\updater\util\XMLUtil.java
call:linkFile
set file=src\updater\util\SeekableFile.java
call:linkFile

mkdir %softwareDirectory%\test\updater\
set file=test\updater\TestCommon.java
call:linkFile


;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;  Software Patch Builder  ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;

set softwareDirectory=SoftwarePatchBuilder

mkdir %softwareDirectory%\src\org\apache\commons\
set file=src\org\apache\commons\codec\
call:linkDir

set file=src\watne\
call:linkDir

mkdir %softwareDirectory%\src\updater\crypto\
set file=src\updater\crypto\AESKey.java
call:linkFile
set file=src\updater\crypto\KeyGenerator.java
call:linkFile
set file=src\updater\crypto\RSAKey.java
call:linkFile

mkdir %softwareDirectory%\src\updater\script\
set file=src\updater\script\Catalog.java
call:linkFile
set file=src\updater\script\Client.java
call:linkFile
set file=src\updater\script\InvalidFormatException.java
call:linkFile
set file=src\updater\script\Patch.java
call:linkFile

mkdir %softwareDirectory%\src\updater\patch\
set file=src\updater\patch\PatchCreator.java
call:linkFile
set file=src\updater\patch\PatchExtractor.java
call:linkFile
set file=src\updater\patch\PatchReadUtil.java
call:linkFile
set file=src\updater\patch\PatchWriteUtil.java
call:linkFile
set file=src\updater\patch\PatchLogReader.java
call:linkFile
set file=src\updater\patch\PatchLogWriter.java
call:linkFile
set file=src\updater\patch\PatchPacker.java
call:linkFile
set file=src\updater\patch\Patcher.java
call:linkFile
set file=src\updater\patch\PatcherListener.java
call:linkFile

mkdir %softwareDirectory%\src\updater\util\
set file=src\updater\util\CommonUtil.java
call:linkFile
set file=src\updater\util\InterruptibleInputStream.java
call:linkFile
set file=src\updater\util\InterruptibleOutputStream.java
call:linkFile
set file=src\updater\util\XMLUtil.java
call:linkFile
set file=src\updater\util\SeekableFile.java
call:linkFile

mkdir %softwareDirectory%\test\updater\
set file=test\updater\TestCommon.java
call:linkFile


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;  Software Patch Downloader  ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

set softwareDirectory=SoftwarePatchDownloader

mkdir %softwareDirectory%\src\updater\gui\
set file=src\updater\gui\JTitledPanel.java
call:linkFile
set file=src\updater\gui\UpdaterWindow.java
call:linkFile

mkdir %softwareDirectory%\src\updater\script\
set file=src\updater\script\Catalog.java
call:linkFile
set file=src\updater\script\Client.java
call:linkFile
set file=src\updater\script\InvalidFormatException.java
call:linkFile
set file=src\updater\script\Patch.java
call:linkFile

mkdir %softwareDirectory%\src\updater\util\
set file=src\updater\util\CommonUtil.java
call:linkFile
set file=src\updater\util\DownloadProgressUtil.java
call:linkFile
set file=src\updater\util\XMLUtil.java
call:linkFile

mkdir %softwareDirectory%\test\updater\
set file=test\updater\TestCommon.java
call:linkFile



:linkFile
del %softwareDirectory%\%file%
mklink %softwareDirectory%\%file% %workingDirectory%\SoftwareUpdaterCommon\%file%
goto:eof

:linkDir
rmdir %softwareDirectory%\%file%
mklink /J %softwareDirectory%\%file% %workingDirectory%\SoftwareUpdaterCommon\%file%
goto:eof
