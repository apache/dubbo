package redis.server.netty;

import io.netty.buffer.ByteBuf;
import redis.netty4.*;
import redis.util.*;

import java.lang.reflect.Field;
import java.security.SecureRandom;
import java.util.*;

import static java.lang.Double.parseDouble;
import static java.lang.Integer.MAX_VALUE;
import static redis.netty4.BulkReply.NIL_REPLY;
import static redis.netty4.IntegerReply.integer;
import static redis.netty4.StatusReply.OK;
import static redis.netty4.StatusReply.QUIT;
import static redis.util.Encoding.bytesToNum;
import static redis.util.Encoding.numToBytes;

public class SimpleRedisServer implements RedisServer {

    private static final StatusReply PONG = new StatusReply("PONG");
    private long started = now();

    private BytesKeyObjectMap<Object> data = new BytesKeyObjectMap<Object>();
    private BytesKeyObjectMap<Long> expires = new BytesKeyObjectMap<Long>();

    private static int[] mask = {128, 64, 32, 16, 8, 4, 2, 1};

    private static RedisException invalidValue() {
        return new RedisException("Operation against a key holding the wrong kind of value");
    }

    private static RedisException notInteger() {
        return new RedisException("value is not an integer or out of range");
    }

    private static RedisException notFloat() {
        return new RedisException("value is not a float or out of range");
    }

    @SuppressWarnings("unchecked")
    private BytesKeyObjectMap<byte[]> _gethash(byte[] key0, boolean create) throws RedisException {
        Object o = _get(key0);
        if (o == null) {
            o = new BytesKeyObjectMap();
            if (create) {
                data.put(key0, o);
            }
        }
        if (!(o instanceof HashMap)) {
            throw invalidValue();
        }
        return (BytesKeyObjectMap<byte[]>) o;
    }

    @SuppressWarnings("unchecked")
    private BytesKeySet _getset(byte[] key0, boolean create) throws RedisException {
        Object o = _get(key0);
        if (o == null) {
            o = new BytesKeySet();
            if (create) {
                data.put(key0, o);
            }
        }
        if (!(o instanceof BytesKeySet)) {
            throw invalidValue();
        }
        return (BytesKeySet) o;
    }

    @SuppressWarnings("unchecked")
    private ZSet _getzset(byte[] key0, boolean create) throws RedisException {
        Object o = _get(key0);
        if (o == null) {
            o = new ZSet();
            if (create) {
                data.put(key0, o);
            }
        }
        if (!(o instanceof ZSet)) {
            throw invalidValue();
        }
        return (ZSet) o;
    }

    private Object _get(byte[] key0) {
        Object o = data.get(key0);
        if (o != null) {
            Long l = expires.get(key0);
            if (l != null) {
                if (l < now()) {
                    data.remove(key0);
                    return null;
                }
            }
        }
        return o;
    }

    private IntegerReply _change(byte[] key0, long delta) throws RedisException {
        Object o = _get(key0);
        if (o == null) {
            _put(key0, numToBytes(delta, false));
            return integer(delta);
        } else if (o instanceof byte[]) {
            try {
                long integer = bytesToNum((byte[]) o) + delta;
                _put(key0, numToBytes(integer, false));
                return integer(integer);
            } catch (IllegalArgumentException e) {
                throw new RedisException(e.getMessage());
            }
        } else {
            throw notInteger();
        }
    }

    private BulkReply _change(byte[] key0, double delta) throws RedisException {
        Object o = _get(key0);
        if (o == null) {
            byte[] bytes = _tobytes(delta);
            _put(key0, bytes);
            return new BulkReply(bytes);
        } else if (o instanceof byte[]) {
            try {
                double number = _todouble((byte[]) o) + delta;
                byte[] bytes = _tobytes(number);
                _put(key0, bytes);
                return new BulkReply(bytes);
            } catch (IllegalArgumentException e) {
                throw new RedisException(e.getMessage());
            }
        } else {
            throw notInteger();
        }
    }

    private static int _test(byte[] bytes, long offset) throws RedisException {
        long div = offset / 8;
        if (div > MAX_VALUE) throw notInteger();
        int i;
        if (bytes.length < div + 1) {
            i = 0;
        } else {
            int mod = (int) (offset % 8);
            int value = bytes[((int) div)] & 0xFF;
            i = value & mask[mod];
        }
        return i != 0 ? 1 : 0;
    }

    private byte[] _getbytes(byte[] aKey2) throws RedisException {
        byte[] src;
        Object o = _get(aKey2);
        if (o instanceof byte[]) {
            src = (byte[]) o;
        } else if (o != null) {
            throw invalidValue();
        } else {
            src = new byte[0];
        }
        return src;
    }

    @SuppressWarnings("unchecked")
    private List<BytesValue> _getlist(byte[] key0, boolean create) throws RedisException {
        Object o = _get(key0);
        if (o instanceof List) {
            return (List<BytesValue>) o;
        } else if (o == null) {
            if (create) {
                ArrayList<BytesValue> list = new ArrayList<BytesValue>();
                _put(key0, list);
                return list;
            } else {
                return null;
            }
        } else {
            throw invalidValue();
        }
    }

    private Object _put(byte[] key, Object value) {
        expires.remove(key);
        return data.put(key, value);
    }

    private Object _put(byte[] key, byte[] value, long expiration) {
        expires.put(key, expiration);
        return data.put(key, value);
    }

    private static boolean matches(byte[] key, byte[] pattern, int kp, int pp) {
        if (kp == key.length) {
            return pp == pattern.length || (pp == pattern.length - 1 && pattern[pp] == '*');
        } else if (pp == pattern.length) {
            return false;
        }
        byte c = key[kp];
        byte p = pattern[pp];
        switch (p) {
            case '?':
                // Always matches, move to next character in key and pattern
                return matches(key, pattern, kp + 1, pp + 1);
            case '*':
                // Matches this character than either matches end or try next character
                return matches(key, pattern, kp + 1, pp + 1) || matches(key, pattern, kp + 1, pp);
            case '\\':
                // Matches the escaped character and the rest
                return c == pattern[pp + 1] && matches(key, pattern, kp + 1, pp + 2);
            case '[':
                // Matches one of the characters and the rest
                boolean found = false;
                pp++;
                do {
                    byte b = pattern[pp++];
                    if (b == ']') {
                        break;
                    } else {
                        if (b == c) found = true;
                    }
                } while (true);
                return found && matches(key, pattern, kp + 1, pp);
            default:
                // This matches and the rest
                return c == p && matches(key, pattern, kp + 1, pp + 1);
        }
    }


    private static int _toposint(byte[] offset1) throws RedisException {
        long offset = bytesToNum(offset1);
        if (offset < 0 || offset > MAX_VALUE) {
            throw notInteger();
        }
        return (int) offset;
    }

    private static int _toint(byte[] offset1) throws RedisException {
        long offset = bytesToNum(offset1);
        if (offset > MAX_VALUE) {
            throw notInteger();
        }
        return (int) offset;
    }

    private static int _torange(byte[] offset1, int length) throws RedisException {
        long offset = bytesToNum(offset1);
        if (offset > MAX_VALUE) {
            throw notInteger();
        }
        if (offset < 0) {
            offset = (length + offset);
        }
        if (offset >= length) {
            offset = length - 1;
        }
        return (int) offset;
    }

    private static Random r = new SecureRandom();
    private static Field tableField;
    private static Field nextField;
    private static Field mapField;

    static {
        try {
            tableField = HashMap.class.getDeclaredField("table");
            tableField.setAccessible(true);
            try {
                nextField = Class.forName("java.util.HashMap$Entry").getDeclaredField("next");

            } catch (ClassNotFoundException e) {
                nextField = Class.forName("java.util.HashMap$Node").getDeclaredField("next");
            }
            nextField.setAccessible(true);
            mapField = HashSet.class.getDeclaredField("map");
            mapField.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
            tableField = null;
            nextField = null;
        }
    }

    private static RedisException noSuchKey() {
        return new RedisException("no such key");
    }

    private long now() {
        return System.currentTimeMillis();
    }

    /**
     * Append a value to a key
     * String
     *
     * @param key0
     * @param value1
     * @return IntegerReply
     */
    @Override
    public IntegerReply append(byte[] key0, byte[] value1) throws RedisException {
        Object o = _get(key0);
        int length1 = value1.length;
        if (o instanceof byte[]) {
            byte[] src = (byte[]) o;
            int length0 = src.length;
            byte[] bytes = new byte[length0 + length1];
            System.arraycopy(src, 0, bytes, 0, length0);
            System.arraycopy(value1, 0, bytes, length0, length1);
            _put(key0, bytes);
            return integer(bytes.length);
        } else if (o == null) {
            _put(key0, value1);
            return integer(length1);
        } else {
            throw invalidValue();
        }
    }

    /**
     * Count set bits in a string
     * String
     *
     * @param key0
     * @param start1
     * @param end2
     * @return IntegerReply
     */
    @Override
    public IntegerReply bitcount(byte[] key0, byte[] start1, byte[] end2) throws RedisException {
        Object o = _get(key0);
        if (o instanceof byte[]) {
            byte[] bytes = (byte[]) o;
            int size = bytes.length;
            int s = _torange(start1, size);
            int e = _torange(end2, size);
            if (e < s) e = s;
            int total = 0;
            for (int i = s; i <= e; i++) {
                int b = bytes[i] & 0xFF;
                for (int j = 0; j < 8; j++) {
                    if ((b & mask[j]) != 0) {
                        total++;
                    }
                }
            }
            return integer(total);
        } else if (o == null) {
            return integer(0);
        } else {
            throw invalidValue();
        }
    }

