param(
    [string]$DdlPath,
    [string]$OutputPath
)

$ErrorActionPreference = 'Stop'
$scriptDirectory = if ($PSScriptRoot) { $PSScriptRoot } else { Join-Path (Get-Location) 'database/docs/erd' }
if (-not $DdlPath) { $DdlPath = Join-Path $scriptDirectory '../../mysql/ddl/firstday_mysql_current.sql' }
if (-not $OutputPath) { $OutputPath = Join-Path $scriptDirectory 'firstday-mysql-erd.svg' }
$ddl = Get-Content -LiteralPath $DdlPath -Raw -Encoding UTF8

function Escape-Xml([string]$value) {
    if ($null -eq $value) { return '' }
    return [System.Security.SecurityElement]::Escape($value)
}

$domainOrder = @(
    '회원·정책', '기업·채용공고', '지원·전형', '이력서·자기소개서',
    '기업 콘텐츠', '고객센터·신고', '관리자 운영'
)

$domainColors = @{
    '회원·정책' = @('#EAF3FF', '#2563EB')
    '기업·채용공고' = @('#ECFDF5', '#059669')
    '지원·전형' = @('#FFF7ED', '#EA580C')
    '이력서·자기소개서' = @('#F5F3FF', '#7C3AED')
    '기업 콘텐츠' = @('#FFF1F2', '#E11D48')
    '고객센터·신고' = @('#ECFEFF', '#0891B2')
    '관리자 운영' = @('#F8FAFC', '#475569')
}

$domainTables = @{
    '회원·정책' = @('users','personal_profiles','user_desired_jobs','policies','user_policy_consents')
    '기업·채용공고' = @('companies','job_categories','skills','job_postings','job_posting_skills','saved_jobs','saved_companies')
    '지원·전형' = @('applications','application_status_history','application_memos')
    '이력서·자기소개서' = @('resumes','resume_educations','resume_careers','resume_projects','resume_skills','cover_letters','cover_letter_items','cover_letter_ai_reviews')
    '기업 콘텐츠' = @('company_reviews','interview_reviews','salary_records','review_reactions')
    '고객센터·신고' = @('faq_categories','faqs','inquiry_categories','inquiries','inquiry_attachments','reports')
    '관리자 운영' = @('banners','notices','site_settings','site_versions')
}

$tableDomain = @{}
foreach ($domain in $domainOrder) {
    foreach ($tableName in $domainTables[$domain]) { $tableDomain[$tableName] = $domain }
}

$tables = [ordered]@{}
$tablePattern = '(?ms)CREATE TABLE\s+`?(?<name>[A-Za-z0-9_]+)`?\s*\((?<body>.*?)\)\s*ENGINE='
foreach ($match in [regex]::Matches($ddl, $tablePattern)) {
    $name = $match.Groups['name'].Value
    $body = $match.Groups['body'].Value
    $columns = [System.Collections.Generic.List[object]]::new()
    foreach ($line in ($body -split "`r?`n")) {
        if ($line -match '^\s*`?(?<name>[A-Za-z][A-Za-z0-9_]*)`?\s+(?<type>[A-Za-z]+(?:\([^\)]*\))?(?:\s+UNSIGNED)?)' -and
            $Matches.name -notin @('PRIMARY','UNIQUE','KEY','CONSTRAINT','CHECK','FULLTEXT','FOREIGN')) {
            $columns.Add([pscustomobject]@{ Name = $Matches.name; Type = $Matches.type })
        }
    }

    $primaryKeys = @()
    if ($body -match 'PRIMARY KEY\s*\((?<keys>[^\)]+)\)') {
        $primaryKeys = ($Matches.keys -split ',') | ForEach-Object { $_.Trim().Trim('`') }
    }

    $foreignKeys = [System.Collections.Generic.List[object]]::new()
    $fkPattern = 'FOREIGN KEY\s*\(`?(?<column>[A-Za-z0-9_]+)`?\)\s*REFERENCES\s*`?(?<target>[A-Za-z0-9_]+)`?\s*\(`?(?<targetColumn>[A-Za-z0-9_]+)`?\)'
    foreach ($fk in [regex]::Matches($body, $fkPattern)) {
        $foreignKeys.Add([pscustomobject]@{
            Column = $fk.Groups['column'].Value
            Target = $fk.Groups['target'].Value
            TargetColumn = $fk.Groups['targetColumn'].Value
        })
    }

    $tables[$name] = [pscustomobject]@{
        Name = $name
        Columns = $columns
        PrimaryKeys = @($primaryKeys)
        ForeignKeys = $foreignKeys
    }
}

