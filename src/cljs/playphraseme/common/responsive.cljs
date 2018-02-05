(ns playphraseme.common.responsive
  (:require [re-frame.core :refer [dispatch subscribe]]
            [clojure.string :as string]))

(defn screen-width []
  (-> js/window.screen .-width))

(defn screen-height []
  (-> js/window.screen .-height))

(defn window-width []
  (-> js/window .-inerWidth)
  (-> js/document.body .-clientWidth))

(defn window-height []
  (or
   (-> js/window .-inerHeight)
   (-> js/document.body .-clientHeight)))

(defn mobile? []
  (-> (window-width) (< 756)))

(defn get-head-html []
  (aget (js/document.querySelector "head") "innerText"))

(defn set-head-html [val]
  (aset (js/document.querySelector "head") "innerText" val))

(defn append-head-html [val]
  (set-head-html (str (get-head-html) val)))

(defn remove-css [src]
  (let [head     (-> "head" js/document.getElementsByTagName (aget 0))
        children (-> head .-children)
        length   (-> children .-length)]
    (dotimes [i length]
      (let [el (aget children i)]
        (when (some-> el .-tagName string/lower-case (= "link"))
          (when (= (-> el (.getAttribute "href")) src)
            (-> el .remove)))))))

(defn load-css [src]
  (let [link (-> "link" js/document.createElement)
        head (-> "head" js/document.getElementsByTagName (aget 0))]
    (-> link (.setAttribute "rel" "stylesheet"))
    (-> link (.setAttribute "href" src))
    (-> head (.appendChild link))))

(defn load-local-css [name]
  (load-css (str "/css/" name ".css")))

(defn remove-local-css [name]
  (remove-css (str "/css/" name ".css")))

(defn calculate-window-scale [min-width min-height]
  (let [w          (window-width)
        h          (window-height)
        scale-w    (/ w min-width)
        scale-h    (/ w min-height)]
    (min scale-w scale-h)))

(defn update-layout []
  (let [w (window-width)
        show-left-column? (> w 960)
        show-right-column? (> w 1400)]
   (dispatch [:responsive-scale
              (calculate-window-scale
               (+ (when show-left-column? 200) 680 (when show-right-column? 200))
               800)])
   (dispatch [:responsive-show-left-column? show-left-column?])
   (dispatch [:responsive-show-right-column? show-right-column?]))
  (when-not (= @(subscribe [:mobile?]) (mobile?))
    (load-local-css (if (mobile?) "mobile" "desktop"))
    (remove-local-css (if-not (mobile?) "mobile" "desktop"))
    (dispatch [:mobile? (mobile?)])))


(defn start []
  (update-layout)
  (-> js/window (.addEventListener "resize" update-layout true)))
