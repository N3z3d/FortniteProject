---
name: 'bmm-qa-automate'
description: 'Generate automated API and E2E tests for implemented code using the project''s existing test framework (detects existing well known in use test frameworks). Use after implementation to add test coverage. NOT for code review or story validation - use CR for that.'
argument-hint: [context]
---

IT IS CRITICAL THAT YOU FOLLOW THIS COMMAND:
1. Always LOAD the FULL @{project-root}/_bmad/core/tasks/workflow.xml
2. READ its entire contents - this is the CORE OS for EXECUTING the specific workflow-config @{project-root}/_bmad/bmm/workflows/qa-generate-e2e-tests/workflow.yaml
3. Pass the yaml path @{project-root}/_bmad/bmm/workflows/qa-generate-e2e-tests/workflow.yaml as 'workflow-config' parameter to the workflow.xml instructions
4. Execute all instructions exactly as written.
5. If the user provided extra context as $ARGUMENTS, use it as workflow input context.