    /**
     * Perform bitwise operations between strings
     * String
     *
     * @param operation0
     * @param destkey1
     * @param key2
     * @return IntegerReply
     */
    @Override
    public IntegerReply bitop(byte[] operation0, byte[] destkey1, byte[][] key2) throws RedisException {
        BitOp bitOp = BitOp.valueOf(new String(operation0).toUpperCase());
        int size = 0;
        for (byte[] aKey2 : key2) {
            int length = aKey2.length;
            if (length > size) {
                size = length;
            }
        }
        byte[] bytes = null;
        for (byte[] aKey2 : key2) {
            byte[] src;
            src = _getbytes(aKey2);
            if (bytes == null) {
                bytes = new byte[size];
                if (bitOp == BitOp.NOT) {
                    if (key2.length > 1) {
                        throw new RedisException("invalid number of arguments for 'bitop' NOT operation");
                    }
                    for (int i = 0; i < src.length; i++) {
                        bytes[i] = (byte) ~(src[i] & 0xFF);
                    }
                } else {
                    System.arraycopy(src, 0, bytes, 0, src.length);
                }
            } else {
                for (int i = 0; i < src.length; i++) {
                    int d = bytes[i] & 0xFF;
                    int s = src[i] & 0xFF;
                    switch (bitOp) {
                        case AND:
                            bytes[i] = (byte) (d & s);
                            break;
                        case OR:
                            bytes[i] = (byte) (d | s);
                            break;
                        case XOR:
                            bytes[i] = (byte) (d ^ s);
                            break;
                    }
                }
            }
        }
        _put(destkey1, bytes);
        return integer(bytes == null ? 0 : bytes.length);
    }

    enum BitOp {AND, OR, XOR, NOT}

    /**
     * Decrement the integer value of a key by one
     * String
     *
     * @param key0
     * @return IntegerReply
     */
    @Override
    public IntegerReply decr(byte[] key0) throws RedisException {
        return _change(key0, -1);
    }

    /**
     * Decrement the integer value of a key by the given number
     * String
     *
     * @param key0
     * @param decrement1
     * @return IntegerReply
     */
    @Override
    public IntegerReply decrby(byte[] key0, byte[] decrement1) throws RedisException {
        return _change(key0, -bytesToNum(decrement1));
    }

    /**
     * Get the value of a key
     * String
     *
     * @param key0
     * @return BulkReply
     */
    @Override
    public BulkReply get(byte[] key0) throws RedisException {
        Object o = _get(key0);
        if (o instanceof byte[]) {
            return new BulkReply((byte[]) o);
        }
        if (o == null) {
            return NIL_REPLY;
        } else {
            throw invalidValue();
        }
    }

    /**
     * Returns the bit value at offset in the string value stored at key
     * String
     *
     * @param key0
     * @param offset1
     * @return IntegerReply
     */
    @Override
    public IntegerReply getbit(byte[] key0, byte[] offset1) throws RedisException {
        Object o = _get(key0);
        if (o instanceof byte[]) {
            long offset = bytesToNum(offset1);
            byte[] bytes = (byte[]) o;
            return _test(bytes, offset) == 1 ? integer(1) : integer(0);
        } else if (o == null) {
            return integer(0);
        } else {
            throw invalidValue();
        }
    }

    /**
     * Get a substring of the string stored at a key
     * String
     *
     * @param key0
     * @param start1
     * @param end2
     * @return BulkReply
     */
    @Override
    public BulkReply getrange(byte[] key0, byte[] start1, byte[] end2) throws RedisException {
        byte[] bytes = _getbytes(key0);
        int size = bytes.length;
        int s = _torange(start1, size);
        int e = _torange(end2, size);
        if (e < s) e = s;
        int length = e - s + 1;
        byte[] out = new byte[length];
        System.arraycopy(bytes, s, out, 0, length);
        return new BulkReply(out);
    }

    /**
     * Set the string value of a key and return its old value
     * String
     *
     * @param key0
     * @param value1
     * @return BulkReply
     */
    @Override
    public BulkReply getset(byte[] key0, byte[] value1) throws RedisException {
        Object put = _put(key0, value1);
        if (put == null || put instanceof byte[]) {
            return put == null ? NIL_REPLY : new BulkReply((byte[]) put);
        } else {
            // Put it back
            data.put(key0, put);
            throw invalidValue();
        }
    }

    /**
     * Increment the integer value of a key by one
     * String
     *
     * @param key0
     * @return IntegerReply
     */
    @Override
    public IntegerReply incr(byte[] key0) throws RedisException {
        return _change(key0, 1);
    }

    /**
     * Increment the integer value of a key by the given amount
     * String
     *
     * @param key0
     * @param increment1
     * @return IntegerReply
     */
    @Override
    public IntegerReply incrby(byte[] key0, byte[] increment1) throws RedisException {
        return _change(key0, bytesToNum(increment1));
    }

    /**
     * Increment the float value of a key by the given amount
     * String
     *
     * @param key0
     * @param increment1
     * @return BulkReply
     */
    @Override
    public BulkReply incrbyfloat(byte[] key0, byte[] increment1) throws RedisException {
        return _change(key0, _todouble(increment1));
    }

    /**
     * Get the values of all the given keys
     * String
     *
     * @param key0
     * @return MultiBulkReply
     */
    @Override
    public MultiBulkReply mget(byte[][] key0) throws RedisException {
        int length = key0.length;
        Reply[] replies = new Reply[length];
        for (int i = 0; i < length; i++) {
            Object o = _get(key0[i]);
            if (o instanceof byte[]) {
                replies[i] = new BulkReply((byte[]) o);
            } else {
                replies[i] = NIL_REPLY;
            }
        }
        return new MultiBulkReply(replies);
    }

    /**
     * Set multiple keys to multiple values
     * String
     *
     * @param key_or_value0
     * @return StatusReply
     */
    @Override
    public StatusReply mset(byte[][] key_or_value0) throws RedisException {
        int length = key_or_value0.length;
        if (length % 2 != 0) {
            throw new RedisException("wrong number of arguments for MSET");
        }
        for (int i = 0; i < length; i += 2) {
            _put(key_or_value0[i], key_or_value0[i + 1]);
        }
        return OK;
    }

    /**
     * Set multiple keys to multiple values, only if none of the keys exist
     * String
     *
     * @param key_or_value0
     * @return IntegerReply
     */
    @Override
    public IntegerReply msetnx(byte[][] key_or_value0) throws RedisException {
        int length = key_or_value0.length;
        if (length % 2 != 0) {
            throw new RedisException("wrong number of arguments for MSETNX");
        }
        for (int i = 0; i < length; i += 2) {
            if (_get(key_or_value0[i]) != null) {
                return integer(0);
            }
        }
        for (int i = 0; i < length; i += 2) {
            _put(key_or_value0[i], key_or_value0[i + 1]);
        }
        return integer(1);
    }

    /**
     * Set the value and expiration in milliseconds of a key
     * String
     *
     * @param key0
     * @param milliseconds1
     * @param value2
     * @return Reply
     */
    @Override
    public Reply psetex(byte[] key0, byte[] milliseconds1, byte[] value2) throws RedisException {
        _put(key0, value2, bytesToNum(milliseconds1) + now());
        return OK;
    }

    /**
     * Set the string value of a key
     * String
     *
     * @param key0
     * @param value1
     * @return StatusReply
     */
    @Override
    public StatusReply set(byte[] key0, byte[] value1) throws RedisException {
        _put(key0, value1);
        return OK;
    }

    /**
     * Sets or clears the bit at offset in the string value stored at key
     * String
     *
     * @param key0
     * @param offset1
     * @param value2
     * @return IntegerReply
     */
    @Override
    public IntegerReply setbit(byte[] key0, byte[] offset1, byte[] value2) throws RedisException {
        int bit = (int) bytesToNum(value2);
        if (bit != 0 && bit != 1) throw notInteger();
        Object o = _get(key0);
        if (o instanceof byte[] || o == null) {
            long offset = bytesToNum(offset1);
            long div = offset / 8;
            if (div + 1 > MAX_VALUE) throw notInteger();

            byte[] bytes = (byte[]) o;
            if (bytes == null || bytes.length < div + 1) {
                byte[] tmp = bytes;
                bytes = new byte[(int) div + 1];
                if (tmp != null) System.arraycopy(tmp, 0, bytes, 0, tmp.length);
                _put(key0, bytes);
            }
            int mod = (int) (offset % 8);
            int value = bytes[((int) div)] & 0xFF;
            int i = value & mask[mod];
            if (i == 0) {
                if (bit != 0) {
                    bytes[((int) div)] += mask[mod];
                }
                return integer(0);
            } else {
                if (bit == 0) {
                    bytes[((int) div)] -= mask[mod];
                }
                return integer(1);
            }
        } else {
            throw invalidValue();
        }
    }

    /**
     * Set the value and expiration of a key
     * String
     *
     * @param key0
     * @param seconds1
     * @param value2
     * @return StatusReply
     */
    @Override
    public StatusReply setex(byte[] key0, byte[] seconds1, byte[] value2) throws RedisException {
        _put(key0, value2, bytesToNum(seconds1) * 1000 + now());
        return OK;
    }

    /**
     * Set the value of a key, only if the key does not exist
     * String
     *
     * @param key0
     * @param value1
     * @return IntegerReply
     */
    @Override
    public IntegerReply setnx(byte[] key0, byte[] value1) throws RedisException {
        if (_get(key0) == null) {
            _put(key0, value1);
            return integer(1);
        }
        return integer(0);
    }

    /**
     * Overwrite part of a string at key starting at the specified offset
     * String
     *
     * @param key0
     * @param offset1
     * @param value2
     * @return IntegerReply
     */
    @Override
    public IntegerReply setrange(byte[] key0, byte[] offset1, byte[] value2) throws RedisException {
        byte[] bytes = _getbytes(key0);
        int offset = _toposint(offset1);
        int length = value2.length + offset;
        if (bytes.length < length) {
            byte[] tmp = bytes;
            bytes = new byte[length];
            System.arraycopy(tmp, 0, bytes, 0, offset);
            _put(key0, bytes);
        }
        System.arraycopy(value2, 0, bytes, offset, value2.length);
        return integer(bytes.length);
    }

