<!---
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

# Dubbo AI Skills

This directory contains AI Skill files for Apache Dubbo. Skills give AI
coding assistants (Claude Code, GitHub Copilot, Cursor, and others)
structured, on-demand knowledge about Dubbo's modules so they generate
correct, idiomatic Dubbo 3 code rather than outdated Dubbo 2 patterns.

## Why this directory exists

AI coding tools do not have reliable knowledge of Dubbo 3's internals.
When asked to generate Dubbo provider code, they commonly:

- Use `@Service` (Spring) instead of `@DubboService`
- Register services at the interface level (Dubbo 2) instead of the
  application level (Dubbo 3)
- Omit the MetadataCenter when it is required for Dubbo 2→3 migration
- Default to the `dubbo` TCP protocol instead of `tri` (Triple/HTTP2)
- Confuse the Registry Center with the Config Center

Each SKILL.md file in this directory gives an AI tool the specific
context it needs to avoid these mistakes for one module.

## How to install

### Claude Code

Copy one or more skill folders into Claude Code's global skills directory:

```bash
# Install the overview skill (recommended starting point)
cp -r skills/dubbo-overview ~/.claude/skills/

# Install all skills at once
cp -r skills/* ~/.claude/skills/
```

Claude Code automatically detects and offers to load skills from
`~/.claude/skills/` based on the `description` field in each SKILL.md.

### Cursor

```bash
# Install into your project (project-scoped)
cp -r skills/dubbo-overview .cursor/skills/

# Or install globally
cp -r skills/dubbo-overview ~/.cursor/skills/
```

### GitHub Copilot (VS Code)

Copy the skill folder into your project root under `.agents/skills/`:

```bash
mkdir -p .agents/skills
cp -r skills/dubbo-overview .agents/skills/
```

### Manual use with any AI tool

Paste the contents of any SKILL.md directly into your system prompt or
context window before asking Dubbo-related questions.

## Skills available

| Skill              | Module covered                              | Status         |
|--------------------|---------------------------------------------|----------------|
| dubbo-overview     | Architecture, topology, three-center setup  | ✅ Available   |
| dubbo-rpc          | Protocol / Invoker / Proxy / Filter chain   | 🚧 Planned     |
| dubbo-registry     | Service discovery, Nacos, Zookeeper, K8s    | 🚧 Planned     |
| dubbo-cluster      | Load balance, fault tolerance, routing      | 🚧 Planned     |
| dubbo-config       | Config center, properties, dynamic config   | 🚧 Planned     |
| dubbo-metadata     | Metadata center, Dubbo 2→3 migration        | 🚧 Planned     |
| dubbo-remoting     | Transport layer, Netty, Exchange            | 🚧 Planned     |
| dubbo-admin        | Admin console, traffic rules, service test  | 🚧 Planned     |

## SKILL.md format

Each skill file follows this structure:

```
---
name: <skill-name>
description: >
  <what this skill covers, written as the phrases a developer would use
  when asking an AI tool about this module>
license: Apache-2.0
---

# What this module is
# How it works
# How to use it
# How to extend it (SPI)
# Common mistakes
```

The `description` field is the trigger: AI tools read only `name` and
`description` at startup to decide when to load a skill. Write it with
the exact phrases developers actually use.

## Background

This skills directory is part of the GSoC 2026 project
**"Convert Dubbo Capabilities into AI Skills"**.

Project mentors: Albumen Kevin (albumenj@apache.org),
Yu Yu (rainyu@apache.org)

See the full project idea at: https://s.apache.org/gsoc2026ideas

## Contributing

To add a new skill:

1. Create `skills/<module-name>/SKILL.md`
2. Follow the 5-section format above
3. Keep each SKILL.md under 2,000 tokens (roughly 1,500 words)
4. Add the ASF Apache 2.0 license header at the top
5. Add a row to the table in this README
6. Open a pull request against the `3.3` branch

Questions? Post to dev@dubbo.apache.org with subject `[AI Skills]`.
