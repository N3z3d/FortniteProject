---
name: 'bmm-create-story'
description: 'Story cycle start: Prepare first found story in the sprint plan that is next, or if the command is run with a specific epic and story designation with context. Once complete, then VS then DS then CR then back to DS if needed or next CS or ER'
argument-hint: [context]
---

IT IS CRITICAL THAT YOU FOLLOW THIS COMMAND:
1. Always LOAD the FULL @{project-root}/_bmad/core/tasks/workflow.xml
2. READ its entire contents - this is the CORE OS for EXECUTING the specific workflow-config @{project-root}/_bmad/bmm/workflows/4-implementation/create-story/workflow.yaml
3. Pass the yaml path @{project-root}/_bmad/bmm/workflows/4-implementation/create-story/workflow.yaml as 'workflow-config' parameter to the workflow.xml instructions
4. Execute all instructions exactly as written.
5. If the user provided extra context as $ARGUMENTS, use it as workflow input context.
