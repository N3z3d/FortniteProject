param(
  [string]$ProjectRoot,
  [string]$CodexHomePath
)

$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
  $scriptPath = $MyInvocation.MyCommand.Path
  if ([string]::IsNullOrWhiteSpace($scriptPath)) {
    throw 'Cannot resolve script path to infer project root.'
  }

  $scriptDir = Split-Path -Parent $scriptPath
  $ProjectRoot = (Resolve-Path (Join-Path $scriptDir '..\..')).Path
}

if ([string]::IsNullOrWhiteSpace($CodexHomePath)) {
  $CodexHomePath = Join-Path $ProjectRoot '.codex'
}

function Sanitize-Ascii([string]$text) {
  if ([string]::IsNullOrWhiteSpace($text)) {
    return ''
  }

  $collapsed = [regex]::Replace($text, '\s+', ' ').Trim()
  $builder = New-Object System.Text.StringBuilder

  foreach ($ch in $collapsed.ToCharArray()) {
    $code = [int]$ch
    if ($code -ge 32 -and $code -le 126) {
      [void]$builder.Append($ch)
    }
  }

  return $builder.ToString()
}

$promptDir = Join-Path $CodexHomePath 'prompts'
$bmadHelpCsvPath = Join-Path $ProjectRoot '_bmad\_config\bmad-help.csv'
$agentManifestCsvPath = Join-Path $ProjectRoot '_bmad\_config\agent-manifest.csv'

if (-not (Test-Path $bmadHelpCsvPath)) {
  throw "Missing BMAD help catalog: $bmadHelpCsvPath"
}

if (-not (Test-Path $agentManifestCsvPath)) {
  throw "Missing BMAD agent catalog: $agentManifestCsvPath"
}

New-Item -ItemType Directory -Path $promptDir -Force | Out-Null

$workflowRows = Import-Csv -Path $bmadHelpCsvPath |
  Where-Object {
    -not [string]::IsNullOrWhiteSpace($_.command) -and
    -not [string]::IsNullOrWhiteSpace($_.'workflow-file')
  }

$workflowPromptCount = 0
foreach ($group in ($workflowRows | Group-Object command)) {
  $command = $group.Name.Trim()
  $first = $group.Group[0]
  $workflowFile = $first.'workflow-file'.Trim()

  $commandDisplayNames = ($group.Group |
    Select-Object -ExpandProperty name |
    Where-Object { -not [string]::IsNullOrWhiteSpace($_) } |
    Sort-Object -Unique) -join ' / '

  $description = Sanitize-Ascii $first.description
  if ([string]::IsNullOrWhiteSpace($description)) {
    $description = "BMAD command: $commandDisplayNames"
  }

  $shortName = $command -replace '^bmad-', ''
  $descriptionEscaped = $description.Replace("'", "''")

  $frontmatter = @(
    '---'
    "name: '$shortName'"
    "description: '$descriptionEscaped'"
    'argument-hint: [context]'
    '---'
    ''
  )

  $body = @(
    'IT IS CRITICAL THAT YOU FOLLOW THIS COMMAND:'
  )

  if ($workflowFile.ToLowerInvariant().EndsWith('.yaml')) {
    $body += @(
      "1. Always LOAD the FULL @{project-root}/_bmad/core/tasks/workflow.xml"
      "2. READ its entire contents - this is the CORE OS for EXECUTING the specific workflow-config @{project-root}/$workflowFile"
      "3. Pass the yaml path @{project-root}/$workflowFile as 'workflow-config' parameter to the workflow.xml instructions"
      '4. Execute all instructions exactly as written.'
      '5. If the user provided extra context as $ARGUMENTS, use it as workflow input context.'
    )
  }
  else {
    $body += @(
      "LOAD the FULL @{project-root}/$workflowFile"
      'READ its entire contents and follow its directions exactly.'
      'If the user provided extra context as $ARGUMENTS, include it when executing the task.'
    )
  }

  $fileContent = ($frontmatter + $body) -join "`n"
  $promptPath = Join-Path $promptDir ($command + '.md')

  try {
    Set-Content -Path $promptPath -Value $fileContent -Encoding utf8
    $workflowPromptCount += 1
  }
  catch {
    Write-Warning "Skipping prompt write due to access issue: $promptPath"
  }
}

$agentRows = Import-Csv -Path $agentManifestCsvPath |
  Where-Object {
    -not [string]::IsNullOrWhiteSpace($_.name) -and
    -not [string]::IsNullOrWhiteSpace($_.path)
  }

$agentPromptCount = 0
foreach ($agent in $agentRows) {
  $agentName = $agent.name.Trim()
  $agentPath = $agent.path.Trim()

  $command = if ($agentName -eq 'bmad-master') {
    'bmad-agent-bmad-master'
  }
  elseif ($agent.module -eq 'bmm') {
    "bmad-agent-bmm-$agentName"
  }
  else {
    "bmad-agent-$agentName"
  }

  $description = "BMAD agent: $agentName"
  $descriptionEscaped = $description.Replace("'", "''")

  $frontmatter = @(
    '---'
    "name: '$agentName'"
    "description: '$descriptionEscaped'"
    'argument-hint: [task]'
    '---'
    ''
  )

  $body = @(
    "You must fully embody this agent's persona and follow all activation instructions exactly as specified."
    'NEVER break character until explicitly asked to exit the agent role.'
    ''
    '<agent-activation CRITICAL="TRUE">'
    "1. LOAD the FULL agent file from {project-root}/$agentPath"
    '2. READ its entire contents - this contains the complete persona, menu, and instructions'
    '3. FOLLOW every activation step exactly as written'
    '4. Stay in character while executing user requests that match this agent role'
    '5. If the user provided extra context as $ARGUMENTS, apply it after activation'
    '</agent-activation>'
  )

  $fileContent = ($frontmatter + $body) -join "`n"
  $promptPath = Join-Path $promptDir ($command + '.md')

  try {
    Set-Content -Path $promptPath -Value $fileContent -Encoding utf8
    $agentPromptCount += 1
  }
  catch {
    Write-Warning "Skipping prompt write due to access issue: $promptPath"
  }
}

Write-Output "Generated $workflowPromptCount workflow prompts and $agentPromptCount agent prompts in $promptDir"