$cardWidth = 320
$headerHeight = 42
$rowHeight = 23
$cardGapX = 38
$cardGapY = 34
$domainPadding = 26
$domainTitleHeight = 52
$outerPadding = 36
$domainColumns = 3

$positions = @{}
$domainLayouts = [System.Collections.Generic.List[object]]::new()
$currentY = $outerPadding + 80

foreach ($domain in $domainOrder) {
    $names = @($domainTables[$domain] | Where-Object { $tables.Contains($_) })
    $rows = [math]::Ceiling($names.Count / $domainColumns)
    $rowHeights = @()
    for ($row = 0; $row -lt $rows; $row++) {
        $maxHeight = 0
        for ($col = 0; $col -lt $domainColumns; $col++) {
            $index = $row * $domainColumns + $col
            if ($index -ge $names.Count) { continue }
            $table = $tables[$names[$index]]
            $height = $headerHeight + (($table.Columns.Count + 1) * $rowHeight)
            if ($height -gt $maxHeight) { $maxHeight = $height }
        }
        $rowHeights += $maxHeight
    }

    $domainWidth = ($domainColumns * $cardWidth) + (($domainColumns - 1) * $cardGapX) + (2 * $domainPadding)
    $domainHeight = $domainTitleHeight + (2 * $domainPadding) + (($rowHeights | Measure-Object -Sum).Sum) + ([math]::Max(0, $rows - 1) * $cardGapY)
    $domainLayouts.Add([pscustomobject]@{ Name=$domain; X=$outerPadding; Y=$currentY; Width=$domainWidth; Height=$domainHeight })

    $rowY = $currentY + $domainTitleHeight + $domainPadding
    for ($row = 0; $row -lt $rows; $row++) {
        for ($col = 0; $col -lt $domainColumns; $col++) {
            $index = $row * $domainColumns + $col
            if ($index -ge $names.Count) { continue }
            $table = $tables[$names[$index]]
            $height = $headerHeight + (($table.Columns.Count + 1) * $rowHeight)
            $x = $outerPadding + $domainPadding + ($col * ($cardWidth + $cardGapX))
            $positions[$table.Name] = [pscustomobject]@{ X=$x; Y=$rowY; Width=$cardWidth; Height=$height }
        }
        $rowY += $rowHeights[$row] + $cardGapY
    }
    $currentY += $domainHeight + 44
}

