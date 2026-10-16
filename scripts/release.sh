#!/usr/bin/env bash
# Cut a release: publish the tag (not Latest yet), dispatch the Android release workflow to build
# and sign the APKs from it, then wait and check the whole thing landed.
#
# The release is created NOT-latest on purpose. Until CI has attached the APKs, /releases/latest
# keeps resolving to the previous, complete release. The workflow marks this one Latest when its
# APKs are up. A version with a `-` (1.1.0-rc.1) is a release candidate: published as a
# prerelease and never marked Latest.
#
# Usage:  scripts/release.sh "$(cat notes.md)"
# Bump version-name in gradle/libs.versions.toml, commit and push dev BEFORE running. The release
# is tagged from origin/dev, so the preflight refuses unless HEAD is exactly that.
#
# Requires: `gh` authed, the signing secrets set on the repo (see the workflow header).
set -euo pipefail
cd "$(dirname "$0")/.."

REPO="SimoHypers/limusic-android"
BRANCH="dev"
WORKFLOW="Android release"
NOTES="${1:-}"

die() { echo "ERROR: $*" >&2; exit 1; }

# semver_gt A B: A ranks above B. x.y.z numerically, then a release above its own prereleases, then
# prerelease suffixes by version sort (`sort -V` alone puts 1.1.0-rc.1 after 1.1.0).
semver_gt() {
  [ "$1" != "$2" ] || return 1
  local a="${1%%-*}" b="${2%%-*}"
  if [ "$a" != "$b" ]; then
    [ "$(printf '%s\n%s\n' "$a" "$b" | sort -V | tail -1)" = "$a" ]
    return
  fi
  [ "$1" != "$a" ] || return 0
  [ "$2" != "$b" ] || return 1
  [ "$(printf '%s\n%s\n' "${1#*-}" "${2#*-}" | sort -V | tail -1)" = "${1#*-}" ]
}

VERSION="$(sed -n 's/^version-name *= *"\(.*\)"/\1/p' gradle/libs.versions.toml)"
[[ "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-rc\.[0-9]+)?$ ]] \
  || die "version-name '$VERSION' must be X.Y.Z or X.Y.Z-rc.N (the versionCode is built from it)"
TAG="v$VERSION"
RC=0; [[ "$VERSION" != *-* ]] || RC=1
echo "==> Releasing $TAG$([ "$RC" = 0 ] || echo " (release candidate, prerelease)")"

echo "==> Preflight…"
[ -n "$NOTES" ] || die "no release notes. Usage: scripts/release.sh \"\$(cat notes.md)\""
command -v gh >/dev/null || die "gh is not installed"
gh auth status >/dev/null 2>&1 || die "gh is not authenticated. Run: gh auth login"

git diff --quiet && git diff --cached --quiet \
  || die "uncommitted changes to tracked files. Commit or stash them first"
[ "$(git branch --show-current)" = "$BRANCH" ] || die "not on $BRANCH"
git fetch --quiet origin "$BRANCH"
[ "$(git rev-parse HEAD)" = "$(git rev-parse "origin/$BRANCH")" ] \
  || die "HEAD is not origin/$BRANCH. Push first: the tag is cut from the remote"
git cat-file -e "origin/$BRANCH:.github/workflows/android-release.yml" 2>/dev/null \
  || die "the release workflow is not on origin/$BRANCH, so it can't be dispatched"

! gh release view "$TAG" --repo "$REPO" >/dev/null 2>&1 \
  || die "$TAG already exists. Bump version-name; never reuse a version"

# Above the newest published version. An RC is compared with every tag, a release with releases
# only, so a 1.0.4 fix can ship while 1.1.0-rc.2 is out.
while read -r t; do
  [ -n "$t" ] || continue
  v="${t#v}"
  if [ "$RC" = 0 ] && [[ "$v" == *-* ]]; then continue; fi
  semver_gt "$VERSION" "$v" || die "$VERSION is not above the published $v"
done < <(gh release list --repo "$REPO" --limit 50 --json tagName --jq '.[].tagName')

echo "==> Publishing $TAG (not Latest until CI attaches the APKs)…"
NOTES_FILE="$(mktemp)"; trap 'rm -f "$NOTES_FILE"' EXIT
printf '%s\n' "$NOTES" > "$NOTES_FILE"
gh release create "$TAG" --repo "$REPO" \
  --target "$(git rev-parse HEAD)" \
  --title "$TAG" \
  --notes-file "$NOTES_FILE" \
  --latest=false \
  $([ "$RC" = 1 ] && echo --prerelease)

latest_run() { gh run list --repo "$REPO" --workflow "$WORKFLOW" --limit 1 --json databaseId --jq '.[0].databaseId // 0'; }
BEFORE="$(latest_run)"
echo "==> Dispatching the build…"
gh workflow run "$WORKFLOW" --repo "$REPO" --ref "$BRANCH" -f tag="$TAG"
RUN=""
for _ in $(seq 1 30); do
  id="$(latest_run)"
  if [ "$id" != "$BEFORE" ] && [ "$id" != 0 ]; then RUN="$id"; break; fi
  sleep 2
done

echo "==> Waiting for CI (Ctrl-C is safe, the build keeps running without this terminal)…"
CI_OK=1
if [ -n "$RUN" ]; then
  gh run watch "$RUN" --repo "$REPO" --exit-status || CI_OK=0
else
  echo "    could not resolve the run id, watch it under Actions instead"; CI_OK=0
fi

OK=1
ASSETS="$(gh release view "$TAG" --repo "$REPO" --json assets --jq '.assets[].name')"
for abi in arm64-v8a armeabi-v7a x86_64 universal; do
  grep -qx "limusic-$VERSION-$abi.apk" <<<"$ASSETS" || { echo "    MISSING: limusic-$VERSION-$abi.apk"; OK=0; }
done
if [ "$RC" = 0 ]; then
  LATEST="$(gh api "repos/$REPO/releases/latest" --jq .tag_name 2>/dev/null || true)"
  [ "$LATEST" = "$TAG" ] || { echo "    NOT LATEST: /releases/latest still resolves to ${LATEST:-nothing}"; OK=0; }
fi

if [ "$CI_OK" = 1 ] && [ "$OK" = 1 ]; then
  echo "==> $TAG is complete: four APKs attached$([ "$RC" = 0 ] && echo ", marked Latest")."
else
  echo "==> $TAG IS PUBLISHED BUT INCOMPLETE. Check the run, then re-dispatch:" >&2
  echo "      gh run list --repo $REPO --limit 5" >&2
  echo "      gh workflow run \"$WORKFLOW\" --repo $REPO --ref $BRANCH -f tag=$TAG" >&2
  echo "    A re-dispatch takes the workflow from $BRANCH but the source from the tag: a fix" >&2
  echo "    anywhere outside .github/workflows/ needs a new version." >&2
  exit 1
fi
