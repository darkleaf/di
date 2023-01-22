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

(defprotocol Stoppable
  (unwrap [this])
  (stop [this]
    "Stops the object. Returns nothing."))

(defprotocol Factory
  (dependencies [this]
    "Returns a map of a key and a dependency type.
     A type can be `:required` or `:optional`.")
  (build [this dependencies]
    "Builds a stoppable object from dependencies."))
