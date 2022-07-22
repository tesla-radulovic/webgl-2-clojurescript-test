(ns webgl-clojurescript-tutorial.core
    (:require
      [thi.ng.geom.gl.core :as gl]
      [thi.ng.geom.matrix :as mat]
      [thi.ng.geom.triangle :as tri]
      [thi.ng.geom.core :as geom]
      [thi.ng.geom.gl.glmesh :as glmesh]
      [thi.ng.geom.gl.camera :as cam]
      [thi.ng.geom.gl.shaders :as shaders]
      [thi.ng.geom.gl.webgl.constants :as glc]
      [thi.ng.geom.gl.webgl.animator :as anim] ))

(enable-console-print!)

(defonce canvas (.getElementById js/document "main"))

(println (.  js/window -innerWidth ) )

(println (.  js/window -innerHeight ) )

(set! (.-height canvas) (. js/window -innerHeight ))

(set! (.-width canvas) (. js/window -innerWidth))

(defonce gl-ctx (.getContext canvas "webgl2")  ) 

(defonce camera (cam/perspective-camera {:aspect (/ (.-height canvas) (.-width canvas))  }))


(def shader-spec
  {:vs "void main() {
          gl_Position = proj * view * model * vec4(position, 1.0);
       }"
   :fs "
       out vec4 outColor;
       void main() {
           outColor = vec4(0.5, 0.5, 1.0, 1.0);
       }"
   :uniforms {:view       :mat4
	       :proj       :mat4
	       :model	   :mat4
	       }
   :attribs  {:position   :vec3}
   :version "300 es"
   })


  


(def triangle (geom/as-mesh (tri/triangle3 [[1 0 0] [0 0 0] [0 1 0]])
                            {:mesh (glmesh/gl-mesh 3)}))

(def shader (shaders/make-shader-from-spec gl-ctx shader-spec))

(defn combine-model-shader-and-camera
  [model shader-spec camera]
  (-> model
    (gl/as-gl-buffer-spec {})
    (assoc :shader shader)
    (gl/make-buffers-in-spec gl-ctx glc/static-draw)
    (cam/apply camera)))

(defn spin
  [t]
  (geom/rotate-y mat/M44 (/ t 10)))

(defn draw-frame! [t]
  (doto gl-ctx
        (gl/clear-color-and-depth-buffer 0 0 0 1 1)
        (gl/draw-with-shader (assoc-in (combine-model-shader-and-camera triangle shader-spec camera)
	                               [:uniforms :model] (spin t)   ))))


;;(def key-events!
;;  (list (fn [event] (println event)) )   ) 

;;(def key-down!
;;  [event]
;;  (println "hi lol"))

(defn keydown [event] (println (.-key event)))

(defonce register-dom-events
  (do (.addEventListener js/document "keydown" (fn [event] (keydown event))  ) true))



(defonce running
  (anim/animate (fn [t] (draw-frame! t) true)))


(println "Hello, World!")

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:text "Hello world!"}))


(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
;;  (map (fn [func] (func "hhh") key-events!))
  )
