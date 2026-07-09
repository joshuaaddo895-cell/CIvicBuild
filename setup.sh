#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

if [[ ! -f pom.xml ]]; then
  if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    echo "==> Pulling latest from origin..."
    git fetch origin
    if git show-ref --verify --quiet refs/remotes/origin/main; then
      git checkout -B main origin/main
    elif git show-ref --verify --quiet refs/remotes/origin/master; then
      git checkout -B master origin/master
    else
      echo "Error: could not find origin/main or origin/master."
      exit 1
    fi
  else
    echo "Error: pom.xml not found and this is not a git repo."
    echo "Clone first: git clone https://github.com/prinzanaxy-max/CivicBuild.git ."
    exit 1
  fi
fi

echo "==> Installing Maven dependencies..."
chmod +x mvnw
./mvnw clean install -DskipTests

if [[ -f .env.docker.example && ! -f .env ]]; then
  echo "==> Creating .env from .env.docker.example..."
  cp .env.docker.example .env
  echo "Created .env — fill in your credentials when ready."
elif [[ -f .env ]]; then
  echo ".env already exists; leaving it unchanged."
else
  echo "Warning: .env.docker.example not found; .env was not created."
fi

echo "==> Setup complete."
