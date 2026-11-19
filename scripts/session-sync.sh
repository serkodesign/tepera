#!/usr/bin/env bash
# SessionStart hook: перевіряє, чи гілка відстала від origin (напр. після роботи на іншому пристрої).
# Вивід потрапляє в контекст Claude. Нічого не змінює, окрім git fetch.
git fetch --quiet 2>/dev/null
branch=$(git branch --show-current)
behind=$(git rev-list --count HEAD..@{u} 2>/dev/null)
ahead=$(git rev-list --count @{u}..HEAD 2>/dev/null)
dirty=$(git status --porcelain --untracked-files=no | wc -l | tr -d ' ')
echo "GIT-SYNC: гілка $branch; позаду origin на ${behind:-?} комітів; попереду на ${ahead:-?}; змінених відстежуваних файлів: $dirty"
