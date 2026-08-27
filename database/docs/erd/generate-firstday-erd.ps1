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
            $columns.Add([pscustomobject]@{
                Name = $Matches.name
                Type = $Matches.type
                Nullable = ($line -notmatch '\bNOT\s+NULL\b')
            })
        }
    }

    $primaryKeys = @()
    if ($body -match 'PRIMARY KEY\s*\((?<keys>[^\)]+)\)') {
        $primaryKeys = ($Matches.keys -split ',') | ForEach-Object { $_.Trim().Trim('`') }
    }

    $uniqueKeys = [System.Collections.Generic.List[object]]::new()
    $uniquePattern = '(?:UNIQUE\s+(?:KEY|INDEX)?\s*(?:`?[A-Za-z0-9_]+`?)?\s*)\((?<keys>[^\)]+)\)'
    foreach ($unique in [regex]::Matches($body, $uniquePattern)) {
        $uniqueKeys.Add(@(($unique.Groups['keys'].Value -split ',') | ForEach-Object { $_.Trim().Trim('`') }))
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
        UniqueKeys = $uniqueKeys
        ForeignKeys = $foreignKeys
    }
}

# Circular dependencies can be added after table creation (for example,
# users.company_id -> companies.company_id), so include ALTER TABLE FKs too.
$alterFkPattern = '(?ms)ALTER TABLE\s+`?(?<table>[A-Za-z0-9_]+)`?.*?FOREIGN KEY\s*\(`?(?<column>[A-Za-z0-9_]+)`?\)\s*REFERENCES\s*`?(?<target>[A-Za-z0-9_]+)`?\s*\(`?(?<targetColumn>[A-Za-z0-9_]+)`?\)\s*;'
foreach ($alterFk in [regex]::Matches($ddl, $alterFkPattern)) {
    $tableName = $alterFk.Groups['table'].Value
    if (-not $tables.Contains($tableName)) { continue }
    $columnName = $alterFk.Groups['column'].Value
    $alreadyExists = @($tables[$tableName].ForeignKeys | Where-Object {
        $_.Column -eq $columnName -and $_.Target -eq $alterFk.Groups['target'].Value
    }).Count -gt 0
    if ($alreadyExists) { continue }
    $tables[$tableName].ForeignKeys.Add([pscustomobject]@{
        Column = $columnName
        Target = $alterFk.Groups['target'].Value
        TargetColumn = $alterFk.Groups['targetColumn'].Value
    })
}

$cardWidth = 330
$headerHeight = 42
$rowHeight = 23
$outerPadding = 40

$positions = @{}

# Network-style placement: core tables form the center and related tables fan
# out around them. This avoids long aligned lanes and distributes FK paths.
$preferredPositions = @{
    'users' = @(2820, 1840); 'companies' = @(1880, 1660)
    'job_postings' = @(2720, 650); 'applications' = @(3650, 1460)
    'job_categories' = @(1660, 430); 'skills' = @(3650, 300)
    'job_posting_skills' = @(3170, 230); 'saved_jobs' = @(3260, 2650)
    'saved_companies' = @(1960, 2740); 'personal_profiles' = @(2440, 2850)
    'user_desired_jobs' = @(1220, 900); 'policies' = @(720, 1570)
    'user_policy_consents' = @(1130, 2150)
    'application_status_history' = @(3200, 2200); 'application_memos' = @(4070, 2610)
    'resumes' = @(4560, 820); 'resume_educations' = @(5040, 320)
    'resume_careers' = @(5470, 760); 'resume_projects' = @(5100, 1320)
    'resume_skills' = @(5520, 1700); 'cover_letters' = @(4540, 1880)
    'cover_letter_items' = @(5000, 2200); 'cover_letter_ai_reviews' = @(5440, 2500)
    'company_reviews' = @(1230, 2910); 'interview_reviews' = @(1710, 3480)
    'salary_records' = @(720, 3430); 'review_reactions' = @(2520, 3820)
    'faq_categories' = @(80, 300); 'faqs' = @(2350, 1510)
    'inquiry_categories' = @(80, 900); 'inquiries' = @(500, 1060)
    'inquiry_attachments' = @(80, 1470); 'reports' = @(480, 2470)
    'banners' = @(5920, 260); 'notices' = @(5920, 860)
    'site_settings' = @(5920, 1450); 'site_versions' = @(5920, 1990)
}

