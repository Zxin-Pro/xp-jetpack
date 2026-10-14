#!/usr/bin/env bash
# =============================================================================
# XP Jetpack · 一键推送到 GitHub
#
# 用法：
#   export GITHUB_USERNAME="你的用户名"
#   export GITHUB_REPO="xp-jetpack"
#   export GITHUB_TOKEN="ghp_xxx"          # 需要 repo 权限（classic 或 fine-grained）
#   bash push_to_github.sh
#
# 做四件事：
#   1. 创建（或复用）远端仓库
#   2. 把 22 个版本分支分别推上去（每个分支是独立的 Gradle 项目）
#   3. 推送 main（README / VERSIONS / ARCHITECTURE / CI）
#   4. 打印 Actions 地址
#
# 目录要求（与本脚本同级）：
#   out/<branch>/    —— 22 个分支（由 tools/gen.py 生成）
#   main/            —— main 分支内容
# =============================================================================
set -euo pipefail

: "${GITHUB_USERNAME:?请先 export GITHUB_USERNAME}"
: "${GITHUB_REPO:=xp-jetpack}"
: "${GITHUB_TOKEN:?请先 export GITHUB_TOKEN（不要硬编码进脚本）}"

ROOT="$(cd "$(dirname "$0")" && pwd)"
WORK="$(mktemp -d)"
REMOTE="https://${GITHUB_USERNAME}:${GITHUB_TOKEN}@github.com/${GITHUB_USERNAME}/${GITHUB_REPO}.git"

echo "==> 1/4 创建仓库 ${GITHUB_USERNAME}/${GITHUB_REPO}"
if gh repo view "${GITHUB_USERNAME}/${GITHUB_REPO}" >/dev/null 2>&1; then
  echo "    仓库已存在，跳过创建"
else
  gh repo create "${GITHUB_USERNAME}/${GITHUB_REPO}" --public \
    --description "XP Jetpack · 把玩家经验压缩成空气的指令喷气背包（Forge / NeoForge / Fabric，1.20.1 → 26.3）"
fi

push_branch() {
  local branch="$1" src="$2" msg="$3"
  echo "==> 推送 ${branch}"
  rm -rf "${WORK:?}/${branch}"
  mkdir -p "${WORK}/${branch}"
  cp -a "${src}/." "${WORK}/${branch}/"
  (
    cd "${WORK}/${branch}"
    git init -q -b "${branch}"
    git config user.email "noreply@github.com"
    git config user.name "${GITHUB_USERNAME}"
    git add -A
    git commit -q -m "${msg}"
    git remote add origin "${REMOTE}"
    git push -q -f origin "${branch}"
  )
}

echo "==> 2/4 推送 22 个版本分支"
for dir in "${ROOT}"/out/*/; do
  branch="$(basename "${dir}")"
  push_branch "${branch}" "${dir}" "feat(${branch}): XP Jetpack for ${branch#*-}"
done

echo "==> 3/4 推送 main"
push_branch "main" "${ROOT}/main" "docs: XP Jetpack 多版本项目说明、版本支持表与 CI"

echo "==> 4/4 完成"
echo "    仓库:   https://github.com/${GITHUB_USERNAME}/${GITHUB_REPO}"
echo "    Actions: https://github.com/${GITHUB_USERNAME}/${GITHUB_REPO}/actions"
echo "    分支列表: https://github.com/${GITHUB_USERNAME}/${GITHUB_REPO}/branches"