    /**
     * Get the length of the value stored in a key
     * String
     *
     * @param key0
     * @return IntegerReply
     */
    @Override
    public IntegerReply strlen(byte[] key0) throws RedisException {
        return integer(_getbytes(key0).length);
    }

    /**
     * Authenticate to the server
     * Connection
     *
     * @param password0
     * @return StatusReply
     */
    public StatusReply auth(byte[] password0) throws RedisException {
        throw new RedisException("Not supported");
    }

    /**
     * Echo the given string
     * Connection
     *
     * @param message0
     * @return BulkReply
     */
    @Override
    public BulkReply echo(byte[] message0) throws RedisException {
        return new BulkReply(message0);
    }

    /**
     * Ping the server
     * Connection
     *
     * @return StatusReply
     */
    @Override
    public StatusReply ping() throws RedisException {
        return PONG;
    }

    /**
     * Close the connection
     * Connection
     *
     * @return StatusReply
     */
    @Override
    public StatusReply quit() throws RedisException {
        return QUIT;
    }

    /**
     * Change the selected database for the current connection
     * Connection
     *
     * @param index0
     * @return StatusReply
     */
    @Override
    public StatusReply select(byte[] index0) throws RedisException {
        throw new RedisException("Not supported");
    }

    /**
     * Asynchronously rewrite the append-only file
     * Server
     *
     * @return StatusReply
     */
    @Override
    public StatusReply bgrewriteaof() throws RedisException {
        throw new RedisException("Not supported");
    }

    /**
     * Asynchronously save the dataset to disk
     * Server
     *
     * @return StatusReply
     */
    @Override
    public StatusReply bgsave() throws RedisException {
        throw new RedisException("Not supported");
    }

    /**
     * Kill the connection of a client
     * Server
     *
     * @param ip_port0
     * @return Reply
     */
    @Override
    public Reply client_kill(byte[] ip_port0) throws RedisException {
        throw new RedisException("Not supported");
    }

    /**
     * Get the list of client connections
     * Server
     *
     * @return Reply
     */
    @Override
    public Reply client_list() throws RedisException {
        throw new RedisException("Not supported");
    }

    /**
     * Get the current connection name
     * Server
     *
     * @return Reply
     */
    @Override
    public Reply client_getname() throws RedisException {
        throw new RedisException("Not supported");
    }

    /**
     * Set the current connection name
     * Server
     *
     * @param connection_name0
     * @return Reply
     */
    @Override
    public Reply client_setname(byte[] connection_name0) throws RedisException {
        throw new RedisException("Not supported");
    }

    /**
     * Get the value of a configuration parameter
     * Server
     *
     * @param parameter0
     * @return Reply
     */
    @Override
    public Reply config_get(byte[] parameter0) throws RedisException {
        throw new RedisException("Not supported");
    }

    /**
     * Set a configuration parameter to the given value
     * Server
     *
     * @param parameter0
     * @param value1
     * @return Reply
     */
    @Override
    public Reply config_set(byte[] parameter0, byte[] value1) throws RedisException {
        throw new RedisException("Not supported");
    }

    /**
     * Reset the stats returned by INFO
     * Server
     *
     * @return Reply
     */
    @Override
    public Reply config_resetstat() throws RedisException {
        throw new RedisException("Not supported");
    }

    /**
     * Return the number of keys in the selected database
     * Server
     *
     * @return IntegerReply
     */
    @Override
    public IntegerReply dbsize() throws RedisException {
        return integer(data.size());
    }

    /**
     * Get debugging information about a key
     * Server
     *
     * @param key0
     * @return Reply
     */
    @Override
    public Reply debug_object(byte[] key0) throws RedisException {
        throw new RedisException("Not supported");
    }

    /**
     * Make the server crash
     * Server
     *
     * @return Reply
     */
    @Override
    public Reply debug_segfault() throws RedisException {
        throw new RedisException("Not supported");
    }

    /**
     * Remove all keys from all databases
     * Server
     *
     * @return StatusReply
     */
    @Override
    public StatusReply flushall() throws RedisException {
        data.clear();
        return OK;
    }

    /**
     * Remove all keys from the current database
     * Server
     *
     * @return StatusReply
     */
    @Override
    public StatusReply flushdb() throws RedisException {
        data.clear();
        return OK;
    }

    /**
     * Get information and statistics about the server
     * Server
     *
     * @return BulkReply
     */
    @Override
    public BulkReply info(byte[] section) throws RedisException {
        StringBuilder sb = new StringBuilder();
        sb.append("redis_version:2.6.0\n");
        sb.append("keys:").append(data.size()).append("\n");
        sb.append("uptime:").append(now() - started).append("\n");
        return new BulkReply(sb.toString().getBytes());
    }

    /**
     * Get the UNIX time stamp of the last successful save to disk
     * Server
     *
     * @return IntegerReply
     */
    @Override
    public IntegerReply lastsave() throws RedisException {
        return integer(-1);
    }

    /**
     * Listen for all requests received by the server in real time
     * Server
     *
     * @return Reply
     */
    @Override
    public Reply monitor() throws RedisException {
        // TODO: Blocking
        return null;
    }

    /**
     * Synchronously save the dataset to disk
     * Server
     *
     * @return Reply
     */
    @Override
    public Reply save() throws RedisException {
        throw new RedisException("Not supported");
    }

    /**
     * Synchronously save the dataset to disk and then shut down the server
     * Server
     *
     * @param NOSAVE0
     * @param SAVE1
     * @return StatusReply
     */
    @Override
    public StatusReply shutdown(byte[] NOSAVE0, byte[] SAVE1) throws RedisException {
        throw new RedisException("Not supported");
    }

    /**
     * Make the server a slave of another instance, or promote it as master
     * Server
     *
     * @param host0
     * @param port1
     * @return StatusReply
     */
    @Override
    public StatusReply slaveof(byte[] host0, byte[] port1) throws RedisException {
        // TODO
        return null;
    }

    /**
     * Manages the Redis slow queries log
     * Server
     *
     * @param subcommand0
     * @param argument1
     * @return Reply
     */
    @Override
    public Reply slowlog(byte[] subcommand0, byte[] argument1) throws RedisException {
        throw new RedisException("Not supported");
    }

    /**
     * Internal command used for replication
     * Server
     *
     * @return Reply
     */
    @Override
    public Reply sync() throws RedisException {
        // TODO: Blocking
        return null;
    }

    /**
     * Return the current server time
     * Server
     *
     * @return MultiBulkReply
     */
    @Override
    public MultiBulkReply time() throws RedisException {
        long millis = System.currentTimeMillis();
        long seconds = millis / 1000;
        Reply[] replies = new Reply[]{
                new BulkReply(numToBytes(seconds)),
                new BulkReply(numToBytes((millis - seconds * 1000) * 1000))
        };
        return new MultiBulkReply(replies);
    }

    /**
     * Remove and get the first element in a list, or block until one is available
     * List
     *
     * @param key0
     * @return MultiBulkReply
     */
    @Override
    public MultiBulkReply blpop(byte[][] key0) throws RedisException {
        // TODO: Blocking
        return null;
    }

    /**
     * Remove and get the last element in a list, or block until one is available
     * List
     *
     * @param key0
     * @return MultiBulkReply
     */
    @Override
    public MultiBulkReply brpop(byte[][] key0) throws RedisException {
        // TODO: Blocking
        return null;
    }

    /**
     * Pop a value from a list, push it to another list and return it; or block until one is available
     * List
     *
     * @param source0
     * @param destination1
     * @param timeout2
     * @return BulkReply
     */
    @Override
    public BulkReply brpoplpush(byte[] source0, byte[] destination1, byte[] timeout2) throws RedisException {
        // TODO: Blocking
        return null;
    }

    /**
     * Get an element from a list by its index
     * List
     *
     * @param key0
     * @param index1
     * @return BulkReply
     */
    @SuppressWarnings("unchecked")
    @Override
    public BulkReply lindex(byte[] key0, byte[] index1) throws RedisException {
        int index = _toposint(index1);
        List<BytesValue> list = _getlist(key0, true);
        if (list == null || list.size() <= index) {
            return NIL_REPLY;
        } else {
            return new BulkReply(list.get(index).getBytes());
        }
    }

    /**
     * Insert an element before or after another element in a list
     * List
     *
     * @param key0
     * @param where1
     * @param pivot2
     * @param value3
     * @return IntegerReply
     */
    @Override
    public IntegerReply linsert(byte[] key0, byte[] where1, byte[] pivot2, byte[] value3) throws RedisException {
        Where where = Where.valueOf(new String(where1).toUpperCase());
        List<BytesValue> list = _getlist(key0, true);
        BytesKey pivot = new BytesKey(pivot2);
        int i = list.indexOf(pivot);
        if (i == -1) {
            return integer(-1);
        }
        list.add(i + (where == Where.BEFORE ? 0 : 1), new BytesKey(value3));
        return integer(list.size());
    }

    enum Where {BEFORE, AFTER}

    /**
     * Get the length of a list
     * List
     *
     * @param key0
     * @return IntegerReply
     */
    @Override
    public IntegerReply llen(byte[] key0) throws RedisException {
        List<BytesValue> list = _getlist(key0, false);
        return list == null ? integer(0) : integer(list.size());
    }

    /**
     * Remove and get the first element in a list
     * List
     *
     * @param key0
     * @return BulkReply
     */
    @Override
    public BulkReply lpop(byte[] key0) throws RedisException {
        List<BytesValue> list = _getlist(key0, false);
        if (list == null || list.size() == 0) {
            return NIL_REPLY;
        } else {
            return new BulkReply(list.remove(0).getBytes());
        }
    }

    /**
     * Prepend one or multiple values to a list
     * List
     *
     * @param key0
     * @param value1
     * @return IntegerReply
     */
    @Override
    public IntegerReply lpush(byte[] key0, byte[][] value1) throws RedisException {
        List<BytesValue> list = _getlist(key0, true);
        for (byte[] value : value1) {
            list.add(0, new BytesKey(value));
        }
        return integer(list.size());
    }

