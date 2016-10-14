package org.nrg.containers.model;

import java.util.HashMap;
import java.util.Map;

public class Context extends HashMap<String, String> {
    private Context() {}

    public static Context newContext() {
        return new Context();
    }

    public static Context fromMap(final Map<String, String> map) {
        final Context context = new Context();
        context.putAll(map);
        return context;
    }

}
