#requires -Version 5.1
[CmdletBinding()]
param(
 [ValidateSet('android','backend','website')][string]$Component='android',
 [string]$Repository,
 [ValidateSet('Development','Validation')][string]$Mode='Development',
 [string]$LocalRegistry
)
$ErrorActionPreference='Stop'
Set-StrictMode -Version Latest
function Get-CanonicalPath([string]$Path) {
 $item=Get-Item -LiteralPath $Path -ErrorAction Stop
 if(-not $item.PSIsContainer){throw 'Repository must be a directory'}
 for($p=$item;$null -ne $p;$p=$p.Parent){
  if($p.Attributes -band [IO.FileAttributes]::ReparsePoint){throw 'Use a real path, not a junction or symlink'}
 }
 return $item.FullName.TrimEnd([char[]]'\/')
}
function Invoke-ReadGit([string[]]$Arguments) {
 $output=@(& git --no-optional-locks -C $script:resolved @Arguments 2>$null)
 if($LASTEXITCODE -ne 0){throw "Git inspection failed: $($Arguments[0]); missing information or insufficient history"}
 return ($output -join "`n").Trim()
}
function Get-RepositoryId([string]$Remote) {
 $pattern='^(?:https://github\.com/|ssh://git@github\.com/|git@github\.com:)([A-Za-z0-9_.-]+)/([A-Za-z0-9_.-]+?)(?:\.git)?/?$'
 if($Remote -notmatch $pattern){throw 'Unsupported origin URL; expected a canonical GitHub HTTPS or SSH address'}
 return ('github.com/'+$Matches[1]+'/'+$Matches[2]).ToLowerInvariant()
}
$identity=Get-Content -Raw -Encoding UTF8 -LiteralPath (Join-Path $PSScriptRoot 'source-identity.json') | ConvertFrom-Json
if($identity.schemaVersion -ne 2 -or $identity.applicationId -ne 'com.mychangli.app'){throw 'Invalid portable identity manifest'}
$expected=$identity.repositories.$Component
if($Mode -eq 'Development') {
 if(-not $LocalRegistry){$LocalRegistry=Join-Path $PSScriptRoot '../../../04_Tools/local-source-entrypoints.json'}
 if(-not (Test-Path -LiteralPath $LocalRegistry -PathType Leaf)){throw 'Local entry registry missing. Register this machine, or explicitly use Validation for a controlled clone.'}
 $registry=Get-Content -Raw -Encoding UTF8 -LiteralPath $LocalRegistry | ConvertFrom-Json
 if($registry.schemaVersion -ne 1){throw 'Invalid local entry registry'}
 $registered=$registry.repositories.$Component
 if(-not [IO.Path]::IsPathRooted($registered)){$registered=Join-Path (Split-Path -Parent (Resolve-Path -LiteralPath $LocalRegistry).Path) $registered}
 if(-not $Repository){$Repository=$registered}
 $resolved=Get-CanonicalPath $Repository
 if($resolved -ne (Get-CanonicalPath $registered)){throw 'Unregistered development directory; no fallback is allowed'}
} else {
 if([string]::IsNullOrWhiteSpace($Repository)){throw 'Validation requires an explicit Repository directory'}
 $resolved=Get-CanonicalPath $Repository
}
foreach($segment in ($resolved -split '[\\/]')){
 if($segment -in $identity.forbiddenDirectoryNames){throw 'Historical source entry is forbidden, including in Validation mode'}
}
$root=Invoke-ReadGit @('rev-parse','--show-toplevel')
if((Get-CanonicalPath $root) -ne $resolved){throw 'Repository must be the exact Git worktree root'}
$remote=Invoke-ReadGit @('remote','get-url','origin')
if((Get-RepositoryId $remote) -ne $expected.repositoryId){throw 'Repository owner/name identity mismatch'}
$baseline=Invoke-ReadGit @('rev-parse','--verify',($expected.baseline+'^{commit}'))
if($baseline -ne $expected.baseline){throw 'Baseline resolution mismatch'}
& git --no-optional-locks -C $resolved merge-base --is-ancestor $baseline HEAD 2>$null
if($LASTEXITCODE -ne 0){throw 'Formal baseline is not a proven ancestor (old line or shallow history). Obtain full authorized history; never reset or switch automatically.'}
foreach($marker in $expected.markers){
 $file=Join-Path $resolved $marker.path
 if(-not (Test-Path -LiteralPath $file -PathType Leaf)){throw "Product marker missing: $($marker.path)"}
 if((Get-Content -LiteralPath $file -Raw -Encoding UTF8) -notmatch $marker.pattern){throw "Product identity mismatch: $($marker.path)"}
}
$head=Invoke-ReadGit @('rev-parse','HEAD')
$branch=Invoke-ReadGit @('branch','--show-current')
$status=Invoke-ReadGit @('status','--porcelain=v1','--untracked-files=all')
$count=if($status){@($status -split "`n").Count}else{0}
Write-Host "Identity PASS: component=$Component; mode=$Mode; root=$resolved; branch=$branch; HEAD=$head; change entries=$count"
if($count){Write-Host 'Differences preserved. Review tracked and untracked changes before any release.'}