    /**
     * Prepend a value to a list, only if the list exists
     * List
     *
     * @param key0
     * @param value1
     * @return IntegerReply
     */
    @Override
    public IntegerReply lpushx(byte[] key0, byte[] value1) throws RedisException {
        List<BytesValue> list = _getlist(key0, false);
        if (list == null) {
            return integer(0);
        } else {
            list.add(0, new BytesKey(value1));
        }
        return integer(list.size());
    }

    /**
     * Get a range of elements from a list
     * List
     *
     * @param key0
     * @param start1
     * @param stop2
     * @return MultiBulkReply
     */
    @Override
    public MultiBulkReply lrange(byte[] key0, byte[] start1, byte[] stop2) throws RedisException {
        List<BytesValue> list = _getlist(key0, false);
        if (list == null) {
            return MultiBulkReply.EMPTY;
        } else {
            int size = list.size();
            int s = _torange(start1, size);
            int e = _torange(stop2, size);
            if (e < s) e = s;
            int length = e - s + 1;
            Reply[] replies = new Reply[length];
            for (int i = s; i <= e; i++) {
                replies[i - s] = new BulkReply(list.get(i).getBytes());
            }
            return new MultiBulkReply(replies);
        }
    }

    /**
     * Remove elements from a list
     * List
     *
     * @param key0
     * @param count1
     * @param value2
     * @return IntegerReply
     */
    @Override
    public IntegerReply lrem(byte[] key0, byte[] count1, byte[] value2) throws RedisException {
        List<BytesValue> list = _getlist(key0, false);
        if (list == null) {
            return integer(0);
        } else {
            int count = _toint(count1);
            BytesKey value = new BytesKey(value2);
            int size = list.size();
            int dir = 1;
            int s = 0;
            int e = size;
            int rem = 0;
            boolean all = count == 0;
            if (count < 0) {
                count = -count;
                dir = -1;
                s = e;
                e = -1;
            }
            for (int i = s; (all || count != 0) && i != e; i += dir) {
                if (list.get(i).equals(value)) {
                    list.remove(i);
                    e -= dir;
                    i -= dir;
                    rem++;
                    count--;
                }
            }
            return integer(rem);
        }
    }

    /**
     * Set the value of an element in a list by its index
     * List
     *
     * @param key0
     * @param index1
     * @param value2
     * @return StatusReply
     */
    @Override
    public StatusReply lset(byte[] key0, byte[] index1, byte[] value2) throws RedisException {
        List<BytesValue> list = _getlist(key0, false);
        if (list == null) {
            throw noSuchKey();
        }
        int size = list.size();
        int index = _toposint(index1);
        if (index < size) {
            list.set(index, new BytesKey(value2));
            return OK;
        } else {
            throw invalidValue();
        }
    }

    /**
     * Trim a list to the specified range
     * List
     *
     * @param key0
     * @param start1
     * @param stop2
     * @return StatusReply
     */
    @Override
    public StatusReply ltrim(byte[] key0, byte[] start1, byte[] stop2) throws RedisException {
        List<BytesValue> list = _getlist(key0, false);
        if (list == null) {
            return OK;
        } else {
            int l = list.size();
            int s = _torange(start1, l);
            int e = _torange(stop2, l);
            // Doesn't change expiration
            data.put(key0, list.subList(s, e + 1));
            return OK;
        }
    }

    /**
     * Remove and get the last element in a list
     * List
     *
     * @param key0
     * @return BulkReply
     */
    @Override
    public BulkReply rpop(byte[] key0) throws RedisException {
        List<BytesValue> list = _getlist(key0, false);
        int l;
        if (list == null || (l = list.size()) == 0) {
            return NIL_REPLY;
        } else {
            byte[] bytes = list.get(l - 1).getBytes();
            list.remove(l - 1);
            return new BulkReply(bytes);
        }
    }

    /**
     * Remove the last element in a list, append it to another list and return it
     * List
     *
     * @param source0
     * @param destination1
     * @return BulkReply
     */
    @Override
    public BulkReply rpoplpush(byte[] source0, byte[] destination1) throws RedisException {
        List<BytesValue> source = _getlist(source0, false);
        int l;
        if (source == null || (l = source.size()) == 0) {
            return NIL_REPLY;
        } else {
            List<BytesValue> dest = _getlist(destination1, true);
            BytesValue popped = source.get(l - 1);
            source.remove(l - 1);
            dest.add(0, popped);
            return new BulkReply(popped.getBytes());
        }
    }

    /**
     * Append one or multiple values to a list
     * List
     *
     * @param key0
     * @param value1
     * @return IntegerReply
     */
    @Override
    public IntegerReply rpush(byte[] key0, byte[][] value1) throws RedisException {
        List<BytesValue> list = _getlist(key0, true);
        for (byte[] bytes : value1) {
            list.add(new BytesKey(bytes));
        }
        return integer(list.size());
    }

    /**
     * Append a value to a list, only if the list exists
     * List
     *
     * @param key0
     * @param value1
     * @return IntegerReply
     */
    @Override
    public IntegerReply rpushx(byte[] key0, byte[] value1) throws RedisException {
        List<BytesValue> list = _getlist(key0, false);
        if (list == null) {
            return integer(0);
        } else {
            list.add(new BytesKey(value1));
            return integer(list.size());
        }
    }

    /**
     * Delete a key
     * Generic
     *
     * @param key0
     * @return IntegerReply
     */
    @Override
    public IntegerReply del(byte[][] key0) throws RedisException {
        int total = 0;
        for (byte[] bytes : key0) {
            Object remove = data.remove(bytes);
            if (remove != null) {
                total++;
            }
            expires.remove(bytes);
        }
        return integer(total);
    }

    /**
     * Return a serialized version of the value stored at the specified key.
     * Generic
     *
     * @param key0
     * @return BulkReply
     */
    @Override
    public BulkReply dump(byte[] key0) throws RedisException {
        throw new RedisException("Not supported");
    }

    /**
     * Determine if a key exists
     * Generic
     *
     * @param key0
     * @return IntegerReply
     */
    @Override
    public IntegerReply exists(byte[] key0) throws RedisException {
        Object o = _get(key0);
        return o == null ? integer(0) : integer(1);
    }

    /**
     * Set a key's time to live in seconds
     * Generic
     *
     * @param key0
     * @param seconds1
     * @return IntegerReply
     */
    @Override
    public IntegerReply expire(byte[] key0, byte[] seconds1) throws RedisException {
        Object o = _get(key0);
        if (o == null) {
            return integer(0);
        } else {
            expires.put(key0, bytesToNum(seconds1) * 1000 + now());
            return integer(1);
        }
    }

    /**
     * Set the expiration for a key as a UNIX timestamp
     * Generic
     *
     * @param key0
     * @param timestamp1
     * @return IntegerReply
     */
    @Override
    public IntegerReply expireat(byte[] key0, byte[] timestamp1) throws RedisException {
        Object o = _get(key0);
        if (o == null) {
            return integer(0);
        } else {
            expires.put(key0, bytesToNum(timestamp1) * 1000);
            return integer(1);
        }
    }

    /**
     * Find all keys matching the given pattern
     * Generic
     *
     * @param pattern0
     * @return MultiBulkReply
     */
    @Override
    public MultiBulkReply keys(byte[] pattern0) throws RedisException {
        if (pattern0 == null) {
            throw new RedisException("wrong number of arguments for KEYS");
        }
        List<Reply<ByteBuf>> replies = new ArrayList<Reply<ByteBuf>>();
        Iterator<Object> it = data.keySet().iterator();
        while (it.hasNext()) {
            BytesKey key = (BytesKey) it.next();
            byte[] bytes = key.getBytes();
            boolean expired = false;
            Long l = expires.get(key);
            if (l != null) {
                if (l < now()) {
                    expired = true;
                    it.remove();
                }
            }
            if (matches(bytes, pattern0, 0, 0) && !expired) {
                replies.add(new BulkReply(bytes));
            }
        }
        return new MultiBulkReply(replies.toArray(new Reply[replies.size()]));
    }

    /**
     * Atomically transfer a key from a Redis instance to another one.
     * Generic
     *
     * @param host0
     * @param port1
     * @param key2
     * @param destination_db3
     * @param timeout4
     * @return StatusReply
     */
    @Override
    public StatusReply migrate(byte[] host0, byte[] port1, byte[] key2, byte[] destination_db3, byte[] timeout4) throws RedisException {
        // TODO: Multiserver
        return null;
    }

    /**
     * Move a key to another database
     * Generic
     *
     * @param key0
     * @param db1
     * @return IntegerReply
     */
    @Override
    public IntegerReply move(byte[] key0, byte[] db1) throws RedisException {
        throw new RedisException("Not supported");
    }

    /**
     * Inspect the internals of Redis objects
     * Generic
     *
     * @param subcommand0
     * @param arguments1
     * @return Reply
     */
    @Override
    public Reply object(byte[] subcommand0, byte[][] arguments1) throws RedisException {
        throw new RedisException("Not supported");
    }

    /**
     * Remove the expiration from a key
     * Generic
     *
     * @param key0
     * @return IntegerReply
     */
    @Override
    public IntegerReply persist(byte[] key0) throws RedisException {
        Object o = _get(key0);
        if (o == null) {
            return integer(0);
        } else {
            Long remove = expires.remove(key0);
            return remove == null ? integer(0) : integer(1);
        }
    }

    /**
     * Set a key's time to live in milliseconds
     * Generic
     *
     * @param key0
     * @param milliseconds1
     * @return IntegerReply
     */
    @Override
    public IntegerReply pexpire(byte[] key0, byte[] milliseconds1) throws RedisException {
        Object o = _get(key0);
        if (o == null) {
            return integer(0);
        } else {
            expires.put(key0, bytesToNum(milliseconds1) + now());
            return integer(1);
        }
    }

