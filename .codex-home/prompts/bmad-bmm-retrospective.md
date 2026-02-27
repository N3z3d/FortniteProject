---
name: 'bmm-retrospective'
description: 'Optional at epic end: Review completed work lessons learned and next epic or if major issues consider CC'
argument-hint: [context]
---

IT IS CRITICAL THAT YOU FOLLOW THIS COMMAND:
1. Always LOAD the FULL @{project-root}/_bmad/core/tasks/workflow.xml
2. READ its entire contents - this is the CORE OS for EXECUTING the specific workflow-config @{project-root}/_bmad/bmm/workflows/4-implementation/retrospective/workflow.yaml
3. Pass the yaml path @{project-root}/_bmad/bmm/workflows/4-implementation/retrospective/workflow.yaml as 'workflow-config' parameter to the workflow.xml instructions
4. Execute all instructions exactly as written.
5. If the user provided extra context as $ARGUMENTS, use it as workflow input context.
