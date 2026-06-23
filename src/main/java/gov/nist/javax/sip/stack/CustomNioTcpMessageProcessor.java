package gov.nist.javax.sip.stack;

import gov.nist.core.HostPort;

import java.io.IOException;
import java.net.InetAddress;

public class CustomNioTcpMessageProcessor extends NioTcpMessageProcessor {

    public CustomNioTcpMessageProcessor(InetAddress ipAddress,
                                         SIPTransactionStack sipStack, int port) {
        super(ipAddress, sipStack, port);
    }

    @Override
    public MessageChannel createMessageChannel(HostPort targetHostPort) throws IOException {
        MessageChannel retval = null;
        try {
            String key = MessageChannel.getKey(targetHostPort, transport);
            retval = messageChannels.get(key);
            //here we use double-checked locking trying to reduce contention
        } finally {
        }
        return retval;
    }

    @Override
    public MessageChannel createMessageChannel(InetAddress targetHost, int port) throws IOException {
        String key = MessageChannel.getKey(targetHost, port, transport);
        MessageChannel retval = messageChannels.get(key);
        //here we use double-checked locking trying to reduce contention
        return retval;
    }
}