    /**
     * Set the expiration for a key as a UNIX timestamp specified in milliseconds
     * Generic
     *
     * @param key0
     * @param milliseconds_timestamp1
     * @return IntegerReply
     */
    @Override
    public IntegerReply pexpireat(byte[] key0, byte[] milliseconds_timestamp1) throws RedisException {
        Object o = _get(key0);
        if (o == null) {
            return integer(0);
        } else {
            expires.put(key0, bytesToNum(milliseconds_timestamp1));
            return integer(1);
        }
    }

    /**
     * Get the time to live for a key in milliseconds
     * Generic
     *
     * @param key0
     * @return IntegerReply
     */
    @Override
    public IntegerReply pttl(byte[] key0) throws RedisException {
        Object o = _get(key0);
        if (o == null) {
            return integer(-1);
        } else {
            Long aLong = expires.get(key0);
            if (aLong == null) {
                return integer(-1);
            } else {
                return integer(aLong - now());
            }
        }
    }

    /**
     * Return a random key from the keyspace
     * Generic
     *
     * @return BulkReply
     */
    @Override
    public BulkReply randomkey() throws RedisException {
        // This implementation mirrors that of Redis. I'm not
        // sure I believe that this is a great algorithm but
        // it beats the alternatives that are very inefficient.
        if (tableField != null) {
            int size = data.size();
            if (size == 0) {
                return NIL_REPLY;
            }
            try {
                BytesKey key = getRandomKey(data);
                return new BulkReply(key.getBytes());
            } catch (Exception e) {
                throw new RedisException(e);
            }
        }
        return null;
    }

    private BytesKey getRandomKey(Map data1) throws IllegalAccessException {
        Map.Entry[] table = (Map.Entry[]) tableField.get(data1);
        int length = table.length;
        Map.Entry entry;
        do {
            entry = table[r.nextInt(length)];
        } while (entry == null);

        int entries = 0;
        Map.Entry current = entry;
        do {
            entries++;
            current = (Map.Entry) nextField.get(current);
        } while (current != null);
        int choose = r.nextInt(entries);
        current = entry;
        while (choose-- != 0) current = (Map.Entry) nextField.get(current);
        return (BytesKey) current.getKey();
    }

    /**
     * Rename a key
     * Generic
     *
     * @param key0
     * @param newkey1
     * @return StatusReply
     */
    @Override
    public StatusReply rename(byte[] key0, byte[] newkey1) throws RedisException {
        Object o = _get(key0);
        if (o == null) {
            throw noSuchKey();
        } else {
            data.put(newkey1, data.remove(key0));
            expires.put(newkey1, expires.remove(key0));
            return OK;
        }
    }

    /**
     * Rename a key, only if the new key does not exist
     * Generic
     *
     * @param key0
     * @param newkey1
     * @return IntegerReply
     */
    @Override
    public IntegerReply renamenx(byte[] key0, byte[] newkey1) throws RedisException {
        Object o = _get(key0);
        if (o == null) {
            throw noSuchKey();
        } else {
            Object newo = _get(newkey1);
            if (newo == null) {
                data.put(newkey1, data.remove(key0));
                expires.put(newkey1, expires.remove(key0));
                return integer(1);
            } else {
                return integer(0);
            }
        }
    }

    /**
     * Create a key using the provided serialized value, previously obtained using DUMP.
     * Generic
     *
     * @param key0
     * @param ttl1
     * @param serialized_value2
     * @return StatusReply
     */
    @Override
    public StatusReply restore(byte[] key0, byte[] ttl1, byte[] serialized_value2) throws RedisException {
        throw new RedisException("Not supported");
    }

    /**
     * Sort the elements in a list, set or sorted set
     * Generic
     * <p/>
     * SORT key [BY pattern]
     * [LIMIT offset count]
     * [GET pattern [GET pattern ...]]
     * [ASC|DESC]
     * [ALPHA]
     * [STORE destination]
     *
     * @param key0
     * @param pattern1_offset_or_count2_pattern3
     * @return Reply
     */
    @Override
    public Reply sort(byte[] key0, byte[][] pattern1_offset_or_count2_pattern3) throws RedisException {
        // TODO
        return null;
    }

    /**
     * Get the time to live for a key
     * Generic
     *
     * @param key0
     * @return IntegerReply
     */
    @Override
    public IntegerReply ttl(byte[] key0) throws RedisException {
        Object o = _get(key0);
        if (o == null) {
            return integer(-1);
        } else {
            Long aLong = expires.get(key0);
            if (aLong == null) {
                return integer(-1);
            } else {
                return integer((aLong - now()) / 1000);
            }
        }
    }

    /**
     * Determine the type stored at key
     * Generic
     *
     * @param key0
     * @return StatusReply
     */
    @Override
    public StatusReply type(byte[] key0) throws RedisException {
        Object o = _get(key0);
        if (o == null) {
            return new StatusReply("none");
        } else if (o instanceof byte[]) {
            return new StatusReply("string");
        } else if (o instanceof Map) {
            return new StatusReply("hash");
        } else if (o instanceof List) {
            return new StatusReply("list");
        } else if (o instanceof SortedSet) {
            return new StatusReply("zset");
        } else if (o instanceof Set) {
            return new StatusReply("set");
        }
        return null;
    }

    /**
     * Forget about all watched keys
     * Transactions
     *
     * @return StatusReply
     */
    @Override
    public StatusReply unwatch() throws RedisException {
        // TODO: Transactions
        return null;
    }

    /**
     * Watch the given keys to determine execution of the MULTI/EXEC block
     * Transactions
     *
     * @param key0
     * @return StatusReply
     */
    @Override
    public StatusReply watch(byte[][] key0) throws RedisException {
        // TODO: Transactions
        return null;
    }

    /**
     * Execute a Lua script server side
     * Scripting
     *
     * @param script0
     * @param numkeys1
     * @param key2
     * @return Reply
     */
    @Override
    public Reply eval(byte[] script0, byte[] numkeys1, byte[][] key2) throws RedisException {
        throw new RedisException("Not supported");
    }

    /**
     * Execute a Lua script server side
     * Scripting
     *
     * @param sha10
     * @param numkeys1
     * @param key2
     * @return Reply
     */
    @Override
    public Reply evalsha(byte[] sha10, byte[] numkeys1, byte[][] key2) throws RedisException {
        throw new RedisException("Not supported");
    }

    /**
     * Check existence of scripts in the script cache.
     * Scripting
     *
     * @param script0
     * @return Reply
     */
    @Override
    public Reply script_exists(byte[][] script0) throws RedisException {
        throw new RedisException("Not supported");
    }

    /**
     * Remove all the scripts from the script cache.
     * Scripting
     *
     * @return Reply
     */
    @Override
    public Reply script_flush() throws RedisException {
        throw new RedisException("Not supported");
    }

    /**
     * Kill the script currently in execution.
     * Scripting
     *
     * @return Reply
     */
    @Override
    public Reply script_kill() throws RedisException {
        throw new RedisException("Not supported");
    }

    /**
     * Load the specified Lua script into the script cache.
     * Scripting
     *
     * @param script0
     * @return Reply
     */
    @Override
    public Reply script_load(byte[] script0) throws RedisException {
        throw new RedisException("Not supported");
    }

    /**
     * Delete one or more hash fields
     * Hash
     *
     * @param key0
     * @param field1
     * @return IntegerReply
     */
    @Override
    public IntegerReply hdel(byte[] key0, byte[][] field1) throws RedisException {
        BytesKeyObjectMap<byte[]> hash = _gethash(key0, false);
        int total = 0;
        for (byte[] hkey : field1) {
            total += hash.remove(hkey) == null ? 0 : 1;
        }
        return integer(total);
    }

    /**
     * Determine if a hash field exists
     * Hash
     *
     * @param key0
     * @param field1
     * @return IntegerReply
     */
    @Override
    public IntegerReply hexists(byte[] key0, byte[] field1) throws RedisException {
        return _gethash(key0, false).get(field1) == null ? integer(0) : integer(1);
    }

    /**
     * Get the value of a hash field
     * Hash
     *
     * @param key0
     * @param field1
     * @return BulkReply
     */
    @Override
    public BulkReply hget(byte[] key0, byte[] field1) throws RedisException {
        byte[] bytes = _gethash(key0, false).get(field1);
        if (bytes == null) {
            return NIL_REPLY;
        } else {
            return new BulkReply(bytes);
        }
    }

    /**
     * Get all the fields and values in a hash
     * Hash
     *
     * @param key0
     * @return MultiBulkReply
     */
    @Override
    public MultiBulkReply hgetall(byte[] key0) throws RedisException {
        BytesKeyObjectMap<byte[]> hash = _gethash(key0, false);
        int size = hash.size();
        Reply[] replies = new Reply[size * 2];
        int i = 0;
        for (Map.Entry<Object, byte[]> entry : hash.entrySet()) {
            replies[i++] = new BulkReply(((BytesKey) entry.getKey()).getBytes());
            replies[i++] = new BulkReply(entry.getValue());
        }
        return new MultiBulkReply(replies);
    }

    /**
     * Increment the integer value of a hash field by the given number
     * Hash
     *
     * @param key0
     * @param field1
     * @param increment2
     * @return IntegerReply
     */
    @Override
    public IntegerReply hincrby(byte[] key0, byte[] field1, byte[] increment2) throws RedisException {
        BytesKeyObjectMap<byte[]> hash = _gethash(key0, true);
        byte[] field = hash.get(field1);
        int increment = _toint(increment2);
        if (field == null) {
            hash.put(field1, increment2);
            return new IntegerReply(increment);
        } else {
            int i = _toint(field);
            int value = i + increment;
            hash.put(field1, numToBytes(value, false));
            return new IntegerReply(value);
        }
    }

