package redis.server.netty;

import redis.netty4.BulkReply;
import redis.netty4.IntegerReply;
import redis.netty4.MultiBulkReply;
import redis.netty4.Reply;
import redis.netty4.StatusReply;
import redis.util.BytesKeyObjectMap;

public interface RedisServer {


    /**
     * Append a value to a key
     * String
     *
     * @param key0
     * @param value1
     * @return IntegerReply
     */
    public IntegerReply append(byte[] key0, byte[] value1) throws RedisException;

    /**
     * Count set bits in a string
     * String
     *
     * @param key0
     * @param start1
     * @param end2
     * @return IntegerReply
     */
    public IntegerReply bitcount(byte[] key0, byte[] start1, byte[] end2) throws RedisException;

    /**
     * Perform bitwise operations between strings
     * String
     *
     * @param operation0
     * @param destkey1
     * @param key2
     * @return IntegerReply
     */
    public IntegerReply bitop(byte[] operation0, byte[] destkey1, byte[][] key2) throws RedisException;

    /**
     * Decrement the integer value of a key by one
     * String
     *
     * @param key0
     * @return IntegerReply
     */
    public IntegerReply decr(byte[] key0) throws RedisException;

    /**
     * Decrement the integer value of a key by the given number
     * String
     *
     * @param key0
     * @param decrement1
     * @return IntegerReply
     */
    public IntegerReply decrby(byte[] key0, byte[] decrement1) throws RedisException;

    /**
     * Get the value of a key
     * String
     *
     * @param key0
     * @return BulkReply
     */
    public BulkReply get(byte[] key0) throws RedisException;

    /**
     * Returns the bit value at offset in the string value stored at key
     * String
     *
     * @param key0
     * @param offset1
     * @return IntegerReply
     */
    public IntegerReply getbit(byte[] key0, byte[] offset1) throws RedisException;

    /**
     * Get a substring of the string stored at a key
     * String
     *
     * @param key0
     * @param start1
     * @param end2
     * @return BulkReply
     */
    public BulkReply getrange(byte[] key0, byte[] start1, byte[] end2) throws RedisException;

    /**
     * Set the string value of a key and return its old value
     * String
     *
     * @param key0
     * @param value1
     * @return BulkReply
     */
    public BulkReply getset(byte[] key0, byte[] value1) throws RedisException;

    /**
     * Increment the integer value of a key by one
     * String
     *
     * @param key0
     * @return IntegerReply
     */
    public IntegerReply incr(byte[] key0) throws RedisException;

    /**
     * Increment the integer value of a key by the given amount
     * String
     *
     * @param key0
     * @param increment1
     * @return IntegerReply
     */
    public IntegerReply incrby(byte[] key0, byte[] increment1) throws RedisException;

    /**
     * Increment the float value of a key by the given amount
     * String
     *
     * @param key0
     * @param increment1
     * @return BulkReply
     */
    public BulkReply incrbyfloat(byte[] key0, byte[] increment1) throws RedisException;

    /**
     * Get the values of all the given keys
     * String
     *
     * @param key0
     * @return MultiBulkReply
     */
    public MultiBulkReply mget(byte[][] key0) throws RedisException;

    /**
     * Set multiple keys to multiple values
     * String
     *
     * @param key_or_value0
     * @return StatusReply
     */
    public StatusReply mset(byte[][] key_or_value0) throws RedisException;

    /**
     * Set multiple keys to multiple values, only if none of the keys exist
     * String
     *
     * @param key_or_value0
     * @return IntegerReply
     */
    public IntegerReply msetnx(byte[][] key_or_value0) throws RedisException;

    /**
     * Set the value and expiration in milliseconds of a key
     * String
     *
     * @param key0
     * @param milliseconds1
     * @param value2
     * @return Reply
     */
    public Reply psetex(byte[] key0, byte[] milliseconds1, byte[] value2) throws RedisException;

    /**
     * Set the string value of a key
     * String
     *
     * @param key0
     * @param value1
     * @return StatusReply
     */
    public StatusReply set(byte[] key0, byte[] value1) throws RedisException;

    /**
     * Sets or clears the bit at offset in the string value stored at key
     * String
     *
     * @param key0
     * @param offset1
     * @param value2
     * @return IntegerReply
     */
    public IntegerReply setbit(byte[] key0, byte[] offset1, byte[] value2) throws RedisException;

