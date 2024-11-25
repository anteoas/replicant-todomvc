(ns todomvc.views-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [lookup.core :as l]
   [todomvc.actions :as a]
   [todomvc.views :as sut]
   [clojure.string :as string]))

#_{:clj-kondo/ignore [:private-call]}
(deftest maybe-add
  (testing "Adding non-blank"
    (let [result (sut/maybe-add [] "New item")]
      (is (= 1
             (count result))
          "it adds an item")
      (is (= "New item"
             (:item/title (first result)))
          "it populates the title")
      (is (false? (:item/completed? (first result)))
          "it adds the item as uncompleted")
      (is (uuid? (:item/id (first result)))
          "it gives the item an id"))
    (is (= "Second item" (-> (sut/maybe-add [{:item/title "First item"}] "Second item")
                             second
                             :item/title))
        "it adds the item to the end of the list")
    (is (= "New item" (-> (sut/maybe-add [] "  New item  ")
                          first
                          :item/title))
        "it trims the string before adding using it as the title for the item"))

  (testing "Blank or empty"
    (is (= 0 (count (sut/maybe-add [] "")))
        "it does not add a new item when the string is empty")
    (is (= 0 (count (sut/maybe-add [] "   ")))
        "it does not add a new item when the string is blank")))

(defn- select-attribute
  [selector path data]
  (let [elements (l/select selector data)]
    (->> (keep (fn [element]
                 (when (map? (second element))
                   (get-in (second element) path)))
               elements))))

(defn- flatten-actionss [actionss]
  (reduce into [] actionss))

(defn- select-actions [selector path data]
  (->> (select-attribute selector path data)
       flatten-actionss))

#_{:clj-kondo/ignore [:private-call]}
(defn test-add-view-mount [state]
  #_{:clj-kondo/ignore [:inline-def :clojure-lsp/unused-public-var]}
  (comment
    (def state {}))
  (let [on-mount-actions (->> (sut/add-view state)
                              (select-actions :input.new-todo [:replicant/on-mount]))
        {:keys [new-state effects] :as result} (a/handle-actions state
                                                                 {:replicant/node :input-dom-node}
                                                                 on-mount-actions)]
    (is (= :input-dom-node
           (:add/draft-input-element new-state)))
    (is (empty? effects)
        "it does so without other side-effects")
    result))

#_{:clj-kondo/ignore [:private-call]}
(defn test-add-view-input [state add-text]
  #_{:clj-kondo/ignore [:inline-def :clojure-lsp/unused-public-var]}
  (comment
    (def state {:add/draft-input-element :input-dom-node})
    (def add-text "Input"))
  (let [on-input-actions (->> (sut/add-view state)
                              (select-actions :input.new-todo [:on :input]))
        {:keys [new-state effects] :as result} (a/handle-actions state
                                                                 {:replicant/js-event (clj->js {:target {:value add-text}})}
                                                                 on-input-actions)]
    (is (= add-text
           (:add/draft new-state)))
    (is (empty? effects)
        "it does so without other side-effects")
    result))

