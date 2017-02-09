package redis.util;

import java.util.HashSet;

/**
 * Map that uses byte[]s for keys. Wraps them for you. Passing a non-byte[] or
 * non-BytesKey will result in a CCE.
*/
public class BytesKeySet extends HashSet<BytesKey> {

  public boolean add(byte[] member) {
    return super.add(new BytesKey(member));
  }

  @Override
  public boolean contains(Object o) {
    return o instanceof byte[] ? contains((byte[]) o) : super.contains((BytesKey) o);
  }

  @Override
  public boolean remove(Object o) {
    return o instanceof byte[] ? remove((byte[]) o) : super.remove((BytesKey) o);
  }

  public boolean contains(byte[] member) {
    return super.contains(new BytesKey(member));
  }

  public boolean remove(byte[] member) {
    return super.remove(new BytesKey(member));
  }
}
