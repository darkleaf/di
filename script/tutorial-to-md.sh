#!/usr/bin/env bash
set -euo pipefail

convert () {
  local src=$1 dst=$2
  mkdir -p "$dst"
  for f in "$src"/*.clj; do
    [ -e "$f" ] || continue
    name=$(basename "$f" .clj)
    awk '
      /^;;/ {
        if (in_code) {
          print "```"; in_code = 0
          if (blanks == 0) blanks = 1
        }
        for (i = 0; i < blanks; i++) print ""
        blanks = 0
        sub(/^;; ?/, ""); print; emitted = 1; next
      }
      /^[[:space:]]*$/ { blanks++; next }
      {
        if (!in_code) {
          if (emitted && blanks == 0) blanks = 1
          for (i = 0; i < blanks; i++) print ""
          blanks = 0
          print "```clojure"; in_code = 1
        } else {
          for (i = 0; i < blanks; i++) print ""
          blanks = 0
        }
        print; emitted = 1
      }
      END { if (in_code) print "```" }
    ' "$f" > "$dst/${name}.md"
  done
}

convert test/darkleaf/di/tutorial  doc/tutorial
convert test/darkleaf/di/how_to    doc/how_to
convert test/darkleaf/di/reference doc/reference
