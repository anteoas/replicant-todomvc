(ns tasks
  (:require
   [babashka.fs :as fs]
   [babashka.http-client :as http]
   [babashka.process :as p]))


(def inspector-zip-url "https://github.com/cjohansen/gadget-inspector/archive/refs/heads/master.zip")

(defn ^:export build-inspector-extension! []
  (println "Building Gadget Inspector")
  (let [build-dir (fs/create-dirs (fs/path "gadget-inspector"))
        zip-file (fs/path build-dir "gadget-inspector.zip")
        _ (println "Downloading inspector")
        zip-data (:body (http/get inspector-zip-url {:as :bytes}))
        gadget-inspector-dir (fs/path build-dir "gadget-inspector-master")
        _ (println "Saving zip to:" zip-file)
        _ (fs/write-bytes zip-file zip-data)
        _ (println "Unzipping  to:" gadget-inspector-dir)
        _ (fs/unzip zip-file build-dir {:replace-existing true})
        _ (println "Buildling Chrome Extension")
        result (p/shell {:dir gadget-inspector-dir} "make" "extension")]
    (if (zero? (:exit result))
      (do
        (println "Extension built to:" (-> (fs/path gadget-inspector-dir "extension")
                                           (fs/absolutize)
                                           str))
        (println "Open chrome://extensions/ in your Chromium browser"))
      (println "Something went wrong building the extension:" result))))

(comment
  (build-inspector-extension!)
  :rcf)

(defn ^:export copy-to-app-dir! []
  (println "Copying to app dir `./replicant-todomvc` ...")

  (let [app-dir "replicant-todomvc"]
    ;; Node modules
    (fs/create-dirs (fs/path app-dir "node_modules" "todomvc-common")
                    {:replace-existing true})
    (fs/copy (fs/path "node_modules" "todomvc-common" "base.css")
             (fs/path app-dir "node_modules" "todomvc-common" "base.css")
             {:replace-existing true})
    (fs/copy (fs/path "node_modules" "todomvc-common" "base.js")
             (fs/path app-dir "node_modules" "todomvc-common" "base.js")
             {:replace-existing true})
    (fs/create-dirs (fs/path app-dir "node_modules" "todomvc-app-css"))
    (fs/copy (fs/path "node_modules" "todomvc-app-css" "index.css")
             (fs/path app-dir "node_modules" "todomvc-app-css" "index.css")
             {:replace-existing true})

    ;; App
    (fs/copy-tree (fs/path "src")
                  (fs/path app-dir "src")
                  {:replace-existing true})
    (fs/copy (fs/path "deps.edn")
             (fs/path app-dir "deps.edn")
             {:replace-existing true})
    (fs/copy (fs/path "package.json")
             (fs/path app-dir "package.json")
             {:replace-existing true})
    (fs/copy (fs/path "README.md")
             (fs/path app-dir "README.md")
             {:replace-existing true})
    (fs/copy (fs/path "shadow-cljs.edn")
             (fs/path app-dir "shadow-cljs.edn")
             {:replace-existing true})
    (fs/copy (fs/path "index.html")
             (fs/path app-dir "index.html")
             {:replace-existing true})
    (fs/create-dirs (fs/path app-dir "js" "compiled"))
    (fs/copy (fs/path "js" "compiled" "main.js")
             (fs/path app-dir "js" "compiled" "main.js")
             {:replace-existing true})
    (p/shell "tree" app-dir)
  ))