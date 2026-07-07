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

Docs live on cljdoc.org. Tutorial/how-to articles are generated from
the matching test files by `script/tutorial-to-md.sh`; the generated
`.md` are gitignored and built in CI.

- **Release flow:** pushing tag `X.Y.Z` makes CI generate the
  articles, commit them on a detached HEAD, tag that commit
  `cljdoc-X.Y.Z`, and deploy to Clojars. The pom's `<scm><tag>` points
  cljdoc at that detached commit, not at `master`.
- **Updating docs without a new release:** cljdoc re-imports articles
  from the `cljdoc-X.Y.Z` tag. The new tagged commit MUST be a
  descendant of the existing cljdoc commit — its SHA is hard-coded in
  the deployed pom, and orphaning it makes the build silently lose
  all articles. So: rebuild the commit on top of the old tag,
  force-move the tag, trigger a rebuild on cljdoc.org.
- **Cross-links:** between articles use root-relative `/doc/...`
  paths (a bare `doc/...` renders as a broken external link).
  Article → API var: full cljdoc URL with the `CURRENT` version
  placeholder, e.g.
  `https://cljdoc.org/d/org.clojars.darkleaf/di/CURRENT/api/darkleaf.di.core#->memoize`.

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
