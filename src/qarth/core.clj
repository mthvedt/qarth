(ns qarth.core
  "Core NS for Qarth. Right now this is just some support
  for qarth.oauth and implementations."
  (use slingshot.slingshot))

(defn unauthorized
  "Throw an unauthorized Slingshot exception.
  Use this in preference to throw+, since that leaks environment information
  like API keys."
  [message]
  (throw+ {::unauthorized true} message))
