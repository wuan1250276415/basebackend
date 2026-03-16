#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMMON_DIR="${ROOT_DIR}/basebackend-common"

if [[ ! -d "${COMMON_DIR}" ]]; then
  echo "未找到目录: ${COMMON_DIR}" >&2
  exit 2
fi

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "${TMP_DIR}"' EXIT

# 从目录扫描当前真实存在的 common 子模块（作为基线集合）
collect_fs_modules() {
  find "${COMMON_DIR}" -mindepth 1 -maxdepth 1 -type d -name "basebackend-common-*" \
    -exec test -f "{}/pom.xml" ";" -print \
    | sed 's#^.*/##' \
    | LC_ALL=C sort -u
}

# 提取 basebackend-common/pom.xml 中 <modules> 的模块声明
collect_pom_modules() {
  sed -n '/<modules>/,/<\/modules>/p' "${COMMON_DIR}/pom.xml" \
    | sed -n 's#.*<module>\(basebackend-common-[^<]*\)</module>.*#\1#p' \
    | LC_ALL=C sort -u
}

# 提取 basebackend-common/pom.xml 中 dependencyManagement 的 common 模块 artifactId
collect_dependency_management_modules() {
  sed -n '/<dependencyManagement>/,/<\/dependencyManagement>/p' "${COMMON_DIR}/pom.xml" \
    | sed -n 's#.*<artifactId>\(basebackend-common-[^<]*\)</artifactId>.*#\1#p' \
    | LC_ALL=C sort -u
}

# 从文档中提取出现的 common 模块名（集合比对）
collect_doc_modules() {
  local doc_file="$1"
  if [[ ! -f "${doc_file}" ]]; then
    return 0
  fi

  if command -v rg >/dev/null 2>&1; then
    rg -o --no-filename 'basebackend-common-[a-z0-9-]+' "${doc_file}" \
      | LC_ALL=C sort -u || true
    return 0
  fi

  grep -Eo 'basebackend-common-[a-z0-9-]+' "${doc_file}" \
    | LC_ALL=C sort -u || true
}

compare_with_baseline() {
  local target_name="$1"
  local baseline_file="$2"
  local target_file="$3"

  local missing
  local extra
  missing="$(comm -23 "${baseline_file}" "${target_file}" || true)"
  extra="$(comm -13 "${baseline_file}" "${target_file}" || true)"

  if [[ -z "${missing}" && -z "${extra}" ]]; then
    echo "✅ ${target_name} 与目录基线一致"
    return 0
  fi

  echo "❌ ${target_name} 与目录基线不一致"
  if [[ -n "${missing}" ]]; then
    echo "  - 缺失模块："
    while IFS= read -r module; do
      [[ -z "${module}" ]] || echo "    - ${module}"
    done <<< "${missing}"
  fi
  if [[ -n "${extra}" ]]; then
    echo "  - 多余模块："
    while IFS= read -r module; do
      [[ -z "${module}" ]] || echo "    - ${module}"
    done <<< "${extra}"
  fi
  return 1
}

BASELINE_FILE="${TMP_DIR}/modules.fs.txt"
POM_MODULES_FILE="${TMP_DIR}/modules.pom.txt"
DM_MODULES_FILE="${TMP_DIR}/modules.dependency-management.txt"
README_MODULES_FILE="${TMP_DIR}/modules.readme.txt"
CLAUDE_MODULES_FILE="${TMP_DIR}/modules.claude.txt"

collect_fs_modules > "${BASELINE_FILE}"
collect_pom_modules > "${POM_MODULES_FILE}"
collect_dependency_management_modules > "${DM_MODULES_FILE}"
collect_doc_modules "${COMMON_DIR}/README.md" > "${README_MODULES_FILE}"
collect_doc_modules "${COMMON_DIR}/CLAUDE.md" > "${CLAUDE_MODULES_FILE}"

if [[ ! -s "${BASELINE_FILE}" ]]; then
  echo "未扫描到任何 common 子模块，请检查目录结构。" >&2
  exit 2
fi

echo "=== basebackend-common 模块一致性检查 ==="
echo "目录基线模块数: $(wc -l < "${BASELINE_FILE}" | tr -d ' ')"
echo ""

FAILED=0

compare_with_baseline "pom.xml <modules>" "${BASELINE_FILE}" "${POM_MODULES_FILE}" || FAILED=1
compare_with_baseline "pom.xml <dependencyManagement>" "${BASELINE_FILE}" "${DM_MODULES_FILE}" || FAILED=1
compare_with_baseline "README.md 模块清单" "${BASELINE_FILE}" "${README_MODULES_FILE}" || FAILED=1
compare_with_baseline "CLAUDE.md 模块清单" "${BASELINE_FILE}" "${CLAUDE_MODULES_FILE}" || FAILED=1

if [[ "${FAILED}" -ne 0 ]]; then
  echo ""
  echo "模块一致性检查失败，请同步目录、pom 与文档清单。"
  exit 1
fi

echo ""
echo "模块一致性检查通过。"
