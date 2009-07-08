(ns penumbra.examples.render-to-texture)

(use 'penumbra.opengl 'penumbra.window 'penumbra.texture)

(def t (atom nil))

(defn square []
  (draw-quads
    (tex 0 0) (vertex 0 0 0)
    (tex 1 0) (vertex 1 0 0)
    (tex 1 1) (vertex 1 1 0)
    (tex 0 1) (vertex 0 1 0)))

(defn init []
  (enable :normalize)
  (enable :depth-test)
  (enable :multisample)
  (enable :texture-2d)
  (shade-model :flat)
  (reset! t (create-2d-texture 128 128 #([0 1 0 0]))))

(defn reshape [x y width height]
  (ortho-view 0 0 (* 10 (/ (float width) height)) 10 0 10)
  (load-identity)
  (translate (* 5 (/ (float width) height)) 5 -5))

(defn mouse-drag [[dx dy] _]
  )

(defn display [delta time]
  (color 1 0 0)
  (scale 4 4 4)
  (translate -0.5 -0.5 0)
  (bind-tex @t)
  (square))

(start {:display display, :mouse-drag mouse-drag, :reshape reshape, :init init})


