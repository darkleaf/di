# Project notes for Claude

## Workflow

- **Commit only when the user asks.** Finish the work, report the
  result, and leave the changes in the working tree. This includes
  amends.
- **Do not `git push origin master` automatically.** Commit locally and
  wait for the user to push. `master` is protected (PRs required) and
  direct pushes only work via admin bypass, so each one is a deliberate
  choice the user makes.
- Always include the `Co-Authored-By: <% ACTUAL MODEL %> <noreply@anthropic.com>` trailer in commits authored together.

## cljdoc

Docs are published on cljdoc.org. Source articles live in `doc/`:

- `doc/cljdoc.edn` — navigation tree (`{:cljdoc.doc/tree ...}`).
- `doc/integrant.md`, `doc/example.md` — checked-in articles.
- `doc/tutorial/*.md` and `doc/how_to/*.md` — **generated** from the
  matching `test/darkleaf/di/{tutorial,how_to}/*.clj` files by
  `script/tutorial-to-md.sh`. Paths are gitignored; the files only
  exist in CI-built release commits.
- `doc/reference/*.md` comes in two flavours:
  - `doc/reference/<slug>_test.md` — **generated** from
    `test/darkleaf/di/reference/<slug>_test.clj` by the same script
    (e.g. `inspect_test.md`). Gitignored via `/doc/reference/*_test.md`;
    exists only in CI-built release commits.
  - `doc/reference/<slug>.md` (no `_test` suffix) — **plain tracked
    markdown**, not generated. Descriptive prose; verified examples
    live in regular tests (e.g. `dependency_types_test.clj` for the
    Factory protocol page).

### Local preview

cljdoc renders locally via its Docker image (verified 2026-07-01).
The steps, and the gotchas that cost time:

1. Build and install the jar to `~/.m2` — cljdoc reads API
   docstrings from there:

   ```
   clojure -T:dev:build              # produces target/di.jar + pom (DEV-SNAPSHOT)
   clojure -X:dev:deploy :installer :local   # installs DEV-SNAPSHOT to ~/.m2
   ```

2. **cljdoc reads articles from a git revision, not the working
   tree.** The generated `*_test.md` are gitignored, so a normal
   commit does not contain them and the preview shows a TOC with no
   article bodies. Make a throwaway commit that force-adds them
   (mirrors the CI `git add -f`), ingest that SHA, then reset it
   away afterwards — never push it:

   ```
   bash script/tutorial-to-md.sh
   git add -f doc/tutorial doc/how_to doc/reference
   git -c commit.gpgsign=false commit -m "TEMP preview (do not push)"
   ```

3. Start the server, then ingest the temp SHA:

   ```
   docker run -d --name cljdoc-preview -p 8000:8000 \
     -v "$HOME/.m2:/root/.m2" -v /tmp/cljdoc-preview:/app/data \
     --platform linux/amd64 cljdoc/cljdoc

   docker run --rm -v "$(pwd):/repo-to-import" \
     -v "$HOME/.m2:/root/.m2" -v /tmp/cljdoc-preview:/app/data \
     --platform linux/amd64 --entrypoint clojure cljdoc/cljdoc \
     -Sforce -M:cli ingest --project org.clojars.darkleaf/di \
     --version DEV-SNAPSHOT --git /repo-to-import --rev "$(git rev-parse HEAD)"
   ```

   Read it at `http://localhost:8000/d/org.clojars.darkleaf/di/DEV-SNAPSHOT`
   (`/d/...` 302-redirects to the first article — that is normal).

4. Clean up: `git reset --soft <real-commit>` then
   `git restore --staged doc/tutorial doc/how_to doc/reference`
   (returns the generated md to gitignored/untracked), and
   `docker rm -f cljdoc-preview`.

