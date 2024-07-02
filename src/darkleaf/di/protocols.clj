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
  (dependencies [this]
    "Returns a map of a key and a dependency type.
     A type can be `:required` or `:optional`.")
  (build [this dependencies]
    "Builds an object from dependencies.")
  (demolish [this obj]
    "Demolishes or stops an object."))
