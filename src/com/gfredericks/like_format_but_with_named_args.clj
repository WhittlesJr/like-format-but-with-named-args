(ns com.gfredericks.like-format-but-with-named-args)

(defn parse-named-format-string
  "Given a format string as described by the docstring on the named-format
  function, returns a pair [clojure-format-string names] where
  `clojure-format-string` is a similar format string suitable to pass
  to clojure.core/format, and `names` is a vector of keywords
  representing the names used in the format specifiers in the input
  string."
  [format-str]
  (loop [out-str "", keys [], ^String unparsed format-str]
    (let [i (.indexOf unparsed "%")]
      (if (neg? i)
        [(str out-str unparsed) keys]
        (let [before-% (subs unparsed 0 i)
              at-and-after-% (subs unparsed i)]
          (if (= [\% \%] (take 2 at-and-after-%))
            (recur (str out-str before-% "%%") keys (subs at-and-after-% 2))
            (if-let [[_ keyname remaining] (re-matches #"(?s)%([^\s~%]+)~(.*)" at-and-after-%)]
              (recur (str out-str before-% "%")
                     (conj keys (keyword keyname))
                     remaining)
              (throw (ex-info "Bad format string!" {:arg format-str})))))))))

(defn named-format
  "Like clojure.core/format but uses named args. `format-string` is
  similar to that accepted by clojure.core/format, but with names on
  each formatting expression. `arg-map` is a map with keyword keys
  matching the names used in the format string. E.g.,:

  (named-format \"Hello my name is %name~s and I am %height~.3f feet tall.\"
                {:name   \"Joe Biden\"
                 :height (* 2 Math/PI)})
  ;; => \"Hello my name is Joe Biden and I am 6.283 feet tall.\"

  Names are terminated with a \\~ character, and immediately following the
  \\~ should be a format specifier as per clojure.core/format."
  ;; inlined version so we can parse at compile time if possible. Not
  ;; sure how to do this without essentially writing the function
  ;; three times. Whatever.
  {:inline (fn [format-string arg-map]
             (if (string? format-string)
               (let [[clojure-format-string names] (parse-named-format-string format-string)]
                 (if (empty? names)
                   ;; passing it through format just in case we need to do a
                   ;; "%%" -> "%" conversion.
                   (format clojure-format-string)
                   `(let [arg-map# ~arg-map]
                      (apply format ~clojure-format-string (map #(get arg-map# %) ~names)))))
               `(let [[clojure-format-string# names#] (parse-named-format-string ~format-string)
                      arg-map# ~arg-map]
                  (apply format clojure-format-string# (map #(get arg-map# %) names#)))))
   :inline-arities #{2}}
  [format-string arg-map]
  (let [[clojure-format-string names] (parse-named-format-string format-string)]
    (apply format clojure-format-string (map #(get arg-map %) names))))
