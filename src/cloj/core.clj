(ns cloj.core
  (:gen-class)
  (:import [java.io.File]
           [java.lang.Runnable])
  (:require [cloj.watch :refer :all]
            [clojure.java.io :as io]
            [clojure.tools.cli :refer :all]))

; Jetbrains backup files generalls end with ___jb___, ___jb_bak___, ___jb_bak_old___, etc.
(def jetbrains-backup-file-suffix "___jb")


;; (def path-prefix "/home/ThomasMadsen/dev/git/egym/server/")
;; (def source-path
;;   (apply str path-prefix "webapp/src/main/java/de/egym/cms/page/"))
;; (def target-path
;;   (apply str path-prefix "webapp/target-gradle/libs/egym-webapp/WEB-INF/classes/de/egym/cms/page/"))

(def source-dir nil)
(def target-dir nil)

(def file-directory-map {})

(defn unix-path-to-string
  [path]
  (.toString (.toUri path)))

(defn file-name-of
  [path]
  (.toString (.getFileName path)))

(defn is-jetbrains-backup-file?
  [file]
  (.contains (.toString (.getFileName file)) jetbrains-backup-file-suffix))

(defn mirror-change-create
  [path]
  (if-not (is-jetbrains-backup-file? path)
    (do 
      (println "Mirroring file creation > " path)
      (let [mapping (get file-directory-map (file-name-of path))
            file-name (file-name-of path)]
        (let [relative-source (clojure.string/replace mapping source-dir "")
              path-from (str mapping "/" file-name)
              path-to (str target-dir relative-source "/" file-name)]
;          (println "relative source for [" file-name "] is " relative-source)
;          (println "from " (str mapping "/" file-name))
;          (println "to " (str target-dir relative-source "/" file-name))
          (io/copy (io/file path-from) (io/file path-to)))))))

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

(defn watch-directory
  [file]
  (let [path (.getAbsolutePath file)]
    (println "Spying on directory ->" path)
    (watch-path path 
                :create event-handler
                :modify event-handler
                :delete event-handler)))

(defn stop
  []
  (stop-watchers))

(defn verify-is-directory
  [file]
  (.isDirectory file))

(defn mirror-directories
  "Recursively starts a watcher on every single file in the directory and mirrors changes from source to dest"
  [source-dir target-dir]
  (def source-dir (.getAbsolutePath source-dir))
  (def target-dir (.getAbsolutePath target-dir))
  (println "Mirroring directories >")
  (println "\tsource > " source-dir)
  (println "\ttarget > " target-dir)
  (println "========================================")
  (doseq [file (file-seq source-dir)]
    (let [key (.getName file)
          val (.getAbsolutePath (.getParentFile file))]
      (def file-directory-map (assoc file-directory-map key val))
      (if (verify-is-directory file)
        (watch-directory file)))))

(def cli-options
  [["-s" "--source SOURCE" "Source directory to mirror files from."
    :parse-fn #(io/file %)
    :validate [verify-is-directory "Source is not a directory."]]
   ["-t" "--target TARGET" "Target directory to mirror files to."
    :parse-fn #(io/file %)
    :validate [verify-is-directory "Target is not a directory."]]])

(defn exit
  [status errors]
  (println errors)
  (System/exit status))

(defn -main
  [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
     (:help options) (exit 0 "Specify source and target directory with -s DIRECTORY -t DIRECTORY respectively.")
     (not= (count options) 2) (exit 1 "Insufficient arguments provided.")
     errors (exit 1 (str "Errors: \n\n" (clojure.string/join \newline errors)))
     :else (do
             (mirror-directories (:source options) (:target options))
             ; Sit in a loop until we are terminated
             (while true)))))
