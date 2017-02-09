package redis.util;

/**
 * Doesn't precalculate the hashCode since it is a value and not a key.
 * <p/>
 * User: sam
 * Date: 10/20/12
 * Time: 5:08 PM
 */
public class BytesValue {
  protected final byte[] bytes;

  public BytesValue(byte[] bytes) {
    this.bytes = bytes;
  }

  @Override
  public int hashCode() {
    int hashCode = 0;
    for (byte aByte : this.bytes) {
      hashCode += 43 * aByte;
    }
    return hashCode;
  }

  public static boolean equals(byte[] thisBytes, byte[] otherBytes) {
    int length = thisBytes.length;
    if (length != otherBytes.length) {
      return false;
    }
    int half = length / 2;
    for (int i = 0; i < half; i++) {
      int end = length - i - 1;
      if (thisBytes[end] != otherBytes[end]) return false;
      if (thisBytes[i] != otherBytes[i]) return false;
    }
    if (half != length - half) {
      if (thisBytes[half] != otherBytes[half]) return false;
    }
    return true;
  }

  public boolean equals(Object o) {
    return o instanceof BytesKey && equals(bytes, ((BytesKey) o).bytes);
  }

  @Override
  public String toString() {
    return new String(bytes);
  }

  public byte[] getBytes() {
    return bytes;
  }
}
