@Override
protected void doTask(Channel channel) {

    Client client = (Client) channel;

    // FIX: Do not reconnect if client is closed or unavailable
    if (client.isClosed() || !client.isAvailable()) {
        cancel();
        return;
    }

    try {
        Long lastRead = lastRead(channel);
        Long now = now();

        // Reconnect when connection is not established
        if (!channel.isConnected()) {
            try {
                logger.info("Initial connection to " + channel);
                client.reconnect();
            } catch (Exception e) {
                logger.error(
                        TRANSPORT_FAILED_RECONNECT,
                        "",
                        "",
                        "Fail to connect to " + channel,
                        e
                );
            }

        // Reconnect when heartbeat read idle timeout
        } else if (lastRead != null && now - lastRead > idleTimeout) {
            logger.warn(
                    TRANSPORT_FAILED_RECONNECT,
                    "",
                    "",
                    "Reconnect to channel " + channel
                            + ", because heartbeat read idle timeout: "
                            + idleTimeout + "ms"
            );
            try {
                client.reconnect();
            } catch (Exception e) {
                logger.error(
                        TRANSPORT_FAILED_RECONNECT,
                        "",
                        "",
                        channel + " reconnect failed during idle time.",
                        e
                );
            }
        }
    } catch (Throwable t) {
        logger.warn(
                INTERNAL_ERROR,
                "unknown error in remoting module",
                "",
                "Exception when reconnect to remote channel "
                        + channel.getRemoteAddress(),
                t
        );
    }
}