    /**
     * Set the value and expiration of a key
     * String
     *
     * @param key0
     * @param seconds1
     * @param value2
     * @return StatusReply
     */
    public StatusReply setex(byte[] key0, byte[] seconds1, byte[] value2) throws RedisException;

    /**
     * Set the value of a key, only if the key does not exist
     * String
     *
     * @param key0
     * @param value1
     * @return IntegerReply
     */
    public IntegerReply setnx(byte[] key0, byte[] value1) throws RedisException;

    /**
     * Overwrite part of a string at key starting at the specified offset
     * String
     *
     * @param key0
     * @param offset1
     * @param value2
     * @return IntegerReply
     */
    public IntegerReply setrange(byte[] key0, byte[] offset1, byte[] value2) throws RedisException;

    /**
     * Get the length of the value stored in a key
     * String
     *
     * @param key0
     * @return IntegerReply
     */
    public IntegerReply strlen(byte[] key0) throws RedisException;

    /**
     * Echo the given string
     * Connection
     *
     * @param message0
     * @return BulkReply
     */
    public BulkReply echo(byte[] message0) throws RedisException;

    /**
     * Ping the server
     * Connection
     *
     * @return StatusReply
     */
    public StatusReply ping() throws RedisException;

    /**
     * Close the connection
     * Connection
     *
     * @return StatusReply
     */
    public StatusReply quit() throws RedisException;

    /**
     * Change the selected database for the current connection
     * Connection
     *
     * @param index0
     * @return StatusReply
     */
    public StatusReply select(byte[] index0) throws RedisException;

    /**
     * Asynchronously rewrite the append-only file
     * Server
     *
     * @return StatusReply
     */
    public StatusReply bgrewriteaof() throws RedisException;

    /**
     * Asynchronously save the dataset to disk
     * Server
     *
     * @return StatusReply
     */
    public StatusReply bgsave() throws RedisException;

    /**
     * Kill the connection of a client
     * Server
     *
     * @param ip_port0
     * @return Reply
     */
    public Reply client_kill(byte[] ip_port0) throws RedisException;

    /**
     * Get the list of client connections
     * Server
     *
     * @return Reply
     */
    public Reply client_list() throws RedisException;

    /**
     * Get the current connection name
     * Server
     *
     * @return Reply
     */
    public Reply client_getname() throws RedisException;

    /**
     * Set the current connection name
     * Server
     *
     * @param connection_name0
     * @return Reply
     */
    public Reply client_setname(byte[] connection_name0) throws RedisException;

    /**
     * Get the value of a configuration parameter
     * Server
     *
     * @param parameter0
     * @return Reply
     */
    public Reply config_get(byte[] parameter0) throws RedisException;

    /**
     * Set a configuration parameter to the given value
     * Server
     *
     * @param parameter0
     * @param value1
     * @return Reply
     */
    public Reply config_set(byte[] parameter0, byte[] value1) throws RedisException;

    /**
     * Reset the stats returned by INFO
     * Server
     *
     * @return Reply
     */
    public Reply config_resetstat() throws RedisException;

    /**
     * Return the number of keys in the selected database
     * Server
     *
     * @return IntegerReply
     */
    public IntegerReply dbsize() throws RedisException;

    /**
     * Get debugging information about a key
     * Server
     *
     * @param key0
     * @return Reply
     */
    public Reply debug_object(byte[] key0) throws RedisException;

    /**
     * Make the server crash
     * Server
     *
     * @return Reply
     */
    public Reply debug_segfault() throws RedisException;

    /**
     * Remove all keys from all databases
     * Server
     *
     * @return StatusReply
     */
    public StatusReply flushall() throws RedisException;

    /**
     * Remove all keys from the current database
     * Server
     *
     * @return StatusReply
     */
    public StatusReply flushdb() throws RedisException;

    /**
     * Get information and statistics about the server
     * Server
     *
     * @param section0
     * @return BulkReply
     */
    public BulkReply info(byte[] section0) throws RedisException;

    /**
     * Get the UNIX time stamp of the last successful save to disk
     * Server
     *
     * @return IntegerReply
     */
    public IntegerReply lastsave() throws RedisException;

    /**
     * Listen for all requests received by the server in real time
     * Server
     *
     * @return Reply
     */
    public Reply monitor() throws RedisException;

