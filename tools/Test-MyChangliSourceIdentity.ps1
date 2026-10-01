#requires -Version 5.1
param([ValidateSet('android','backend','website')][string]$Component='android',[string]$Repository)
$ErrorActionPreference='Stop'
$identity=Get-Content -Raw -Encoding UTF8 -LiteralPath (Join-Path $PSScriptRoot 'source-identity.json') | ConvertFrom-Json
$expected=$identity.repositories.$Component
if([string]::IsNullOrWhiteSpace($Repository)){$Repository=$expected.path}
$resolved=(Resolve-Path -LiteralPath $Repository).Path
if($resolved.TrimEnd('\') -ne ([IO.Path]::GetFullPath($expected.path)).TrimEnd('\')){throw "Wrong development directory: $resolved"}
$remote=(& git -C $resolved remote get-url origin)
if($LASTEXITCODE -ne 0 -or $remote.TrimEnd('/') -ne $expected.remote){throw 'Repository identity mismatch'}
& git -C $resolved merge-base --is-ancestor $expected.baseline HEAD
if($LASTEXITCODE -ne 0){throw 'Formal baseline is not an ancestor; stop without switching or resetting'}
if($Component -eq 'android'){
 $gradle=Get-Content -Raw -LiteralPath (Join-Path $resolved 'app\build.gradle.kts')
 if($gradle -notmatch 'applicationId\s*=\s*"com\.mychangli\.app"'){throw 'Product applicationId mismatch'}
}
$head=& git -C $resolved rev-parse HEAD
$branch=& git -C $resolved branch --show-current
$changes=@(& git -C $resolved status --porcelain=v1 --untracked-files=all)
if($LASTEXITCODE -ne 0){throw 'Cannot inspect workspace differences'}
Write-Host "Identity PASS: $Component $resolved; branch=$branch; HEAD=$head; change entries=$($changes.Count)"
if($changes.Count){Write-Host 'Existing differences are preserved; inspect git diff and untracked files before formal release.'}
