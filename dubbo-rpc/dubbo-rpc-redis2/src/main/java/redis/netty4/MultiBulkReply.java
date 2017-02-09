package redis.netty4;

import redis.Charsets;
import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

import static redis.netty4.RedisReplyDecoder.readLong;
import static redis.util.Encoding.NEG_ONE_WITH_CRLF;
import static redis.util.Encoding.numToBytes;

/**
 * Nested replies.
 */
public class MultiBulkReply implements Reply<Reply[]> {
  public static final char MARKER = '*';
  public static final MultiBulkReply EMPTY = new MultiBulkReply(new Reply[0]);

  private Reply[] replies;
  private int size = -2;
  private int index = 0;

  public MultiBulkReply() {
  }

  public void read(RedisReplyDecoder rd, ByteBuf is) throws IOException {
    if (size == -2) {
      long l = readLong(is);
      if (l > Integer.MAX_VALUE) {
        throw new IllegalArgumentException("Java only supports arrays up to " + Integer.MAX_VALUE + " in size");
      }
      size = (int) l;
      if (size == -1) {
        replies = null;
      } else {
        if (size < 0) {
          throw new IllegalArgumentException("Invalid size: " + size);
        }
        replies = new Reply[size];
      }
      rd.checkpoint();
    }
    for (int i = index; i < size; i++) {
      replies[i] = rd.readReply(is);
      index = i + 1;
      rd.checkpoint();
    }
  }

  public MultiBulkReply(Reply[] replies) {
    this.replies = replies;
    size = replies.length;
  }

  @Override
  public Reply[] data() {
    return replies;
  }

  @Override
  public void write(ByteBuf os) throws IOException {
    os.writeByte(MARKER);
    if (replies == null) {
      os.writeBytes(NEG_ONE_WITH_CRLF);
    } else {
      os.writeBytes(numToBytes(replies.length, true));
      for (Reply reply : replies) {
        reply.write(os);
      }
    }
  }

  public List<String> asStringList(Charset charset) {
    if (replies == null) return null;
    List<String> strings = new ArrayList<String>(replies.length);
    for (Reply reply : replies) {
      if (reply instanceof BulkReply) {
        strings.add(((BulkReply) reply).asString(charset));
      } else {
        throw new IllegalArgumentException("Could not convert " + reply + " to a string");
      }
    }
    return strings;
  }

  public Set<String> asStringSet(Charset charset) {
    if (replies == null) return null;
    Set<String> strings = new HashSet<String>(replies.length);
    for (Reply reply : replies) {
      if (reply instanceof BulkReply) {
        strings.add(((BulkReply) reply).asString(charset));
      } else {
        throw new IllegalArgumentException("Could not convert " + reply + " to a string");
      }
    }
    return strings;
  }

  public Map<String, String> asStringMap(Charset charset) {
    if (replies == null) return null;
    int length = replies.length;
    Map<String, String> map = new HashMap<String, String>(length);
    if (length % 2 != 0) {
      throw new IllegalArgumentException("Odd number of replies");
    }
    for (int i = 0; i < length; i += 2) {
      Reply key = replies[i];
      Reply value = replies[i + 1];
      if (key instanceof BulkReply) {
        if (value instanceof BulkReply) {
          map.put(((BulkReply) key).asString(charset), ((BulkReply) value).asString(charset));
        } else {
          throw new IllegalArgumentException("Could not convert value: " + value + " to a string");
        }
      } else {
        throw new IllegalArgumentException("Could not convert key: " + key + " to a string");
      }
    }
    return map;
  }

  public String toString() {
    return asStringList(Charsets.UTF_8).toString();
  }
}