    /**
     * Increment the float value of a hash field by the given amount
     * Hash
     *
     * @param key0
     * @param field1
     * @param increment2
     * @return BulkReply
     */
    @Override
    public BulkReply hincrbyfloat(byte[] key0, byte[] field1, byte[] increment2) throws RedisException {
        BytesKeyObjectMap<byte[]> hash = _gethash(key0, true);
        byte[] field = hash.get(field1);
        double increment = _todouble(increment2);
        if (field == null) {
            hash.put(field1, increment2);
            return new BulkReply(increment2);
        } else {
            double d = _todouble(field);
            double value = d + increment;
            byte[] bytes = _tobytes(value);
            hash.put(field1, bytes);
            return new BulkReply(bytes);
        }
    }

    /**
     * Get all the fields in a hash
     * Hash
     *
     * @param key0
     * @return MultiBulkReply
     */
    @Override
    public MultiBulkReply hkeys(byte[] key0) throws RedisException {
        BytesKeyObjectMap<byte[]> hash = _gethash(key0, false);
        int size = hash.size();
        Reply[] replies = new Reply[size];
        int i = 0;
        for (Object hkey : hash.keySet()) {
            replies[i++] = new BulkReply(((BytesKey) hkey).getBytes());
        }
        return new MultiBulkReply(replies);
    }

    /**
     * Get the number of fields in a hash
     * Hash
     *
     * @param key0
     * @return IntegerReply
     */
    @Override
    public IntegerReply hlen(byte[] key0) throws RedisException {
        BytesKeyObjectMap<byte[]> hash = _gethash(key0, false);
        return integer(hash.size());
    }

    /**
     * Get the values of all the given hash fields
     * Hash
     *
     * @param key0
     * @param field1
     * @return MultiBulkReply
     */
    @Override
    public MultiBulkReply hmget(byte[] key0, byte[][] field1) throws RedisException {
        BytesKeyObjectMap<byte[]> hash = _gethash(key0, false);
        int length = field1.length;
        Reply[] replies = new Reply[length];
        for (int i = 0; i < length; i++) {
            byte[] bytes = hash.get(field1[i]);
            if (bytes == null) {
                replies[i] = NIL_REPLY;
            } else {
                replies[i] = new BulkReply(bytes);
            }
        }
        return new MultiBulkReply(replies);
    }

    /**
     * Set multiple hash fields to multiple values
     * Hash
     *
     * @param key0
     * @param field_or_value1
     * @return StatusReply
     */
    @Override
    public StatusReply hmset(byte[] key0, byte[][] field_or_value1) throws RedisException {
        BytesKeyObjectMap<byte[]> hash = _gethash(key0, true);
        if (field_or_value1.length % 2 != 0) {
            throw new RedisException("wrong number of arguments for HMSET");
        }
        for (int i = 0; i < field_or_value1.length; i += 2) {
            hash.put(field_or_value1[i], field_or_value1[i + 1]);
        }
        return OK;
    }

    /**
     * Set the string value of a hash field
     * Hash
     *
     * @param key0
     * @param field1
     * @param value2
     * @return IntegerReply
     */
    @Override
    public IntegerReply hset(byte[] key0, byte[] field1, byte[] value2) throws RedisException {
        BytesKeyObjectMap<byte[]> hash = _gethash(key0, true);
        Object put = hash.put(field1, value2);
        return put == null ? integer(1) : integer(0);
    }

    /**
     * Set the value of a hash field, only if the field does not exist
     * Hash
     *
     * @param key0
     * @param field1
     * @param value2
     * @return IntegerReply
     */
    @Override
    public IntegerReply hsetnx(byte[] key0, byte[] field1, byte[] value2) throws RedisException {
        BytesKeyObjectMap<byte[]> hash = _gethash(key0, true);
        byte[] bytes = hash.get(field1);
        if (bytes == null) {
            hash.put(field1, value2);
            return integer(1);
        } else {
            return integer(0);
        }
    }

    /**
     * Get all the values in a hash
     * Hash
     *
     * @param key0
     * @return MultiBulkReply
     */
    @Override
    public MultiBulkReply hvals(byte[] key0) throws RedisException {
        BytesKeyObjectMap<byte[]> hash = _gethash(key0, false);
        int size = hash.size();
        Reply[] replies = new Reply[size];
        int i = 0;
        for (byte[] hvalue : hash.values()) {
            replies[i++] = new BulkReply(hvalue);
        }
        return new MultiBulkReply(replies);
    }

    /**
     * Post a message to a channel
     * Pubsub
     *
     * @param channel0
     * @param message1
     * @return IntegerReply
     */
    @Override
    public IntegerReply publish(byte[] channel0, byte[] message1) throws RedisException {
        // TODO: Pubsub
        return null;
    }

    /**
     * Add one or more members to a set
     * Set
     *
     * @param key0
     * @param member1
     * @return IntegerReply
     */
    @Override
    public IntegerReply sadd(byte[] key0, byte[][] member1) throws RedisException {
        BytesKeySet set = _getset(key0, true);
        int total = 0;
        for (byte[] bytes : member1) {
            if (set.add(bytes)) total++;
        }
        return integer(total);
    }

    /**
     * Get the number of members in a set
     * Set
     *
     * @param key0
     * @return IntegerReply
     */
    @Override
    public IntegerReply scard(byte[] key0) throws RedisException {
        BytesKeySet bytesKeys = _getset(key0, false);
        return integer(bytesKeys.size());
    }

    /**
     * Subtract multiple sets
     * Set
     *
     * @param key0
     * @return MultiBulkReply
     */
    @Override
    public MultiBulkReply sdiff(byte[][] key0) throws RedisException {
        BytesKeySet set = _sdiff(key0);
        return _setreply(set);
    }

    private BytesKeySet _sdiff(byte[][] key0) throws RedisException {
        BytesKeySet set = null;
        for (byte[] key : key0) {
            if (set == null) {
                set = new BytesKeySet();
                set.addAll(_getset(key, false));
            } else {
                BytesKeySet c = _getset(key, false);
                set.removeAll(c);
            }
        }
        if (set == null) {
            throw new RedisException("wrong number of arguments for 'sdiff' command");
        }
        return set;
    }

    /**
     * Subtract multiple sets and store the resulting set in a key
     * Set
     *
     * @param destination0
     * @param key1
     * @return IntegerReply
     */
    @Override
    public IntegerReply sdiffstore(byte[] destination0, byte[][] key1) throws RedisException {
        Object o = _get(destination0);
        if (o == null || o instanceof Set) {
            BytesKeySet set = _sdiff(key1);
            _put(destination0, set);
            return integer(set.size());
        } else {
            throw invalidValue();
        }
    }

    /**
     * Intersect multiple sets
     * Set
     *
     * @param key0
     * @return MultiBulkReply
     */
    @Override
    public MultiBulkReply sinter(byte[][] key0) throws RedisException {
        BytesKeySet set = _sinter(key0);
        return _setreply(set);
    }

    private BytesKeySet _sinter(byte[][] key0) throws RedisException {
        BytesKeySet set = null;
        for (byte[] key : key0) {
            if (set == null) {
                set = _getset(key, false);
            } else {
                BytesKeySet inter = new BytesKeySet();
                BytesKeySet newset = _getset(key, false);
                for (BytesKey bytesKey : newset) {
                    if (set.contains(bytesKey)) {
                        inter.add(bytesKey);
                    }
                }
                set = inter;
            }
        }
        if (set == null) {
            throw new RedisException("wrong number of arguments for 'sinter' command");
        }
        return set;
    }

    /**
     * Intersect multiple sets and store the resulting set in a key
     * Set
     *
     * @param destination0
     * @param key1
     * @return IntegerReply
     */
    @Override
    public IntegerReply sinterstore(byte[] destination0, byte[][] key1) throws RedisException {
        Object o = _get(destination0);
        if (o == null || o instanceof Set) {
            BytesKeySet set = _sinter(key1);
            _put(destination0, set);
            return integer(set.size());
        } else {
            throw invalidValue();
        }
    }

    /**
     * Determine if a given value is a member of a set
     * Set
     *
     * @param key0
     * @param member1
     * @return IntegerReply
     */
    @Override
    public IntegerReply sismember(byte[] key0, byte[] member1) throws RedisException {
        BytesKeySet set = _getset(key0, false);
        return set.contains(member1) ? integer(1) : integer(0);
    }

    /**
     * Get all the members in a set
     * Set
     *
     * @param key0
     * @return MultiBulkReply
     */
    @Override
    public MultiBulkReply smembers(byte[] key0) throws RedisException {
        BytesKeySet set = _getset(key0, false);
        return _setreply(set);
    }

    private MultiBulkReply _setreply(BytesKeySet set) {
        Reply[] replies = new Reply[set.size()];
        int i = 0;
        for (BytesKey value : set) {
            replies[i++] = new BulkReply(value.getBytes());
        }
        return new MultiBulkReply(replies);
    }

    /**
     * Move a member from one set to another
     * Set
     *
     * @param source0
     * @param destination1
     * @param member2
     * @return IntegerReply
     */
    @Override
    public IntegerReply smove(byte[] source0, byte[] destination1, byte[] member2) throws RedisException {
        BytesKeySet source = _getset(source0, false);
        if (source.remove(member2)) {
            BytesKeySet dest = _getset(destination1, true);
            dest.add(member2);
            return integer(1);
        } else {
            return integer(0);
        }
    }

    /**
     * Remove and return a random member from a set
     * Set
     *
     * @param key0
     * @return BulkReply
     */
    @Override
    public BulkReply spop(byte[] key0) throws RedisException {
        if (mapField == null || tableField == null) {
            throw new RedisException("Not supported");
        }
        BytesKeySet set = _getset(key0, false);
        if (set.size() == 0) return NIL_REPLY;
        try {
            BytesKey key = getRandomKey((Map) mapField.get(set));
            set.remove(key);
            return new BulkReply(key.getBytes());
        } catch (IllegalAccessException e) {
            throw new RedisException("Not supported");
        }
    }

