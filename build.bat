@echo off

set UNITY=%ProgramFiles%\Unity\Editor\Unity.exe

rem Gradle
cd /d "%~dp0projects\AndroidStudio\LibLauncherProxy"
call gradlew --stacktrace :liblauncherproxy:clean :liblauncherproxy:putAarMainReleaseIntoUnityproject :liblauncherproxy:putAarForAnyReleaseIntoUnityproject

rem Unity
cd /d %~dp0
call "%UNITY%" -batchmode -projectPath "%~dp0projects\Unity\LibLauncherProxy" -exportPackage Assets\LibLauncherProxy\Plugins\Android\liblauncherproxy-main-release.aar "%~dp0LibLauncherProxyForMarshmallow.unitypackage" -quit
call "%UNITY%" -batchmode -projectPath "%~dp0projects\Unity\LibLauncherProxy" -exportPackage Assets\LibLauncherProxy\Plugins\Android\liblauncherproxy-forAny-release.aar "%~dp0LibLauncherProxy.unitypackage" -quit