#_{:clj-kondo/ignore [:private-call]}
(deftest add-view
  (testing "it saves draft input element on mount"
    (let [initial-state {:app/todo-items [{:item/completed? true
                                           :item/id "id-1"}]
                         :app/mark-all-checkbox-checked? true}
          {:keys [new-state]} (test-add-view-mount initial-state)]

      (testing "it updates the draft from the `.new-todo` input event"
        (let [input-text "Input"
              {:keys [new-state]} (test-add-view-input new-state input-text)]

          (testing "it handles the form submit event"
            (let [on-submit-actions (->> (sut/add-view new-state)
                                         (select-actions :form [:on :submit]))
                  {:keys [new-state effects]} (a/handle-actions new-state
                                                                {}
                                                                on-submit-actions)]
              (is (= input-text
                     (-> new-state :app/todo-items second :item/title))
                  "it adds the new item to the todo items")
              (is (false? (-> new-state :app/todo-items second :item/completed?))
                  "it adds the new item as uncompleted")
              (is (false? (:app/mark-all-checkbox-checked? new-state))
                  "it updates the mark-all state to true")
              (is (= ""
                     (:add/draft new-state))
                  "it clears the draft")
              (is (some #{[:dom/fx.prevent-default]}
                        (set effects))
                  "it prevents the default form submit action")
              (is (some #{[:dom/fx.set-input-text :input-dom-node ""]}
                        (set effects))
                  "it clears the input element")))))

      (testing "it trims the input"
        (let [input-text "Input"
              untrimmed-test (str "  " input-text "  ")
              {:keys [new-state]} (test-add-view-input new-state untrimmed-test)]

          (testing "it handles the form submit event"
            (let [on-submit-actions (->> (sut/add-view new-state)
                                         (select-actions :form [:on :submit]))
                  {:keys [new-state]} (a/handle-actions new-state
                                                        {}
                                                        on-submit-actions)]
              (is (= input-text
                     (-> new-state :app/todo-items second :item/title))
                  "it adds the new item with trimmed text to the todo items")))))

      (testing "it doesn't add an item when the input is empty"
        (let [input-text ""
              {:keys [new-state]} (test-add-view-input new-state input-text)]

          (testing "it handles the form submit event"
            (let [on-submit-actions (->> (sut/add-view new-state)
                                         (select-actions :form [:on :submit]))
                  {:keys [new-state effects]} (a/handle-actions new-state
                                                                {}
                                                                on-submit-actions)]
              (is (= (:app/todo-items initial-state)
                     (:app/todo-items new-state))
                  "it does not add a new item to the todo items")
              (is (= ""
                     (:add/draft new-state))
                  "it clears the draft")
              (is (some #{[:dom/fx.prevent-default]}
                        (set effects))
                  "it prevents the default form submit action")
              (is (some #{[:dom/fx.set-input-text :input-dom-node ""]}
                        (set effects))
                  "the input element remains blank"))))))))