    /**
     * Get a random member from a set
     * Set
     *
     * @param key0
     * @return BulkReply
     */
    @Override
    public Reply srandmember(byte[] key0, byte[] count1) throws RedisException {
        if (mapField == null || tableField == null) {
            throw new RedisException("Not supported");
        }
        BytesKeySet set = _getset(key0, false);
        int size = set.size();
        try {
            if (count1 == null) {
                if (size == 0) return NIL_REPLY;
                BytesKey key = getRandomKey((Map) mapField.get(set));
                return new BulkReply(key.getBytes());
            } else {
                int count = _toint(count1);
                int distinct = count < 0 ? -1 : 1;
                count *= distinct;
                if (count > size && distinct > 0) count = size;
                Reply[] replies = new Reply[count];
                Set<BytesKey> found;
                if (distinct > 0) {
                    found = new HashSet<BytesKey>(count);
                } else {
                    found = null;
                }
                for (int i = 0; i < count; i++) {
                    BytesKey key;
                    do {
                        key = getRandomKey((Map) mapField.get(set));
                    } while (found != null && !found.add(key));
                    replies[i] = new BulkReply(key.getBytes());
                }
                return new MultiBulkReply(replies);
            }
        } catch (IllegalAccessException e) {
            throw new RedisException("Not supported");
        }
    }

    /**
     * Remove one or more members from a set
     * Set
     *
     * @param key0
     * @param member1
     * @return IntegerReply
     */
    @Override
    public IntegerReply srem(byte[] key0, byte[][] member1) throws RedisException {
        BytesKeySet set = _getset(key0, false);
        int total = 0;
        for (byte[] member : member1) {
            if (set.remove(member)) {
                total++;
            }
        }
        return new IntegerReply(total);
    }

    /**
     * Add multiple sets
     * Set
     *
     * @param key0
     * @return MultiBulkReply
     */
    @Override
    public MultiBulkReply sunion(byte[][] key0) throws RedisException {
        BytesKeySet set = _sunion(key0);
        return _setreply(set);
    }

    private BytesKeySet _sunion(byte[][] key0) throws RedisException {
        BytesKeySet set = null;
        for (byte[] key : key0) {
            if (set == null) {
                set = new BytesKeySet();
                set.addAll(_getset(key, false));
            } else {
                set.addAll(_getset(key, false));
            }
        }
        if (set == null) {
            throw new RedisException("wrong number of arguments for 'sunion' command");
        }
        return set;
    }

    /**
     * Add multiple sets and store the resulting set in a key
     * Set
     *
     * @param destination0
     * @param key1
     * @return IntegerReply
     */
    @Override
    public IntegerReply sunionstore(byte[] destination0, byte[][] key1) throws RedisException {
        Object o = _get(destination0);
        if (o == null || o instanceof Set) {
            BytesKeySet set = _sunion(key1);
            _put(destination0, set);
            return integer(set.size());
        } else {
            throw invalidValue();
        }
    }

    /**
     * Add one or more members to a sorted set, or update its score if it already exists
     * Sorted_set
     *
     * @param args
     * @return IntegerReply
     */
    @Override
    public IntegerReply zadd(byte[][] args) throws RedisException {
        if (args.length < 3 || (args.length - 1) % 2 == 1) {
            throw new RedisException("wrong number of arguments for 'zadd' command");
        }
        byte[] key = args[0];
        ZSet zset = _getzset(key, true);
        int total = 0;
        for (int i = 1; i < args.length; i += 2) {
            byte[] value = args[i + 1];
            byte[] score = args[i];
            if (zset.add(new BytesKey(value), _todouble(score))) {
                total++;
            }
        }
        return integer(total);
    }

    private double _todouble(byte[] score) {
        return parseDouble(new String(score));
    }

    /**
     * Get the number of members in a sorted set
     * Sorted_set
     *
     * @param key0
     * @return IntegerReply
     */
    @Override
    public IntegerReply zcard(byte[] key0) throws RedisException {
        ZSet zset = _getzset(key0, false);
        return integer(zset.size());
    }

    /**
     * Count the members in a sorted set with scores within the given values
     * Sorted_set
     *
     * @param key0
     * @param min1
     * @param max2
     * @return IntegerReply
     */
    @Override
    public IntegerReply zcount(byte[] key0, byte[] min1, byte[] max2) throws RedisException {
        if (key0 == null || min1 == null || max2 == null) {
            throw new RedisException("wrong number of arguments for 'zcount' command");
        }
        ZSet zset = _getzset(key0, false);
        Score min = _toscorerange(min1);
        Score max = _toscorerange(max2);
        Iterable<ZSetEntry> entries = zset.subSet(_todouble(min1), _todouble(max2));
        int total = 0;
        for (ZSetEntry entry : entries) {
            if (entry.getScore() == min.value && !min.inclusive) {
                continue;
            }
            if (entry.getScore() == max.value && !max.inclusive) {
                continue;
            }
            total++;
        }
        return integer(total);
    }

    /**
     * Increment the score of a member in a sorted set
     * Sorted_set
     *
     * @param key0
     * @param increment1
     * @param member2
     * @return BulkReply
     */
    @Override
    public BulkReply zincrby(byte[] key0, byte[] increment1, byte[] member2) throws RedisException {
        ZSet zset = _getzset(key0, true);
        ZSetEntry entry = zset.get(member2);
        double increment = _todouble(increment1);
        if (entry == null) {
            zset.add(new BytesKey(member2), increment);
            return new BulkReply(increment1);
        } else {
            zset.remove(member2);
            zset.add(entry.getKey(), entry.getScore() + increment);
            return new BulkReply(_tobytes(entry.getScore()));
        }
    }

    /**
     * Intersect multiple sorted sets and store the resulting sorted set in a new key
     * Sorted_set
     *
     * @param destination0
     * @param numkeys1
     * @param key2
     * @return IntegerReply
     */
    @Override
    public IntegerReply zinterstore(byte[] destination0, byte[] numkeys1, byte[][] key2) throws RedisException {
        return _zstore(destination0, numkeys1, key2, "zinterstore", false);
    }

    private IntegerReply _zstore(byte[] destination0, byte[] numkeys1, byte[][] key2, String name, boolean union) throws RedisException {
        if (destination0 == null || numkeys1 == null) {
            throw new RedisException("wrong number of arguments for '" + name + "' command");
        }
        int numkeys = _toint(numkeys1);
        if (key2.length < numkeys) {
            throw new RedisException("wrong number of arguments for '" + name + "' command");
        }
        int position = numkeys;
        double[] weights = null;
        Aggregate type = null;
        if (key2.length > position) {
            if ("weights".equals(new String(key2[position]).toLowerCase())) {
                position++;
                if (key2.length < position + numkeys) {
                    throw new RedisException("wrong number of arguments for '" + name + "' command");
                }
                weights = new double[numkeys];
                for (int i = position; i < position + numkeys; i++) {
                    weights[i - position] = _todouble(key2[i]);
                }
                position += numkeys;
            }
            if (key2.length > position + 1) {
                if ("aggregate".equals(new String(key2[position]).toLowerCase())) {
                    type = Aggregate.valueOf(new String(key2[position + 1]).toUpperCase());
                }
            } else if (key2.length != position) {
                throw new RedisException("wrong number of arguments for '" + name + "' command");
            }
        }
        del(new byte[][]{destination0});
        ZSet destination = _getzset(destination0, true);
        for (int i = 0; i < numkeys; i++) {
            ZSet zset = _getzset(key2[i], false);
            if (i == 0) {
                if (weights == null) {
                    destination.addAll(zset);
                } else {
                    double weight = weights[i];
                    for (ZSetEntry entry : zset) {
                        destination.add(entry.getKey(), entry.getScore() * weight);
                    }
                }
            } else {
                for (ZSetEntry entry : zset) {
                    BytesKey key = entry.getKey();
                    ZSetEntry current = destination.get(key);
                    destination.remove(key);
                    if (union || current != null) {
                        double newscore = entry.getScore() * (weights == null ? 1 : weights[i]);
                        if (type == null || type == Aggregate.SUM) {
                            if (current != null) {
                                newscore += current.getScore();
                            }
                        } else if (type == Aggregate.MIN) {
                            if (current != null && newscore > current.getScore()) {
                                newscore = current.getScore();
                            }
                        } else if (type == Aggregate.MAX) {
                            if (current != null && newscore < current.getScore()) {
                                newscore = current.getScore();
                            }
                        }
                        destination.add(key, newscore);
                    }
                }
                if (!union) {
                    for (ZSetEntry entry : new ZSet(destination)) {
                        BytesKey key = entry.getKey();
                        if (zset.get(key) == null) {
                            destination.remove(key);
                        }
                    }
                }
            }
        }
        return integer(destination.size());
    }

    enum Aggregate {SUM, MIN, MAX}

    /**
     * Return a range of members in a sorted set, by index
     * Sorted_set
     *
     * @param key0
     * @param start1
     * @param stop2
     * @param withscores3
     * @return MultiBulkReply
     */
    @Override
    public MultiBulkReply zrange(byte[] key0, byte[] start1, byte[] stop2, byte[] withscores3) throws RedisException {
        if (key0 == null || start1 == null || stop2 == null) {
            throw new RedisException("invalid number of argumenst for 'zrange' command");
        }
        boolean withscores = _checkcommand(withscores3, "withscores", true);
        ZSet zset = _getzset(key0, false);
        int size = zset.size();
        int start = _torange(start1, size);
        int end = _torange(stop2, size);
        Iterator<ZSetEntry> iterator = zset.iterator();
        List<Reply<ByteBuf>> list = new ArrayList<Reply<ByteBuf>>();
        for (int i = 0; i < size; i++) {
            if (iterator.hasNext()) {
                ZSetEntry next = iterator.next();
                if (i >= start && i <= end) {
                    list.add(new BulkReply(next.getKey().getBytes()));
                    if (withscores) {
                        list.add(new BulkReply(_tobytes(next.getScore())));
                    }
                } else if (i > end) {
                    break;
                }
            }
        }
        return new MultiBulkReply(list.toArray(new Reply[list.size()]));
    }

