# Project notes for Claude

## cljdoc

Docs are published on cljdoc.org. Source articles live in `doc/`:

- `doc/cljdoc.edn` — navigation tree (`{:cljdoc.doc/tree ...}`).
- `doc/integrant.md`, `doc/example.md` — checked-in articles.
- `doc/tutorial/*.md` — **generated** from `test/darkleaf/di/tutorial/*.clj`
  by `script/tutorial-to-md.sh`. Path is in `.gitignore`; the files only
  exist in CI-built release commits, never on `master`.

### Release flow

`git push origin X.Y.Z` triggers `.github/workflows/ci.yml` → `release` job:

1. Runs the tutorial-to-md script.
2. Substitutes `%TAG%` in `Readme.md` with the tag name.
3. Commits the result on a detached HEAD.
4. Tags that commit `cljdoc-X.Y.Z` and pushes the tag (only the tag; the
   commit itself is not on any branch).
5. Builds the jar with `RELEASE_VERSION=X.Y.Z` and deploys to Clojars.

The deployed pom's `<scm><tag>` is the SHA of the detached commit, so
cljdoc fetches docs from there (not from `master`).

### Updating docs without a new release

cljdoc supports a `cljdoc-<VERSION>` tag override: it imports articles and
`cljdoc.edn` from that tag instead of the SCM commit. This affects only
articles and TOC — docstrings still come from the published jar.

**Critical gotcha:** the new commit you tag MUST be a descendant of the
original CI-generated `cljdoc-<VERSION>` commit (whose SHA is hard-coded
in the deployed pom). If you make the new commit on top of `master`,
GitHub eventually GCs the orphaned SCM SHA, cljdoc clone fails with
`unknown-revision`, and the build silently produces a docs page with no
articles.

Correct procedure:

```
# 1. Make the article changes on master (so they live in repo history)
git commit -m "..."
git push origin master

# 2. Rebuild the cljdoc-<VERSION> commit on top of the prior one
git worktree add --detach /tmp/wt cljdoc-X.Y.Z   # or the prior SHA
cp doc/<changed-files> /tmp/wt/doc/
(cd /tmp/wt && git add . && \
   git -c commit.gpgsign=false commit -m "..." && \
   git -c tag.gpgsign=false tag -f cljdoc-X.Y.Z)

# 3. Force-push the moved tag
git push origin :refs/tags/cljdoc-X.Y.Z
git push origin cljdoc-X.Y.Z
git worktree remove /tmp/wt
```

Then trigger a rebuild on `https://cljdoc.org/d/org.clojars.darkleaf/di/X.Y.Z`.

### Article cross-links

cljdoc rewrites markdown links between articles. Use either:

- relative to the source file: `tutorial/a_intro_test.md` (from `doc/example.md`)
- root-relative: `/doc/tutorial/a_intro_test.md`

A bare `doc/tutorial/a_intro_test.md` (no leading slash) is **not**
recognised and renders as a broken external link.

## Release/build gotchas (not cljdoc-specific but related)

- `build.clj` reads version from `RELEASE_VERSION` env var. Local builds
  produce a `DEV-SNAPSHOT` jar.
- The published pom must declare `org.clojure/clojure` explicitly in root
  `:deps` of `deps.edn`. Otherwise tools.build's basis inherits Clojure
  1.10.3 from the system `deps.edn`, the pom advertises 1.10.3, and the
  cljdoc analyzer launches with 1.10.3 — which does not understand
  `:as-alias` and fails with a fake cyclic-load error.
- The published pom must include a `<licenses>` block (Clojars rejects
  uploads without one with `403 Forbidden`). Done via `:pom-data` in
  `b/write-pom`; requires `tools.build` ≥ 0.10.
- `deps-deploy` reads the pom from the filesystem, not from inside the
  jar. The `:deploy` alias passes `:pom-file
  "target/classes/META-INF/maven/org.clojars.darkleaf/di/pom.xml"` so it
  finds the one `b/write-pom` produced.
- Local git config has `tag.gpgsign=true`. To make a release-style
  lightweight tag, override per-command:
  `git -c tag.gpgsign=false tag X.Y.Z`.
- Branch `master` is protected (PRs required). Direct pushes work
  because the user has admin bypass, but each one logs a "Bypassed rule
  violations" entry.

## Clojars

Once a non-snapshot version is deployed, it cannot be re-deployed even
if the previous attempt failed validation. A burned version (`3.6.1`
was an example: missing license blew up after the upload) requires
bumping to a fresh version, not re-attempting the same tag.

A deploy token is required (not the account password). Stored in
GitHub Actions secrets as `CLOJARS_USERNAME` / `CLOJARS_PASSWORD`.
