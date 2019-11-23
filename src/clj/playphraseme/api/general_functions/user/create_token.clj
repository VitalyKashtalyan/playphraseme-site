(ns playphraseme.api.general-functions.user.create-token
  (:require [playphraseme.app.config :refer [env]]
            [clj-time.core :as time]
            [buddy.sign.jwt :as jwt]
            [playphraseme.common.util :as util]))

(defn create-token
  "Create a signed json web token. The token contents are; name, email, id,
   permissions and token expiration time. Tokens are valid for 60 minutes."
  ([user] (create-token user (* 60 24 30)))
  ([user expiration-minutes]
   (let [token-contents (-> user
                            (select-keys [:permissions :name :email :id])
                            (util/update-dafault :permissions ["basic"])
                            (assoc :exp (time/plus (time/now) (time/minutes expiration-minutes))))]
     (jwt/sign token-contents (:auth-key env) {:alg :hs512}))))