**Images did not render under a local-path ingest** (observed
2026-07-01). Whether that is a cljdoc bug or intended behaviour I did
not confirm — worth checking upstream before relying on it. What was
observed: cljdoc rewrites a root-relative `/doc/images/x.svg` to the
SCM's raw URL (`cljdoc.util.scm/rev-raw-base-url` → `<url>/raw/<rev>/…`).
With `--git /repo-to-import` that `<url>` is the local path, so the
`<img>` resolved to `/repo-to-import/raw/<sha>/…`, which the server
did not serve (404). The rewrite target on cljdoc.org would instead be
the GitHub repo from the pom (`https://github.com/darkleaf/di/raw/<sha>/…`),
which should load once the commit is pushed — but this was not
verified end-to-end. The markdown reference (`![](/doc/images/…)`)
follows convention and the file is valid, so the local 404 is at
least not caused by the docs themselves. To preview an image locally,
try ingesting with `--git https://github.com/darkleaf/di` on an
already-pushed rev.

Inlining the image is **not** an option: cljdoc's HTML sanitizer
(`cljdoc.render.sanitize`) allows `<img>` only with an `http`/`https`
`src` (no `data:` URIs) and does not allow the `<svg>` tag at all.
An image must be an http(s) URL.

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

- relative to the source file: `tutorial/a_your_first_system_test.md` (from `doc/example.md`)
- root-relative: `/doc/tutorial/a_your_first_system_test.md`

A bare `doc/tutorial/a_your_first_system_test.md` (no leading slash)
is **not** recognised and renders as a broken external link.

For an article-to-API-var link, use the full cljdoc URL with the
`CURRENT` placeholder — cljdoc rewrites `CURRENT` to the version
the reader is viewing:

```
[`di/->memoize`](https://cljdoc.org/d/org.clojars.darkleaf/di/CURRENT/api/darkleaf.di.core#->memoize)
```

Wikilinks (`[[ns/var]]`) only work inside docstrings, not in
articles.

## Documentation conventions

Settled during the v6 restructure.

### Audience

- Primary: a Clojure developer familiar with Integrant or Component.
- Secondary: a Clojure developer who has never used a DI framework.
  Don't assume Integrant knowledge in chapter bodies.
- Many readers are non-native English speakers. Optimise prose
  for them: short sentences, no English `;` in prose (use
  periods), no obscure idioms (`slip past it` is the kind that
  trips people up; everyday phrasing like *side effect* or
  *before any traffic* is fine).

### Voice

- Matter-of-fact, declarative. Match `doc/integrant.md` and
  Stuart Sierra's *Reloaded Workflow* tone. No marketing language.
- No comparisons to other DI libraries inside tutorial or how-to
  chapters. The Integrant comparison lives in `doc/integrant.md`
  on its own.

### Terminology

- **"Middleware"** is introduced once, in the tutorial's Registries
  chapter (`e_registries_test`), with a one-line definition and a
  link to `doc/reference/middleware_argument.md`. After that point,
  use the word plainly wherever it is the natural term. The first
  four chapters (A–D) pass no such arguments, so the word does not
  come up there. In how-to recipes use it freely, linking to the
  reference on first use because recipes are read out of order.
  (An earlier draft banned the word outright. That backfired: it
  forced vague paraphrases like "shapes `di/start` accepts", and it
  contradicted the reference page's own title, "The middleware
  argument". Naming the concept once and then using it is clearer
  for non-native readers than paraphrasing around it.)
- **A key names a "component" of the system, never a "node".**
  The parts of a system are its components. Do not call them nodes
  (or "things") in docs.
- **Math-style names** (`a`, `b`, `c`, …) are the project's
  authorial style. Keep them — don't substitute concrete names
  without a reason.
- **Keyword vs symbol** is about *intent*, not swap-ability. Both
  can be substituted via the registry. A keyword means the author
  decided to abstract the dependency (most commonly inside a
  library or reusable internal module). A symbol points at a
  specific var.

### Directory layout

- Tutorial chapters: `test/darkleaf/di/tutorial/[a-l]_<slug>_test.clj`.
  Letter prefix `a..l` matches chapter order (1–12) alphabetically.
- How-to recipes: `test/darkleaf/di/how_to/<slug>_test.clj`. No
  order prefix.
