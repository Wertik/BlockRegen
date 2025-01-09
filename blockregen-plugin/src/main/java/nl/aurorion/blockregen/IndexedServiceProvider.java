package nl.aurorion.blockregen;

import java.util.HashMap;
import java.util.Map;

public class IndexedServiceProvider<T> {
    private final Map<String, T> services = new HashMap<>();

    public IndexedServiceProvider() {
    }

    public void register(String id, T service) {
        this.services.put(id, service);
    }

    public T getService(String id) {
        return this.services.get(id);
    }

    public boolean hasService(String id) {
        return this.services.containsKey(id);
    }
}
