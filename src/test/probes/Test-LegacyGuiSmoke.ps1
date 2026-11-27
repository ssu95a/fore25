[CmdletBinding(DefaultParameterSetName = 'Default')]
param(
    [Parameter(ParameterSetName = 'TableAdapterRowRefreshClose')]
    [switch] $TableAdapterRowRefreshClose,
    [Parameter(ParameterSetName = 'TableAdapterBulkMarkClose')]
    [switch] $TableAdapterBulkMarkClose,
    [Parameter(ParameterSetName = 'TableAdapterBulkMarkErrorDelivery')]
    [switch] $TableAdapterBulkMarkErrorDelivery,
    [Parameter(ParameterSetName = 'TableAdapterSearchMissDelivery')]
    [switch] $TableAdapterSearchMissDelivery,
    [Parameter(ParameterSetName = 'TableAdapterSearchWorkerClose')]
    [switch] $TableAdapterSearchWorkerClose,
    [Parameter(ParameterSetName = 'TableAdapterBulkWorkerClose')]
    [switch] $TableAdapterBulkWorkerClose,
    [Parameter(ParameterSetName = 'CurrencyLovDelayedCompletion')]
    [switch] $CurrencyLovDelayedCompletion,
    [Parameter(ParameterSetName = 'TableAdapterSearchListener')]
    [switch] $TableAdapterSearchListener,
    [Parameter(ParameterSetName = 'TextAreaToolbarOwnership')]
    [switch] $TextAreaToolbarOwnership,
    [Parameter(ParameterSetName = 'TableEditRowIdentity')]
    [switch] $TableEditRowIdentity,
    [Parameter(ParameterSetName = 'TableAdapterBulkClose')]
    [switch] $TableAdapterBulkClose,
    [Parameter(ParameterSetName = 'TableAdapterQueuedClose')]
    [switch] $TableAdapterQueuedClose,
    [Parameter(ParameterSetName = 'TextFieldLovDelayedCompletion')]
    [switch] $TextFieldLovDelayedCompletion,
    [Parameter(ParameterSetName = 'TableAdapterPagingClose')]
    [switch] $TableAdapterPagingClose,
    [Parameter(ParameterSetName = 'TextAreaLovDelayedCompletion')]
    [switch] $TextAreaLovDelayedCompletion,
    [Parameter(ParameterSetName = 'ExternalLovReadOnly')]
    [switch] $ExternalLovReadOnly,
    [Parameter(ParameterSetName = 'WrapButtonOwnership')]
    [switch] $WrapButtonOwnership,
    [Parameter(ParameterSetName = 'TabPaneSkinListeners')]
    [switch] $TabPaneSkinListeners,
    [Parameter(ParameterSetName = 'TextAreaSkinDisposal')]
    [switch] $TextAreaSkinDisposal,
    [Parameter(ParameterSetName = 'ValidatorSubchanges')]
    [switch] $ValidatorSubchanges,
    [Parameter(ParameterSetName = 'TableAdapterClose')]
    [switch] $TableAdapterClose,
    [Parameter(ParameterSetName = 'TextAreaLovLifecycle')]
    [switch] $TextAreaLovLifecycle,
    [Parameter(ParameterSetName = 'TextFieldInnerButtonOwnership')]
    [switch] $TextFieldInnerButtonOwnership,
    [Parameter(ParameterSetName = 'ButtonTargetAssociation')]
    [switch] $ButtonTargetAssociation,
    [Parameter(ParameterSetName = 'FEActionDelegation')]
    [switch] $FEActionDelegation,
    [Parameter(ParameterSetName = 'TextAreaContextMenuLifecycle')]
    [switch] $TextAreaContextMenuLifecycle,
    [Parameter(ParameterSetName = 'TextFieldContextMenuLifecycle')]
    [switch] $TextFieldContextMenuLifecycle,
    [Parameter(ParameterSetName = 'TextFieldSkinDisposal')]
    [switch] $TextFieldSkinDisposal,
    [Parameter(ParameterSetName = 'LovBindingInterleaving')]
    [switch] $LovBindingInterleaving,
    [Parameter(ParameterSetName = 'BoundLovSetter')]
    [switch] $BoundLovSetter,
    [Parameter(ParameterSetName = 'WrapButtonDeferred')]
    [switch] $WrapButtonDeferred,
    [Parameter(ParameterSetName = 'Editors')]
    [switch] $Editors,
    [Parameter(ParameterSetName = 'TableEditing')]
    [switch] $TableEditing,
    [Parameter(ParameterSetName = 'TableCancel')]
    [switch] $TableCancel,
    [Parameter(ParameterSetName = 'EditorReentry')]
    [switch] $EditorReentry,
    [Parameter(ParameterSetName = 'DataBinding')]
    [switch] $DataBinding,
    [Parameter(ParameterSetName = 'FormLifecycle')]
    [switch] $FormLifecycle,
    [Parameter(ParameterSetName = 'FormCancel')]
    [switch] $FormCancel,
    [Parameter(ParameterSetName = 'SceneKeyLookup')]
    [switch] $SceneKeyLookup,
    [Parameter(ParameterSetName = 'LovListenerLifecycle')]
    [switch] $LovListenerLifecycle,
    [Parameter(ParameterSetName = 'LovDefaultValidation')]
    [switch] $LovDefaultValidation,
    [Parameter(ParameterSetName = 'LovIntegration')]
    [switch] $LovIntegration,
    [Parameter(ParameterSetName = 'SingletonCloseVeto')]
    [switch] $SingletonCloseVeto,
    [Parameter(ParameterSetName = 'SingletonLifecycleMatrix')]
    [switch] $SingletonLifecycleMatrix,
    [Parameter(ParameterSetName = 'ValidatorScopedUnbind')]
    [switch] $ValidatorScopedUnbind,
    [Parameter(ParameterSetName = 'ValidatorMultiControl')]
    [switch] $ValidatorMultiControl,
    [Parameter(ParameterSetName = 'TextFields')]
    [switch] $TextFields,
    [Parameter(ParameterSetName = 'ValueFields')]
    [switch] $ValueFields,
    [Parameter(ParameterSetName = 'ValueLifecycle')]
    [switch] $ValueLifecycle,
    [Parameter(ParameterSetName = 'MoneyDecimal')]
    [switch] $MoneyDecimal,
    [Parameter(ParameterSetName = 'DatePickerBinding')]
    [switch] $DatePickerBinding,
    [Parameter(ParameterSetName = 'NodeTraversal')]
    [switch] $NodeTraversal,
    [Parameter(ParameterSetName = 'RadioReadOnly')]
    [switch] $RadioReadOnly,
    [Parameter(ParameterSetName = 'ComboReadOnly')]
    [switch] $ComboReadOnly,
    [Parameter(ParameterSetName = 'CheckBoxReadOnly')]
    [switch] $CheckBoxReadOnly,
    [Parameter(ParameterSetName = 'ValueChangedFlag')]
    [switch] $ValueChangedFlag,
    [Parameter(ParameterSetName = 'ControlsFollowup')]
    [switch] $ControlsFollowup,
    [Parameter(ParameterSetName = 'ChoiceAdapter')]
    [switch] $ChoiceAdapter,
    [Parameter(ParameterSetName = 'BinderChoiceNull')]
    [switch] $BinderChoiceNull,
    [Parameter(ParameterSetName = 'ChoiceNullLookup')]
    [switch] $ChoiceNullLookup,
    [Parameter(ParameterSetName = 'ChoiceNonNullSet')]
    [switch] $ChoiceNonNullSet,
    [Parameter(ParameterSetName = 'RadioReadOnly')]
    [Parameter(ParameterSetName = 'ComboReadOnly')]
    [Parameter(ParameterSetName = 'CheckBoxReadOnly')]
    [switch] $RobotInput,
    [ValidateNotNullOrEmpty()]
    [string] $SettingsFile
)

