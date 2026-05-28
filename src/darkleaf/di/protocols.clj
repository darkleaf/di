;; *********************************************************************
;; * Copyright (c) 2022 Mikhail Kuzmin
;; *
;; * This program and the accompanying materials are made
;; * available under the terms of the Eclipse Public License 2.0
;; * which is available at https://www.eclipse.org/legal/epl-2.0/
;; *
;; * SPDX-License-Identifier: EPL-2.0
;; **********************************************************************/

(ns darkleaf.di.protocols)

(defprotocol Factory
  "Defines how to build a single object that di can wire into a system.

  Most users don't implement `Factory` directly — `defn` plus the
  built-in `ref`/`template`/`derive` helpers cover the common cases.
  Implement it when a key needs custom resolution that those can't
  express.

  The runtime queries `dependencies` first, then calls `build` with
  the resolved values."

  (dependencies [this]
    "Returns one of:

    - a map `{key dep-type}` of the factory's dependencies
    - a sequence of `[key dep-type]` pairs (preserves build order —
      useful when builds carry side effects like migrations)
    - `nil` or `{}` for no dependencies

    A key is a symbol (var), a keyword (abstract), or a string (env
    var). A dep-type is `:required` or `:optional`:

    - `:required` — the key must resolve to a non-nil object, otherwise
      `di/start` fails with `::di/missing-dependency`.
    - `:optional` — `build` receives `nil` for this key if it doesn't
      resolve.

    May be called multiple times per `di/start`. Should be pure.")

  (build [this dependencies add-stop]
    "Returns the built object.

    `dependencies` is a map `{key built-object}` for the keys declared
    by `(dependencies this)`. Required keys hold a non-nil value.
    Optional keys may be `nil`.

    `add-stop` registers a zero-arg cleanup procedure to run on system
    shutdown. Call `(add-stop f)` once per resource you allocate.
    Registered cleanups run in LIFO order.")

  (description [this]
    "Returns a map describing the factory. Used by inspection and
    logging. May be empty. See `darkleaf.di.core` for the conventional
    keys."))
