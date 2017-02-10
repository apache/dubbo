package redis.util;

import java.util.HashMap;

/**
 * Map that uses byte[]s for keys. Wraps them for you. Passing a non-byte[] or
 * non-BytesKey will result in a CCE.
*/
public class BytesKeyObjectMap<V> extends HashMap<Object, V> {

  private BytesKey makeKey(Object key) {
    return key instanceof byte[] ? new BytesKey((byte[]) key) : (BytesKey) key;
  }

  @Override
  public V get(Object o) {
    return get(makeKey(o));
  }

  public V get(byte[] bytes) {
    return get(new BytesKey(bytes));
  }

  public V get(BytesKey key) {
    return super.get(key);
  }

  @Override
  public boolean containsKey(Object o) {
    return containsKey(makeKey(o));
  }

  public boolean containsKey(byte[] bytes) {
    return containsKey(new BytesKey(bytes));
  }

  public boolean containsKey(BytesKey key) {
    return super.containsKey(key);
  }

  @Override
  public V put(Object o, V value) {
    return put(makeKey(o), value);
  }

  public V put(byte[] bytes, V value) {
    return put(new BytesKey(bytes), value);
  }

  public V put(BytesKey key, V value) {
    return super.put(key, value);
  }

  @Override
  public V remove(Object o) {
    return remove(makeKey(o));
  }

  public V remove(byte[] bytes) {
    return remove(new BytesKey(bytes));
  }

  public V remove(BytesKey key) {
    return super.remove(key);
  }
}
