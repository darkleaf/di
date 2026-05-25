#!/usr/bin/env bash
set -euo pipefail

src=test/darkleaf/di/tutorial
dst=doc/tutorial

mkdir -p "$dst"

for f in "$src"/*.clj; do
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