- Reference pages: either plain tracked markdown at
  `doc/reference/<slug>.md`, or generated from
  `test/darkleaf/di/reference/<slug>_test.clj` (output
  `doc/reference/<slug>_test.md`, gitignored). Use a generated page
  when the reference is example-heavy and the examples should be
  verified by the test suite (e.g. `inspect`).
- When adding a new doc subdirectory, also update
  `script/tutorial-to-md.sh` (it iterates `tutorial`, `how_to`,
  `reference`) and the `git add -f` line in
  `.github/workflows/ci.yml` release job.
- `*.clj.disabled` files (e.g.
  `test/darkleaf/di/tutorial/x_instrument_test.clj.disabled`,
  `x_override_deps_test.clj.disabled`) are **parked drafts** —
  intentionally out of the build and unpublished. Leave them
  alone: do not re-enable, edit, delete, or generate docs from
  them unless the user explicitly asks.
- Images referenced from articles go in `doc/images/` (a tracked
  directory, unlike the gitignored generated subdirs). Reference
  them root-relative, e.g. `/doc/images/<name>.svg`.

### Test idioms in chapter files

These conventions apply to tutorial and how-to chapter `.clj`
files. Regular tests under `test/darkleaf/di/*_test.clj` should
stay strict — prefer object-identity comparison there so subtle
regressions don't slip through.

- Use `darkleaf.di.utils/catch-some` plus `ex-message` / `ex-data`
  for exception assertions. Compare messages and structured data,
  not exception objects by identity.
- Inline `(ex-info "..." {})` constructions where they fire — do
  not pass exceptions through the registry just to assert on
  them later.
- Add `;; ...` comments above non-obvious assertions to explain
  what they verify.

### Reference pages

- A Reference page earns its keep when it adds material the
  docstring does not: decision-trees, walks through code-level
  patterns, design history, pitfall lists, aggregations across
  multiple sources.
- Avoid duplicating the docstring 1-for-1.

### Recurring mistakes to avoid

Lessons paid for during the design-doc work. The cross-cutting
failure mode is *confident-but-wrong*: smooth prose that isn't
grounded in the code or in the author's actual model. Guard against
each of these.

1. **Author's model over clever framing.** Don't reach for an
   impressive abstraction (category theory, neat dualities,
   phase taxonomies) before checking how the maintainer actually
   thinks about it. Examples that were wrong: "a system is a
   key→object map" (it is the built root object), "compile/build/run
   time" (you don't write components at compile time), the monad
   gloss (cut). State the simplest *true* thing.
2. **No unverified claims, especially superlatives.** Check the
   code before writing "always / never / cannot / static". Wrong
   ones shipped: "Ring middleware always delegates" (it can
   short-circuit), "dependencies is the static schema" (it may be
   computed; the rule is that it is pure and stable). Prefer a
   precise weak claim over a strong vague one.
3. **Use the project's settled terms.** A key names a **component**,
   never a "node". Introduce "middleware" once (Registries chapter),
   then use it plainly — do not paraphrase around it. Avoid
   off-register words ("schemas") and idioms ("earns its keep" —
   non-native readers). Persist any terminology correction to this
   file immediately so it does not recur.
4. **No duplication or padding.** Re-read your own output for ideas
   repeated in adjacent paragraphs, comments that restate a bullet
   list, and enumerations that add no information.
5. **Hold one altitude.** A design doc explains *how it is built and
   why*, not *how to use it*. Don't mix in API/usage notes at equal
   weight, and make every example serve the section's actual point.
6. **Run a mechanical pass before "done".** No `;` in prose;
   fix links after any file rename; put backtick-quoted symbols
   (`` `foo ``) in fenced blocks, not inline (they break Markdown).
7. **Step back, don't only polish.** For an important doc, do the
   grounding up front — read the code, tests, git history, relevant
   PRs, and reference docs from respected libraries — and
   periodically question the whole structure instead of line-editing
   a local optimum.

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