$probeId = 'GUI-001'
$probeClass = 'LegacyGuiSmoke'
$probeArgs = @()
if ($TextAreaToolbarOwnership) {
    $probeId = 'CTRL-066'
    $probeClass = 'TextAreaToolbarOwnershipSmoke'
} elseif ($TableEditRowIdentity) {
    $probeId = 'CTRL-075'
    $probeClass = 'TableEditRowIdentitySmoke'
} elseif ($TableAdapterBulkMarkErrorDelivery) {
    $probeId = 'CTRL-104'
    $probeClass = 'TableAdapterBulkMarkErrorDeliverySmoke'
} elseif ($TableAdapterBulkMarkClose) {
    $probeId = 'CTRL-105'
    $probeClass = 'TableAdapterBulkMarkErrorDeliverySmoke'
    $probeArgs = @('close')
} elseif ($TableAdapterRowRefreshClose) {
    $probeId = 'CTRL-106'
    $probeClass = 'TableAdapterRowRefreshCloseSmoke'
} elseif ($TableAdapterSearchMissDelivery) {
    $probeId = 'CTRL-098'
    $probeClass = 'TableAdapterSearchMissDeliverySmoke'
} elseif ($TableAdapterSearchWorkerClose) {
    $probeId = 'CTRL-093'
    $probeClass = 'TableAdapterSearchWorkerCloseSmoke'
} elseif ($TableAdapterSearchListener) {
    $probeId = 'CTRL-081'
    $probeClass = 'TableAdapterSearchListenerSmoke'
} elseif ($TableAdapterBulkWorkerClose) {
    $probeId = 'CTRL-092'
    $probeClass = 'TableAdapterBulkWorkerCloseSmoke'
} elseif ($TableAdapterBulkClose) {
    $probeId = 'CTRL-076'
    $probeClass = 'TableAdapterBulkCloseSmoke'
} elseif ($TableAdapterQueuedClose) {
    $probeId = 'CTRL-077'
    $probeClass = 'TableAdapterQueuedCloseSmoke'
} elseif ($CurrencyLovDelayedCompletion) {
    $probeId = 'CTRL-091'
    $probeClass = 'CurrencyLovDelayedCompletionSmoke'
} elseif ($TextFieldLovDelayedCompletion) {
    $probeId = 'CTRL-078'
    $probeClass = 'TextFieldLovDelayedCompletionSmoke'
} elseif ($TableAdapterPagingClose) {
    $probeId = 'CTRL-065'
    $probeClass = 'TableAdapterPagingCloseSmoke'
} elseif ($TextAreaLovDelayedCompletion) {
    $probeId = 'CTRL-062'
    $probeClass = 'TextAreaLovDelayedCompletionSmoke'
} elseif ($ExternalLovReadOnly) {
    $probeId = 'CTRL-061'
    $probeClass = 'ExternalLovReadOnlySmoke'
} elseif ($WrapButtonOwnership) {
    $probeId = 'CTRL-055'
    $probeClass = 'WrapButtonOwnershipSmoke'
} elseif ($TabPaneSkinListeners) {
    $probeId = 'CTRL-054'
    $probeClass = 'TabPaneSkinListenersSmoke'
} elseif ($TextAreaSkinDisposal) {
    $probeId = 'CTRL-053'
    $probeClass = 'TextAreaSkinDisposalSmoke'
} elseif ($ValidatorSubchanges) {
    $probeId = 'CTRL-052'
    $probeClass = 'ValidatorSubchangesSmoke'
} elseif ($TableAdapterClose) {
    $probeId = 'CTRL-051'
    $probeClass = 'TableAdapterCloseSmoke'
} elseif ($TextAreaLovLifecycle) {
    $probeId = 'CTRL-050'
    $probeClass = 'TextAreaLovLifecycleSmoke'
} elseif ($TextFieldInnerButtonOwnership) {
    $probeId = 'ARCH-003'
    $probeClass = 'TextFieldInnerButtonOwnershipSmoke'
} elseif ($ButtonTargetAssociation) {
    $probeId = 'ARCH-002'
    $probeClass = 'ru.inversion.fx.form.controls.ButtonTargetAssociationSmoke'
} elseif ($FEActionDelegation) {
    $probeId = 'ARCH-001'
    $probeClass = 'FEActionDelegationSmoke'
} elseif ($TextAreaContextMenuLifecycle) {
    $probeId = 'PERF-015'
    $probeClass = 'TextAreaContextMenuLifecycleSmoke'
} elseif ($TextFieldContextMenuLifecycle) {
    $probeId = 'PERF-014'
    $probeClass = 'TextFieldContextMenuLifecycleSmoke'
} elseif ($TextFieldSkinDisposal) {
    $probeId = 'CTRL-049'
    $probeClass = 'TextFieldSkinDisposalSmoke'
} elseif ($LovBindingInterleaving) {
    $probeId = 'CTRL-045'
    $probeClass = 'LovBindingInterleavingSmoke'
} elseif ($BoundLovSetter) {
    $probeId = 'CTRL-044'
    $probeClass = 'BoundLovSetterAtomicitySmoke'
} elseif ($WrapButtonDeferred) {
    $probeId = 'CTRL-043'
    $probeClass = 'WrapButtonDeferredBindingSmoke'
} elseif ($Editors) {
    $probeId = 'GUI-004'
    $probeClass = 'LegacyEditorsSmoke'
} elseif ($TableEditing) {
    $probeId = 'GUI-005'
    $probeClass = 'LegacyTableEditingSmoke'
} elseif ($TableCancel) {
    $probeId = 'GUI-006'
    $probeClass = 'LegacyTableCancelSmoke'
} elseif ($EditorReentry) {
    $probeId = 'GUI-007'
    $probeClass = 'LegacyTableCancelSmoke'
    $probeArgs = @('reentry')
} elseif ($DataBinding) {
    $probeId = 'GUI-009'
    $probeClass = 'LegacyDataBindingSmoke'
} elseif ($FormLifecycle) {
    $probeId = 'GUI-010'
    $probeClass = 'ru.inversion.fx.form.LegacyFormLifecycleSmoke'
} elseif ($FormCancel) {
    $probeId = 'CTRL-022'
    $probeClass = 'ru.inversion.fx.form.LegacyFormCancelSmoke'
} elseif ($SceneKeyLookup) {
    $probeId = 'CTRL-024'
    $probeClass = 'SceneKeyLookupSmoke'
} elseif ($LovListenerLifecycle) {
    $probeId = 'CTRL-026'
    $probeClass = 'LovListenerLifecycleSmoke'
} elseif ($LovDefaultValidation) {
    $probeId = 'CTRL-027'
    $probeClass = 'LovDefaultValidationSmoke'
} elseif ($LovIntegration) {
    $probeId = 'CTRL-031'
    $probeClass = 'LovIntegrationSmoke'
} elseif ($SingletonCloseVeto) {
    $probeId = 'CTRL-032'
    $probeClass = 'SingletonCloseVetoSmoke'
} elseif ($SingletonLifecycleMatrix) {
    $probeId = 'CTRL-035'
    $probeClass = 'SingletonCloseVetoSmoke'
    $probeArgs = @('matrix')
} elseif ($ValidatorScopedUnbind) {
    $probeId = 'CTRL-033'
    $probeClass = 'ValidatorScopedUnbindSmoke'
} elseif ($ValidatorMultiControl) {
    $probeId = 'CTRL-036'
    $probeClass = 'ValidatorMultiControlSmoke'
} elseif ($TextFields) {
    $probeId = 'GUI-012'
    $probeClass = 'LegacyTextFieldsSmoke'
} elseif ($ValueFields) {
    $probeId = 'VF-001'
    $probeClass = 'LegacyValueFieldsSmoke'
} elseif ($ValueLifecycle) {
    $probeId = 'VF-002'
    $probeClass = 'LegacyValueLifecycleSmoke'
} elseif ($MoneyDecimal) {
    $probeId = 'NUM-001'
    $probeClass = 'LegacyMoneyDecimalSmoke'
} elseif ($DatePickerBinding) {
    $probeId = 'CTRL-002'
    $probeClass = 'DatePickerBindingSmoke'
} elseif ($NodeTraversal) {
    $probeId = 'CTRL-003'
    $probeClass = 'NodeTraversalSmoke'
} elseif ($RadioReadOnly) {
    $probeId = 'CTRL-006'
    $probeClass = 'RadioReadOnlySmoke'
    if ($RobotInput) {
        $probeArgs = @('robot')
    }
} elseif ($ComboReadOnly) {
    $probeId = 'CTRL-007'
    $probeClass = 'ComboReadOnlySmoke'
    if ($RobotInput) {
        $probeArgs = @('robot')
    }
} elseif ($CheckBoxReadOnly) {
    $probeId = 'CTRL-009'
    $probeClass = 'CheckBoxReadOnlySmoke'
    if ($RobotInput) {
        $probeArgs = @('robot')
    }
} elseif ($ValueChangedFlag) {
    $probeId = 'CTRL-013'
    $probeClass = 'ValueChangedFlagSmoke'
} elseif ($ControlsFollowup) {
    $probeId = 'CTRL-CONTROLS-FOLLOWUP'
    $probeClass = 'ControlsFollowupSmoke'
} elseif ($ChoiceAdapter) {
    $probeId = 'CTRL-015'
    $probeClass = 'ChoiceAdapterSmoke'
} elseif ($BinderChoiceNull) {
    $probeId = 'CTRL-017'
    $probeClass = 'BinderChoiceNullSmoke'
} elseif ($ChoiceNullLookup) {
    $probeId = 'CTRL-018'
    $probeClass = 'ChoiceNullLookupSmoke'
} elseif ($ChoiceNonNullSet) {
    $probeId = 'CTRL-019'
    $probeClass = 'ChoiceNonNullSetSmoke'
}
$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
$mavenSettings = @()
if ($PSBoundParameters.ContainsKey('SettingsFile')) {
    if (!(Test-Path -LiteralPath $SettingsFile -PathType Leaf)) {
        throw "$probeId settings file does not exist: $SettingsFile"
    }
    $mavenSettings = @('-s', (Resolve-Path -LiteralPath $SettingsFile).ProviderPath)
}
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
    & $maven @mavenSettings '-Pbuild' '-Dmaven.buildNumber.skip=true' '-DskipTests' 'compile' 'dependency:build-classpath' "-Dmdep.outputFile=$dependencyFile" '-DincludeScope=test'
    if ($LASTEXITCODE -ne 0) { throw "$probeId Maven project compile/classpath failed" }
} finally {
    Pop-Location
}
if (!(Test-Path -LiteralPath $dependencyFile -PathType Leaf)) { throw "$probeId Maven dependency classpath missing" }
$dependencies = [IO.File]::ReadAllText($dependencyFile).Trim()
if (!$dependencies) { throw "$probeId Maven dependency classpath empty" }

$classpath = (Join-Path $root 'target/classes') + [IO.Path]::PathSeparator + $dependencies
$sourceName = ($probeClass -split '\.')[-1]
$probeSource = Join-Path $PSScriptRoot "gui/$sourceName.java"
$compileArgs = Join-Path $output 'javac.args'
$runArgs = Join-Path $output 'java.args'
$runClasspath = $output + [IO.Path]::PathSeparator + $classpath
[IO.File]::WriteAllLines($compileArgs, @('-cp', ('"' + $classpath.Replace('\', '/') + '"'),
    '-d', ('"' + $output.Replace('\', '/') + '"'), ('"' + $probeSource.Replace('\', '/') + '"')))
[IO.File]::WriteAllLines($runArgs, @('--enable-native-access=ALL-UNNAMED',
    '-cp', ('"' + $runClasspath.Replace('\', '/') + '"'), $probeClass) + $probeArgs)
& $javac "@$compileArgs"
if ($LASTEXITCODE -ne 0) { throw "$probeId smoke source compilation failed" }
& $java "@$runArgs"
if ($LASTEXITCODE -ne 0) { throw "$probeId JavaFX runtime/assertion failed" }
