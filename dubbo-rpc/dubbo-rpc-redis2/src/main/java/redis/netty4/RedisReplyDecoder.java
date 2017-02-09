package redis.netty4;

import redis.Charsets;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.io.IOException;
import java.util.List;

/**
 * Netty codec for Redis
 */

public class RedisReplyDecoder extends ReplayingDecoder<Void> {

  public static final char CR = '\r';
  public static final char LF = '\n';
  private static final char ZERO = '0';
  // We track the current multibulk reply in the case
  // where we do not get a complete reply in a single
  // decode invocation.
  private MultiBulkReply reply;

  public RedisReplyDecoder() {
    this(true);
  }

  @Override
  protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> objects) throws Exception {
    objects.add(receive(byteBuf));
  }

  public RedisReplyDecoder(boolean checkpointEnabled) {
    this.checkpointEnabled = checkpointEnabled;
  }

  private boolean checkpointEnabled;

  public ByteBuf readBytes(ByteBuf is) throws IOException {
    long l = readLong(is);
    if (l > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Java only supports arrays up to " + Integer.MAX_VALUE + " in size");
    }
    int size = (int) l;
    if (size == -1) {
      return null;
    }
    ByteBuf buffer = is.readSlice(size);
    int cr = is.readByte();
    int lf = is.readByte();
    if (cr != CR || lf != LF) {
      throw new IOException("Improper line ending: " + cr + ", " + lf);
    }
    return buffer;
  }

  public static long readLong(ByteBuf is) throws IOException {
    long size = 0;
    int sign = 1;
    int read = is.readByte();
    if (read == '-') {
      read = is.readByte();
      sign = -1;
    }
    do {
      if (read == CR) {
        if (is.readByte() == LF) {
          break;
        }
      }
      int value = read - ZERO;
      if (value >= 0 && value < 10) {
        size *= 10;
        size += value;
      } else {
        throw new IOException("Invalid character in integer");
      }
      read = is.readByte();
    } while (true);
    return size * sign;
  }

  public Reply receive(final ByteBuf is) throws IOException {
    if (reply != null) {
      return decodeMultiBulkReply(is);
    }
    return readReply(is);
  }

  public Reply readReply(ByteBuf is) throws IOException {
    int code = is.readByte();
    switch (code) {
      case StatusReply.MARKER: {
        String status = is.readBytes(is.bytesBefore((byte) '\r')).toString(Charsets.UTF_8);
        is.skipBytes(2);
        return new StatusReply(status);
      }
      case ErrorReply.MARKER: {
        String error = is.readBytes(is.bytesBefore((byte) '\r')).toString(Charsets.UTF_8);
        is.skipBytes(2);
        return new ErrorReply(error);
      }
      case IntegerReply.MARKER: {
        return new IntegerReply(readLong(is));
      }
      case BulkReply.MARKER: {
        return new BulkReply(readBytes(is));
      }
      case MultiBulkReply.MARKER: {
        if (reply == null) {
          return decodeMultiBulkReply(is);
        } else {
          return new RedisReplyDecoder(false).decodeMultiBulkReply(is);
        }
      }
      default: {
        throw new IOException("Unexpected character in stream: " + code);
      }
    }
  }

  @Override
  public void checkpoint() {
    if (checkpointEnabled) {
      super.checkpoint();
    }
  }

  public MultiBulkReply decodeMultiBulkReply(ByteBuf is) throws IOException {
    try {
      if (reply == null) {
        reply = new MultiBulkReply();
        checkpoint();
      }
      reply.read(this, is);
      return reply;
    } finally {
      reply = null;
    }
  }

}
