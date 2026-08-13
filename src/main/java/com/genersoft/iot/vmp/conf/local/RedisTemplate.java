package com.genersoft.iot.vmp.conf.local;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 双模式 RedisTemplate 门面。
 * <p>
 * 配置了 spring.data.redis.host（redis 模式）时，内部委托给 spring-data-redis 的真实 RedisTemplate，
 * 序列化、TTL、跨进程消息行为与旧版完全一致；未配置（内存模式）时，使用 JVM 内存实现，无需 Redis 进程。
 * <p>
 * 类名与签名刻意保持与 spring-data-redis 一致，业务代码只需要修改 import。
 */
public class RedisTemplate<K, V> {

    private final InMemoryStore store;
    private final LocalMsgBus msgBus;
    private final org.springframework.data.redis.core.RedisTemplate<K, V> delegate;

    public RedisTemplate(InMemoryStore store, LocalMsgBus msgBus,
                         org.springframework.data.redis.core.RedisTemplate<K, V> delegate) {
        this.store = store;
        this.msgBus = msgBus;
        this.delegate = delegate;
    }

    public boolean isRedisMode() {
        return delegate != null;
    }

    public Object getConnectionFactory() {
        return delegate == null ? null : delegate.getConnectionFactory();
    }

    public void convertAndSend(String channel, Object message) {
        if (delegate != null) {
            delegate.convertAndSend(channel, message);
        } else {
            msgBus.convertAndSend(channel, message);
        }
    }

    public Boolean hasKey(K key) {
        return delegate != null ? delegate.hasKey(key) : store.hasKey(keyToString(key));
    }

    public Boolean expire(K key, long timeout, TimeUnit unit) {
        if (delegate != null) {
            return delegate.expire(key, timeout, unit);
        }
        store.expire(keyToString(key), timeout, unit);
        return true;
    }

