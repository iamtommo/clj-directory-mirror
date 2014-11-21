(ns cloj.core
  (:gen-class)
  (:import [java.io.File]
           [java.lang.Runnable])
  (:require [cloj.watch :refer :all]
            [clojure.java.io :as io]))

(def backup-file-suffix "___jb")
(def path-prefix "/home/ThomasMadsen/dev/git/egym/server/")

(def source-path
  (apply str path-prefix "webapp/src/main/java/de/egym/cms/page/"))

(def target-path
  (apply str path-prefix "webapp/target-gradle/libs/egym-webapp/WEB-INF/classes/de/egym/cms/page/"))

(defn print-event
  "Prints basic info about the event"
  [event path]
  (let [name (.getFileName path)]
    (println event " --> "
             (clojure.string/join ", "
                                  [name
                                   (.toAbsolutePath name)
                                   (.toUri name)
                                   (.getParent name)
                                   (.toRealPath name)]))))

(defn unix-path-to-string
  [unixpath]
  (.toString (.toUri unixpath)))

(defn file-name-of
  [unixpath]
  (.toString (.getFileName unixpath)))

(defn mirror-change-create
  [path]
  (if-not (.contains (.toString (.getFileName path)) backup-file-suffix)
    ;; backup files are suffixed with ___jb___ and ___jb_bak___
    (do 
      (println "Mirroring file creation > ")
      (let [src (str source-path (.getFileName path))
           trg (str target-path (.getFileName path))]
        (println "\t from > " src)
        (println "\t to   > " trg)
        (io/copy (io/file src) (io/file trg))))))

(defn mirror-change-modify
  [path]
  (println "Ignored modify event at " (file-name-of path)))

(defn mirror-change-delete
  [path]
  (println "Ignored delete event at " (file-name-of path)))

(defn event-handler
  [event path]
  (cond
   (= event :create)
     (mirror-change-create path)
   (= event :modify)
   ; This works because a create event simply copies the file from source to target
     (mirror-change-create path)
   (= event :delete)
     (mirror-change-delete path)))

(defn watch-file
  [file]
  (let [path (.getAbsolutePath file)]
    (println "Spying on directory ->" path)
    (watch-path (.getAbsolutePath file) 
                :create event-handler
                :modify event-handler
                :delete event-handler)))

(defn stop
  []
  (stop-watchers))

(defn watch-directory
  "Recursively starts a watcher on every single file in the directory"
  [directory]
  (let [directory-file (clojure.java.io/file directory)]
    (map #(if (.isDirectory %) (watch-file %)) (file-seq directory-file))))

;; (defn -main
;;   [& args]
;;   (if (> (count args) 0)
;;     (let [project-directory (nth args 0)]
;;       (watch-directory project-directory))
;;     (println "Directory not provided...")))
