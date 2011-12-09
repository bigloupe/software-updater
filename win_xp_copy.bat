: for Windows XP, Vista or up please use win_vista_synbolic_links.bat

set workingDirectory=%CD%


set softwareDirectory=SoftwareLauncher

mkdir "%softwareDirectory%\src\org\apache\commons"
call:copyDir src\org\apache\commons\codec
mkdir "%softwareDirectory%\src"
call:copyDir src\watne
mkdir "%softwareDirectory%\src\updater\concurrent"
call:copyFile src\updater\concurrent\ConcurrentLock.java
call:copyFile src\updater\concurrent\LockType.java
call:copyFile src\updater\concurrent\LockUtil.java
mkdir "%softwareDirectory%\src\updater\crypto"
call:copyFile src\updater\crypto\AESKey.java
mkdir "%softwareDirectory%\src\updater\gui"
call:copyFile src\updater\gui\JTitledPanel.java
call:copyFile src\updater\gui\UpdaterWindow.java
mkdir "%softwareDirectory%\src\updater\script"
call:copyFile src\updater\script\Client.java
call:copyFile src\updater\script\InvalidFormatException.java
call:copyFile src\updater\script\Patch.java
mkdir "%softwareDirectory%\src\updater\patch"
call:copyFile src\updater\patch\Compression.java
call:copyFile src\updater\patch\OperationType.java
call:copyFile src\updater\patch\PatchReadUtil.java
call:copyFile src\updater\patch\PatchRecord.java
call:copyFile src\updater\patch\LogAction.java
call:copyFile src\updater\patch\LogReader.java
call:copyFile src\updater\patch\LogWriter.java
call:copyFile src\updater\patch\Patcher.java
call:copyFile src\updater\patch\PatcherListener.java
mkdir "%softwareDirectory%\src\updater\util"
call:copyFile src\updater\util\CommonUtil.java
call:copyFile src\updater\util\Interruptible.java
call:copyFile src\updater\util\InterruptibleInputStream.java
call:copyFile src\updater\util\InterruptibleOutputStream.java
call:copyFile src\updater\util\Pausable.java
call:copyFile src\updater\util\SeekableFile.java
call:copyFile src\updater\util\XMLUtil.java
mkdir "%softwareDirectory%\test\updater"
call:copyFile test\updater\TestCommon.java


set softwareDirectory=SoftwarePatchBuilder

mkdir "%softwareDirectory%\src\org\apache\commons"
call:copyDir src\org\apache\commons\codec
mkdir "%softwareDirectory%\src"
call:copyDir src\watne
mkdir "%softwareDirectory%\src\updater\crypto"
call:copyFile src\updater\crypto\AESKey.java
call:copyFile src\updater\crypto\KeyGenerator.java
call:copyFile src\updater\crypto\RSAKey.java
mkdir "%softwareDirectory%\src\updater\script"
call:copyFile src\updater\script\Catalog.java
call:copyFile src\updater\script\Client.java
call:copyFile src\updater\script\InvalidFormatException.java
call:copyFile src\updater\script\Patch.java
mkdir "%softwareDirectory%\src\updater\patch"
call:copyFile src\updater\patch\Compression.java
call:copyFile src\updater\patch\OperationType.java
call:copyFile src\updater\patch\PatchCreator.java
call:copyFile src\updater\patch\PatchExtractor.java
call:copyFile src\updater\patch\PatchReadUtil.java
call:copyFile src\updater\patch\PatchRecord.java
call:copyFile src\updater\patch\PatchWriteUtil.java
call:copyFile src\updater\patch\LogAction.java
call:copyFile src\updater\patch\LogReader.java
call:copyFile src\updater\patch\LogWriter.java
call:copyFile src\updater\patch\PatchPacker.java
call:copyFile src\updater\patch\Patcher.java
call:copyFile src\updater\patch\PatcherListener.java
mkdir "%softwareDirectory%\src\updater\util"
call:copyFile src\updater\util\CommonUtil.java
call:copyFile src\updater\util\Interruptible.java
call:copyFile src\updater\util\InterruptibleInputStream.java
call:copyFile src\updater\util\InterruptibleOutputStream.java
call:copyFile src\updater\util\Pausable.java
call:copyFile src\updater\util\SeekableFile.java
call:copyFile src\updater\util\XMLUtil.java
mkdir "%softwareDirectory%\test\updater"
call:copyFile test\updater\TestCommon.java


set softwareDirectory=SoftwarePatchDownloader

mkdir "%softwareDirectory%\src\updater\concurrent"
call:copyFile src\updater\concurrent\ConcurrentLock.java
call:copyFile src\updater\concurrent\LockType.java
call:copyFile src\updater\concurrent\LockUtil.java
mkdir "%softwareDirectory%\src\updater\gui"
call:copyFile src\updater\gui\JTitledPanel.java
call:copyFile src\updater\gui\UpdaterWindow.java
mkdir "%softwareDirectory%\src\updater\script"
call:copyFile src\updater\script\Catalog.java
call:copyFile src\updater\script\Client.java
call:copyFile src\updater\script\InvalidFormatException.java
call:copyFile src\updater\script\Patch.java
mkdir "%softwareDirectory%\src\updater\util"
call:copyFile src\updater\util\CommonUtil.java
call:copyFile src\updater\util\DownloadProgressListener.java
call:copyFile src\updater\util\DownloadProgressUtil.java
call:copyFile src\updater\util\DownloadResult.java
call:copyFile src\updater\util\HTTPDownloader.java
call:copyFile src\updater\util\Interruptible.java
call:copyFile src\updater\util\Pausable.java
call:copyFile src\updater\util\XMLUtil.java
mkdir "%softwareDirectory%\test\updater"
call:copyFile test\updater\TestCommon.java


echo.&pause&goto:eof


:copyFile
SETLOCAL
del "%softwareDirectory%\%~1"
copy "%workingDirectory%\SoftwareUpdaterCommon\%~1" "%softwareDirectory%\%~1"
ENDLOCAL
goto:eof

:copyDir
SETLOCAL
rmdir /S /Q "%softwareDirectory%\%~1"
mkdir "%softwareDirectory%\%~1"
xcopy /E "%workingDirectory%\SoftwareUpdaterCommon\%~1" "%softwareDirectory%\%~1"
ENDLOCAL
goto:eof