$layoutScaleX = 1.35
$layoutScaleY = 1.30
foreach ($table in $tables.Values) {
    if (-not $preferredPositions.ContainsKey($table.Name)) { continue }
    $point = $preferredPositions[$table.Name]
    $height = $headerHeight + (($table.Columns.Count + 1) * $rowHeight)
    $positions[$table.Name] = [pscustomobject]@{
        X=[math]::Round($point[0] * $layoutScaleX)
        Y=[math]::Round($point[1] * $layoutScaleY)
        Width=$cardWidth
        Height=$height
    }
}

$svgWidth = 8500
$svgHeight = 5750
$foreignKeyCount = ($tables.Values | ForEach-Object { $_.ForeignKeys.Count } | Measure-Object -Sum).Sum
$sb = [System.Text.StringBuilder]::new()
[void]$sb.AppendLine("<svg xmlns=`"http://www.w3.org/2000/svg`" width=`"$svgWidth`" height=`"$svgHeight`" viewBox=`"0 0 $svgWidth $svgHeight`" role=`"img`" aria-labelledby=`"title desc`">")
[void]$sb.AppendLine('<title id="title">첫출근 전체 데이터베이스 ERD</title>')
[void]$sb.AppendLine('<desc id="desc">37개 MySQL 테이블의 컬럼과 외래키 관계를 네트워크 형태로 표현한 다이어그램</desc>')
[void]$sb.AppendLine(@'
<defs>
  <filter id="shadow" x="-10%" y="-10%" width="120%" height="130%"><feDropShadow dx="0" dy="2" stdDeviation="3" flood-color="#0F172A" flood-opacity="0.12"/></filter>
  <marker id="zero-many" markerWidth="19" markerHeight="16" refX="17" refY="8" orient="auto-start-reverse" markerUnits="userSpaceOnUse">
    <circle cx="3" cy="8" r="2.5" fill="#F8FAFC" stroke="#64748B" stroke-width="1.4"/>
    <path d="M8 8 L17 2 M8 8 L17 8 M8 8 L17 14" fill="none" stroke="#64748B" stroke-width="1.4"/>
  </marker>
  <marker id="zero-one" markerWidth="18" markerHeight="16" refX="16" refY="8" orient="auto-start-reverse" markerUnits="userSpaceOnUse">
    <circle cx="3" cy="8" r="2.5" fill="#F8FAFC" stroke="#64748B" stroke-width="1.4"/>
    <path d="M11 2 L11 14 M16 2 L16 14" fill="none" stroke="#64748B" stroke-width="1.4"/>
  </marker>
  <marker id="exactly-one" markerWidth="13" markerHeight="16" refX="9" refY="8" orient="auto" markerUnits="userSpaceOnUse">
    <path d="M3 2 L3 14 M9 2 L9 14" fill="none" stroke="#64748B" stroke-width="1.4"/>
  </marker>
  <style>
    text { font-family: Pretendard, "Noto Sans KR", Arial, sans-serif; }
    .page-title { font-size: 34px; font-weight: 800; fill: #0F172A; }
    .page-subtitle { font-size: 15px; fill: #64748B; }
    .domain-title { font-size: 20px; font-weight: 800; }
    .table-title { font-size: 16px; font-weight: 800; fill: white; }
    .column { font-size: 12px; fill: #334155; }
    .key { font-size: 10px; font-weight: 800; }
    .type { font-size: 10px; fill: #94A3B8; text-anchor: end; }
    .relation { fill: none; stroke: #64748B; stroke-width: 1.35; opacity: .76; }
    .relation-halo { fill: none; stroke: #F8FAFC; stroke-width: 5.5; opacity: 1; }
    .relation.identifying { stroke-width: 1.8; }
    .relation.non-identifying { stroke-dasharray: 8 6; }
    .legend-title { font-size: 13px; font-weight: 800; fill: #334155; }
    .legend-text { font-size: 12px; fill: #64748B; }
  </style>
</defs>
'@)
[void]$sb.AppendLine('<rect width="100%" height="100%" fill="#F8FAFC"/>')
[void]$sb.AppendLine('<text x="36" y="48" class="page-title">첫출근 데이터베이스 ERD</text>')
[void]$sb.AppendLine("<text x=`"36`" y=`"75`" class=`"page-subtitle`">MySQL 8.0 · $($tables.Count) tables · $foreignKeyCount foreign keys · PK / FK / 전체 컬럼</text>")
[void]$sb.AppendLine(@'
<g transform="translate(36 98)">
  <rect width="1110" height="130" rx="12" fill="#FFFFFF" stroke="#E2E8F0"/>
  <text x="18" y="25" class="legend-title">관계 표기</text>
  <path d="M100 22 H175" class="relation identifying"/><text x="188" y="26" class="legend-text">식별 관계 (FK가 자식 PK에 포함)</text>
  <path d="M430 22 H505" class="relation non-identifying"/><text x="518" y="26" class="legend-text">비식별 관계</text>
  <path d="M30 55 H72" class="relation identifying" marker-end="url(#exactly-one)"/><text x="84" y="59" class="legend-text">1</text>
  <path d="M135 55 H177" class="relation identifying" marker-end="url(#zero-one)"/><text x="189" y="59" class="legend-text">0..1</text>
  <path d="M270 55 H312" class="relation identifying" marker-end="url(#zero-many)"/><text x="324" y="59" class="legend-text">0개 이상 (0..N)</text>
  <text x="490" y="59" class="legend-text">연결 테이블의 두 1:N 관계가 M:N을 구성</text>
  <text x="18" y="92" class="legend-title">기능 영역</text>
  <rect x="100" y="78" width="18" height="18" rx="4" fill="#2563EB"/><text x="126" y="92" class="legend-text">회원·정책</text>
  <rect x="220" y="78" width="18" height="18" rx="4" fill="#059669"/><text x="246" y="92" class="legend-text">기업·채용공고</text>
  <rect x="390" y="78" width="18" height="18" rx="4" fill="#EA580C"/><text x="416" y="92" class="legend-text">지원·전형</text>
  <rect x="520" y="78" width="18" height="18" rx="4" fill="#7C3AED"/><text x="546" y="92" class="legend-text">이력서·자기소개서</text>
  <rect x="720" y="78" width="18" height="18" rx="4" fill="#E11D48"/><text x="746" y="92" class="legend-text">기업 콘텐츠</text>
  <rect x="860" y="78" width="18" height="18" rx="4" fill="#0891B2"/><text x="886" y="92" class="legend-text">고객센터·신고</text>
  <rect x="100" y="104" width="18" height="18" rx="4" fill="#475569"/><text x="126" y="118" class="legend-text">관리자 운영</text>
</g>
'@)

function Test-RouteSegmentHitsRectangle($a, $b, $rectangle) {
    $padding = 8
    $left = $rectangle.X - $padding
    $right = $rectangle.X + $rectangle.Width + $padding
    $top = $rectangle.Y - $padding
    $bottom = $rectangle.Y + $rectangle.Height + $padding
    if ($a.X -eq $b.X) {
        $minY = [math]::Min($a.Y, $b.Y)
        $maxY = [math]::Max($a.Y, $b.Y)
        return $a.X -gt $left -and $a.X -lt $right -and $maxY -gt $top -and $minY -lt $bottom
    }
    $minX = [math]::Min($a.X, $b.X)
    $maxX = [math]::Max($a.X, $b.X)
    return $a.Y -gt $top -and $a.Y -lt $bottom -and $maxX -gt $left -and $minX -lt $right
}

function Get-RouteScore($points, [string]$sourceName, [string]$targetName) {
    $hits = 0
    $length = 0
    $relationPenalty = 0
    for ($i = 0; $i -lt $points.Count - 1; $i++) {
        $a = $points[$i]
        $b = $points[$i + 1]
        $length += [math]::Abs($a.X - $b.X) + [math]::Abs($a.Y - $b.Y)
        foreach ($otherName in $positions.Keys) {
            if ($otherName -eq $sourceName -or $otherName -eq $targetName) { continue }
            if (Test-RouteSegmentHitsRectangle $a $b $positions[$otherName]) { $hits++ }
        }
        foreach ($used in $usedRouteSegments) {
            $horizontal = $a.Y -eq $b.Y
            $usedHorizontal = $used.A.Y -eq $used.B.Y
            if ($horizontal -and $usedHorizontal -and $a.Y -eq $used.A.Y) {
                $overlap = [math]::Min([math]::Max($a.X,$b.X), [math]::Max($used.A.X,$used.B.X)) - [math]::Max([math]::Min($a.X,$b.X), [math]::Min($used.A.X,$used.B.X))
                if ($overlap -gt 1) { $relationPenalty += 80000000 + ($overlap * 1000) }
            } elseif (-not $horizontal -and -not $usedHorizontal -and $a.X -eq $used.A.X) {
                $overlap = [math]::Min([math]::Max($a.Y,$b.Y), [math]::Max($used.A.Y,$used.B.Y)) - [math]::Max([math]::Min($a.Y,$b.Y), [math]::Min($used.A.Y,$used.B.Y))
                if ($overlap -gt 1) { $relationPenalty += 80000000 + ($overlap * 1000) }
            } elseif ($horizontal -ne $usedHorizontal) {
                $h = if ($horizontal) { [pscustomobject]@{A=$a;B=$b} } else { $used }
                $v = if ($horizontal) { $used } else { [pscustomobject]@{A=$a;B=$b} }
                $withinX = $v.A.X -gt [math]::Min($h.A.X,$h.B.X) -and $v.A.X -lt [math]::Max($h.A.X,$h.B.X)
                $withinY = $h.A.Y -gt [math]::Min($v.A.Y,$v.B.Y) -and $h.A.Y -lt [math]::Max($v.A.Y,$v.B.Y)
                if ($withinX -and $withinY) { $relationPenalty += 2000000 }
            }
        }
    }
    return ($hits * 1000000000000) + $relationPenalty + $length
}

function Convert-RouteToPath($points) {
    $path = "M $($points[0].X) $($points[0].Y)"
    for ($i = 1; $i -lt $points.Count; $i++) { $path += " L $($points[$i].X) $($points[$i].Y)" }
    return $path
}

$routeXChannels = [System.Collections.Generic.List[double]]::new()
$routeYChannels = [System.Collections.Generic.List[double]]::new()
$usedRouteSegments = [System.Collections.Generic.List[object]]::new()
$portTotals = @{}
$portIndexes = @{}
$routeXChannels.Add(28); $routeXChannels.Add($svgWidth - 28)
$routeYChannels.Add(205); $routeYChannels.Add($svgHeight - 28)
foreach ($rectangle in $positions.Values) {
    $routeXChannels.Add($rectangle.X - 28)
    $routeXChannels.Add($rectangle.X + $rectangle.Width + 28)
    $routeYChannels.Add($rectangle.Y - 28)
    $routeYChannels.Add($rectangle.Y + $rectangle.Height + 28)
}

function Add-PortTotal([string]$key) {
    $portTotals[$key] = $(if ($portTotals.ContainsKey($key)) { $portTotals[$key] + 1 } else { 1 })
}

# Count every endpoint first so high-degree tables can use their full height.
foreach ($table in $tables.Values) {
    if (-not $positions.ContainsKey($table.Name)) { continue }
    $source = $positions[$table.Name]
    foreach ($fk in $table.ForeignKeys) {
        if (-not $positions.ContainsKey($fk.Target)) { continue }
        $target = $positions[$fk.Target]
        $sourceSide = if ($table.Name -eq $fk.Target -or $source.X -lt $target.X) { 'right' } else { 'left' }
        $targetSide = if ($table.Name -eq $fk.Target) { 'right' } elseif ($source.X -lt $target.X) { 'left' } else { 'right' }
        Add-PortTotal "$($table.Name).$sourceSide"
        Add-PortTotal "$($fk.Target).$targetSide"
    }
}

function Get-PortY([string]$key, [string]$tableName, [double]$baseY) {
    $index = if ($portIndexes.ContainsKey($key)) { $portIndexes[$key] } else { 0 }
    $portIndexes[$key] = $index + 1
    $total = $portTotals[$key]
    if ($total -lt 4) { return $baseY + ($index * 9) }

    $rectangle = $positions[$tableName]
    $top = $rectangle.Y + $headerHeight + 11
    $bottom = $rectangle.Y + $rectangle.Height - 11
    if ($total -eq 1) { return ($top + $bottom) / 2 }
    return $top + (($bottom - $top) * $index / ($total - 1))
}

$relationSb = [System.Text.StringBuilder]::new()
$relationOrdinal = 0

# Relations are drawn behind the table cards.
foreach ($table in $tables.Values) {
    if (-not $positions.ContainsKey($table.Name)) { continue }
    $source = $positions[$table.Name]
    foreach ($fk in $table.ForeignKeys) {
        $relationOrdinal++
        if (-not $positions.ContainsKey($fk.Target)) { continue }
        $target = $positions[$fk.Target]
        $sourceColumnIndex = [array]::IndexOf(@($table.Columns.Name), $fk.Column)
        $targetTable = $tables[$fk.Target]
        $targetColumnIndex = [array]::IndexOf(@($targetTable.Columns.Name), $fk.TargetColumn)
        $sourceColumn = @($table.Columns | Where-Object Name -eq $fk.Column)[0]
        $isIdentifying = $table.PrimaryKeys -contains $fk.Column
        $isSinglePrimaryKey = $table.PrimaryKeys.Count -eq 1 -and $table.PrimaryKeys[0] -eq $fk.Column
        $isSingleUniqueKey = $false
        foreach ($uniqueKey in $table.UniqueKeys) {
            if ($uniqueKey.Count -eq 1 -and $uniqueKey[0] -eq $fk.Column) { $isSingleUniqueKey = $true; break }
        }
        $childMarker = if ($isSinglePrimaryKey -or $isSingleUniqueKey) { 'zero-one' } else { 'zero-many' }
        $parentMarker = if ($sourceColumn.Nullable) { 'zero-one' } else { 'exactly-one' }
        $relationClass = if ($isIdentifying) { 'identifying' } else { 'non-identifying' }
        $markerAttributes = "marker-start=`"url(#$childMarker)`" marker-end=`"url(#$parentMarker)`""
        $sourceSide = if ($table.Name -eq $fk.Target -or $source.X -lt $target.X) { 'right' } else { 'left' }
        $targetSide = if ($table.Name -eq $fk.Target) { 'right' } elseif ($source.X -lt $target.X) { 'left' } else { 'right' }
        $sourceBaseY = $source.Y + $headerHeight + 13 + ($sourceColumnIndex * $rowHeight)
        $targetBaseY = $target.Y + $headerHeight + 13 + ($targetColumnIndex * $rowHeight)
        $y1 = Get-PortY "$($table.Name).$sourceSide" $table.Name $sourceBaseY
        $y2 = Get-PortY "$($fk.Target).$targetSide" $fk.Target $targetBaseY

        if ($table.Name -eq $fk.Target) {
            $x1 = $source.X + $source.Width
            $x2 = $source.X + $source.Width
            $loopX = $x1 + 42
            $selfRoute = @([pscustomobject]@{X=$x1;Y=$y1},[pscustomobject]@{X=$loopX;Y=$y1},[pscustomobject]@{X=$loopX;Y=$y2},[pscustomobject]@{X=$x2;Y=$y2})
            for($segmentIndex=0;$segmentIndex -lt $selfRoute.Count-1;$segmentIndex++){$usedRouteSegments.Add([pscustomobject]@{A=$selfRoute[$segmentIndex];B=$selfRoute[$segmentIndex+1]})}
            $selfPathData = Convert-RouteToPath $selfRoute
            [void]$relationSb.AppendLine("<path class=`"relation-halo`" d=`"$selfPathData`"/>")
            [void]$relationSb.AppendLine("<path class=`"relation $relationClass`" $markerAttributes d=`"$selfPathData`"><title>$(Escape-Xml "$($table.Name).$($fk.Column) → $($fk.Target).$($fk.TargetColumn)")</title></path>")
            continue
        }

        if ($source.X -lt $target.X) {
            $x1 = $source.X + $source.Width
            $x2 = $target.X
        } else {
            $x1 = $source.X
            $x2 = $target.X + $target.Width
        }
        $candidates = [System.Collections.Generic.List[object]]::new()
        $midX = ($x1 + $x2) / 2
        $candidates.Add(@(
            [pscustomobject]@{X=$x1;Y=$y1}, [pscustomobject]@{X=$midX;Y=$y1},
            [pscustomobject]@{X=$midX;Y=$y2}, [pscustomobject]@{X=$x2;Y=$y2}
        ))
        foreach ($channelX in $routeXChannels) {
            $minimumEndpointX = [math]::Min($x1, $x2)
            $maximumEndpointX = [math]::Max($x1, $x2)
            if ($channelX -le $minimumEndpointX -or $channelX -ge $maximumEndpointX) { continue }
            $candidates.Add(@(
                [pscustomobject]@{X=$x1;Y=$y1}, [pscustomobject]@{X=$channelX;Y=$y1},
                [pscustomobject]@{X=$channelX;Y=$y2}, [pscustomobject]@{X=$x2;Y=$y2}
            ))
        }
        $direction = if ($source.X -lt $target.X) { 1 } else { -1 }
        $stubDistance = 28 + ($relationOrdinal * 3)
        $sourceStubX = $x1 + ($direction * $stubDistance)
        $targetStubX = $x2 - ($direction * $stubDistance)
        foreach ($channelY in $routeYChannels) {
            $candidates.Add(@(
                [pscustomobject]@{X=$x1;Y=$y1}, [pscustomobject]@{X=$sourceStubX;Y=$y1},
                [pscustomobject]@{X=$sourceStubX;Y=$channelY}, [pscustomobject]@{X=$targetStubX;Y=$channelY},
                [pscustomobject]@{X=$targetStubX;Y=$y2}, [pscustomobject]@{X=$x2;Y=$y2}
            ))
        }
        $bestRoute = $null
        $bestScore = [double]::PositiveInfinity
        foreach ($candidate in $candidates) {
            $score = Get-RouteScore $candidate $table.Name $fk.Target
            if ($score -lt $bestScore) { $bestScore = $score; $bestRoute = $candidate }
        }
        $pathData = Convert-RouteToPath $bestRoute
        $obstacleHits = [math]::Floor($bestScore / 1000000000000)
        for($segmentIndex=0;$segmentIndex -lt $bestRoute.Count-1;$segmentIndex++){$usedRouteSegments.Add([pscustomobject]@{A=$bestRoute[$segmentIndex];B=$bestRoute[$segmentIndex+1]})}
        [void]$relationSb.AppendLine("<path class=`"relation-halo`" d=`"$pathData`"/>")
        [void]$relationSb.AppendLine("<path class=`"relation $relationClass`" data-obstacle-hits=`"$obstacleHits`" $markerAttributes d=`"$pathData`"><title>$(Escape-Xml "$($table.Name).$($fk.Column) → $($fk.Target).$($fk.TargetColumn)")</title></path>")
    }
}

# Relationships stay behind table cards. Routes are obstacle-aware, and marker
# reference points place their tips on the border while the shapes remain out.
[void]$sb.Append($relationSb.ToString())

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
