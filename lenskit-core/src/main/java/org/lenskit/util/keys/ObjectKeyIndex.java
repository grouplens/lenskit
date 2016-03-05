
public class ObjectKeyIndex<K> extends Object {
    private Object2IntOpenHashMap<K> key2idx;
    private ObjectArrayList<K> keyList;

    public ObjectKeyIndex() {
        this.key2idx = new Object2IntOpenHashMap<>();
        this.keyList = new ObjectArrayList<>();
    }

    int getIndex(K key) {
        return key2idx.get(key);
    }

    K getKey(int idx) {
        return keyList.get(idx);
    }

    boolean containsKey(K key) {
        return key2idx.containsKey(key);
    }

    int size() {
        return keyList.size();
    }

    int setKey(K key) {
        if (key2idx.containsKey(key)) {
            return key2idx.get(key);
        } else {
            int idx = keyList.size();
            key2idx.put(key, idx);
            keyList.add(key);
            return idx;
        }
    }
}
