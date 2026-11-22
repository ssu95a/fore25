[CmdletBinding(DefaultParameterSetName = 'Default')]
param(
    [Parameter(ParameterSetName = 'Editors')]
    [switch] $Editors,
    [Parameter(ParameterSetName = 'TableEditing')]
    [switch] $TableEditing
)

$probeId = 'GUI-001'
$probeClass = 'LegacyGuiSmoke'
if ($Editors) {
    $probeId = 'GUI-004'
    $probeClass = 'LegacyEditorsSmoke'
} elseif ($TableEditing) {
    $probeId = 'GUI-005'
    $probeClass = 'LegacyTableEditingSmoke'
}
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
    throw "$probeId requires Java 25. $java reported: $javaVersion"
}
$javacVersion = & $javac '--version'
if ($LASTEXITCODE -ne 0 -or ($javacVersion -join "`n") -notmatch '^javac 25(?:[. +\-]|$)') {
    throw "$probeId requires javac 25. $javac reported: $javacVersion"
}

$maven = (Get-Command mvn -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1).Source
if (!$maven) {
    $maven = Join-Path $root 'mvnw.cmd'
    if (!(Test-Path -LiteralPath $maven -PathType Leaf)) { throw "$probeId requires mvn or mvnw.cmd" }
}

$output = Join-Path $root ("target/$($probeId.ToLowerInvariant())-" + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $output | Out-Null
$dependencyFile = Join-Path $output 'dependencies.txt'
Push-Location $root
try {
    & $maven '-Pbuild' '-Dmaven.buildNumber.skip=true' '-DskipTests' 'compile' 'dependency:build-classpath' "-Dmdep.outputFile=$dependencyFile" '-DincludeScope=test'
    if ($LASTEXITCODE -ne 0) { throw "$probeId Maven project compile/classpath failed" }
} finally {
    Pop-Location
}
if (!(Test-Path -LiteralPath $dependencyFile -PathType Leaf)) { throw "$probeId Maven dependency classpath missing" }
$dependencies = [IO.File]::ReadAllText($dependencyFile).Trim()
if (!$dependencies) { throw "$probeId Maven dependency classpath empty" }

$classpath = (Join-Path $root 'target/classes') + [IO.Path]::PathSeparator + $dependencies
$probeSource = Join-Path $PSScriptRoot "gui/$probeClass.java"
$compileArgs = Join-Path $output 'javac.args'
$runArgs = Join-Path $output 'java.args'
$runClasspath = $output + [IO.Path]::PathSeparator + $classpath
[IO.File]::WriteAllLines($compileArgs, @('-cp', ('"' + $classpath.Replace('\', '/') + '"'),
    '-d', ('"' + $output.Replace('\', '/') + '"'), ('"' + $probeSource.Replace('\', '/') + '"')))
[IO.File]::WriteAllLines($runArgs, @('--enable-native-access=ALL-UNNAMED',
    '-cp', ('"' + $runClasspath.Replace('\', '/') + '"'), $probeClass))
& $javac "@$compileArgs"
if ($LASTEXITCODE -ne 0) { throw "$probeId smoke source compilation failed" }
& $java "@$runArgs"
if ($LASTEXITCODE -ne 0) { throw "$probeId JavaFX runtime/assertion failed" }
