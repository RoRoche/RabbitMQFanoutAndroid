package com.guddy.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Fanout {
    //region Constants
    private static final String TYPE_FANOUT = "fanout";
    //endregion

    //region Fields
    public final String exchangeName;
    private final Connection mConnection;
    private final ThreadLocal<Channel> mChannels = new ThreadLocal<Channel>();
    //endregion

    //region Constructor
    public Fanout(final String psHost, final String psExchangeName) throws IOException, TimeoutException {
        this.exchangeName = psExchangeName;

        // create a connection factory
        final ConnectionFactory loConnectionFactory = new ConnectionFactory();

        loConnectionFactory.setHost(psHost);
        // get a new connection
        mConnection = loConnectionFactory.newConnection();
        // declare exchange
        getChannel().exchangeDeclare(psExchangeName, TYPE_FANOUT);
    }
    //endregion

    //region Visible API
    public final Channel getChannel() throws IOException {
        Channel loChannel = mChannels.get();
        if (loChannel == null) {
            loChannel = mConnection.createChannel();
            mChannels.set(loChannel);
        }
        return loChannel;
    }

    public void close() throws IOException {
        mConnection.close();
    }
    //endregion
}
