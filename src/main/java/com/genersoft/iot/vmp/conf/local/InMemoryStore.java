package com.genersoft.iot.vmp.conf.local;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 内存版数据存储，模拟 redis 常用数据结构（string/hash/zset/list），
 * 支持 key 过期与 "*"/"?" 通配符匹配。仅在未配置 spring.data.redis.host 时使用。
 */
public class InMemoryStore {

    private static final long NO_EXPIRE = 0L;

    private static class Entry {
        Object data;
        long expireAt;
    }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    // ==================== key 操作 ====================

    public boolean hasKey(String key) {
        purge(key);
        return store.containsKey(key);
    }

    public void expire(String key, long timeout, TimeUnit unit) {
        Entry entry = store.get(key);
        if (entry != null && timeout > 0) {
            entry.expireAt = System.currentTimeMillis() + unit.toMillis(timeout);
        }
    }

    public long getExpire(String key, TimeUnit unit) {
        purge(key);
        Entry entry = store.get(key);
        if (entry == null || entry.expireAt == NO_EXPIRE) {
            return -1L;
        }
        return unit.convert(entry.expireAt - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    public boolean delete(String key) {
        purge(key);
        return store.remove(key) != null;
    }

    public long delete(Collection<String> keys) {
        long count = 0;
        for (String key : keys) {
            if (store.remove(key) != null) {
                count++;
            }
        }
        return count;
    }

    public Set<String> keys(String pattern) {
        Pattern regex = globToRegex(pattern);
        Set<String> result = new HashSet<>();
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Entry> entry : store.entrySet()) {
            if (entry.getValue().expireAt != NO_EXPIRE && entry.getValue().expireAt <= now) {
                store.remove(entry.getKey());
                continue;
            }
            if (regex.matcher(entry.getKey()).matches()) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public List<String> scan(String pattern) {
        return new ArrayList<>(keys(pattern));
    }

    public void purgeExpired() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(entry -> entry.getValue().expireAt != NO_EXPIRE && entry.getValue().expireAt <= now);
    }

    private void purge(String key) {
        Entry entry = store.get(key);
        if (entry != null && entry.expireAt != NO_EXPIRE && entry.expireAt <= System.currentTimeMillis()) {
            store.remove(key);
        }
    }

    private Entry entry(String key) {
        purge(key);
        return store.computeIfAbsent(key, k -> new Entry());
    }

    // ==================== string 操作 ====================

    public void set(String key, Object value) {
        Entry entry = entry(key);
        entry.data = value;
    }

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        Entry entry = entry(key);
        entry.data = value;
        if (timeout > 0) {
            entry.expireAt = System.currentTimeMillis() + unit.toMillis(timeout);
        }
    }

    public Object get(String key) {
        Entry entry = entry(key);
        return entry.data;
    }

    public Long increment(String key, long delta) {
        Entry entry = entry(key);
        synchronized (entry) {
            long oldValue = entry.data instanceof Number number ? number.longValue() : 0L;
            entry.data = oldValue + delta;
            return (Long) entry.data;
        }
    }

    // ==================== hash 操作 ====================

    @SuppressWarnings("unchecked")
    private Map<Object, Object> hash(String key) {
        Entry entry = entry(key);
        if (!(entry.data instanceof Map)) {
            entry.data = new ConcurrentHashMap<>();
        }
        return (Map<Object, Object>) entry.data;
    }

    public void hPut(String key, Object hashKey, Object value) {
        hash(key).put(hashKey, value);
    }

    public Object hGet(String key, Object hashKey) {
        Entry entry = entry(key);
        if (!(entry.data instanceof Map)) {
            return null;
        }
        return ((Map<?, ?>) entry.data).get(hashKey);
    }

    @SuppressWarnings("unchecked")
    public long hDelete(String key, Object... hashKeys) {
        Entry entry = entry(key);
        if (!(entry.data instanceof Map)) {
            return 0L;
        }
        long count = 0;
        for (Object hashKey : hashKeys) {
            if (((Map<Object, Object>) entry.data).remove(hashKey) != null) {
                count++;
            }
        }
        return count;
    }

    public List<Object> hValues(String key) {
        return new ArrayList<>(hash(key).values());
    }

    public Map<Object, Object> hEntries(String key) {
        return new HashMap<>(hash(key));
    }

    public List<Object> hMultiGet(String key, Collection<?> hashKeys) {
        Map<Object, Object> map = hash(key);
        List<Object> result = new ArrayList<>();
        for (Object hashKey : hashKeys) {
            result.add(map.get(hashKey));
        }
        return result;
    }

    public void hPutAll(String key, Map<?, ?> map) {
        hash(key).putAll(map);
    }

    public long hSize(String key) {
        return hash(key).size();
    }

    public List<Map.Entry<Object, Object>> hScan(String key, String pattern) {
        Pattern regex = globToRegex(pattern);
        List<Map.Entry<Object, Object>> result = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : hash(key).entrySet()) {
            if (regex.matcher(String.valueOf(entry.getKey())).matches()) {
                result.add(entry);
            }
        }
        return result;
    }

