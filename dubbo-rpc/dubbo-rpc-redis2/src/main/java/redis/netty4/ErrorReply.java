package redis.netty4;

import redis.Charsets;
import io.netty.buffer.ByteBuf;

import java.io.IOException;

public class ErrorReply implements Reply<String> {
  public static final char MARKER = '-';
  public static final ErrorReply NYI_REPLY = new ErrorReply("Not yet implemented");
  private final String error;

  public ErrorReply(String error) {
    this.error = error;
  }

  @Override
  public String data() {
    return error;
  }

  @Override
  public void write(ByteBuf os) throws IOException {
    os.writeByte(MARKER);
    os.writeBytes(error.getBytes(Charsets.UTF_8));
    os.writeBytes(CRLF);
  }

  public String toString() {
    return error;
  }
}
