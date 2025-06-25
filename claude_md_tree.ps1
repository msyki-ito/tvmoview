$root = Get-Location
$output = "$root\PROJECT_STRUCTURE.md"
Remove-Item $output -ErrorAction SilentlyContinue

function WriteCompactStructure {
    param($dir)

    Get-ChildItem -Recurse $dir -Include *.kt,*.java | Where-Object {
        $_.FullName -notmatch '\\(build|\.git|\.gradle|\.idea)\\'
    } | ForEach-Object {
        $relPath = $_.FullName.Substring($root.Length + 1).Replace("\", "/")
        Add-Content $output "${relPath}:"
        $lines = Get-Content $_.FullName
        for ($i = 0; $i -lt $lines.Length; $i++) {
            $line = $lines[$i]
            if ($line -match '^\s*(class|object)\s+(\w+)') {
                Add-Content $output "  C $($matches[2])"
            } elseif ($line -match '^\s*fun\s+(\w+)') {
                Add-Content $output "  F $($matches[1])"
            } elseif ($line -match '^\s*@Composable\s*$') {
                if ($lines[$i + 1] -match '^\s*fun\s+(\w+)') {
                    Add-Content $output "  F $($matches[1]) [@Composable]"
                }
            }
        }
    }
}

WriteCompactStructure $root
Write-Host "`n✅ PROJECT_STRUCTURE.md 出力完了"
