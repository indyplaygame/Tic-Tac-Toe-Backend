package api.indy.util;

import java.util.HashMap;
import java.util.Map;

public class Util {
    public static String getCookie(String[] cookies, String name) {
        for(String cookie : cookies) {
            String[] parts = cookie.split("=");
            if(parts[0].equals(name)) return parts[1];
        }

        return null;
    }

    public static Map<String, String> getParams(String query) {
        Map<String, String> params = new HashMap<>();

        if(query != null) {
            for(String param : query.split("&")) {
                String[] parts = param.split("=");
                if(parts.length == 2) params.put(parts[0], parts[1]);
            }
        }

        return params;
    }

    public static String getParam(String query, String param) {
        return getParams(query).get(param);
    }
}
