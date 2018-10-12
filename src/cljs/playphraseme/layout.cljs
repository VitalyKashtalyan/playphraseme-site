(ns playphraseme.layout
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [markdown.core :refer [md->html]]
            [playphraseme.common.ui :as ui]
            [playphraseme.common.util :as util]
            [playphraseme.views.search.view :as search-page]
            [playphraseme.common.localization :refer [ls]]
            [playphraseme.common.phrases :as phrases]
            [playphraseme.common.rest-api :as rest-api]
            [playphraseme.model]
            [playphraseme.common.responsive :as resp])
  (:require-macros
   [re-frame-macros.core :as mcr :refer [let-sub]]
   [cljs.core.async.macros :refer [go go-loop]]))

(defn header-button
  ([label href icon-class]
   [:a.header-button {:href href} [:i {:class icon-class}] " " label])
  ([label href]
   [:a.header-button {:href href} label]))

(defn facebook-like-button []
  #_(let-sub [scale :responsive-scale]
           [:div.fb-like
            {:style (resp/fb-button-css @scale)
             :data-share      "false"
             :data-show-faces "false"
             :data-size       "small"
             :data-width      "100"
             :data-action     "like"
             :data-layout     "button"
             :data-href       "https://www.facebook.com/playphrase/"}]))

(defn header []
  (let-sub [:page
            :all-movies-count
            :all-phrases-count]
           (go
             (rf/dispatch [:all-phrases-count (<! (rest-api/count-all-phrases))])
             #_(rf/dispatch [:all-movies-count (<! (rest-api/count-all-movies))]))
           (fn []
             [:div.header
              {:class (util/class->str (when-not (= @page :search) "invert"))}
              [:div.top
               (when-not (= @page :search)
                 [header-button "Home" "/#/" "fas fa-home"])
               (when-not (= @page :login)
                 (if (rest-api/authorized?)
                   [header-button
                    (str (ls :navigation.logout) " (" (:name @(rf/subscribe [:auth-data])) ")")
                    "/#/logout" "fas fa-user-circle"]
                   [header-button (ls :navigation.login.register) "/#/login" "fas fa-user-circle"]))
               (when-not (= @page :support)
                 [header-button (ls :navigation.support) "/#/support" "far fa-envelope"])
               [ui/flexer]
               [header-button "Github" "https://github.com/potapenko/playphraseme-site" "fab fa-github-square"]
               [header-button  "Facebook" "https://www.facebook.com/playphrase/" "fab fa-facebook"]
               ^{:key "fixed-key"}
               [facebook-like-button]]
              [:div.bottom
               [:div.logo {:on-click (fn [e]
                                       (if (-> e .-altKey)
                                         (phrases/search-random-bad-phrase)
                                         (phrases/search-random-phrase)))}
                [:span.red "Play"]
                [:span.black "Phrase"]
                [:span.gray ".me"]]
               [ui/flexer]
               #_[:div.statistic
                [:span.count @all-movies-count]
                [:span.info (ls :statistic.movies)]]
               #_[ui/spacer 10]
               [:div.statistic
                [:span.count (some->> @all-phrases-count #_(util/format "%,12d"))]
                [:span.info (ls :statistic.phrases)]]
               #_[:div.translate-direction
                  [:span.select-button "En"] [:span.arrow ">"] [:span.select-button "En"]]]])))

(defn left-column []
  [:div.left-column ""])

(defn right-column []
  [:div.right-column ""])

(defn root []
  (let-sub [scale :responsive-scale
            :responsive-show-left-column?
            :responsive-show-right-column?]
    (r/create-class
     {:component-did-mount
      (fn []
        (when resp/ios?
         (util/add-class (util/selector "body") "ios")))
      :reagent-render
      (fn [current-page]
       [:div.layout-container
        {:style (resp/zoom-css @scale)
         :class (util/class->str
                 (when resp/ios? :ios)
                 (when resp/android? :android)
                 (when resp/safari? :safari)
                 (when resp/chrome? :chrome))}
        (when @responsive-show-left-column?
          [left-column])
        [:div.mobile-query]
        [:div.layout-main
         {:style (resp/container-height-css @scale)}
         [header]
         [:div.current-page-container
          current-page]]
        (when @responsive-show-right-column?
          [right-column])])})))