    /**
     * Synchronously save the dataset to disk
     * Server
     *
     * @return Reply
     */
    public Reply save() throws RedisException;

    /**
     * Synchronously save the dataset to disk and then shut down the server
     * Server
     *
     * @param NOSAVE0
     * @param SAVE1
     * @return StatusReply
     */
    public StatusReply shutdown(byte[] NOSAVE0, byte[] SAVE1) throws RedisException;

    /**
     * Make the server a slave of another instance, or promote it as master
     * Server
     *
     * @param host0
     * @param port1
     * @return StatusReply
     */
    public StatusReply slaveof(byte[] host0, byte[] port1) throws RedisException;

    /**
     * Manages the Redis slow queries log
     * Server
     *
     * @param subcommand0
     * @param argument1
     * @return Reply
     */
    public Reply slowlog(byte[] subcommand0, byte[] argument1) throws RedisException;

    /**
     * Internal command used for replication
     * Server
     *
     * @return Reply
     */
    public Reply sync() throws RedisException;

    /**
     * Return the current server time
     * Server
     *
     * @return MultiBulkReply
     */
    public MultiBulkReply time() throws RedisException;

    /**
     * Remove and get the first element in a list, or block until one is available
     * List
     *
     * @param key0
     * @return MultiBulkReply
     */
    public MultiBulkReply blpop(byte[][] key0) throws RedisException;

    /**
     * Remove and get the last element in a list, or block until one is available
     * List
     *
     * @param key0
     * @return MultiBulkReply
     */
    public MultiBulkReply brpop(byte[][] key0) throws RedisException;

    /**
     * Pop a value from a list, push it to another list and return it; or block until one is available
     * List
     *
     * @param source0
     * @param destination1
     * @param timeout2
     * @return BulkReply
     */
    public BulkReply brpoplpush(byte[] source0, byte[] destination1, byte[] timeout2) throws RedisException;

    /**
     * Get an element from a list by its index
     * List
     *
     * @param key0
     * @param index1
     * @return BulkReply
     */
    public BulkReply lindex(byte[] key0, byte[] index1) throws RedisException;

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
    public IntegerReply linsert(byte[] key0, byte[] where1, byte[] pivot2, byte[] value3) throws RedisException;

    /**
     * Get the length of a list
     * List
     *
     * @param key0
     * @return IntegerReply
     */
    public IntegerReply llen(byte[] key0) throws RedisException;

    /**
     * Remove and get the first element in a list
     * List
     *
     * @param key0
     * @return BulkReply
     */
    public BulkReply lpop(byte[] key0) throws RedisException;

    /**
     * Prepend one or multiple values to a list
     * List
     *
     * @param key0
     * @param value1
     * @return IntegerReply
     */
    public IntegerReply lpush(byte[] key0, byte[][] value1) throws RedisException;

    /**
     * Prepend a value to a list, only if the list exists
     * List
     *
     * @param key0
     * @param value1
     * @return IntegerReply
     */
    public IntegerReply lpushx(byte[] key0, byte[] value1) throws RedisException;

    /**
     * Get a range of elements from a list
     * List
     *
     * @param key0
     * @param start1
     * @param stop2
     * @return MultiBulkReply
     */
    public MultiBulkReply lrange(byte[] key0, byte[] start1, byte[] stop2) throws RedisException;

    /**
     * Remove elements from a list
     * List
     *
     * @param key0
     * @param count1
     * @param value2
     * @return IntegerReply
     */
    public IntegerReply lrem(byte[] key0, byte[] count1, byte[] value2) throws RedisException;

    /**
     * Set the value of an element in a list by its index
     * List
     *
     * @param key0
     * @param index1
     * @param value2
     * @return StatusReply
     */
    public StatusReply lset(byte[] key0, byte[] index1, byte[] value2) throws RedisException;

    /**
     * Trim a list to the specified range
     * List
     *
     * @param key0
     * @param start1
     * @param stop2
     * @return StatusReply
     */
    public StatusReply ltrim(byte[] key0, byte[] start1, byte[] stop2) throws RedisException;

    /**
     * Remove and get the last element in a list
     * List
     *
     * @param key0
     * @return BulkReply
     */
    public BulkReply rpop(byte[] key0) throws RedisException;

