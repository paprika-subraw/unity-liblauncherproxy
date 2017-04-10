$UNITY = "${env:ProgramFiles}\Unity\Editor\Unity.exe"

$BASE_DIR = Split-Path -Path $MyInvocation.MyCommand.Definition -Parent
Set-Location $BASE_DIR

# Gradle
Set-Location .\projects\AndroidStudio\LibLauncherProxy
.\gradlew.bat :liblauncherproxy:clean :liblauncherproxy:putAarMainReleaseIntoUnityproject :liblauncherproxy:putAarForAnyReleaseIntoUnityproject

# Unity
Set-Location $BASE_DIR
Start-Process -Wait $UNITY "-batchmode -projectPath ""$BASE_DIR\projects\Unity\LibLauncherProxy"" -exportPackage Assets\LibLauncherProxy\Plugins\Android\liblauncherproxy-main-release.aar ""$BASE_DIR\LibLauncherProxyForMarshmallow.unitypackage"" -quit"
Start-Process -Wait $UNITY "-batchmode -projectPath ""$BASE_DIR\projects\Unity\LibLauncherProxy"" -exportPackage Assets\LibLauncherProxy\Plugins\Android\liblauncherproxy-forAny-release.aar ""$BASE_DIR\LibLauncherProxy.unitypackage"" -quit"
