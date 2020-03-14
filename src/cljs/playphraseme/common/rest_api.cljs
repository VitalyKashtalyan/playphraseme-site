(ns playphraseme.common.rest-api
  (:require [cljs-http.client :as http]
            [re-frame.core :as rf]
            [cljs.core.async :refer [<! put! chan] :as async]
            [re-frame.core :refer [subscribe dispatch]]
            [clojure.string :as string]
            [playphraseme.common.util :as util]
            [playphraseme.common.localstorage :as localstorage])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn not-found-md [res]
  (when (string? res) (println res))
  (let [err-res {:code 404 :success false :body "not found"}]
    (cond
      (= (:code res) 404) err-res
      :else res)))

(def default-middlewares [not-found-md])

(defn call-api [http-fn router-uri opts & middlewares]
  (go
    (let [res         (<! (http-fn (str "/api/v1" router-uri) opts))
          middlewares (concat default-middlewares middlewares)]
      (loop [res     res
             [v & t] middlewares]
        (if (and res v)
          (recur (v res) t)
          res)))))

(defn success? [res]
  (-> res :success true?))

(defn error? [res]
  (-> res :success false?))

(defn auth-error? [res]
  (= (:code res) 401))

(defn get-auth-token []
  (localstorage/get-item "auth-token"))

(defn authorization-header []
  {"authorization" (str "Token " (get-auth-token))})

(defn api-headers []
  {:headers (authorization-header) :content-type :json :accept :json})

(defn logout []
  (localstorage/remove-item! "auth-token")
  (dispatch [:set-auth-data nil]))

(defn- auth* [auth-ch]
  (go
    (let [res (<! auth-ch)]
      (if (success? res)
        (do
          (localstorage/set-item! "auth-token" (-> res :body :token))
          (dispatch [:set-auth-data (->> res :body)])))
      res)))

(defn auth
  ([token]
   (localstorage/set-item! "auth-token" token)
   (auth* (http/get "/api/v1/session" (api-headers))))
  ([name password]
   (logout)
   (auth* (http/get "/api/v1/auth" {:basic-auth {:username name :password password}}))))

(defn authorized? []
  @(rf/subscribe [:logged?]))

(defn authorize! []
  (go
    (if-let [token (get-auth-token)]
      (let [res (<! (auth token))]
        (success? res))
      false)))

(defn register-user [name email password]
  (go
    (let [res (<! (http/post "/api/v1/user" {:json-params {:email email :name name :password password}}))]
      (when (success? res)
        (<! (auth email password)))
      res)))

(defn reset-password-request [email]
  (call-api http/post "/password/reset-request" {:json-params {:user-email email}}))

(defn reset-password-confirm [password reset-key]
  (call-api http/post "/password/reset-confirm" {:json-params {:new-password password :reset-key reset-key}}))

(defn goto-login-page! []
  (util/go-url! "/#/login"))

(defn login-md [res]
  (if (auth-error? res)
    (do (goto-login-page!) nil)
    res))

(defn search-phrase
  ([text] (search-phrase text 10 0))
  ([text limit skip]
   (call-api http/get "/phrases/search"
             (merge (api-headers) {:query-params {:q text :limit limit :skip skip}})
             login-md :body)))

(defn get-phrase [phrase-id]
  (call-api http/get (str "/phrases/phrase/" phrase-id)
            (api-headers) login-md :body))

(defn count-phrase [text]
  (call-api http/get "/phrases/count"
            (merge (api-headers) {:query-params {:q text}})
            login-md :body))

(defn count-all-phrases []
  (call-api http/get "/phrases/all-phrases-count"
            (merge (api-headers)) :body))

(defn count-all-movies []
  (call-api http/get "/phrases/all-movies-count"
            (merge (api-headers))
            login-md :body))

(defn video-url [id]
  (call-api http/get "/phrases/video-url"
            (merge (api-headers) {:query-params {:id id}})
            login-md :body))

(defn video-download [id]
  (call-api http/get "/phrases/video-download"
            (merge (api-headers) {:query-params {:id id}})
            login-md :body))

(defn common-phrases [text]
  (call-api http/get "/phrases/common-phrases"
            (merge (api-headers) {:query-params {:q text}})
            login-md :body))

(defn favorites
  ([] (favorites 10 0))
  ([limit skip]
   (call-api http/get "/favorites"
             (merge (api-headers) {:query-params {:limit limit :skip skip}})
             login-md :body)))

(defn add-favorite [phrase-id]
  (call-api http/post (str "/favorites/" phrase-id)
            (api-headers) login-md :body))

(defn get-favorite [phrase-id]
  (call-api http/get (str "/favorites/" phrase-id)
            (api-headers) login-md :body))

(defn delete-favorite [phrase-id]
  (call-api http/delete (str "/favorites/" phrase-id)
            (api-headers) login-md :body))

(defn add-playlist [playlist-data]
  (call-api http/post "/playlists" {:json-params {:playlist playlist-data}} :body))

(defn get-playlist [playlist-id]
  (call-api http/get (str "/playlists/" playlist-id)
            (api-headers) login-md :body))

(defn get-config-value [key]
  (call-api http/get "/configs"
            (merge (api-headers) {:query-params {:id (name key)}})
            login-md :body))

(comment
  (go (println (<! (search-phrase "hello"))))
  (go (println (<! (count-phrase "hello"))))
  (go (println (<! (count-all-phrases))))
  (go (println (<! (count-all-movies))))
  (go (println (<! (count-all-movies))))
  (go (println (<! (video-url "543bd8c8d0430558da9bfeb1"))))
  (go (println (<! (video-download "543bd8c8d0430558da9bfeb1"))))
  (go (println (<! (favorites))))
  (go (println (<! (add-favorite "543bd8c8d0430558da9bfeb1"))))
  (go (println (<! (get-favorite "543bd8c8d0430558da9bfeb1"))))
  (go (println (<! (delete-favorite "543bd8c8d0430558da9bfeb1"))))
  (go (println (<! (get-phrase "543bd8c8d0430558da9bfeb1"))))
  (go (println (<! (common-phrases "hello"))))
  (go (println (<! (get-config-value "search-on-mobile"))))






  )