    /**
     * Remove the last element in a list, append it to another list and return it
     * List
     *
     * @param source0
     * @param destination1
     * @return BulkReply
     */
    public BulkReply rpoplpush(byte[] source0, byte[] destination1) throws RedisException;

    /**
     * Append one or multiple values to a list
     * List
     *
     * @param key0
     * @param value1
     * @return IntegerReply
     */
    public IntegerReply rpush(byte[] key0, byte[][] value1) throws RedisException;

    /**
     * Append a value to a list, only if the list exists
     * List
     *
     * @param key0
     * @param value1
     * @return IntegerReply
     */
    public IntegerReply rpushx(byte[] key0, byte[] value1) throws RedisException;

    /**
     * Delete a key
     * Generic
     *
     * @param key0
     * @return IntegerReply
     */
    public IntegerReply del(byte[][] key0) throws RedisException;

    /**
     * Return a serialized version of the value stored at the specified key.
     * Generic
     *
     * @param key0
     * @return BulkReply
     */
    public BulkReply dump(byte[] key0) throws RedisException;

    /**
     * Determine if a key exists
     * Generic
     *
     * @param key0
     * @return IntegerReply
     */
    public IntegerReply exists(byte[] key0) throws RedisException;

    /**
     * Set a key's time to live in seconds
     * Generic
     *
     * @param key0
     * @param seconds1
     * @return IntegerReply
     */
    public IntegerReply expire(byte[] key0, byte[] seconds1) throws RedisException;

    /**
     * Set the expiration for a key as a UNIX timestamp
     * Generic
     *
     * @param key0
     * @param timestamp1
     * @return IntegerReply
     */
    public IntegerReply expireat(byte[] key0, byte[] timestamp1) throws RedisException;

    /**
     * Find all keys matching the given pattern
     * Generic
     *
     * @param pattern0
     * @return MultiBulkReply
     */
    public MultiBulkReply keys(byte[] pattern0) throws RedisException;

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
    public StatusReply migrate(byte[] host0, byte[] port1, byte[] key2, byte[] destination_db3, byte[] timeout4) throws RedisException;

    /**
     * Move a key to another database
     * Generic
     *
     * @param key0
     * @param db1
     * @return IntegerReply
     */
    public IntegerReply move(byte[] key0, byte[] db1) throws RedisException;

    /**
     * Inspect the internals of Redis objects
     * Generic
     *
     * @param subcommand0
     * @param arguments1
     * @return Reply
     */
    public Reply object(byte[] subcommand0, byte[][] arguments1) throws RedisException;

    /**
     * Remove the expiration from a key
     * Generic
     *
     * @param key0
     * @return IntegerReply
     */
    public IntegerReply persist(byte[] key0) throws RedisException;

    /**
     * Set a key's time to live in milliseconds
     * Generic
     *
     * @param key0
     * @param milliseconds1
     * @return IntegerReply
     */
    public IntegerReply pexpire(byte[] key0, byte[] milliseconds1) throws RedisException;

    /**
     * Set the expiration for a key as a UNIX timestamp specified in milliseconds
     * Generic
     *
     * @param key0
     * @param milliseconds_timestamp1
     * @return IntegerReply
     */
    public IntegerReply pexpireat(byte[] key0, byte[] milliseconds_timestamp1) throws RedisException;

    /**
     * Get the time to live for a key in milliseconds
     * Generic
     *
     * @param key0
     * @return IntegerReply
     */
    public IntegerReply pttl(byte[] key0) throws RedisException;

    /**
     * Return a random key from the keyspace
     * Generic
     *
     * @return BulkReply
     */
    public BulkReply randomkey() throws RedisException;

    /**
     * Rename a key
     * Generic
     *
     * @param key0
     * @param newkey1
     * @return StatusReply
     */
    public StatusReply rename(byte[] key0, byte[] newkey1) throws RedisException;

    /**
     * Rename a key, only if the new key does not exist
     * Generic
     *
     * @param key0
     * @param newkey1
     * @return IntegerReply
     */
    public IntegerReply renamenx(byte[] key0, byte[] newkey1) throws RedisException;

    /**
     * Create a key using the provided serialized value, previously obtained using DUMP.
     * Generic
     *
     * @param key0
     * @param ttl1
     * @param serialized_value2
     * @return StatusReply
     */
    public StatusReply restore(byte[] key0, byte[] ttl1, byte[] serialized_value2) throws RedisException;

