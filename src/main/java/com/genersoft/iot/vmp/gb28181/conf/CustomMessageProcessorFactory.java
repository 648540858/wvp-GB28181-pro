package com.genersoft.iot.vmp.gb28181.conf;

import gov.nist.javax.sip.stack.CustomNioTcpMessageProcessor;
import gov.nist.javax.sip.stack.MessageProcessor;
import gov.nist.javax.sip.stack.NioMessageProcessorFactory;
import gov.nist.javax.sip.stack.SIPTransactionStack;

import javax.sip.ListeningPoint;
import java.io.IOException;
import java.net.InetAddress;

public class CustomMessageProcessorFactory extends NioMessageProcessorFactory {

    @Override
    public MessageProcessor createMessageProcessor(
            SIPTransactionStack sipStack, InetAddress ipAddress,
            int port, String transport) throws IOException {
        if (transport.equalsIgnoreCase(ListeningPoint.TCP)) {
            return new CustomNioTcpMessageProcessor(ipAddress, sipStack, port);
        }
        return super.createMessageProcessor(sipStack, ipAddress, port, transport);
    }

}