#_{:clj-kondo/ignore [:private-call]}
(deftest edit-view
  (testing "rendering the edit view"
    (let [initial-state {:edit/editing-item-index 0}
          edit-view (sut/edit-view initial-state 0 {})]
      (is (seq (l/select :input.edit edit-view))
          "it renders the edit view when the index matches the editing item index"))

    (let [initial-state {}
          edit-view (sut/edit-view initial-state 0 {})]
      (is (nil? edit-view)
          "it does not render the edit view when its index does not match the editing item index"))

    (let [initial-state  {:edit/editing-item-index 0
                          :edit/keyup-code "Escape"}
          edit-view (sut/edit-view initial-state 0 {})]
      (is (nil? edit-view)
          "it does not render the edit view when the keycode is 'Escape'")))

  (testing "edit-view on mount"
    (let [initial-state {:edit/editing-item-index 0}
          on-mount-actions (->> (sut/edit-view initial-state 0 {})
                                (select-actions :input.edit [:replicant/on-mount]))
          {:keys [new-state effects]} (a/handle-actions initial-state
                                                        {:replicant/node :input-dom-node}
                                                        on-mount-actions)]
      (is (some #{[:dom/fx.focus-element :input-dom-node]}
                (set effects))
          "it focuses the input element")
      (is (= initial-state
             new-state)
          "it does not modify the state")))

  (testing "it populates the edit view from the edited item"
    (let [item {:item/title "Title"}
          initial-state {:edit/editing-item-index 0}
          edit-view (sut/edit-view initial-state 0 item)]
      (is (= [(:item/title item)]
             (select-attribute :input.edit [:value] edit-view))
          "it populates the input with the item title")))

  (testing "it updates the draft from the input event"
    (let [initial-state {:edit/editing-item-index 0}
          on-input-actions (->> (sut/edit-view initial-state 0 {})
                                (select-actions :input.edit [:on :input]))
          {:keys [new-state effects]} (a/handle-actions initial-state
                                                        {:replicant/js-event (clj->js {:target {:value "Input"}})}
                                                        on-input-actions)]
      (is (= "Input"
             (:edit/draft new-state))
          "it updates the draft")
      (is (empty? effects)
          "it does so without other side-effects")))

  (testing "it saves the keycode to the state on keyup"
    (let [initial-state {:edit/editing-item-index 0}
          on-keyup-actions (->> (sut/edit-view initial-state 0 {})
                                (select-actions :input.edit [:on :keyup]))
          {:keys [new-state effects]} (a/handle-actions initial-state
                                                        {:replicant/js-event (clj->js {:code "Escape"})}
                                                        on-keyup-actions)]
      (is (= "Escape"
             (:edit/keyup-code new-state))
          "it saves the keycode")
      (is (empty? effects)
          "it does so without other side-effects")))

  (testing "it removes the editing index on blur"
    (let [initial-state {:edit/editing-item-index 0}
          on-blur-actions (->> (sut/edit-view initial-state 0 {})
                               (select-actions :input.edit [:on :blur]))
          {:keys [new-state effects]} (a/handle-actions initial-state
                                                        {}
                                                        on-blur-actions)]
      on-blur-actions
      (is (nil?
           (:edit/editing-item-index new-state))
          "it removes the editing index")
      (is (empty? effects)
          "it does so without other side-effects")))

  (testing "it removes the editing index on form submit"
    (let [initial-state {:edit/editing-item-index 0}
          on-submit-actions (->> (sut/edit-view initial-state 0 {})
                                 (select-actions :form [:on :submit]))
          {:keys [new-state effects]} (a/handle-actions initial-state
                                                        {}
                                                        on-submit-actions)]
      (is (nil?
           (:edit/editing-item-index new-state))
          "it removes the editing index")
      (is (some #{[:dom/fx.prevent-default]}
                (set effects))
          "it prevents the default form submission")))

  (testing "end editing"
    (doseq [[input trim-case] [["Input" "no trimming needed"]
                               ["  Input  " "trimming needed"]]]
      (let [item {:item/title "Title"}
            initial-state {:edit/editing-item-index 0
                           :edit/draft input
                           :app/todo-items [item]}
            on-unmount-actions (->> (sut/edit-view initial-state 0 item)
                                    (select-actions :form [:replicant/on-unmount]))
            {:keys [new-state effects]} (a/handle-actions initial-state
                                                          {}
                                                          on-unmount-actions)]
        (testing trim-case
          (is (= (string/trim input)
                 (-> new-state :app/todo-items first :item/title))
              "it updates the item title correctly")
          (is (nil? (:edit/editing-item-index new-state))
              "it removes the editing index")
          (is (nil? (:edit/keyup-code new-state))
              "it removes the keycode")
          (is (empty? effects)
              "it does not have other side-effects"))))

    (doseq [[input input-case] [["" "Remove empty"]
                                ["  " "Remove blank"]]]
      (let [item {:item/title "Title2"}
            items [{:item/title "Title1"}
                   item
                   {:item/title "Title3"}]
            initial-state {:edit/editing-item-index 1
                           :edit/keyup-code "Enter"
                           :edit/draft input
                           :app/todo-items items}
            on-unmount-actions (->> (sut/edit-view initial-state 1 (second items))
                                    (select-actions :form [:replicant/on-unmount]))
            {:keys [new-state effects]} (a/handle-actions initial-state
                                                          {}
                                                          on-unmount-actions)]
        (testing input-case
          (is (= [(first items) (last items)]
                 (:app/todo-items new-state))
              "it removes the item")
          (is (nil? (:edit/editing-item-index new-state))
              "it removes the editing index")
          (is (nil? (:edit/keyup-code new-state))
              "it removes the keycode")
          (is (empty? effects)
              "it does not have other side-effects"))))

    (let [completed-item {:item/title "Title2"
                          :item/completed? true}
          uncompleted-item {:item/title "Title2"
                            :item/completed? false}]
      (doseq [[items mark-all-state mark-all-case items-behaviour] [[[{:item/title "Title1"
                                                                       :item/completed? false}
                                                                      completed-item
                                                                      {:item/title "Title3"
                                                                       :item/completed? false}]
                                                                     false
                                                                     "remaining items are uncompleted"
                                                                     "it sets the mark-all state to false"]
                                                                    [[{:item/title "Title1"
                                                                       :item/completed? true}
                                                                      uncompleted-item
                                                                      {:item/title "Title3"
                                                                       :item/completed? true}]
                                                                     true
                                                                     "remaining items are completed"
                                                                     "it sets the mark-all state to true"]]]
        (let [input ""
              initial-state {:edit/editing-item-index 1
                             :edit/keyup-code "Enter"
                             :edit/draft input
                             :app/todo-items items}
              on-unmount-actions (->> (sut/edit-view initial-state 1 (second items))
                                      (select-actions :form [:replicant/on-unmount]))
              {:keys [new-state]} (a/handle-actions initial-state
                                                            {}
                                                            on-unmount-actions)]
          (testing mark-all-case
            (is (= mark-all-state
                   (:app/mark-all-checkbox-checked? new-state))
                items-behaviour)))))

    (testing "Escape key"
      ; Notes:
      ; :app/mark-all-state
      ;   * The initial states has no :app/mark-all-state key, even though in “reality” it would have
      ;     We do this so that we can test that its not updated
      ;
      ; :edit/keyup-code and the element lifecycle
      ;   * The state rendering the view had no `:edit/keyup-code "Escape"` entry,
      ;     hence the edit UI was rendered
      ;   * The state captured at unmounting the view _had_ an `:edit/keyup-code "Escape"` entry,
      ;     and this should cause the indexed todo item to be left unchanged
      (let [item {:item/title "Title2"
                  :item/completed? true}
            initial-items [{:item/title "Title1"
                            :item/completed? false}
                           item
                           {:item/title "Title3"
                            :item/completed? false}]
            rendering-state {:edit/editing-item-index 1
                             :edit/keyup-code "KeyT" ; Doesn't matter, but if the "t" in "Input" was typed last...
                             :edit/draft "Input"
                             :app/todo-items initial-items}
            unmounting-state (assoc rendering-state :edit/keyup-code "Escape")
            on-unmount-actions (->> (sut/edit-view rendering-state 1 item)
                                    (select-actions :form [:replicant/on-unmount]))
            {:keys [new-state effects]} (a/handle-actions unmounting-state
                                                          {}
                                                          on-unmount-actions)]
        (is (= initial-items
               (:app/todo-items new-state))
            "it doesn't update any item")
        (is (nil? (:edit/editing-item-index new-state))
            "it removes the editing index")
        (is (nil? (:edit/keyup-code new-state))
            "it removes the keycode")
        (is (empty? effects)
            "it does not have other side-effects")
        (is (nil? (:app/mark-all-checkbox-checked? new-state))
            "it does not update the mark-all state")))))