    /**
     * Sort the elements in a list, set or sorted set
     * Generic
     *
     * @param key0
     * @param pattern1
     * @return Reply
     */
    public Reply sort(byte[] key0, byte[][] pattern1) throws RedisException;

    /**
     * Get the time to live for a key
     * Generic
     *
     * @param key0
     * @return IntegerReply
     */
    public IntegerReply ttl(byte[] key0) throws RedisException;

    /**
     * Determine the type stored at key
     * Generic
     *
     * @param key0
     * @return StatusReply
     */
    public StatusReply type(byte[] key0) throws RedisException;

    /**
     * Forget about all watched keys
     * Transactions
     *
     * @return StatusReply
     */
    public StatusReply unwatch() throws RedisException;

    /**
     * Watch the given keys to determine execution of the MULTI/EXEC block
     * Transactions
     *
     * @param key0
     * @return StatusReply
     */
    public StatusReply watch(byte[][] key0) throws RedisException;

    /**
     * Execute a Lua script server side
     * Scripting
     *
     * @param script0
     * @param numkeys1
     * @param key2
     * @return Reply
     */
    public Reply eval(byte[] script0, byte[] numkeys1, byte[][] key2) throws RedisException;

    /**
     * Execute a Lua script server side
     * Scripting
     *
     * @param sha10
     * @param numkeys1
     * @param key2
     * @return Reply
     */
    public Reply evalsha(byte[] sha10, byte[] numkeys1, byte[][] key2) throws RedisException;

    /**
     * Check existence of scripts in the script cache.
     * Scripting
     *
     * @param script0
     * @return Reply
     */
    public Reply script_exists(byte[][] script0) throws RedisException;

    /**
     * Remove all the scripts from the script cache.
     * Scripting
     *
     * @return Reply
     */
    public Reply script_flush() throws RedisException;

    /**
     * Kill the script currently in execution.
     * Scripting
     *
     * @return Reply
     */
    public Reply script_kill() throws RedisException;

    /**
     * Load the specified Lua script into the script cache.
     * Scripting
     *
     * @param script0
     * @return Reply
     */
    public Reply script_load(byte[] script0) throws RedisException;

    /**
     * Delete one or more hash fields
     * Hash
     *
     * @param key0
     * @param field1
     * @return IntegerReply
     */
    public IntegerReply hdel(byte[] key0, byte[][] field1) throws RedisException;

    /**
     * Determine if a hash field exists
     * Hash
     *
     * @param key0
     * @param field1
     * @return IntegerReply
     */
    public IntegerReply hexists(byte[] key0, byte[] field1) throws RedisException;

    /**
     * Get the value of a hash field
     * Hash
     *
     * @param key0
     * @param field1
     * @return BulkReply
     */
    public BulkReply hget(byte[] key0, byte[] field1) throws RedisException;

    /**
     * Get all the fields and values in a hash
     * Hash
     *
     * @param key0
     * @return MultiBulkReply
     */
    public MultiBulkReply hgetall(byte[] key0) throws RedisException;

    /**
     * Increment the integer value of a hash field by the given number
     * Hash
     *
     * @param key0
     * @param field1
     * @param increment2
     * @return IntegerReply
     */
    public IntegerReply hincrby(byte[] key0, byte[] field1, byte[] increment2) throws RedisException;

    /**
     * Increment the float value of a hash field by the given amount
     * Hash
     *
     * @param key0
     * @param field1
     * @param increment2
     * @return BulkReply
     */
    public BulkReply hincrbyfloat(byte[] key0, byte[] field1, byte[] increment2) throws RedisException;

    /**
     * Get all the fields in a hash
     * Hash
     *
     * @param key0
     * @return MultiBulkReply
     */
    public MultiBulkReply hkeys(byte[] key0) throws RedisException;

    /**
     * Get the number of fields in a hash
     * Hash
     *
     * @param key0
     * @return IntegerReply
     */
    public IntegerReply hlen(byte[] key0) throws RedisException;

    /**
     * Get the values of all the given hash fields
     * Hash
     *
     * @param key0
     * @param field1
     * @return MultiBulkReply
     */
    public MultiBulkReply hmget(byte[] key0, byte[][] field1) throws RedisException;