$svgWidth = $domainLayouts[0].Width + (2 * $outerPadding)
$svgHeight = $currentY + $outerPadding
$sb = [System.Text.StringBuilder]::new()
[void]$sb.AppendLine("<svg xmlns=`"http://www.w3.org/2000/svg`" width=`"$svgWidth`" height=`"$svgHeight`" viewBox=`"0 0 $svgWidth $svgHeight`" role=`"img`" aria-labelledby=`"title desc`">")
[void]$sb.AppendLine('<title id="title">첫출근 전체 데이터베이스 ERD</title>')
[void]$sb.AppendLine('<desc id="desc">37개 MySQL 테이블의 컬럼과 외래키 관계를 기능 영역별로 표현한 다이어그램</desc>')
[void]$sb.AppendLine(@'
<defs>
  <filter id="shadow" x="-10%" y="-10%" width="120%" height="130%"><feDropShadow dx="0" dy="2" stdDeviation="3" flood-color="#0F172A" flood-opacity="0.12"/></filter>
  <marker id="arrow" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto"><path d="M0,0 L8,4 L0,8 Z" fill="#94A3B8"/></marker>
  <style>
    text { font-family: Pretendard, "Noto Sans KR", Arial, sans-serif; }
    .page-title { font-size: 34px; font-weight: 800; fill: #0F172A; }
    .page-subtitle { font-size: 15px; fill: #64748B; }
    .domain-title { font-size: 20px; font-weight: 800; }
    .table-title { font-size: 16px; font-weight: 800; fill: white; }
    .column { font-size: 12px; fill: #334155; }
    .key { font-size: 10px; font-weight: 800; }
    .type { font-size: 10px; fill: #94A3B8; text-anchor: end; }
    .relation { fill: none; stroke: #94A3B8; stroke-width: 1.25; opacity: .66; marker-end: url(#arrow); }
  </style>
</defs>
'@)
[void]$sb.AppendLine('<rect width="100%" height="100%" fill="#F8FAFC"/>')
[void]$sb.AppendLine('<text x="36" y="48" class="page-title">첫출근 데이터베이스 ERD</text>')
[void]$sb.AppendLine('<text x="36" y="75" class="page-subtitle">MySQL 8.0 · 37 tables · PK / FK / 전체 컬럼 · 기능 영역별 구분</text>')

foreach ($layout in $domainLayouts) {
    $colors = $domainColors[$layout.Name]
    [void]$sb.AppendLine("<rect x=`"$($layout.X)`" y=`"$($layout.Y)`" width=`"$($layout.Width)`" height=`"$($layout.Height)`" rx=`"18`" fill=`"$($colors[0])`" stroke=`"$($colors[1])`" stroke-opacity=`".25`"/>")
    [void]$sb.AppendLine("<text x=`"$($layout.X + 26)`" y=`"$($layout.Y + 34)`" class=`"domain-title`" fill=`"$($colors[1])`">$(Escape-Xml $layout.Name)</text>")
}

# Relations are drawn behind the table cards.
foreach ($table in $tables.Values) {
    if (-not $positions.ContainsKey($table.Name)) { continue }
    $source = $positions[$table.Name]
    foreach ($fk in $table.ForeignKeys) {
        if (-not $positions.ContainsKey($fk.Target)) { continue }
        $target = $positions[$fk.Target]
        $x1 = $source.X + $source.Width
        $y1 = $source.Y + 21
        $x2 = $target.X
        $y2 = $target.Y + 21
        if ($source.X -ge $target.X) {
            $x1 = $source.X
            $x2 = $target.X + $target.Width
        }
        $midX = ($x1 + $x2) / 2
        [void]$sb.AppendLine("<path class=`"relation`" d=`"M $x1 $y1 C $midX $y1, $midX $y2, $x2 $y2`"><title>$(Escape-Xml "$($table.Name).$($fk.Column) → $($fk.Target).$($fk.TargetColumn)")</title></path>")
    }
}

foreach ($domain in $domainOrder) {
    $colors = $domainColors[$domain]
    foreach ($name in $domainTables[$domain]) {
        if (-not $positions.ContainsKey($name)) { continue }
        $table = $tables[$name]
        $p = $positions[$name]
        [void]$sb.AppendLine("<g filter=`"url(#shadow)`">")
        [void]$sb.AppendLine("<rect x=`"$($p.X)`" y=`"$($p.Y)`" width=`"$($p.Width)`" height=`"$($p.Height)`" rx=`"10`" fill=`"white`"/>")
        [void]$sb.AppendLine("<path d=`"M $($p.X + 10) $($p.Y) H $($p.X + $p.Width - 10) Q $($p.X + $p.Width) $($p.Y) $($p.X + $p.Width) $($p.Y + 10) V $($p.Y + $headerHeight) H $($p.X) V $($p.Y + 10) Q $($p.X) $($p.Y) $($p.X + 10) $($p.Y) Z`" fill=`"$($colors[1])`"/>")
        [void]$sb.AppendLine("<text x=`"$($p.X + 14)`" y=`"$($p.Y + 27)`" class=`"table-title`">$(Escape-Xml $name)</text>")
        $row = 0
        foreach ($column in $table.Columns) {
            $rowY = $p.Y + $headerHeight + 17 + ($row * $rowHeight)
            if ($row % 2 -eq 1) { [void]$sb.AppendLine("<rect x=`"$($p.X)`" y=`"$($rowY - 16)`" width=`"$($p.Width)`" height=`"$rowHeight`" fill=`"#F8FAFC`"/>") }
            $isPk = $table.PrimaryKeys -contains $column.Name
            $isFk = @($table.ForeignKeys | Where-Object Column -eq $column.Name).Count -gt 0
            $badge = if ($isPk -and $isFk) { 'PK·FK' } elseif ($isPk) { 'PK' } elseif ($isFk) { 'FK' } else { '' }
            $badgeColor = if ($isPk) { '#F59E0B' } elseif ($isFk) { '#0EA5E9' } else { '#CBD5E1' }
            if ($badge) {
                [void]$sb.AppendLine("<text x=`"$($p.X + 12)`" y=`"$rowY`" class=`"key`" fill=`"$badgeColor`">$badge</text>")
            }
            [void]$sb.AppendLine("<text x=`"$($p.X + 60)`" y=`"$rowY`" class=`"column`">$(Escape-Xml $column.Name)</text>")
            [void]$sb.AppendLine("<text x=`"$($p.X + $p.Width - 12)`" y=`"$rowY`" class=`"type`">$(Escape-Xml $column.Type)</text>")
            $row++
        }
        [void]$sb.AppendLine('</g>')
    }
}

[void]$sb.AppendLine('</svg>')
$outputDirectory = Split-Path -Parent $OutputPath
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
[System.IO.File]::WriteAllText($OutputPath, $sb.ToString(), [System.Text.UTF8Encoding]::new($false))
Write-Output "Generated $OutputPath ($($tables.Count) tables)"
