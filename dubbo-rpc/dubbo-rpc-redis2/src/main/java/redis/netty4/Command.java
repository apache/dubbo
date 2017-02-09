package redis.netty4;

import redis.Charsets;
import io.netty.buffer.ByteBuf;

import java.io.IOException;

import static redis.util.Encoding.numToBytes;

/**
 * Command serialization.  We special case when there are few 4 or fewer parameters
 * since most commands fall into that category. Passing bytes, channelbuffers and
 * strings / objects are all allowed. All strings are assumed to be UTF-8.
 */
public class Command {
    public static final byte[] ARGS_PREFIX = "*".getBytes();
    public static final byte[] CRLF = "\r\n".getBytes();
    public static final byte[] BYTES_PREFIX = "$".getBytes();
    public static final byte[] EMPTY_BYTES = new byte[0];

    private final Object name;
    private final Object[] objects;
    private final Object object1;
    private final Object object2;
    private final Object object3;
    private final boolean inline;

    public Command(Object[] objects) {
        this(null, null, null, null, objects, false);
    }

    public Command(Object[] objects, boolean inline) {
        this(null, null, null, null, objects, inline);
    }

    public Command(Object name) {
        this(name, null, null, null, null, false);
    }

    public Command(Object name, Object[] objects) {
        this(name, null, null, null, objects, false);
    }

    public Command(Object name, Object object1) {
        this(name, object1, null, null, null, false);
    }

    public Command(Object name, Object object1, Object object2) {
        this(name, object1, object2, null, null, false);
    }

    public Command(Object name, Object object1, Object object2, Object object3) {
        this(name, object1, object2, object3, null, false);
    }

    private Command(Object name, Object object1, Object object2, Object object3, Object[] objects, boolean inline) {
        this.name = name;
        this.object1 = object1;
        this.object2 = object2;
        this.object3 = object3;
        this.objects = objects;
        this.inline = inline;
    }

    public byte[] getName() {
        // It is either the name or the first objects in the objects array
        if (name != null) return getBytes(name);
        return getBytes(objects[0]);
    }

    public boolean isInline() {
        return inline;
    }

    private byte[] getBytes(Object object) {
        byte[] argument;
        if (object == null) {
            argument = EMPTY_BYTES;
        } else if (object instanceof byte[]) {
            argument = (byte[]) object;
        } else if (object instanceof ByteBuf) {
            argument = ((ByteBuf) object).array();
        } else if (object instanceof String) {
            argument = ((String) object).getBytes(Charsets.UTF_8);
        } else {
            argument = object.toString().getBytes(Charsets.UTF_8);
        }
        return argument;
    }

    public void toArguments(Object[] arguments, Class<?>[] types) {
        int position = 0;
        for (Class<?> type : types) {
            if (type == byte[].class) {
                if (position >= arguments.length) {
                    throw new IllegalArgumentException("wrong number of arguments for '" + new String(getName()) + "' command");
                }
                if (objects.length - 1 > position) {
                    arguments[position] = objects[1 + position];
                }
            } else {
                int left = objects.length - position - 1;
                byte[][] lastArgument = new byte[left][];
                for (int i = 0; i < left; i++) {
                    lastArgument[i] = (byte[]) objects[i + position + 1];
                }
                arguments[position] = lastArgument;
            }
            position++;
        }
    }

    public Object[] getObjects() {
        return objects;
    }

    public void write(ByteBuf os) throws IOException {
        writeDirect(os, name, object1, object2, object3, objects);
    }

    public static void writeDirect(ByteBuf os, Object name, Object object1, Object object2, Object object3, Object[] objects) throws IOException {
        int others = (object1 == null ? 0 : 1) + (object2 == null ? 0 : 1) +
                (object3 == null ? 0 : 1) + (name == null ? 0 : 1);
        int length = objects == null ? 0 : objects.length;
        os.writeBytes(ARGS_PREFIX);
        os.writeBytes(numToBytes(length + others, true));
        if (name != null) writeObject(os, name);
        if (object1 != null) writeObject(os, object1);
        if (object2 != null) writeObject(os, object2);
        if (object3 != null) writeObject(os, object3);
        if (objects != null) {
            for (Object object : objects) {
                writeObject(os, object);
            }
        }
    }

    private static void writeObject(ByteBuf os, Object object) throws IOException {
        byte[] argument;
        if (object == null) {
            argument = EMPTY_BYTES;
        } else if (object instanceof byte[]) {
            argument = (byte[]) object;
        } else if (object instanceof ByteBuf) {
            writeArgument(os, (ByteBuf) object);
            return;
        } else if (object instanceof String) {
            argument = ((String) object).getBytes(Charsets.UTF_8);
        } else {
            argument = object.toString().getBytes(Charsets.UTF_8);
        }
        writeArgument(os, argument);
    }

    private static void writeArgument(ByteBuf os, byte[] argument) throws IOException {
        os.writeBytes(BYTES_PREFIX);
        os.writeBytes(numToBytes(argument.length, true));
        os.writeBytes(argument);
        os.writeBytes(CRLF);
    }

    private static void writeArgument(ByteBuf os, ByteBuf argument) throws IOException {
        os.writeBytes(BYTES_PREFIX);
        os.writeBytes(numToBytes(argument.readableBytes(), true));
        os.writeBytes(argument);
        os.writeBytes(CRLF);
    }

}
