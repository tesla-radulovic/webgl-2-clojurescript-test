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

(def key-handles (transient {}))

(defn key-input! [keycode]
  (let
    [ input (transient {:is-pressed false :just-pressed false :just-released false}) ] 
    (do
      (assoc! key-handles keycode (cons input (get key-handles keycode nil)))
      input)))

(defn and-input!
  [& inputs]
  (transient {:is-pressed false :just-pressed false :just-released false :and inputs}))

(def a-key (key-input! 65))

(def d-key (key-input! 68))

(def a-and-d (and-input! a-key d-key))

(defn loop-inputs [event f]
  (if-let
    [key-inputs
       (get key-handles
            (.-keyCode event)
            nil)]
    (doseq [key-input key-inputs]
      (f key-input))))

(defn keydown!
  [event]
  (loop-inputs
   event
   #(-> %
     (assoc! :just-pressed (or (not (% :is-pressed)) (% :just-pressed)))
     (assoc! :is-pressed true))))

(defn keyup!
  [event]
  (loop-inputs
   event
   #(-> %
      (assoc! :is-pressed false)
      (assoc! :just-released true))))


(declare update-and! update-input!)

(defn update-and! [and-input?]
  (if-let [inputs (and-input? :and)]
    (let
        [out
         (-> and-input?
           (assoc! :just-pressed
                 (and
                  (every? #(% :is-pressed) inputs)
                  (some #(% :just-pressed) inputs)))
           (assoc! :just-released
                  (and
                   (and-input? :is-pressed)
                   (some #(% :just-released) inputs)))
           (assoc! :is-pressed (every? #(% :is-pressed) inputs)))]
      (do #_(println "here") (doseq [input inputs] (update-input! input)) out))
    and-input?))
      

(defn update-input! [input]
  (-> input
      (assoc! :just-pressed false)
      (assoc! :just-released false)
      (update-and!)))        




(defonce register-dom-events
  (do (.addEventListener js/document "keydown" keydown!)
      (.addEventListener js/document "keyup" keyup!)
      true))


(defn draw-frame! [t]
  (do
    #_(println (a-and-d :is-pressed) (a-and-d :just-pressed) (a-and-d :just-released) )
    (update-input! a-and-d)
    (doto gl-ctx
        (gl/clear-color-and-depth-buffer 0 0 0 1 1)
        (gl/draw-with-shader (assoc-in (combine-model-shader-and-camera triangle shader-spec camera)
	                               [:uniforms :model] (spin t)   )))))


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
  (println "reloaded")
  )
