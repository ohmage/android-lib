
package com.google.gson;

import com.google.gson.internal.StringMap;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Set;

public class GsonUtils {
    public static Set<String> getKeys(JsonObject json) {
        Gson gson = new Gson();
        Type mapType = new TypeToken<StringMap<JsonElement>>() {}.getType();
        StringMap<JsonElement> map = gson.fromJson(json, mapType);
        return map.keySet();
    }
}