    /**
     * Set multiple hash fields to multiple values
     * Hash
     *
     * @param key0
     * @param field_or_value1
     * @return StatusReply
     */
    public StatusReply hmset(byte[] key0, byte[][] field_or_value1) throws RedisException;

    /**
     * Set the string value of a hash field
     * Hash
     *
     * @param key0
     * @param field1
     * @param value2
     * @return IntegerReply
     */
    public IntegerReply hset(byte[] key0, byte[] field1, byte[] value2) throws RedisException;

    /**
     * Set the value of a hash field, only if the field does not exist
     * Hash
     *
     * @param key0
     * @param field1
     * @param value2
     * @return IntegerReply
     */
    public IntegerReply hsetnx(byte[] key0, byte[] field1, byte[] value2) throws RedisException;

    /**
     * Get all the values in a hash
     * Hash
     *
     * @param key0
     * @return MultiBulkReply
     */
    public MultiBulkReply hvals(byte[] key0) throws RedisException;

    /**
     * Post a message to a channel
     * Pubsub
     *
     * @param channel0
     * @param message1
     * @return IntegerReply
     */
    public IntegerReply publish(byte[] channel0, byte[] message1) throws RedisException;

    /**
     * Add one or more members to a set
     * Set
     *
     * @param key0
     * @param member1
     * @return IntegerReply
     */
    public IntegerReply sadd(byte[] key0, byte[][] member1) throws RedisException;

    /**
     * Get the number of members in a set
     * Set
     *
     * @param key0
     * @return IntegerReply
     */
    public IntegerReply scard(byte[] key0) throws RedisException;

    /**
     * Subtract multiple sets
     * Set
     *
     * @param key0
     * @return MultiBulkReply
     */
    public MultiBulkReply sdiff(byte[][] key0) throws RedisException;

    /**
     * Subtract multiple sets and store the resulting set in a key
     * Set
     *
     * @param destination0
     * @param key1
     * @return IntegerReply
     */
    public IntegerReply sdiffstore(byte[] destination0, byte[][] key1) throws RedisException;

    /**
     * Intersect multiple sets
     * Set
     *
     * @param key0
     * @return MultiBulkReply
     */
    public MultiBulkReply sinter(byte[][] key0) throws RedisException;

    /**
     * Intersect multiple sets and store the resulting set in a key
     * Set
     *
     * @param destination0
     * @param key1
     * @return IntegerReply
     */
    public IntegerReply sinterstore(byte[] destination0, byte[][] key1) throws RedisException;

    /**
     * Determine if a given value is a member of a set
     * Set
     *
     * @param key0
     * @param member1
     * @return IntegerReply
     */
    public IntegerReply sismember(byte[] key0, byte[] member1) throws RedisException;

    /**
     * Get all the members in a set
     * Set
     *
     * @param key0
     * @return MultiBulkReply
     */
    public MultiBulkReply smembers(byte[] key0) throws RedisException;

    /**
     * Move a member from one set to another
     * Set
     *
     * @param source0
     * @param destination1
     * @param member2
     * @return IntegerReply
     */
    public IntegerReply smove(byte[] source0, byte[] destination1, byte[] member2) throws RedisException;

    /**
     * Remove and return a random member from a set
     * Set
     *
     * @param key0
     * @return BulkReply
     */
    public BulkReply spop(byte[] key0) throws RedisException;

    /**
     * Get one or multiple random members from a set
     * Set
     *
     * @param key0
     * @param count1
     * @return Reply
     */
    public Reply srandmember(byte[] key0, byte[] count1) throws RedisException;

    /**
     * Remove one or more members from a set
     * Set
     *
     * @param key0
     * @param member1
     * @return IntegerReply
     */
    public IntegerReply srem(byte[] key0, byte[][] member1) throws RedisException;

    /**
     * Add multiple sets
     * Set
     *
     * @param key0
     * @return MultiBulkReply
     */
    public MultiBulkReply sunion(byte[][] key0) throws RedisException;

    /**
     * Add multiple sets and store the resulting set in a key
     * Set
     *
     * @param destination0
     * @param key1
     * @return IntegerReply
     */
    public IntegerReply sunionstore(byte[] destination0, byte[][] key1) throws RedisException;

