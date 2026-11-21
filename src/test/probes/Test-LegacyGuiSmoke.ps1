$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
if ($env:JAVA_HOME) {
    $java = Join-Path $env:JAVA_HOME 'bin/java.exe'
    $javac = Join-Path $env:JAVA_HOME 'bin/javac.exe'
    if (!(Test-Path -LiteralPath $java -PathType Leaf) -or !(Test-Path -LiteralPath $javac -PathType Leaf)) {
        throw "JAVA_HOME must point to a JDK 25: $env:JAVA_HOME"
    }
} else {
    $java = (Get-Command java -CommandType Application -ErrorAction Stop | Select-Object -First 1).Source
    $javac = (Get-Command javac -CommandType Application -ErrorAction Stop | Select-Object -First 1).Source
}
$javaVersion = & $java '--version'
if ($LASTEXITCODE -ne 0 -or ($javaVersion -join "`n") -notmatch '^(?:openjdk|java) 25(?:[. +\-]|$)') {
    throw "GUI-001 requires Java 25. $java reported: $javaVersion"
}
$javacVersion = & $javac '--version'
if ($LASTEXITCODE -ne 0 -or ($javacVersion -join "`n") -notmatch '^javac 25(?:[. +\-]|$)') {
    throw "GUI-001 requires javac 25. $javac reported: $javacVersion"
}

$maven = (Get-Command mvn -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1).Source
if (!$maven) {
    $maven = Join-Path $root 'mvnw.cmd'
    if (!(Test-Path -LiteralPath $maven -PathType Leaf)) { throw 'GUI-001 requires mvn or mvnw.cmd' }
}

$output = Join-Path $root ('target/gui-001-' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $output | Out-Null
$dependencyFile = Join-Path $output 'dependencies.txt'
Push-Location $root
try {
    & $maven '-Pbuild' '-Dmaven.buildNumber.skip=true' '-DskipTests' 'compile' 'dependency:build-classpath' "-Dmdep.outputFile=$dependencyFile" '-DincludeScope=test'
    if ($LASTEXITCODE -ne 0) { throw 'GUI-001 Maven project compile/classpath failed' }
} finally {
    Pop-Location
}
if (!(Test-Path -LiteralPath $dependencyFile -PathType Leaf)) { throw 'GUI-001 Maven dependency classpath missing' }
$dependencies = [IO.File]::ReadAllText($dependencyFile).Trim()
if (!$dependencies) { throw 'GUI-001 Maven dependency classpath empty' }

$classpath = (Join-Path $root 'target/classes') + [IO.Path]::PathSeparator + $dependencies
$probeSource = Join-Path $PSScriptRoot 'gui/LegacyGuiSmoke.java'
$compileArgs = Join-Path $output 'javac.args'
$runArgs = Join-Path $output 'java.args'
$runClasspath = $output + [IO.Path]::PathSeparator + $classpath
[IO.File]::WriteAllLines($compileArgs, @('-cp', ('"' + $classpath.Replace('\', '/') + '"'),
    '-d', ('"' + $output.Replace('\', '/') + '"'), ('"' + $probeSource.Replace('\', '/') + '"')))
[IO.File]::WriteAllLines($runArgs, @('--enable-native-access=ALL-UNNAMED',
    '-cp', ('"' + $runClasspath.Replace('\', '/') + '"'), 'LegacyGuiSmoke'))
& $javac "@$compileArgs"
if ($LASTEXITCODE -ne 0) { throw 'GUI-001 smoke source compilation failed' }
& $java "@$runArgs"
if ($LASTEXITCODE -ne 0) { throw 'GUI-001 JavaFX runtime/assertion failed' }
