# Project notes for Claude

## Workflow

- **Commit only when the user asks** (amends included). Finish the
  work, leave changes in the working tree, and don't announce this
  default — mention git state only when surprising or asked.
- **Never `git push origin master` yourself.** `master` is protected
  (PRs required); direct pushes work only via the user's admin bypass,
  so each one is their deliberate choice.
- Commit trailer: `Co-Authored-By: <% ACTUAL MODEL %> <noreply@anthropic.com>`.

## cljdoc

Docs are published on cljdoc.org. Sources in `doc/`:

- `doc/cljdoc.edn` — navigation tree (`{:cljdoc.doc/tree ...}`).
- `doc/integrant.md`, `doc/example.md` — tracked articles.
- `doc/tutorial/*.md`, `doc/how_to/*.md`, `doc/reference/<slug>_test.md`
  — **generated** from the matching `test/darkleaf/di/...` files by
  `script/tutorial-to-md.sh`. Gitignored; exist only in CI-built
  release commits.
- `doc/reference/<slug>.md` (no `_test` suffix) — plain tracked
  markdown. Descriptive prose; verified examples live in regular tests
  (e.g. `dependency_types_test.clj` for the Factory protocol page).

### Local preview

Via the cljdoc Docker image (verified 2026-07-01):

1. Build and install the jar to `~/.m2` (cljdoc reads docstrings
   from there):

   ```
   clojure -T:dev:build
   clojure -X:dev:deploy :installer :local
   ```

2. **cljdoc reads articles from a git revision, not the working
   tree**, and the generated `*_test.md` are gitignored — so make a
   throwaway commit that force-adds them (mirrors CI), ingest that
   SHA, reset it away afterwards, never push it:

   ```
   bash script/tutorial-to-md.sh
   git add -f doc/tutorial doc/how_to doc/reference
   git -c commit.gpgsign=false commit -m "TEMP preview (do not push)"
   ```

3. Start the server and ingest:

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

   Read at `http://localhost:8000/d/org.clojars.darkleaf/di/DEV-SNAPSHOT`
   (302 to the first article is normal).

4. Clean up: `git reset --soft <real-commit>`, then
   `git restore --staged doc/tutorial doc/how_to doc/reference`,
   and `docker rm -f cljdoc-preview`.

**Images 404 under a local-path ingest** (observed 2026-07-01).
cljdoc rewrites root-relative `/doc/images/x.svg` to `<scm-url>/raw/<rev>/…`;
with `--git /repo-to-import` that URL is a local path the server does
not serve. On cljdoc.org the rewrite target is the GitHub raw URL from
the pom, which should work once pushed (not verified end-to-end). To
preview an image locally, ingest with `--git https://github.com/darkleaf/di`
on a pushed rev. Inlining is not an option: cljdoc's sanitizer allows
`<img>` only with http(s) `src` (no `data:` URIs, no `<svg>`).

### Release flow

`git push origin X.Y.Z` triggers `.github/workflows/ci.yml` → `release`:

1. Runs the tutorial-to-md script.
2. Substitutes `%TAG%` in `Readme.md` with the tag name.
3. Commits the result on a detached HEAD.
4. Tags that commit `cljdoc-X.Y.Z` and pushes only the tag (the commit
   is on no branch).
5. Builds the jar with `RELEASE_VERSION=X.Y.Z` and deploys to Clojars.

The deployed pom's `<scm><tag>` is the detached commit's SHA, so
cljdoc fetches docs from there, not from `master`.

### Updating docs without a new release

cljdoc imports articles and `cljdoc.edn` from a `cljdoc-<VERSION>` tag
override (docstrings still come from the jar).

**Critical gotcha:** the new tagged commit MUST be a descendant of the
original CI-generated `cljdoc-<VERSION>` commit (its SHA is hard-coded
in the deployed pom). A commit on top of `master` orphans that SHA;
GitHub GCs it, cljdoc clone fails with `unknown-revision`, and the
build silently loses all articles.

```
# 1. Land the article changes on master (repo history)
git commit -m "..." && git push origin master

# 2. Rebuild the cljdoc commit on top of the prior one
git worktree add --detach /tmp/wt cljdoc-X.Y.Z
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

- Between articles: relative to the source file
  (`tutorial/a_your_first_system_test.md`) or root-relative
  (`/doc/tutorial/...md`). A bare `doc/...` path (no leading slash)
  renders as a broken external link.
- Article → API var: full cljdoc URL with the `CURRENT` version
  placeholder, e.g.
  `https://cljdoc.org/d/org.clojars.darkleaf/di/CURRENT/api/darkleaf.di.core#->memoize`.
- Wikilinks (`[[ns/var]]`) work only in docstrings, not articles.

## Documentation conventions

Settled during the v6 restructure.

### Audience and voice

- Primary: a Clojure developer familiar with Integrant or Component.
  Secondary: one who has never used a DI framework — don't assume
  Integrant knowledge in chapter bodies.
- Many readers are non-native English speakers: short sentences, no
  `;` in prose (use periods), no obscure idioms.
- Matter-of-fact, declarative; match `doc/integrant.md` and Stuart
  Sierra's *Reloaded Workflow* tone. No marketing language.
- No comparisons to other DI libraries in tutorial or how-to chapters;
  the Integrant comparison lives only in `doc/integrant.md`.

### Terminology

- **"Middleware"** is introduced once, in the tutorial's Registries
  chapter (`e_registries_test`), with a one-line definition and a link
  to `doc/reference/middleware_argument.md`; after that use it plainly.
  Chapters A–D pass no such arguments, so it does not come up there.
  In how-to recipes use it freely, linking the reference on first use
  (recipes are read out of order). Banning the word backfired — it
  forced vague paraphrases; naming a concept once beats paraphrasing.
