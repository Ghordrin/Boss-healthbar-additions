#!/usr/bin/env bash
# Compares the file i/o surface of a commit against the one last submitted to the plugin hub.
#
# The hub's reviewer marks any plugin that performs file i/o for review by a maintainer, and this plugin
# always will: it writes a settings export and a debug log, and reads a file the user picks. The notice is
# not something to avoid. What matters is that a release adds nothing new, so a maintainer comparing it with
# the version already on the hub finds the same work or less.
#
# Run before opening a plugin-hub pull request:
#   tools/check-io-surface.sh                      # HEAD against the default baseline
#   tools/check-io-surface.sh <ref>                # a ref against the default baseline
#   tools/check-io-surface.sh <ref> <baseline-ref>  # both given
#
# Exits non-zero when the surface grew or a forbidden pattern turned up.

set -uo pipefail

REF="${1:-HEAD}"
# The commit the open hub pull request points at. Update this when a release is merged.
BASE="${2:-0986413}"

cd "$(dirname "$0")/.." || exit 2

fail=0

imports() { git grep -hE "^import (java\.(io|nio)|com\.google\.common\.io)" "$1" -- src/main 2>/dev/null | sort | uniq -c; }

# Only the calls that reach the filesystem, so a rename of something unrelated doesn't read as a change.
# write( is counted separately: ConfigWrites.write and Java2D's moveTo share those names, so the plain
# grep is useless, but a new Filepath.write is the likeliest way file i/o grows and has to be caught.
operations() {
	git grep -hoE "\.(openReader|openBufferedReader|openInputStream|openOutputStream|openWriter|openFileChannel|createDirectories|createDirectory|deleteIfExists|deleteRecursively|createTempFile|createTempDir|walk)\(" \
		"$1" -- src/main 2>/dev/null | sort | uniq -c
	git grep -hE "\.write\(" "$1" -- src/main 2>/dev/null \
		| grep -v "ConfigWrites" \
		| sed -E 's/.*(\.write\().*/     \1 not through ConfigWrites/' | sort | uniq -c
}

# Ways round the Filepath API. Any hit is a finding on its own, whatever the baseline holds.
FORBIDDEN='Filepath\.Unchecked|getLegacyPluginDirectory|RUNELITE_DIR|JFileChooser|java\.nio\.file|new File\(|Files\.(read|write|copy|move|delete|newBufferedReader|newBufferedWriter|createDirector)'

section() { printf '\n== %s ==\n' "$1"; }

section "imports, baseline $BASE"
imports "$BASE"
section "imports, $REF"
imports "$REF"

if ! diff <(imports "$BASE") <(imports "$REF") > /dev/null; then
	grew=$(diff <(imports "$BASE") <(imports "$REF") | grep '^>' || true)
	if [ -n "$grew" ]; then
		printf '\nCHANGED - lines only in %s:\n%s\n' "$REF" "$grew"
		fail=1
	else
		printf '\nSmaller than the baseline, which is fine.\n'
	fi
fi

section "filesystem calls, baseline $BASE"
operations "$BASE"
section "filesystem calls, $REF"
operations "$REF"

if ! diff <(operations "$BASE") <(operations "$REF") > /dev/null; then
	printf '\nCHANGED - the filesystem calls differ from the baseline:\n'
	diff <(operations "$BASE") <(operations "$REF") || true
	fail=1
fi

section "ways round the Filepath API in $REF"
if git grep -nE "$FORBIDDEN" "$REF" -- src/main 2>/dev/null; then
	printf '\nFOUND - every file touch has to go through Filepath from getPluginDirectory().\n'
	fail=1
else
	printf 'none\n'
fi

if [ "$fail" -eq 0 ]; then
	printf '\nOK: %s adds no file i/o over %s.\n' "$REF" "$BASE"
else
	printf '\nReview the findings above before submitting.\n'
fi

exit "$fail"