    private boolean _checkcommand(byte[] check, String command, boolean syntax) throws RedisException {
        boolean result;
        if (check != null) {
            if (new String(check).toLowerCase().equals(command)) {
                result = true;
            } else {
                if (syntax) {
                    throw new RedisException("syntax error");
                } else {
                    return false;
                }
            }
        } else {
            result = false;
        }
        return result;
    }

    /**
     * Return a range of members in a sorted set, by score
     * Sorted_set
     *
     * @param key0
     * @param min1
     * @param max2
     * @param withscores_offset_or_count4
     * @return MultiBulkReply
     */
    @Override
    public MultiBulkReply zrangebyscore(byte[] key0, byte[] min1, byte[] max2, byte[][] withscores_offset_or_count4) throws RedisException {
        ZSet zset = _getzset(key0, false);
        if (zset.isEmpty()) return MultiBulkReply.EMPTY;
        List<Reply<ByteBuf>> list = _zrangebyscore(min1, max2, withscores_offset_or_count4, zset, false);
        return new MultiBulkReply(list.toArray(new Reply[list.size()]));
    }

    private List<Reply<ByteBuf>> _zrangebyscore(byte[] min1, byte[] max2, byte[][] withscores_offset_or_count4, ZSet zset, boolean reverse) throws RedisException {
        int position = 0;
        boolean withscores = false;
        if (withscores_offset_or_count4.length > 0) {
            withscores = _checkcommand(withscores_offset_or_count4[0], "withscores", false);
        }
        if (withscores) position++;
        boolean limit = false;
        if (withscores_offset_or_count4.length > position) {
            limit = _checkcommand(withscores_offset_or_count4[position++], "limit", true);
        }
        if (withscores_offset_or_count4.length != position + (limit ? 2 : 0)) {
            throw new RedisException("syntax error");
        }
        int offset = 0;
        int number = Integer.MAX_VALUE;
        if (limit) {
            offset = _toint(withscores_offset_or_count4[position++]);
            number = _toint(withscores_offset_or_count4[position]);
            if (offset < 0 || number < 1) {
                throw notInteger();
            }
        }
        Score min = _toscorerange(min1);
        Score max = _toscorerange(max2);
        List<ZSetEntry> entries = zset.subSet(min.value, max.value);
        if (reverse) Collections.reverse(entries);
        int current = 0;
        List<Reply<ByteBuf>> list = new ArrayList<Reply<ByteBuf>>();
        for (ZSetEntry entry : entries) {
            if (current >= offset && current < offset + number) {
                list.add(new BulkReply(entry.getKey().getBytes()));
                if (withscores) list.add(new BulkReply(_tobytes(entry.getScore())));
            }
            current++;
        }
        return list;
    }

    private Score _toscorerange(byte[] specifier) {
        Score score = new Score();
        String s = new String(specifier).toLowerCase();
        if (s.startsWith("(")) {
            score.inclusive = false;
            s = s.substring(1);
        }
        if (s.equals("-inf")) {
            score.value = Double.NEGATIVE_INFINITY;
        } else if (s.equals("inf") || s.equals("+inf")) {
            score.value = Double.POSITIVE_INFINITY;
        } else {
            score.value = Double.parseDouble(s);
        }
        return score;
    }

    static class Score {
        boolean inclusive = true;
        double value;
    }

    /**
     * Determine the index of a member in a sorted set
     * Sorted_set
     *
     * @param key0
     * @param member1
     * @return Reply
     */
    @Override
    public Reply zrank(byte[] key0, byte[] member1) throws RedisException {
        List<ZSetEntry> zset = _getzset(key0, false).list();
        return _zrank(member1, zset);
    }

    /**
     * Remove one or more members from a sorted set
     * Sorted_set
     *
     * @param key0
     * @param member1
     * @return IntegerReply
     */
    @Override
    public IntegerReply zrem(byte[] key0, byte[][] member1) throws RedisException {
        ZSet zset = _getzset(key0, false);
        if (zset.isEmpty()) return integer(0);
        int total = 0;
        for (byte[] member : member1) {
            if (zset.remove(member)) {
                total++;
            }
        }
        return integer(total);
    }

    /**
     * Remove all members in a sorted set within the given indexes
     * Sorted_set
     *
     * @param key0
     * @param start1
     * @param stop2
     * @return IntegerReply
     */
    @Override
    public IntegerReply zremrangebyrank(byte[] key0, byte[] start1, byte[] stop2) throws RedisException {
        ZSet zset = _getzset(key0, false);
        if (zset.isEmpty()) return integer(0);
        int size = zset.size();
        int start = _torange(start1, size);
        int end = _torange(stop2, size);
        Iterator<ZSetEntry> iterator = zset.iterator();
        List<ZSetEntry> list = new ArrayList<ZSetEntry>();
        for (int i = 0; i < size; i++) {
            if (iterator.hasNext()) {
                ZSetEntry next = iterator.next();
                if (i >= start && i <= end) {
                    list.add(next);
                } else if (i > end) {
                    break;
                }
            }
        }
        int total = 0;
        for (ZSetEntry zSetEntry : list) {
            if (zset.remove(zSetEntry.getKey())) total++;
        }
        return integer(total);
    }

    /**
     * Remove all members in a sorted set within the given scores
     * Sorted_set
     *
     * @param key0
     * @param min1
     * @param max2
     * @return IntegerReply
     */
    @Override
    public IntegerReply zremrangebyscore(byte[] key0, byte[] min1, byte[] max2) throws RedisException {
        ZSet zset = _getzset(key0, false);
        if (zset.isEmpty()) return integer(0);
        Score min = _toscorerange(min1);
        Score max = _toscorerange(max2);
        List<ZSetEntry> entries = zset.subSet(min.value, max.value);
        int total = 0;
        for (ZSetEntry entry : new ArrayList<ZSetEntry>(entries)) {
            if (!min.inclusive && entry.getScore() == min.value) continue;
            if (!max.inclusive && entry.getScore() == max.value) continue;
            if (zset.remove(entry.getKey())) {
                total++;
            }
        }
        return integer(total);
    }

    /**
     * Return a range of members in a sorted set, by index, with scores ordered from high to low
     * Sorted_set
     *
     * @param key0
     * @param start1
     * @param stop2
     * @param withscores3
     * @return MultiBulkReply
     */
    @Override
    public MultiBulkReply zrevrange(byte[] key0, byte[] start1, byte[] stop2, byte[] withscores3) throws RedisException {
        if (key0 == null || start1 == null || stop2 == null) {
            throw new RedisException("invalid number of argumenst for 'zrevrange' command");
        }
        boolean withscores = _checkcommand(withscores3, "withscores", true);
        ZSet zset = _getzset(key0, false);
        int size = zset.size();
        int end = size - _torange(start1, size) - 1;
        int start = size - _torange(stop2, size) - 1;
        Iterator<ZSetEntry> iterator = zset.iterator();
        List<Reply<ByteBuf>> list = new ArrayList<Reply<ByteBuf>>();
        for (int i = 0; i < size; i++) {
            if (iterator.hasNext()) {
                ZSetEntry next = iterator.next();
                if (i >= start && i <= end) {
                    list.add(0, new BulkReply(next.getKey().getBytes()));
                    if (withscores) {
                        list.add(1, new BulkReply(_tobytes(next.getScore())));
                    }
                } else if (i > end) {
                    break;
                }
            }
        }
        return new MultiBulkReply(list.toArray(new Reply[list.size()]));
    }

    /**
     * Return a range of members in a sorted set, by score, with scores ordered from high to low
     * Sorted_set
     *
     * @param key0
     * @param max1
     * @param min2
     * @param withscores_offset_or_count4
     * @return MultiBulkReply
     */
    @Override
    public MultiBulkReply zrevrangebyscore(byte[] key0, byte[] max1, byte[] min2, byte[][] withscores_offset_or_count4) throws RedisException {
        ZSet zset = _getzset(key0, false);
        if (zset.isEmpty()) return MultiBulkReply.EMPTY;
        List<Reply<ByteBuf>> list = _zrangebyscore(min2, max1, withscores_offset_or_count4, zset, true);
        return new MultiBulkReply(list.toArray(new Reply[list.size()]));
    }

    /**
     * Determine the index of a member in a sorted set, with scores ordered from high to low
     * Sorted_set
     *
     * @param key0
     * @param member1
     * @return Reply
     */
    @Override
    public Reply zrevrank(byte[] key0, byte[] member1) throws RedisException {
        List<ZSetEntry> zset = _getzset(key0, false).list();
        Collections.reverse(zset);
        return _zrank(member1, zset);
    }

    private Reply _zrank(byte[] member1, List<ZSetEntry> zset) {
        BytesKey member = new BytesKey(member1);
        int position = 0;
        for (ZSetEntry entry : zset) {
            if (entry.getKey().equals(member)) {
                return integer(position);
            }
            position++;
        }
        return NIL_REPLY;
    }

    /**
     * Get the score associated with the given member in a sorted set
     * Sorted_set
     *
     * @param key0
     * @param member1
     * @return BulkReply
     */
    @Override
    public BulkReply zscore(byte[] key0, byte[] member1) throws RedisException {
        ZSet zset = _getzset(key0, false);
        ZSetEntry entry = zset.get(member1);
        double score = entry.getScore();
        return new BulkReply(_tobytes(score));
    }

    private byte[] _tobytes(double score) {
        return String.valueOf(score).getBytes();
    }

    /**
     * Add multiple sorted sets and store the resulting sorted set in a new key
     * Sorted_set
     *
     * @param destination0
     * @param numkeys1
     * @param key2
     * @return IntegerReply
     */
    @Override
    public IntegerReply zunionstore(byte[] destination0, byte[] numkeys1, byte[][] key2) throws RedisException {
        return _zstore(destination0, numkeys1, key2, "zunionstore", true);
    }

    public BytesKeyObjectMap<Object> getData() {
        return data;
    }
}
