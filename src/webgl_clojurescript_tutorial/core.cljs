(ns webgl-clojurescript-tutorial.core
  (:require
      [thi.ng.geom.gl.core :as gl]
      [thi.ng.math.core :as m]
      [thi.ng.geom.matrix :as mat]
      [thi.ng.geom.vector :as v]
      [thi.ng.geom.triangle :as tri]
      [thi.ng.geom.core :as geom]
      [thi.ng.geom.gl.glmesh :as glmesh]
      [thi.ng.geom.gl.camera :as cam]
      [thi.ng.geom.gl.shaders :as shaders]
      [thi.ng.geom.gl.webgl.constants :as glc]
      [thi.ng.geom.gl.webgl.animator :as anim] ))

(enable-console-print!)

(defonce canvas (.getElementById js/document "main"))

(defonce pointer-lock (do (.addEventListener canvas "click"
                                (fn
                                  [event]
                                  (.requestPointerLock canvas))) true))

(println (.  js/window -innerWidth ) )

(println (.  js/window -innerHeight ) )

(set! (.-height canvas) (. js/window -innerHeight ))

(set! (.-width canvas) (. js/window -innerWidth))

(defonce gl-ctx (.getContext canvas "webgl2")  ) 

(defonce camera (cam/perspective-camera {:aspect 1.0 #_(/ (.-height canvas) (.-width canvas))  }))

(defn aspect-ratio [] (/ (.-height canvas)  (.-width canvas)   ))

(def shader-spec
  {:vs "void main() {
          gl_Position = proj * view * model * vec4(position, 1.0) *  vec4( aspect, 1, 1, 1 );
       }"
   :fs "
       out vec4 outColor;
       void main() {
           outColor = vec4(0.5, 0.5, 1.0, 1.0);
       }"
   :uniforms {:view       :mat4
	      :proj       :mat4
	      :model	  :mat4
              :aspect     :float
	       }
   :attribs  {:position   :vec3}
   :version "300 es"
   })


  


(def triangle (geom/as-mesh (tri/triangle3 [[1 0 0] [0 0 0] [0 1 0]])
                            {:mesh (glmesh/gl-mesh 3)}))

(def view (transient {:y-angle 0.0 :xz-angle 0.0} ))

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

(defn get-view [] (geom/rotate-y mat/M44 (view :y-angle)))

(def key-handles (transient {}))

(def mouse-handles (transient {}))

(defn key-input! [keycode]
  (let
    [ input (transient {:is-pressed false :just-pressed false :just-released false}) ] 
    (do
      (assoc! key-handles keycode (cons input (get key-handles keycode nil)))
      input)))

(defn and-input!
  [& inputs]
  (transient {:is-pressed false :just-pressed false :just-released false :and inputs}))

(def w-key (key-input! 87))

(def s-key (key-input! 83))

(def a-key (key-input! 65))

(def d-key (key-input! 68))

(def space-key (key-input! 32))

(defn loop-key-inputs [event f]
  (if-let
    [key-inputs
       (get key-handles
            (.-keyCode event)
            nil)]
    (doseq [key-input key-inputs]
      (f key-input))))

(defn keydown!
  [event]
  (loop-key-inputs
   event
   #(-> %
     (assoc! :just-pressed (or (not (% :is-pressed)) (% :just-pressed)))
     (assoc! :is-pressed true))))

(defn keyup!
  [event]
  (loop-key-inputs
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
      (do (doseq [input inputs] (update-input! input)) out))
    and-input?))
      

(defn update-input! [input]
  (-> input
      (assoc! :just-pressed false)
      (assoc! :just-released false)
      (update-and!)))


(defn focus? [] (= (.-pointerLockElement js/document) canvas))


(defn update-mouse-view! [event]
  (if (focus?) (->
    view
    (assoc! :y-angle (+ (view :y-angle) (* 0.01 (.-movementX event))))
    (assoc! :y-angle (mod (view :y-angle) (* 2.0 3.1415926535)))
    (assoc! :xz-angle (- (view :xz-angle) (* 0.01 (.-movementY event))))
    (assoc! :xz-angle
            (->
             (view :xz-angle)
             (max (* -0.5 3.1415926535))
             (min (*  0.5 3.1415926535)))))))


(defonce register-dom-events
  (do (.addEventListener js/document "keydown" keydown!)
      (.addEventListener js/document "keyup" keyup!)
      (.addEventListener js/document "mousemove" update-mouse-view!)
      true))

(def eye (v/vec3 -4 0 0 ) )

(defn look [view] (v/vec3
                   (* (.cos js/Math (view :xz-angle) ) (.cos js/Math (view :y-angle) ) )
                   (.sin js/Math (view :xz-angle))
                   (* (.cos js/Math (view :xz-angle) ) (.sin js/Math (view :y-angle) ) ) ))

(defn left-90 [view] (v/vec3
                      (.cos js/Math (+ (* 0.5 3.1415926535) (view :y-angle)))
                      0
                      (.sin js/Math (+ (* 0.5 3.1415926535) (view :y-angle))) ))



(defn draw-frame! [t]
  (do
    #_(println (a-and-d :is-pressed) (a-and-d :just-pressed) (a-and-d :just-released) )

    (if (a-key :is-pressed) (set! eye (m/- eye (m/* (left-90 view) 0.05))))
    (if (d-key :is-pressed) (set! eye (m/+ eye (m/* (left-90 view) 0.05))))
    (if (w-key :is-pressed) (set! eye (m/+ eye (m/* (look view) 0.05))))
    (if (s-key :is-pressed) (set! eye (m/- eye (m/* (look view) 0.05))))
    (if (space-key :is-pressed) (set! eye (m/+ eye (v/vec3 0 0.05 0))))
    
    (if
      (focus?)
      (doseq
          [key [a-key d-key w-key s-key space-key]]
          (update-input! key)))
    #_(println (.cos js/Math t) (.sin js/Math t))
    (doto gl-ctx
        (gl/clear-color-and-depth-buffer 0 0 0 1 1)
        (gl/draw-with-shader
         (->
          (combine-model-shader-and-camera triangle shader-spec camera)
          (assoc-in [:uniforms :model] (spin t ))
          (assoc-in [:uniforms :aspect] (aspect-ratio))
          #_(assoc-in [:uniforms :view] (get-view))
          (assoc-in [:uniforms :view] (mat/look-at
                                       eye
                                       (m/+ eye (look view))
                                       (v/vec3 0 1 0)))
         )))))


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
  (println (get-view))
  
  )