    /**
     * Add one or more members to a sorted set, or update its score if it already exists
     * Sorted_set
     *
     * @param args
     * @return IntegerReply
     */
    public IntegerReply zadd(byte[][] args) throws RedisException;

    /**
     * Get the number of members in a sorted set
     * Sorted_set
     *
     * @param key0
     * @return IntegerReply
     */
    public IntegerReply zcard(byte[] key0) throws RedisException;

    /**
     * Count the members in a sorted set with scores within the given values
     * Sorted_set
     *
     * @param key0
     * @param min1
     * @param max2
     * @return IntegerReply
     */
    public IntegerReply zcount(byte[] key0, byte[] min1, byte[] max2) throws RedisException;

    /**
     * Increment the score of a member in a sorted set
     * Sorted_set
     *
     * @param key0
     * @param increment1
     * @param member2
     * @return BulkReply
     */
    public BulkReply zincrby(byte[] key0, byte[] increment1, byte[] member2) throws RedisException;

    /**
     * Intersect multiple sorted sets and store the resulting sorted set in a new key
     * Sorted_set
     *
     * @param destination0
     * @param numkeys1
     * @param key2
     * @return IntegerReply
     */
    public IntegerReply zinterstore(byte[] destination0, byte[] numkeys1, byte[][] key2) throws RedisException;

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
    public MultiBulkReply zrange(byte[] key0, byte[] start1, byte[] stop2, byte[] withscores3) throws RedisException;

    /**
     * Return a range of members in a sorted set, by score
     * Sorted_set
     *
     * @param key0
     * @param min1
     * @param max2
     * @param withscores3
     * @param offset_or_count4
     * @return MultiBulkReply
     */
    public MultiBulkReply zrangebyscore(byte[] key0, byte[] min1, byte[] max2, byte[][] withscores_offset_or_count4) throws RedisException;

    /**
     * Determine the index of a member in a sorted set
     * Sorted_set
     *
     * @param key0
     * @param member1
     * @return Reply
     */
    public Reply zrank(byte[] key0, byte[] member1) throws RedisException;

    /**
     * Remove one or more members from a sorted set
     * Sorted_set
     *
     * @param key0
     * @param member1
     * @return IntegerReply
     */
    public IntegerReply zrem(byte[] key0, byte[][] member1) throws RedisException;

    /**
     * Remove all members in a sorted set within the given indexes
     * Sorted_set
     *
     * @param key0
     * @param start1
     * @param stop2
     * @return IntegerReply
     */
    public IntegerReply zremrangebyrank(byte[] key0, byte[] start1, byte[] stop2) throws RedisException;

    /**
     * Remove all members in a sorted set within the given scores
     * Sorted_set
     *
     * @param key0
     * @param min1
     * @param max2
     * @return IntegerReply
     */
    public IntegerReply zremrangebyscore(byte[] key0, byte[] min1, byte[] max2) throws RedisException;

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
    public MultiBulkReply zrevrange(byte[] key0, byte[] start1, byte[] stop2, byte[] withscores3) throws RedisException;

    /**
     * Return a range of members in a sorted set, by score, with scores ordered from high to low
     * Sorted_set
     *
     * @param key0
     * @param max1
     * @param min2
     * @param withscores3
     * @param offset_or_count4
     * @return MultiBulkReply
     */
    public MultiBulkReply zrevrangebyscore(byte[] key0, byte[] max1, byte[] min2, byte[][] withscores_offset_or_count4) throws RedisException;

    /**
     * Determine the index of a member in a sorted set, with scores ordered from high to low
     * Sorted_set
     *
     * @param key0
     * @param member1
     * @return Reply
     */
    public Reply zrevrank(byte[] key0, byte[] member1) throws RedisException;

    /**
     * Get the score associated with the given member in a sorted set
     * Sorted_set
     *
     * @param key0
     * @param member1
     * @return BulkReply
     */
    public BulkReply zscore(byte[] key0, byte[] member1) throws RedisException;

    /**
     * Add multiple sorted sets and store the resulting sorted set in a new key
     * Sorted_set
     *
     * @param destination0
     * @param numkeys1
     * @param key2
     * @return IntegerReply
     */
    public IntegerReply zunionstore(byte[] destination0, byte[] numkeys1, byte[][] key2) throws RedisException;

    public BytesKeyObjectMap<Object> getData();
}
