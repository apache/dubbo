package redis.netty4;

import redis.Charsets;
import io.netty.buffer.ByteBuf;

import java.io.IOException;

public class StatusReply implements Reply<String> {
  public static final char MARKER = '+';
  public static final StatusReply OK = new StatusReply("OK");
  public static final StatusReply QUIT = new StatusReply("OK");
  private final String status;
  private final byte[] statusBytes;

  public StatusReply(String status) {
    this.status = status;
    this.statusBytes = status.getBytes(Charsets.UTF_8);
  }

  @Override
  public String data() {
    return status;
  }

  @Override
  public void write(ByteBuf os) throws IOException {
    os.writeByte(MARKER);
    os.writeBytes(statusBytes);
    os.writeBytes(CRLF);
  }

  public String toString() {
    return status;
  }
}
