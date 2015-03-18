(ns cracer.core
  (:require [jinput.core :as jinput]
            [flow-gl.opengl.math :as math]
            [clojure.core.matrix :as matrix]
            [flow-gl.utils :as utils]
            [flow-gl.debug :as debug]
            [flow-gl.csp :as csp]
            (flow-gl.gui [drawable :as drawable]
                         [layout :as layout]
                         [quad-view :as quad-view]
                         [window :as window]
                         [renderer :as renderer]
                         [layoutable :as layoutable])

            (flow-gl.graphics [font :as font]
                              [buffered-image :as buffered-image])

            (flow-gl.opengl.jogl [opengl :as opengl]
                                 [window :as jogl-window]
                                 [quad-batch :as quad-batch]
                                 [render-target :as render-target]))
  (:use flow-gl.utils
        clojure.test))

(defn render-quads [window quad-batch x y angle]
  (let [quad-batch (window/with-gl window gl
                     (let [size (opengl/size gl)]
                       (opengl/clear gl 0 0 0 1)

                       (-> quad-batch
                           (quad-batch/draw-quads gl
                                                  [{:x 0
                                                    :y 0
                                                    :texture-id 0}]
                                                  (:width size)
                                                  (:height size)
                                                  (matrix/mmul (math/translation-matrix (/ (:width size) 2)
                                                                                        (/ (:height size) 2))
                                                               (math/z-rotation-matrix (* angle (/ Math/PI 180)))
                                                               (math/translation-matrix (- x) (- y)))

                                                  #_(math/scaling-matrix 4.0 1.0)
                                                  #_(math/z-rotation-matrix (* 45 (/ Math/PI 180))))
                           (quad-batch/draw-quads gl
                                                  [{:x (/ (:width size) 2)
                                                    :y (/ (:height size) 2)
                                                    :texture-id 1}]
                                                  (:width size)
                                                  (:height size)))))]

    (window/swap-buffers window)
    quad-batch))


(defn set-size [drawable]
  (let [preferred-size (layoutable/preferred-size drawable 1000 1000)]
    (assoc drawable
      :width (:width preferred-size)
      :height (:height preferred-size))))

(defn text [text]
  (drawable/->Text text
                   (font/create "LiberationSans-Regular.ttf" 20)
                   [255 255 255 255]))

(defn initialize [quad-batch gl]
  (quad-batch/add-textures quad-batch gl [(buffered-image/create-from-file "track.png")
                                          (buffered-image/create-from-file "car.png")]))

(defn quads [frame-time]
  [{:x 0
    :y 0
    :texture-id 0}])

(defn wait-for-next-frame [frame-started]
  (let [target-frames-per-second 60]
    (Thread/sleep (max 0
                       (- (/ 1000 target-frames-per-second)
                          (- (System/currentTimeMillis)
                             frame-started))))))

(defn start-view []
  (let [window (jogl-window/create 1000
                                   700
                                   :profile :gl3
                                   :init opengl/initialize
                                   :reshape opengl/resize
                                   :close-automatically true)
        controller-map (jinput/joystick-controller-map)]

    (try
      (loop [quad-batch (window/with-gl window gl
                          (-> (quad-batch/create gl)
                              (initialize gl)))]
        (let [frame-started (System/currentTimeMillis)
              values (jinput/values controller-map :x :y :z)
              quad-batch (render-quads window quad-batch
                                       (* (:x values)
                                          1000)
                                       (* (:y values)
                                          1000)
                                       (* (:z values)
                                          360))]

          (when (window/visible? window)
            (do (wait-for-next-frame frame-started)
                (recur quad-batch)))))

      (println "exiting")
      (catch Exception e
        (println "exception")
        (window/close window)
        (throw e)))))

(defn start []
  (start-view))
