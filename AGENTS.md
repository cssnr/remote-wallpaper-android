# Agent Guide

Android client for Django Files: [django-files/django-files](https://github.com/django-files/django-files)

- `app/` - Android app source (Kotlin, Gradle)
- `Taskfile.yml` - task commands

## Commands

ALWAYS use the `task *` commands

| Command        | Purpose                                             |
| -------------- | --------------------------------------------------- |
| `task compile` | Compile Kotlin (quick check)                        |
| `task build`   | Build all variants (APKs)                           |
| `task lint`    | Prettier check + yamllint + actionlint + shellcheck |
| `task format`  | Prettier write (format non-kotlin files)            |

Do NOT use `-q` or pipe Gradle output through `Select-Object` — both hide progress and make long builds look hung.