(deftest item-view
  (testing "item rendering"
    (let [item {:item/title "Test Item"}
          view (sut/item-view {:app/item-filter :filter/all} 0 item)]
      (is (seq (l/select [:li :.view] view))
          "it renders the item in a list item containing a .view element")
      (is (= [[:label (:item/title item)]]
             (l/select :label view))
          "it displays the item's title"))

    (testing "filtering"
      (is (not= nil?
                (sut/item-view {:app/item-filter :filter/all} 0 {:item/title "Test Item"
                                                                 :item/completed? true}))
          "it renders a completed item when filtering on all")
      (is (not= nil?
                (sut/item-view {:app/item-filter :filter/all} 0 {:item/title "Test Item"
                                                                 :item/completed? false}))
          "it renders an uncompleted item when filtering on all")
      (is (nil?
           (sut/item-view {:app/item-filter :filter/completed} 0 {:item/title "Test Item"
                                                                  :item/completed? false}))
          "it does not render an uncompleted item when filtering on completed")
      (is (not= nil?
                (sut/item-view {:app/item-filter :filter/completed} 0 {:item/title "Test Item"
                                                                       :item/completed? true}))
          "it renders a completed items when filtering on completed")
      (is (nil?
           (sut/item-view {:app/item-filter :filter/active} 0 {:item/title "Test Item"
                                                               :item/completed? true}))
          "it does not render a completed item when filtering on active")
      (is (not= nil?
                (sut/item-view {:app/item-filter :filter/active} 0 {:item/title "Test Item"
                                                                    :item/completed? false}))
          "it renders an uncompleted item when filtering on active"))

    (testing "editing"
      (is (some #{"editing"}
                (->> (sut/item-view {:edit/editing-item-index 0
                                     :app/item-filter :filter/all}
                                    0
                                    {})
                     (select-attribute :li [:class])
                     first))
          "The item should have the 'editing' class when it is being edited")
      (is (not-any? #{"editing"}
                    (->> (sut/item-view {:edit/editing-item-index 0
                                         :app/item-filter :filter/all}
                                        1
                                        {})
                         (select-attribute :li [:class])
                         first))
          "The item should not have the 'editing' class when another item is being edited")
      (is (not-any? #{"editing"}
                    (->> (sut/item-view {:app/item-filter :filter/all}
                                        1
                                        {})
                         (select-attribute :li [:class])
                         first))
          "The item should not have the 'editing' class when no item is being edited"))

    (testing "completed"
      (is (some #{"completed"}
                (->> (sut/item-view {:app/item-filter :filter/all}
                                    0 {:item/title "Test Item"
                                       :item/completed? true})
                     (select-attribute :li [:class])
                     first))
          "The item should have the 'completed' class when it is completed")
      (is (not-any? #{"completed"}
                    (->> (sut/item-view {:app/item-filter :filter/all}
                                        0 {:item/title "Test Item"
                                           :item/completed? false})
                         (select-attribute :li [:class])
                         first))
          "The item should not have the 'completed' class when it is uncompleted")))

  (testing "enabling editing mode"
    (let [index 0
          state {:app/item-filter :filter/all}
          item {:item/title "Test Item"
                :item/completed? false}
          view (sut/item-view state index item)
          on-dblclick-actions (select-actions :li [:on :dblclick] view)
          {:keys [new-state]} (a/handle-actions state {} on-dblclick-actions)]
      (is (= index
             (:edit/editing-item-index new-state))
          "it enables editing mode for the item")
      (is (= (:item/title item)
             (:edit/draft new-state))
          "it sets the draft to the item title")))

  (testing "the toggle checkbox"
    (testing "uncompleted item"
      (let [state {:edit/editing-item-index nil
                   :app/item-filter :filter/all
                   :app/todo-items [{:item/title "Test Item" :item/completed? false}]
                   :app/mark-all-checkbox-checked? false}
            item (first (:app/todo-items state))
            view (sut/item-view state 0 item)]
        (is (false? (-> (select-attribute :input.toggle [:checked] view) first))
            "it is not checked initially")
        (let [on-change-actions (select-actions :input.toggle [:on :change] view)
              {:keys [new-state]} (a/handle-actions state {} on-change-actions)]
          (is (true? (-> new-state :app/todo-items first :item/completed?))
              "it marks the item as completed")
          (is (true? (:app/mark-all-checkbox-checked? new-state))
              "it updates the mark-all state to true"))))

    (testing "completed item"
      (let [state {:edit/editing-item-index nil
                   :app/item-filter :filter/all
                   :app/todo-items [{:item/title "Test Item" :item/completed? true}]
                   :app/mark-all-checkbox-checked? true}
            item (first (:app/todo-items state))
            view (sut/item-view state 0 item)]
        (is (true? (-> (select-attribute :input.toggle [:checked] view) first))
            "it is checked initially")
        (let [on-change-actions (select-actions :input.toggle [:on :change] view)
              {:keys [new-state]} (a/handle-actions state {} on-change-actions)]
          (is (false? (-> new-state :app/todo-items first :item/completed?))
              "it marks the item as not completed")
          (is (false? (:app/mark-all-checkbox-checked? new-state))
              "it updates the mark-all state to false")))))

  (testing "delete button"
    (is (seq (l/select :button.destroy (sut/item-view {:app/item-filter :filter/all} 0 {})))
        "it renders a delete button")

    (testing "Last item"
      (let [state {:app/item-filter :filter/all
                   :app/todo-items [{:item/title "Test Item"
                                     :item/completed? true}]
                   :app/mark-all-checkbox-checked? true}
            item (first (:app/todo-items state))
            view (sut/item-view state 0 item)
            on-click-actions (select-actions :button.destroy [:on :click] view)
            {:keys [new-state effects]} (a/handle-actions state {} on-click-actions)]
        (is (empty? (:app/todo-items new-state))
            "it removes the item from the todo list")
        (is (empty? effects)
            "it does so without other side-effects")
        (is (false? (:app/mark-all-checkbox-checked? new-state))
            "it updates the mark-all state to false")))

    (testing "Last completed item"
      (let [state {:app/item-filter :filter/all
                   :app/todo-items [{:item/title "Test Item 1" :item/completed? true}
                                    {:item/title "Test Item 2" :item/completed? false}
                                    {:item/title "Test Item 3" :item/completed? true}]
                   :app/mark-all-checkbox-checked? false}
            item (second (:app/todo-items state))
            view (sut/item-view state 1 item)
            on-click-actions (select-actions :button.destroy [:on :click] view)
            {:keys [new-state effects]} (a/handle-actions state {} on-click-actions)]
        (is (= 2 (count (:app/todo-items new-state)))
            "it removes the item from the todo list")
        (is (empty? effects)
            "it does so without other side-effects")
        (is (true? (:app/mark-all-checkbox-checked? new-state))
            "it updates mark-all state to true when all remaining items are completed")))

    (testing "Last uncompleted item"
      (let [state {:app/item-filter :filter/all
                   :app/todo-items [{:item/title "Test Item 1" :item/completed? false}
                                    {:item:title "Test Item 2" :item/completed? true}
                                    {:item:title "Test Item 3" :item/completed? false}]
                   :app/mark-all-checkbox-checked? false}
            item (second (:app/todo-items state))
            view (sut/item-view state 1 item)
            on-click-actions (select-actions :button.destroy [:on :click] view)
            {:keys [new-state effects]} (a/handle-actions state {} on-click-actions)]
        (is (= 2 (count (:app/todo-items new-state)))
            "it removes the item from the todo list")
        (is (empty? effects)
            "it does so without other side-effects")
        (is (false? (:app/mark-all-checkbox-checked? new-state))
            "it keeps the mark-all state false when all remaining items are uncompleted")))

    (testing "Uncompleted items remaining"
      (let [state {:app/item-filter :filter/all
                   :app/todo-items [{:item/title "Test Item 1" :item/completed? true}
                                    {:item:title "Test Item 2" :item/completed? false}
                                    {:item:title "Test Item 3" :item/completed? true}]
                   :app/mark-all-checkbox-checked? false}
            item (second (:app/todo-items state))
            view (sut/item-view state 1 item)
            on-click-actions (select-actions :button.destroy [:on :click] view)
            {:keys [new-state effects]} (a/handle-actions state {} on-click-actions)]
        (is (= 2 (count (:app/todo-items new-state)))
            "it removes the item from the todo list")
        (is (empty? effects)
            "it does so without other side-effects")
        (is (true? (:app/mark-all-checkbox-checked? new-state))
            "it updates the mark-all state false when some remaining items are completed and some are uncompleted")))))

