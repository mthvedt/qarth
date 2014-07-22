(ns qarth.impls
  "Namespace that requires and loads all methods bundled with Qarth.

  Using this is slightly slower than using each namespace individually."
  (require (qarth.impl yahoo github google facebook)))