    // ==================== zset 操作 ====================

    @SuppressWarnings("unchecked")
    private Map<Object, Double> zset(String key) {
        Entry entry = entry(key);
        if (!(entry.data instanceof Map)) {
            entry.data = new ConcurrentHashMap<>();
        }
        return (Map<Object, Double>) entry.data;
    }

    public boolean zAdd(String key, Object member, double score) {
        zset(key).put(member, score);
        return true;
    }

    public Double zScore(String key, Object member) {
        Entry entry = entry(key);
        if (!(entry.data instanceof Map)) {
            return null;
        }
        return (Double) ((Map<?, ?>) entry.data).get(member);
    }

    public Set<Object> zRange(String key, long start, long end, boolean reverse) {
        Set<Object> result = new LinkedHashSet<>();
        for (Map.Entry<Object, Double> entry : slice(sortedZset(zset(key), reverse), start, end)) {
            result.add(entry.getKey());
        }
        return result;
    }

    public Set<Object> zRangeByScore(String key, double min, double max) {
        Set<Object> result = new LinkedHashSet<>();
        for (Map.Entry<Object, Double> entry : sortedZset(zset(key), false)) {
            if (entry.getValue() >= min && entry.getValue() <= max) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public Double zIncrementScore(String key, Object member, double delta) {
        Map<Object, Double> map = zset(key);
        synchronized (map) {
            Double oldValue = map.get(member);
            double newValue = (oldValue == null ? 0D : oldValue) + delta;
            map.put(member, newValue);
            return newValue;
        }
    }

    public long zRemove(String key, Object... members) {
        Map<Object, Double> map = zset(key);
        long count = 0;
        for (Object member : members) {
            if (map.remove(member) != null) {
                count++;
            }
        }
        return count;
    }

    public long zRemoveRangeByScore(String key, double min, double max) {
        Map<Object, Double> map = zset(key);
        long count = 0;
        for (Map.Entry<Object, Double> entry : new ArrayList<>(map.entrySet())) {
            if (entry.getValue() >= min && entry.getValue() <= max) {
                map.remove(entry.getKey());
                count++;
            }
        }
        return count;
    }

    public long zCard(String key) {
        return zset(key).size();
    }

    private List<Map.Entry<Object, Double>> sortedZset(Map<Object, Double> map, boolean reverse) {
        List<Map.Entry<Object, Double>> list = new ArrayList<>(map.entrySet());
        list.sort((a, b) -> {
            int compare = reverse
                    ? Double.compare(b.getValue(), a.getValue())
                    : Double.compare(a.getValue(), b.getValue());
            if (compare != 0) {
                return compare;
            }
            return reverse
                    ? String.valueOf(b.getKey()).compareTo(String.valueOf(a.getKey()))
                    : String.valueOf(a.getKey()).compareTo(String.valueOf(b.getKey()));
        });
        return list;
    }

    // ==================== list 操作 ====================

    @SuppressWarnings("unchecked")
    private Deque<Object> list(String key) {
        Entry entry = entry(key);
        if (!(entry.data instanceof Deque)) {
            entry.data = new ConcurrentLinkedDeque<>();
        }
        return (Deque<Object>) entry.data;
    }

    public long lPush(String key, Object value) {
        Deque<Object> deque = list(key);
        deque.addFirst(value);
        return deque.size();
    }

    public long rPush(String key, Object value) {
        Deque<Object> deque = list(key);
        deque.addLast(value);
        return deque.size();
    }

    public Object lPop(String key) {
        return list(key).pollFirst();
    }

    public Object rPop(String key) {
        return list(key).pollLast();
    }

    public long lSize(String key) {
        return list(key).size();
    }

    public List<Object> lRange(String key, long start, long end) {
        return slice(new ArrayList<>(list(key)), start, end);
    }

    public void lTrim(String key, long start, long end) {
        Deque<Object> deque = list(key);
        List<Object> kept = slice(new ArrayList<>(deque), start, end);
        deque.clear();
        deque.addAll(kept);
    }

    // ==================== 通用工具 ====================

    private <T> Set<T> sliceToSet(List<T> list, long start, long end) {
        return new LinkedHashSet<>(slice(list, start, end));
    }

    private <T> List<T> slice(List<T> list, long start, long end) {
        int size = list.size();
        if (size == 0) {
            return new ArrayList<>();
        }
        long from = start < 0 ? Math.max(size + start, 0) : Math.min(start, size);
        long to = end < 0 ? Math.max(size + end, -1) : Math.min(end, size - 1);
        if (from > to || from >= size) {
            return new ArrayList<>();
        }
        return new ArrayList<>(list.subList((int) from, (int) to + 1));
    }

    private Pattern globToRegex(String pattern) {
        StringBuilder regex = new StringBuilder();
        for (char c : pattern.toCharArray()) {
            if (c == '*') {
                regex.append(".*");
            } else if (c == '?') {
                regex.append('.');
            } else {
                regex.append(Pattern.quote(String.valueOf(c)));
            }
        }
        return Pattern.compile(regex.toString());
    }
}