(deftest app-view
  (testing ".todoapp element"
    (is (seq (l/select '.todoapp (sut/app-view {})))
        "it shows a `.todoapp` element")
    (is (= '([:h1 "todos"])
           (l/select '[.todoapp .header h1] (sut/app-view {})))
        "it contains a `.header` element with a h1 element with the text `todos`")

    (testing "the .-new-todo input"
      (is (= '(true)
             (select-attribute '[.todoapp .header input.new-todo]
                               [:autofocus]
                               (sut/app-view {})))
          "it is an `input` contained in the `.header` element inside `.todoapp`")
      (is (= '(true)
             (select-attribute '[.todoapp .header input.new-todo]
                               [:autofocus]
                               (sut/app-view {})))
          "it is present with an empty app state")
      (is (= '(true)
             (select-attribute '[.todoapp .header input.new-todo]
                               [:autofocus]
                               (sut/app-view {:app/todo-items [{:item/title "First item"}]})))
          "it is present with items in the app state")))

  (testing "the `.todoapp` `.main` element"
    (is (empty? (l/select '[.todoapp .main] (sut/app-view {})))
        "it has no `.main` view when there are no items")
    (is (seq (l/select '[.todoapp .main] (sut/app-view {:app/todo-items [{:item/title "First item"}]})))
        "it has a `.main` view when there are items"))

  (testing "the `.todoapp` `.footer` element"
    (is (empty? (l/select '[.todoapp .footer] (sut/app-view {})))
        "it has no `.footer` when there are no items")
    (is (seq (l/select '[.todoapp .footer] (sut/app-view {:app/todo-items [{:item/title "First item"}]})))
        "it has a `.footer` when there are items"))

  (testing "Prevent default"
    (is (every? (partial some #{[:dom/ax.prevent-default]})
                (->> (sut/app-view {:app/todo-items [{:item/title "First item"}]
                                    :edit/editing-item-index 0
                                    :app/item-filter :filter/all})
                     (select-attribute 'form [:on :submit])
                     (map set)))
        "all form-submits have a prevent-default action")))
