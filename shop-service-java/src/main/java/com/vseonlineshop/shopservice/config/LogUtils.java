package com.vseonlineshop.shopservice.config;


import java.util.HashMap;
import java.util.Map;

public final class LogUtils {

    private LogUtils() {
    }

    public static Map<String, Object> safeContext(Object... keyValues) {
        Map<String, Object> map = new HashMap<>();
        if (keyValues == null || keyValues.length == 0) {
            return map;
        }
        for (int i = 0; i < keyValues.length; i += 2) {
            String key = String.valueOf(keyValues[i]);
            // Schutz vor unvollständigen Key-Value Paaren
            Object value = (i + 1 < keyValues.length) ? keyValues[i + 1] : "N/A";
            map.put(key, value != null ? value : "null");
        }
        return map;
    }
}