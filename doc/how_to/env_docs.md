# Documenting environment variables

The environment variables a system reads are its configuration
contract. Whoever deploys it needs that list: every variable that
exists, what each one means, whether it is required, what it
defaults to. Kept by hand, the list drifts — a new component adds
a variable and the wiki forgets it. Generate it from the code
instead.

Two things have to live in the code for that: a description for
each variable, and a way to read the whole set back out.

## Where the description goes

A variable is a key — a string like `"DATABASE_URL"`, or, once
parsed, a keyword like `:env.long/PORT`
([Environment variables](/doc/tutorial/g_environment_variables_test.md)).
The description belongs with it, but neither a string nor a
keyword can carry Clojure metadata. A symbol can — and the
binding, the left-hand side of the deps-map entry, is a symbol.
So the description goes on the binding, under a metadata key of
your own choosing:

```clojure
(defn db
  {::di/kind :component}
  [{^{:app.env/doc "Postgres connection string"} url "DATABASE_URL"}]
  ...)
```

One consequence follows from this. The description sits on the
*use site*, not on the variable. The same variable read by
several components has several bindings, so it can carry several
descriptions — the generator collects them all rather than
assuming there is one.

## Reading it back

`di/inspect` takes the same arguments as `di/start` but builds
nothing; it returns the graph the runtime would walk
([Inspect](/doc/reference/inspect_test.md)). A component that
comes from a var carries that var under `::di/variable` — env
keys, refs and plain values do not — so keeping on `::di/variable`
hands you exactly the var-backed components the system reaches:

```clojure
(->> (di/inspect :app.system/root)
     (keep #(-> % :description ::di/variable)))
;; => (#'app.db/db #'app.mailer/mailer ...)
```

The descriptions are not in that output; they live in the
metadata of the bindings, inside each var's source. So `inspect`
tells you *which* vars, and the vars themselves tell you *what*
they read. `defn` records its argument lists under `:arglists`,
and a component's deps map is the first argument of an arglist:

```clojure
(-> #'db meta :arglists)
;; => ([{url "DATABASE_URL"}])
```

You do not parse that map by hand. `clojure.core/destructure` —
what `let` expands its own bindings with — flattens it into
binding/expression pairs with metadata intact:

```clojure
(partition 2 (destructure '[{^{:app.env/doc "HTTP port"} port :env.long/PORT
                             url "DATABASE_URL"
                             :or {port 8080}}
                            env]))
;; (... map plumbing, dropped ...)
;; (port (clojure.core/get env :env.long/PORT 8080))
;; (url  (clojure.core/get env "DATABASE_URL"))
```

Each variable is one pair, and the pair holds every field:

- the **binding** still carries your `:app.env/doc` — the
  description;
- `get`'s second argument is the **variable** — a string is the
  name as-is, a parsed keyword names both the variable (`PORT`)
  and its type (`:env.long`);
- `get`'s third argument is the `:or` **default**; whether it is
  there is the difference between optional and required.

Pairs whose expression is not a `get` — the map binding itself,
other components — are not variables; drop them.

## The result

Collected across the vars, this gives you a piece of data per
variable: its name, type, whether it is required, its default,
and the set of descriptions written for it. Turning that into a
document is the last step, and it is yours to shape — a markdown
table beside the deployment guide, a section of `--help`, a config
template; strict about missing descriptions or lenient; checked in
a test or not.

The recipe stops here because the point is upstream: `di/inspect`
opens the whole graph to your own configuration tooling — see
[Inspect](/doc/reference/inspect_test.md) for the rest of what it
exposes.