    public Boolean expire(K key, Duration timeout) {
        return expire(key, timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    public Long getExpire(K key, TimeUnit unit) {
        return delegate != null ? delegate.getExpire(key, unit) : store.getExpire(keyToString(key), unit);
    }

    public Boolean delete(K key) {
        return delegate != null ? delegate.delete(key) : store.delete(keyToString(key));
    }

    public Long delete(Collection<K> keys) {
        if (delegate != null) {
            return delegate.delete(keys);
        }
        List<String> keyList = new ArrayList<>();
        for (K key : keys) {
            keyList.add(keyToString(key));
        }
        return store.delete(keyList);
    }

    public Set<K> keys(K pattern) {
        if (delegate != null) {
            return delegate.keys(pattern);
        }
        Set<K> result = new HashSet<>();
        for (String key : store.keys(keyToString(pattern))) {
            result.add((K) key);
        }
        return result;
    }

    public List<Object> scanKeys(String pattern) {
        if (delegate != null) {
            Set<String> resultKeys = delegate.execute((RedisCallback<Set<String>>) connection -> {
                ScanOptions scanOptions = ScanOptions.scanOptions().match("*" + pattern + "*").count(1000).build();
                Cursor<byte[]> scan = connection.scan(scanOptions);
                Set<String> keys = new HashSet<>();
                while (scan.hasNext()) {
                    keys.add(new String(scan.next()));
                }
                return keys;
            });
            return resultKeys == null ? new ArrayList<>() : new ArrayList<>(resultKeys);
        }
        return new ArrayList<Object>(store.scan("*" + pattern + "*"));
    }

    public ValueOperations opsForValue() {
        return new ValueOperations();
    }

    public HashOperations opsForHash() {
        return new HashOperations();
    }

    public ZSetOperations opsForZSet() {
        return new ZSetOperations();
    }

    public ListOperations opsForList() {
        return new ListOperations();
    }

    private String keyToString(K key) {
        return String.valueOf(key);
    }

    public class ValueOperations {

        public void set(K key, V value) {
            if (delegate != null) {
                delegate.opsForValue().set(key, value);
            } else {
                store.set(keyToString(key), value);
            }
        }

        public void set(K key, V value, long timeout, TimeUnit unit) {
            if (delegate != null) {
                delegate.opsForValue().set(key, value, timeout, unit);
            } else {
                store.set(keyToString(key), value, timeout, unit);
            }
        }

        public void set(K key, V value, Duration timeout) {
            set(key, value, timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        public V get(Object key) {
            return delegate != null ? delegate.opsForValue().get(key) : (V) store.get(String.valueOf(key));
        }

        public Long increment(K key, long delta) {
            return delegate != null
                    ? delegate.opsForValue().increment(key, delta)
                    : store.increment(keyToString(key), delta);
        }
    }

    public class HashOperations {

        public void put(K key, Object hashKey, Object value) {
            if (delegate != null) {
                delegate.opsForHash().put(key, hashKey, value);
            } else {
                store.hPut(keyToString(key), hashKey, value);
            }
        }

        public Object get(K key, Object hashKey) {
            return delegate != null
                    ? delegate.opsForHash().get(key, hashKey)
                    : store.hGet(keyToString(key), hashKey);
        }

        public Long delete(K key, Object... hashKeys) {
            return delegate != null
                    ? delegate.opsForHash().delete(key, hashKeys)
                    : store.hDelete(keyToString(key), hashKeys);
        }

        public List<Object> values(K key) {
            return delegate != null
                    ? delegate.opsForHash().values(key)
                    : store.hValues(keyToString(key));
        }

        public Map<Object, Object> entries(K key) {
            if (delegate != null) {
                Map<Object, Object> result = new HashMap<>();
                delegate.opsForHash().entries(key).forEach(result::put);
                return result;
            }
            return store.hEntries(keyToString(key));
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        public List<Object> multiGet(K key, Collection<?> hashKeys) {
            return delegate != null
                    ? delegate.opsForHash().multiGet(key, (Collection) hashKeys)
                    : store.hMultiGet(keyToString(key), hashKeys);
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        public void putAll(K key, Map<?, ?> map) {
            if (delegate != null) {
                delegate.opsForHash().putAll(key, (Map) map);
            } else {
                store.hPutAll(keyToString(key), map);
            }
        }

        public Long size(K key) {
            return delegate != null ? delegate.opsForHash().size(key) : store.hSize(keyToString(key));
        }

        public Cursor<Map.Entry<Object, Object>> scan(K key, ScanOptions options) {
            if (delegate != null) {
                return delegate.opsForHash().scan(key, options);
            }
            String pattern = options.getPattern() == null ? "*" : options.getPattern();
            return new MemoryCursor(store.hScan(keyToString(key), pattern), 0L);
        }
    }

    public class ZSetOperations {

        public Boolean add(K key, V member, double score) {
            return delegate != null
                    ? delegate.opsForZSet().add(key, member, score)
                    : store.zAdd(keyToString(key), member, score);
        }

        public Double score(K key, Object member) {
            return delegate != null
                    ? delegate.opsForZSet().score(key, member)
                    : store.zScore(keyToString(key), member);
        }

        @SuppressWarnings("unchecked")
        public Set<V> range(K key, long start, long end) {
            return delegate != null
                    ? delegate.opsForZSet().range(key, start, end)
                    : (Set<V>) (Set<?>) store.zRange(keyToString(key), start, end, false);
        }

        @SuppressWarnings("unchecked")
        public Set<V> reverseRange(K key, long start, long end) {
            return delegate != null
                    ? delegate.opsForZSet().reverseRange(key, start, end)
                    : (Set<V>) (Set<?>) store.zRange(keyToString(key), start, end, true);
        }

        @SuppressWarnings("unchecked")
        public Set<V> rangeByScore(K key, double min, double max) {
            return delegate != null
                    ? delegate.opsForZSet().rangeByScore(key, min, max)
                    : (Set<V>) (Set<?>) store.zRangeByScore(keyToString(key), min, max);
        }

        public Double incrementScore(K key, V member, double delta) {
            return delegate != null
                    ? delegate.opsForZSet().incrementScore(key, member, delta)
                    : store.zIncrementScore(keyToString(key), member, delta);
        }

        public Long remove(K key, Object... members) {
            return delegate != null
                    ? delegate.opsForZSet().remove(key, members)
                    : store.zRemove(keyToString(key), members);
        }

        public Long removeRangeByScore(K key, double min, double max) {
            return delegate != null
                    ? delegate.opsForZSet().removeRangeByScore(key, min, max)
                    : store.zRemoveRangeByScore(keyToString(key), min, max);
        }

        public Long zCard(K key) {
            return delegate != null ? delegate.opsForZSet().zCard(key) : store.zCard(keyToString(key));
        }
    }

    public class ListOperations {

        public Long leftPush(K key, V value) {
            return delegate != null
                    ? delegate.opsForList().leftPush(key, value)
                    : store.lPush(keyToString(key), value);
        }

        public Long rightPush(K key, V value) {
            return delegate != null
                    ? delegate.opsForList().rightPush(key, value)
                    : store.rPush(keyToString(key), value);
        }

        @SuppressWarnings("unchecked")
        public V leftPop(K key) {
            return delegate != null
                    ? delegate.opsForList().leftPop(key)
                    : (V) store.lPop(keyToString(key));
        }

        @SuppressWarnings("unchecked")
        public V rightPop(K key) {
            return delegate != null
                    ? delegate.opsForList().rightPop(key)
                    : (V) store.rPop(keyToString(key));
        }

        public Long size(K key) {
            return delegate != null ? delegate.opsForList().size(key) : store.lSize(keyToString(key));
        }

        @SuppressWarnings("unchecked")
        public List<V> range(K key, long start, long end) {
            return delegate != null
                    ? delegate.opsForList().range(key, start, end)
                    : (List<V>) (List<?>) store.lRange(keyToString(key), start, end);
        }

        public void trim(K key, long start, long end) {
            if (delegate != null) {
                delegate.opsForList().trim(key, start, end);
            } else {
                store.lTrim(keyToString(key), start, end);
            }
        }
    }

    private static class MemoryCursor implements Cursor<Map.Entry<Object, Object>> {

        private final java.util.Iterator<Map.Entry<Object, Object>> iterator;
        private final long cursorId;
        private long position;
        private boolean closed;

        MemoryCursor(List<Map.Entry<Object, Object>> entries, long cursorId) {
            this.iterator = entries.iterator();
            this.cursorId = cursorId;
        }

        @Override
        public boolean hasNext() {
            return !closed && iterator.hasNext();
        }

        @Override
        public Map.Entry<Object, Object> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            position++;
            return iterator.next();
        }

        @Override
        public void close() {
            closed = true;
        }

        @Override
        public CursorId getId() {
            return CursorId.of(cursorId);
        }

        @Override
        public long getCursorId() {
            return cursorId;
        }

        @Override
        public boolean isClosed() {
            return closed;
        }

        @Override
        public long getPosition() {
            return position;
        }
    }
}
