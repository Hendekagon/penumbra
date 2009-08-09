;   Copyright (c) Zachary Tellman. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns penumbra.compute.data
  (:use [penumbra.opengl core geometry texture])
  (:use [penumbra.interface.slate :only (rectangle)])
  (:import (java.nio FloatBuffer IntBuffer ByteBuffer))
  (:import (com.sun.opengl.util.texture TextureData)))

;;;;;;;;;;;;;;;;;;;

(gl-import glFramebufferTexture2D gl-frame-buffer-texture-2d)
(gl-import glDrawBuffers gl-draw-buffers)
(gl-import glReadBuffer gl-read-buffer)
(gl-import glReadPixels gl-read-pixels)

;;;;;;;;;;;;;;;;;;;

(defn- attachment [point]
  (translate-keyword (keyword (str "color-attachment" point))))

(defn attach [tex point]
  (let [p (attachment point)]
    (gl-frame-buffer-texture-2d :framebuffer p :texture-rectangle 0 0)
    (gl-frame-buffer-texture-2d :framebuffer p :texture-rectangle (:id tex) 0)
    (assoc tex :attach-point p)))

(defn draw-targets [bufs]
  (gl-draw-buffers (count bufs) (int-array (map attachment bufs)) 0))

;;;;;;;;;;;;;;;;;;

(defn- array? [a] (.isArray (class a)))
(defn- flat? [s] (not (or (array? s) (sequential? s))))
(defn- or= [cmp & args] (some #(= cmp %) args))

(defn byte-array [size-or-seq]
  (if (number? size-or-seq)
    (make-array Byte/TYPE size-or-seq)
    (let [a (make-array Byte/TYPE (count size-or-seq))]
      (loop [idx 0, s size-or-seq]
        (if (empty? s)
          a
          (do
            (aset a idx (byte (first s)))
            (recur (inc idx) (next s))))))))

(defn- create-array [size-or-seq type]
  (cond
    (= type :int)           (int-array size-or-seq)
    (= type :float)         (float-array size-or-seq)
    (= type :unsigned-byte) (byte-array size-or-seq)
    :else                   (throw (Exception. "Don't recognize type"))))

(defn- wrap-array [a type]
  (cond
    (= type :float)         (FloatBuffer/wrap a)
    (= type :int)           (IntBuffer/wrap a)
    (= type :unsigned-byte) (ByteBuffer/wrap a)
    :else                   (throw (Exception. "Don't recognize type"))))

(defn- seq-type [s]
  (let [type (-> s first .getClass)]
    (cond
      (or= type Integer Integer/TYPE) :int
      (or= type Float Float/TYPE)     :float
      (or= type Byte Byte/TYPE)       :unsigned-byte
      :else                           (throw (Exception. "Don't recognize type")))))

(defn- internal-format
  ([tex]
    (internal-format (:tuple tex) (:type tex)))
  ([tuple type]
    (cond
      (= type :int)           ({ 1 :luminance-integer, 2 :luminance-alpha-integer, 3 :rgb-integer, 4 :rgba-integer } tuple)
      (= type :float)         ({ 1 :luminance32f, 2 :luminance-alpha32f, 3 :rgb32f, 4 :rgba32f } tuple)
      (= type :unsigned-byte) ({ 1 :luminance, 2 :luminance-alpha, 3 :rgb, 4 :rgba } tuple))))

(defn- pixel-format [tex-or-tuple]
  (if (number? tex-or-tuple)
    ({ 1 :luminance, 2 :luminance-alpha, 3 :rgb, 4 :rgba} tex-or-tuple)
    (pixel-format (:tuple tex-or-tuple))))

;;;;;;;;;;;;;;;;;;;

(defn- init-texture []
  (tex-parameter :texture-rectangle :texture-min-filter :nearest)
  (tex-parameter :texture-rectangle :texture-mag-filter :nearest)
  (tex-parameter :texture-rectangle :texture-wrap-s :clamp)
  (tex-parameter :texture-rectangle :texture-wrap-t :clamp))

(defn create-tex
  ([t] (:width t) (:height t) (internal-format t) (pixel-format t) (:tuple t) (:type t))
  ([w h internal pixel tuple type]
    (let [id (gen-texture)]
      (gl-bind-texture :texture-rectangle id)
      (gl-tex-image-2d
        :texture-rectangle 0
        (translate-keyword internal)
        w h 0
        (translate-keyword pixel)
        (translate-keyword type)
        nil)
      (struct-map tex-struct :id id :width w :height h :depth 1 :type type :tuple tuple))))

(defn write-tex [tex ary]
  (gl-tex-sub-image-2d
    :texture-rectangle
    0 0 0
    (:width tex) (:height tex)
    (translate-keyword (pixel-format tex))
    (translate-keyword (:type tex))
    (wrap-array ary (:type tex)))
  tex)

(defn seq-to-tex
  ([s] (seq-to-tex s 1))
  ([s tuple]
    (let [type      (seq-type s)
          internal  (internal-format tuple type)
          pixel     (pixel-format tuple)
          ary       (if (array? s) s (create-array s type))
          [w h]     (rectangle (count ary))
          tex       (create-tex w h internal pixel tuple type)]
      (write-tex tex ary)
      tex)))

(defn tex [s]
  (seq-to-tex s))

(defn ptex [s]
  (assoc (tex s) :persistent true))

(defn array
  ([tex]
    (array tex (* (:width tex) (:height tex))))
  ([tex size]
    (if (nil? (:attach-point tex)) (throw (Exception. "Cannot read from unattached texture.")))
    (gl-read-buffer (:attach-point tex))
    (let [dim   (* (:width tex) (:height tex) (:tuple tex))
          a     (create-array size (:type tex))]
      (gl-read-pixels
        0 0 (:width tex) (:height tex)
        (translate-keyword (pixel-format tex))
        (translate-keyword (:type tex))
        (wrap-array a (:type tex)))
       a)))

;;;;;;;;;;;;;;;;;;

(defn draw-quad [w h]
  (draw-quads
    (texture 0 0) (vertex 0 0)
    (texture w 0) (vertex 1 0)
    (texture w h) (vertex 1 1)
    (texture 0 h) (vertex 0 1)))