- **A key names a "component"** of the system, never a "node" or
  "thing".
- **Math-style names** (`a`, `b`, `c`, …) are the authorial style —
  keep them.
- **Keyword vs symbol** is about *intent*, not swap-ability (both are
  substitutable via the registry). A keyword means the author chose to
  abstract the dependency; a symbol points at a specific var.

### Directory layout

- Tutorial: `test/darkleaf/di/tutorial/[a-l]_<slug>_test.clj` — letter
  prefix matches chapter order.
- How-to: `test/darkleaf/di/how_to/<slug>_test.clj`, no prefix.
- Reference: tracked `doc/reference/<slug>.md`, or generated from
  `test/darkleaf/di/reference/<slug>_test.clj` when example-heavy and
  the examples should be test-verified (e.g. `inspect`).
- A new doc subdirectory also needs `script/tutorial-to-md.sh` (its
  directory list) and the `git add -f` line in the ci.yml release job.
- `*.clj.disabled` files are **parked drafts** — leave them alone
  unless the user explicitly asks.
- Images go in tracked `doc/images/`; reference root-relative
  (`/doc/images/<name>.svg`).

### Test idioms in chapter files

Applies to tutorial/how-to chapters. Regular tests stay strict
(object-identity comparison).

- `darkleaf.di.utils/catch-some` + `ex-message`/`ex-data` for
  exception assertions; compare messages and data, not identity.
- Inline `(ex-info "..." {})` where it fires — don't pass exceptions
  through the registry to assert later.
- `;; ...` comments above non-obvious assertions.

### Reference pages

Earn their place by adding what the docstring lacks: decision-trees,
code-level patterns, design history, pitfall lists, aggregations.
Never duplicate the docstring 1-for-1.

### Recurring mistakes to avoid

Cross-cutting failure mode: *confident-but-wrong* — smooth prose not
grounded in the code or the maintainer's actual model.

1. **Author's model over clever framing.** Check how the maintainer
   thinks before reaching for an abstraction. Wrong: "a system is a
   key→object map" (it is the built root object), "compile/build/run
   time", a monad gloss. State the simplest *true* thing.
2. **No unverified claims, especially superlatives.** Check the code
   before "always / never / cannot / static". Prefer a precise weak
   claim over a strong vague one.
3. **Use the settled terms** (see Terminology). Avoid off-register
   words ("schemas") and idioms ("earns its keep"). Persist any
   terminology correction to this file immediately.
4. **No duplication or padding.** Re-read output for ideas repeated in
   adjacent paragraphs and enumerations that add nothing.
5. **Hold one altitude.** A design doc explains *how it is built and
   why*, not *how to use it*; every example serves its section's point.
6. **Mechanical pass before "done".** No `;` in prose; fix links after
   renames; backtick-quoted symbols go in fenced blocks, not inline.
7. **Step back, don't only polish.** Ground in code, tests, git
   history, and respected references up front; periodically question
   the whole structure instead of line-editing a local optimum.
8. **Real motivations, not plausible ones.** State the actual
   operational reason a pattern exists — what it saves the user or
   operator (wrong: "geoip db too heavy to build"; right: "a disabled
   feature must not force the operator to download and configure it").
   Same for assertions: say what the check buys in operation. If the
   reason is unknown, ask — don't fill the gap with smooth text.
9. **Name things at first mention.** "Enables two features" — which?
   Enumerate in the same sentence.
10. **Show structure, don't label it.** Arrange code and sections so
    layout carries the message. A disclaimer excusing code's location
    ("in a real project this lives in ...") is a structural defect —
    rearrange to remove the excuse instead of writing it.
11. **A review comment names an instance, not the disease.** After
    fixing the flagged spot, sweep the whole piece for the same
    failure mode — including ones the fix itself introduces.
12. **Advice must survive real scale.** Ask "does this survive
    thousands of keys?" (wrong: "assert on the whole key set of the
    plan"). Label toy scale explicitly, and check whether the text
    already contains the real solution further down — reorder, don't
    add.
13. **Demonstrate through the natural structure, not scaffolding.**
    If an assertion needs an extra root, extra wiring, or a sentence
    explaining the odd setup, it is aimed at the wrong thing (wrong:
    passing geoip to `di/inspect` as an artificial extra root;
    inspecting from the real root made the stronger, honest claim).

## Release/build gotchas

- `build.clj` reads the version from `RELEASE_VERSION`; local builds
  produce `DEV-SNAPSHOT`.
- Root `:deps` of `deps.edn` must declare `org.clojure/clojure`
  explicitly — otherwise the pom inherits 1.10.3 from the system
  deps.edn and the cljdoc analyzer fails on `:as-alias` with a fake
  cyclic-load error.
- The pom must include a `<licenses>` block (Clojars rejects with 403).
  Done via `:pom-data` in `b/write-pom`; needs `tools.build` ≥ 0.10.
- `deps-deploy` reads the pom from the filesystem: the `:deploy` alias
  passes `:pom-file "target/classes/META-INF/maven/org.clojars.darkleaf/di/pom.xml"`.
- Local git has `tag.gpgsign=true`; for release-style lightweight tags
  use `git -c tag.gpgsign=false tag X.Y.Z`.

## Clojars

- A non-snapshot version can never be re-deployed, even after a failed
  validation (`3.6.1` was burned this way) — bump to a fresh version.
- Deploys need a token (not the account password), stored in GitHub
  Actions secrets `CLOJARS_USERNAME` / `CLOJARS_PASSWORD`.
