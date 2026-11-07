param([switch] $ValidateOnly)

$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
$probeSource = Join-Path $PSScriptRoot 'd3/D3SelectionSyncProbe.java'
$outputDir = Join-Path $projectRoot 'target/d3-selection-probe'
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null

function Find-JdkTool([string] $name) {
    if ($env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME "bin/$name.exe"
        if (Test-Path -LiteralPath $candidate) { return $candidate }
    }
    return (Get-Command $name -ErrorAction Stop).Source
}

function Write-Utf8([string] $path, [string] $value) {
    [IO.File]::WriteAllText($path, $value, [Text.UTF8Encoding]::new($false))
}

function Argfile-Quote([string] $value) {
    return '"' + $value.Replace('\', '/').Replace('"', '\"') + '"'
}

$java = Find-JdkTool 'java'
# Синтаксическая проверка настоящего source через JDK parser, без stubs и dependencies.
# Она не заменяет последующую полную компиляцию с корпоративным classpath.
$syntaxChecker = Join-Path $outputDir 'D3SyntaxCheck.java'
Write-Utf8 $syntaxChecker @'
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import javax.tools.*;
import com.sun.source.util.JavacTask;
class D3SyntaxCheck {
    public static void main(String[] args) throws Exception {
        if (Runtime.version().feature() != 25) throw new IllegalStateException("Java 25 required");
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("Full JDK required");
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (var files = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            var source = files.getJavaFileObjectsFromPaths(List.of(Path.of(args[0])));
            var task = (JavacTask) compiler.getTask(null, files, diagnostics,
                    List.of("--release", "25", "-proc:none"), null, source);
            task.parse().forEach(tree -> {});
            boolean errors = false;
            for (var diagnostic : diagnostics.getDiagnostics()) {
                if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                    errors = true;
                    System.err.println(diagnostic);
                }
            }
            if (errors) System.exit(2);
        }
        System.out.println("JAVA SYNTAX=PASS (parse only; corporate type-check/runtime not performed)");
    }
}
'@
& $java $syntaxChecker $probeSource
if ($LASTEXITCODE -ne 0) { throw 'Java probe syntax validation failed' }
if ($ValidateOnly) {
    Write-Output 'VALIDATE ONLY: no Maven build, no JavaFX startup, no D3 verdict.'
    return
}

$javac = Find-JdkTool 'javac'
Write-Output 'MODE=CORPORATE_EXECUTION: Maven compile -> probe compile -> real JavaFX toolkit/binding -> P1-P5'
$maven = Get-Command mvn -ErrorAction SilentlyContinue
if ($maven) { $mavenCommand = $maven.Source }
else {
    $mavenCommand = Join-Path $projectRoot 'mvnw.cmd'
    if (!(Test-Path -LiteralPath $mavenCommand -PathType Leaf)) {
        throw 'Neither mvn on PATH nor project mvnw.cmd was found; D3 UNKNOWN'
    }
}
$dependencyFile = Join-Path $outputDir ('dependencies-' + [Guid]::NewGuid().ToString('N') + '.txt')
Push-Location $projectRoot
try {
    # Сборка и classpath используют только Maven dependencies текущего проекта.
    & $mavenCommand '-Pbuild' '-Dmaven.buildNumber.skip=true' '-DskipTests' 'compile' 'dependency:build-classpath' "-Dmdep.outputFile=$dependencyFile" '-DincludeScope=test'
    if ($LASTEXITCODE -ne 0) { throw 'Corporate Maven compile/classpath failed; D3 UNKNOWN' }
    $dependencies = (Get-Content -LiteralPath $dependencyFile -Raw -Encoding UTF8).Trim()
    if (!$dependencies) { throw 'Maven produced an empty dependency classpath' }
    $projectClasses = Join-Path $projectRoot 'target/classes'
    if (!(Test-Path -LiteralPath (Join-Path $projectClasses 'ru/inversion/dataset/fx/DSFXAdapter.class'))) {
        throw 'Compiled production DSFXAdapter not found'
    }
    $probeClasses = Join-Path $outputDir ('classes-' + [Guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $probeClasses | Out-Null
    $classpath = $projectClasses + [IO.Path]::PathSeparator + $dependencies
    $compileArgs = Join-Path $outputDir 'javac.args'
    Write-Utf8 $compileArgs ((@('--release', '25', '-encoding', 'UTF-8', '-proc:none', '-cp', (Argfile-Quote $classpath), '-d', (Argfile-Quote $probeClasses), (Argfile-Quote $probeSource))) -join "`n")
    & $javac "@$compileArgs"
    if ($LASTEXITCODE -ne 0) { throw 'Probe type-check/compilation failed; D3 UNKNOWN' }
    $runArgs = Join-Path $outputDir 'java.args'
    Write-Utf8 $runArgs ((@('--enable-native-access=ALL-UNNAMED', '-cp', (Argfile-Quote ($probeClasses + [IO.Path]::PathSeparator + $classpath)), 'ru.inversion.probes.d3.D3SelectionSyncProbe')) -join "`n")
    & $java "@$runArgs"
    if ($LASTEXITCODE -ne 0) { throw 'PROBE FAILED: D3 UNKNOWN (see technical cause above)' }
} finally {
    Pop-Location
}
