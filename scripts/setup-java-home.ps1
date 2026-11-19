# Sets JAVA_HOME for the current Windows user so ./gradlew works from any terminal.
# Uses a JDK that is already installed (existing JAVA_HOME or the one bundled with Android Studio).
# Run once:  powershell -ExecutionPolicy Bypass -File scripts\setup-java-home.ps1
# Then open a new terminal.

$jdk = @($env:JAVA_HOME,
         "$env:ProgramFiles\Android\Android Studio\jbr",
         "$env:LOCALAPPDATA\Programs\Android Studio\jbr") |
       Where-Object { $_ -and (Test-Path "$_\bin\java.exe") } | Select-Object -First 1

if ($jdk) {
    [Environment]::SetEnvironmentVariable('JAVA_HOME', $jdk, 'User')
    "JAVA_HOME = $jdk"
    "Open a new terminal, then run: .\gradlew help"
} else {
    'JDK not found. Check where Android Studio is installed (look for its "jbr" folder).'
}
