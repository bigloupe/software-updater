: only for Windows Vista or up (need mklink), needs administrative right

set workingDirectory=%CD%


set softwareDirectory=SoftwareLauncher

mkdir "%softwareDirectory%\src\org\apache\commons"
call:linkDir src\org\apache\commons\codec
mkdir "%softwareDirectory%\src"
call:linkDir src\watne
mkdir "%softwareDirectory%\src\updater\concurrent"
call:linkFile src\updater\concurrent\ConcurrentLock.java
call:linkFile src\updater\concurrent\LockType.java
call:linkFile src\updater\concurrent\LockUtil.java
mkdir "%softwareDirectory%\src\updater\crypto"
call:linkFile src\updater\crypto\AESKey.java
mkdir "%softwareDirectory%\src\updater\gui"
call:linkFile src\updater\gui\JTitledPanel.java
call:linkFile src\updater\gui\UpdaterWindow.java
mkdir "%softwareDirectory%\src\updater\script"
call:linkFile src\updater\script\Client.java
call:linkFile src\updater\script\InvalidFormatException.java
call:linkFile src\updater\script\Patch.java
mkdir "%softwareDirectory%\src\updater\patch"
call:linkFile src\updater\patch\Compression.java
call:linkFile src\updater\patch\OperationType.java
call:linkFile src\updater\patch\PatchReadUtil.java
call:linkFile src\updater\patch\PatchRecord.java
call:linkFile src\updater\patch\LogAction.java
call:linkFile src\updater\patch\LogReader.java
call:linkFile src\updater\patch\LogWriter.java
call:linkFile src\updater\patch\Patcher.java
call:linkFile src\updater\patch\PatcherListener.java
call:linkFile src\updater\patch\ReplacementRecord.java
mkdir "%softwareDirectory%\src\updater\util"
call:linkFile src\updater\util\CommonUtil.java
call:linkFile src\updater\util\GetClientScriptResult.java
call:linkFile src\updater\util\Interruptible.java
call:linkFile src\updater\util\InterruptibleInputStream.java
call:linkFile src\updater\util\InterruptibleOutputStream.java
call:linkFile src\updater\util\Pausable.java
call:linkFile src\updater\util\SeekableFile.java
call:linkFile src\updater\util\XMLUtil.java
mkdir "%softwareDirectory%\test\updater"
call:linkFile test\updater\TestCommon.java


set softwareDirectory=SoftwarePatchBuilder

mkdir "%softwareDirectory%\src\org\apache\commons"
call:linkDir src\org\apache\commons\codec
mkdir "%softwareDirectory%\src"
call:linkDir src\watne
mkdir "%softwareDirectory%\src\updater\concurrent"
call:linkFile src\updater\concurrent\ConcurrentLock.java
mkdir "%softwareDirectory%\src\updater\crypto"
call:linkFile src\updater\crypto\AESKey.java
call:linkFile src\updater\crypto\KeyGenerator.java
call:linkFile src\updater\crypto\RSAKey.java
mkdir "%softwareDirectory%\src\updater\script"
call:linkFile src\updater\script\Catalog.java
call:linkFile src\updater\script\Client.java
call:linkFile src\updater\script\InvalidFormatException.java
call:linkFile src\updater\script\Patch.java
mkdir "%softwareDirectory%\src\updater\patch"
call:linkFile src\updater\patch\Compression.java
call:linkFile src\updater\patch\OperationType.java
call:linkFile src\updater\patch\PatchCreator.java
call:linkFile src\updater\patch\PatchExtractor.java
call:linkFile src\updater\patch\PatchReadUtil.java
call:linkFile src\updater\patch\PatchRecord.java
call:linkFile src\updater\patch\PatchWriteUtil.java
call:linkFile src\updater\patch\LogAction.java
call:linkFile src\updater\patch\LogReader.java
call:linkFile src\updater\patch\LogWriter.java
call:linkFile src\updater\patch\PatchPacker.java
call:linkFile src\updater\patch\Patcher.java
call:linkFile src\updater\patch\PatcherListener.java
call:linkFile src\updater\patch\ReplacementRecord.java
mkdir "%softwareDirectory%\src\updater\util"
call:linkFile src\updater\util\CommonUtil.java
call:linkFile src\updater\util\GetClientScriptResult.java
call:linkFile src\updater\util\Interruptible.java
call:linkFile src\updater\util\InterruptibleInputStream.java
call:linkFile src\updater\util\InterruptibleOutputStream.java
call:linkFile src\updater\util\Pausable.java
call:linkFile src\updater\util\SeekableFile.java
call:linkFile src\updater\util\XMLUtil.java
mkdir "%softwareDirectory%\test\updater"
call:linkFile test\updater\TestCommon.java


set softwareDirectory=SoftwarePatchDownloader

mkdir "%softwareDirectory%\src\updater\concurrent"
call:linkFile src\updater\concurrent\ConcurrentLock.java
call:linkFile src\updater\concurrent\LockType.java
call:linkFile src\updater\concurrent\LockUtil.java
mkdir "%softwareDirectory%\src\updater\gui"
call:linkFile src\updater\gui\JTitledPanel.java
call:linkFile src\updater\gui\UpdaterWindow.java
mkdir "%softwareDirectory%\src\updater\script"
call:linkFile src\updater\script\Catalog.java
call:linkFile src\updater\script\Client.java
call:linkFile src\updater\script\InvalidFormatException.java
call:linkFile src\updater\script\Patch.java
mkdir "%softwareDirectory%\src\updater\util"
call:linkFile src\updater\util\CommonUtil.java
call:linkFile src\updater\util\GetClientScriptResult.java
call:linkFile src\updater\util\DownloadProgressListener.java
call:linkFile src\updater\util\DownloadProgressUtil.java
call:linkFile src\updater\util\DownloadResult.java
call:linkFile src\updater\util\HTTPDownloader.java
call:linkFile src\updater\util\Interruptible.java
call:linkFile src\updater\util\Pausable.java
call:linkFile src\updater\util\XMLUtil.java
mkdir "%softwareDirectory%\test\updater"
call:linkFile test\updater\TestCommon.java


echo.&pause&goto:eof


:linkFile
SETLOCAL
del "%softwareDirectory%\%~1"
mklink "%softwareDirectory%\%~1" "%workingDirectory%\SoftwareUpdaterCommon\%~1"
ENDLOCAL
goto:eof

:linkDir
SETLOCAL
rmdir "%softwareDirectory%\%~1"
mklink /J "%softwareDirectory%\%~1" "%workingDirectory%\SoftwareUpdaterCommon\%~1"
ENDLOCAL
goto:eof
