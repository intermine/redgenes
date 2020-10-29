(ns bluegenes.pages.admin.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [clojure.string :as str]
            [bluegenes.pages.reportpage.utils :as report-utils]))

(reg-sub
 ::root
 (fn [db]
   (:admin db)))

;; Note that this is nil when set to "Default".
(reg-sub
 ::categorize-class
 :<- [::root]
 (fn [admin]
   (:categorize-class admin)))

(reg-sub
 ::categories
 :<- [::root]
 :<- [::categorize-class]
 (fn [[admin categorize]]
   (get-in admin [:categories (or categorize :default)])))

(reg-sub
 ::new-category
 :<- [::root]
 (fn [admin]
   (:new-category admin)))

(reg-sub
 ::available-tool-names
 :<- [:bluegenes.components.tools.subs/installed-tools]
 (fn [tools] ; we do not bother to support filtering by class here
   ;; I think it would be a good idea to do it anyways (re-use `utils/suitable-tools`?)
   ;; We just need to make sure to also support subclasses of the class.
   (->> tools
        (map (fn [tool]
               {:label (get-in tool [:names :human])
                :value (get-in tool [:names :cljs])
                :type "tool"}))
        (sort-by (comp str/lower-case :label)))))

(reg-sub
 ::available-template-names
 :<- [:templates]
 :<- [:current-model]
 (fn [[templates model] [_ class]]
   (->> (report-utils/runnable-templates templates model (some-> class name))
        (map (fn [[_ {:keys [title name]}]]
               {:label title
                :value name
                :type "template"}))
        (sort-by (comp str/lower-case :label)))))

(reg-sub
 ::available-class-names
 :<- [:model]
 (fn [model [_ class]]
   (->> (if class
          (let [{:keys [references collections]} (get model (keyword class))]
            (->> (concat references collections)
                 (map (fn [[_ {:keys [displayName referencedType]}]]
                        {:label displayName
                         :value referencedType
                         :type "class"}))))
          (map (fn [[_ {:keys [displayName name]}]]
                 {:label displayName
                  :value name
                  :type "class"})
               model))
        (sort-by (comp str/lower-case :label)))))

(reg-sub
 ::categories-fallback
 (fn [[_ class]]
   [(subscribe [::available-tool-names class])
    (subscribe [::available-template-names class])
    (subscribe [::available-class-names class])])
 (fn [[tools templates classes] [_ _class]]
   (report-utils/fallback-layout tools classes templates)))