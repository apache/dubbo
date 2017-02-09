package redis.util;

/**
 * Shared encoding code.
 */
public class Encoding {
  public static final byte[] NEG_ONE = convert(-1, false);
  public static final byte[] NEG_ONE_WITH_CRLF = convert(-1, true);
  public static final char LF = '\n';
  public static final char CR = '\r';

  // Cache 256 number conversions. That should cover a huge
  // percentage of numbers passed over the wire.
  private static final int NUM_MAP_LENGTH = 256;
  private static byte[][] numMap = new byte[NUM_MAP_LENGTH][];

  static {
    for (int i = 0; i < NUM_MAP_LENGTH; i++) {
      numMap[i] = convert(i, false);
    }
  }

  private static byte[][] numMapWithCRLF = new byte[NUM_MAP_LENGTH][];

  static {
    for (int i = 0; i < NUM_MAP_LENGTH; i++) {
      numMapWithCRLF[i] = convert(i, true);
    }
  }

  public static byte[] numToBytes(long value) {
    return numToBytes(value, false);
  }

  // Optimized for the direct to ASCII bytes case
  // About 5x faster than using Long.toString.getBytes
  public static byte[] numToBytes(long value, boolean withCRLF) {
    if (value >= 0 && value < NUM_MAP_LENGTH) {
      int index = (int) value;
      return withCRLF ? numMapWithCRLF[index] : numMap[index];
    } else if (value == -1) {
      return withCRLF ? NEG_ONE_WITH_CRLF : NEG_ONE;
    }
    return convert(value, withCRLF);
  }

  private static byte[] convert(long value, boolean withCRLF) {
    boolean negative = value < 0;
    // Checked javadoc: If the argument is equal to 10^n for integer n, then the result is n.
    // Also, if negative, leave another slot for the sign.
    long abs = Math.abs(value);
    int index = (value == 0 ? 0 : (int) Math.log10(abs)) + (negative ? 2 : 1);
    // Append the CRLF if necessary
    byte[] bytes = new byte[withCRLF ? index + 2 : index];
    if (withCRLF) {
      bytes[index] = CR;
      bytes[index + 1] = LF;
    }
    // Put the sign in the slot we saved
    if (negative) bytes[0] = '-';
    long next = abs;
    while ((next /= 10) > 0) {
      bytes[--index] = (byte) ('0' + (abs % 10));
      abs = next;
    }
    bytes[--index] = (byte) ('0' + abs);
    return bytes;
  }

  /**
   * Reads a number from a byte array.
   * @param bytes
   * @return
   */
  public static long bytesToNum(byte[] bytes) {
    int length = bytes.length;
    if (length == 0) {
      throw new IllegalArgumentException("value is not an integer or out of range");
    }
    int position = 0;
    int sign;
    int read = bytes[position++];
    if (read == '-') {
      read = bytes[position++];
      sign = -1;
    } else {
      sign = 1;
    }
    long number = 0;
    do {
      int value = read - '0';
      if (value >= 0 && value < 10) {
        number *= 10;
        number += value;
      } else {
        throw new IllegalArgumentException("value is not an integer or out of range");
      }
      if (position == length) {
        return number * sign;
      }
      read = bytes[position++];
    } while (true);
  }

}
