package applab.surveys;

import java.util.HashMap;

public class TranslationMap {

    private HashMap<String, String> languageMap;
    private String languageCode;
    
    public TranslationMap (String languageCode){
        this.languageCode = languageCode;
        this.languageMap = new HashMap<String, String>();
    }

    public HashMap<String, String> getLanguageMap() {
        return languageMap;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public String getTranslation(String key) {

        // If there is no map for this question then just show the key value.
        if (this.languageMap == null || !this.languageMap.containsKey(key)) {
            return key;
        }
        return this.languageMap.get(key);
    }
    
    public void addTranslation(String key, String xlation) {
        this.languageMap.put(key, xlation);
    }
    
    
}
