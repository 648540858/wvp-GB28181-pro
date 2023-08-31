package com.genersoft.iot.vmp.gb28181.bean;

import gov.nist.core.CommonLogger;
import gov.nist.core.Host;
import gov.nist.core.HostNameParser;
import gov.nist.core.StackLogger;
import gov.nist.javax.sip.SIPConstants;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.address.GenericURI;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.address.TelephoneNumber;
import gov.nist.javax.sip.header.*;
import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import gov.nist.javax.sip.parser.*;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;

public class GBStringMsgParser implements MessageParser {

    protected static boolean computeContentLengthFromMessage = false;

    private static StackLogger logger = CommonLogger.getLogger(StringMsgParser.class);

    /**
     * @since v0.9
     */
    public GBStringMsgParser() {
        super();
    }

    /**
     * Parse a buffer containing a single SIP Message where the body is an array
     * of un-interpreted bytes. This is intended for parsing the message from a
     * memory buffer when the buffer. Incorporates a bug fix for a bug that was
     * noted by Will Sullin of Callcast
     *
     * @param msgBuffer
     *            a byte buffer containing the messages to be parsed. This can
     *            consist of multiple SIP Messages concatenated together.
     * @return a SIPMessage[] structure (request or response) containing the
     *         parsed SIP message.
     * @exception ParseException
     *                is thrown when an illegal message has been encountered
     *                (and the rest of the buffer is discarded).
     * @see ParseExceptionListener
     */
    public SIPMessage parseSIPMessage(byte[] msgBuffer, boolean readBody, boolean strict, ParseExceptionListener parseExceptionListener) throws ParseException {
        if (msgBuffer == null || msgBuffer.length == 0)
            return null;

        int i = 0;

        // Squeeze out any leading control character.
        try {
            while (msgBuffer[i] < 0x20)
                i++;
        }
        catch (ArrayIndexOutOfBoundsException e) {
            // Array contains only control char, return null.
            if (logger.isLoggingEnabled(StackLogger.TRACE_DEBUG)) {
                logger.logDebug("handled only control char so returning null");
            }
            return null;
        }

        // Iterate thru the request/status line and headers.
        String currentLine = null;
        String currentHeader = null;
        boolean isFirstLine = true;
        SIPMessage message = null;
        do
        {
            int lineStart = i;

            // Find the length of the line.
            try {
                while (msgBuffer[i] != '\r' && msgBuffer[i] != '\n')
                    i++;
            }
            catch (ArrayIndexOutOfBoundsException e) {
                // End of the message.
                break;
            }
            int lineLength = i - lineStart;

            // Make it a String.
            try {
                currentLine = new String(msgBuffer, lineStart, lineLength, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new ParseException("Bad message encoding!", 0);
            }

            currentLine = trimEndOfLine(currentLine);

            if (currentLine.length() == 0) {
                // Last header line, process the previous buffered header.
                if (currentHeader != null && message != null) {
                    processHeader(currentHeader, message, parseExceptionListener, msgBuffer);
                }

            }
            else {
                if (isFirstLine) {
                    message = processFirstLine(currentLine, parseExceptionListener, msgBuffer);
                } else {
                    char firstChar = currentLine.charAt(0);
                    if (firstChar == '\t' || firstChar == ' ') {
                        if (currentHeader == null)
                            throw new ParseException("Bad header continuation.", 0);

                        // This is a continuation, append it to the previous line.
                        currentHeader += currentLine.substring(1);
                    }
                    else {
                        if (currentHeader != null && message != null) {
                            processHeader(currentHeader, message, parseExceptionListener, msgBuffer);
                        }
                        currentHeader = currentLine;
                    }
                }
            }

            if (msgBuffer[i] == '\r' && msgBuffer.length > i+1 && msgBuffer[i+1] == '\n')
                i++;

            i++;

            isFirstLine = false;
        } while (currentLine.length() > 0); // End do - while

        if (message == null) throw new ParseException("Bad message", 0);
        message.setSize(i);

        // Check for content legth header
        if (readBody && message.getContentLength() != null ) {
            if ( message.getContentLength().getContentLength() != 0) {
                int bodyLength = msgBuffer.length - i;

                byte[] body = new byte[bodyLength];
                System.arraycopy(msgBuffer, i, body, 0, bodyLength);
                message.setMessageContent(body,!strict,computeContentLengthFromMessage,message.getContentLength().getContentLength());
            } else if (message.getCSeqHeader().getMethod().equalsIgnoreCase("MESSAGE")) {
                int bodyLength = msgBuffer.length - i;

                byte[] body = new byte[bodyLength];
                System.arraycopy(msgBuffer, i, body, 0, bodyLength);
                message.setMessageContent(body,!strict,computeContentLengthFromMessage,bodyLength);
            }else if (!computeContentLengthFromMessage && strict) {
                String last4Chars = new String(msgBuffer, msgBuffer.length - 4, 4);
                if(!"\r\n\r\n".equals(last4Chars)) {
                    throw new ParseException("Extraneous characters at the end of the message ",i);
                }
            }
        }

        return message;
    }

    protected static String trimEndOfLine(String line) {
        if (line == null)
            return line;

        int i = line.length() - 1;
        while (i >= 0 && line.charAt(i) <= 0x20)
            i--;

        if (i == line.length() - 1)
            return line;

        if (i == -1)
            return "";

        return line.substring(0, i+1);
    }

    protected SIPMessage processFirstLine(String firstLine, ParseExceptionListener parseExceptionListener, byte[] msgBuffer) throws ParseException {
        SIPMessage message;
        if (!firstLine.startsWith(SIPConstants.SIP_VERSION_STRING)) {
            message = new SIPRequest();
            try {
                RequestLine requestLine = new RequestLineParser(firstLine + "\n")
                        .parse();
                ((SIPRequest) message).setRequestLine(requestLine);
            } catch (ParseException ex) {
                if (parseExceptionListener != null)
                    try {
                        parseExceptionListener.handleException(ex, message,
                                RequestLine.class, firstLine, new String(msgBuffer, "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                else
                    throw ex;

            }
        } else {
            message = new SIPResponse();
            try {
                StatusLine sl = new StatusLineParser(firstLine + "\n").parse();
                ((SIPResponse) message).setStatusLine(sl);
            } catch (ParseException ex) {
                if (parseExceptionListener != null) {
                    try {
                        parseExceptionListener.handleException(ex, message,
                                StatusLine.class, firstLine, new String(msgBuffer, "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                } else
                    throw ex;

            }
        }
        return message;
    }

    protected void processHeader(String header, SIPMessage message, ParseExceptionListener parseExceptionListener, byte[] rawMessage) throws ParseException {
        if (header == null || header.length() == 0)
            return;

        HeaderParser headerParser = null;
        try {
            headerParser = ParserFactory.createParser(header + "\n");
        } catch (ParseException ex) {
            // https://java.net/jira/browse/JSIP-456
            if (parseExceptionListener != null) {
                parseExceptionListener.handleException(ex, message, null,
                        header, null);
                return;
            } else {
                throw ex;
            }
        }

        try {
            SIPHeader sipHeader = headerParser.parse();
            message.attachHeader(sipHeader, false);
        } catch (ParseException ex) {
            if (parseExceptionListener != null) {
                String headerName = Lexer.getHeaderName(header);
                Class headerClass = NameMap.getClassFromName(headerName);
                if (headerClass == null) {
                    headerClass = ExtensionHeaderImpl.class;

                }
                try {
                    parseExceptionListener.handleException(ex, message,
                            headerClass, header, new String(rawMessage, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    /**
     * Parse an address (nameaddr or address spec) and return and address
     * structure.
     *
     * @param address
     *            is a String containing the address to be parsed.
     * @return a parsed address structure.
     * @since v1.0
     * @exception ParseException
     *                when the address is badly formatted.
     */
    public AddressImpl parseAddress(String address) throws ParseException {
        AddressParser addressParser = new AddressParser(address);
        return addressParser.address(true);
    }

    /**
     * Parse a host:port and return a parsed structure.
     *
     * @param hostport
     *            is a String containing the host:port to be parsed
     * @return a parsed address structure.
     * @since v1.0
     * @exception throws
     *                a ParseException when the address is badly formatted.
     *
    public HostPort parseHostPort(String hostport) throws ParseException {
    Lexer lexer = new Lexer("charLexer", hostport);
    return new HostNameParser(lexer).hostPort();

    }
     */

    /**
     * Parse a host name and return a parsed structure.
     *
     * @param host
     *            is a String containing the host name to be parsed
     * @return a parsed address structure.
     * @since v1.0
     * @exception ParseException
     *                a ParseException when the hostname is badly formatted.
     */
    public Host parseHost(String host) throws ParseException {
        Lexer lexer = new Lexer("charLexer", host);
        return new HostNameParser(lexer).host();

    }

    /**
     * Parse a telephone number return a parsed structure.
     *
     * @param telephone_number
     *            is a String containing the telephone # to be parsed
     * @return a parsed address structure.
     * @since v1.0
     * @exception ParseException
     *                a ParseException when the address is badly formatted.
     */
    public TelephoneNumber parseTelephoneNumber(String telephone_number)
            throws ParseException {
        // Bug fix contributed by Will Scullin
        return new URLParser(telephone_number).parseTelephoneNumber(true);

    }

    /**
     * Parse a SIP url from a string and return a URI structure for it.
     *
     * @param url
     *            a String containing the URI structure to be parsed.
     * @return A parsed URI structure
     * @exception ParseException
     *                if there was an error parsing the message.
     */

    public SipUri parseSIPUrl(String url) throws ParseException {
        try {
            return new URLParser(url).sipURL(true);
        } catch (ClassCastException ex) {
            throw new ParseException(url + " Not a SIP URL ", 0);
        }
    }

    /**
     * Parse a uri from a string and return a URI structure for it.
     *
     * @param url
     *            a String containing the URI structure to be parsed.
     * @return A parsed URI structure
     * @exception ParseException
     *                if there was an error parsing the message.
     */

    public GenericURI parseUrl(String url) throws ParseException {
        return new URLParser(url).parse();
    }

    /**
     * Parse an individual SIP message header from a string.
     *
     * @param header
     *            String containing the SIP header.
     * @return a SIPHeader structure.
     * @exception ParseException
     *                if there was an error parsing the message.
     */
    public static SIPHeader parseSIPHeader(String header) throws ParseException {
        int start = 0;
        int end = header.length() - 1;
        try {
            // Squeeze out any leading control character.
            while (header.charAt(start) <= 0x20)
                start++;

            // Squeeze out any trailing control character.
            while (header.charAt(end) <= 0x20)
                end--;
        }
        catch (ArrayIndexOutOfBoundsException e) {
            // Array contains only control char.
            throw new ParseException("Empty header.", 0);
        }

        StringBuilder buffer = new StringBuilder(end + 1);
        int i = start;
        int lineStart = start;
        boolean endOfLine = false;
        while (i <= end) {
            char c = header.charAt(i);
            if (c == '\r' || c == '\n') {
                if (!endOfLine) {
                    buffer.append(header.substring(lineStart, i));
                    endOfLine = true;
                }
            }
            else {
                if (endOfLine) {
                    endOfLine = false;
                    if (c == ' ' || c == '\t') {
                        buffer.append(' ');
                        lineStart = i + 1;
                    }
                    else {
                        lineStart = i;
                    }
                }
            }

            i++;
        }
        buffer.append(header.substring(lineStart, i));
        buffer.append('\n');

        HeaderParser hp = ParserFactory.createParser(buffer.toString());
        if (hp == null)
            throw new ParseException("could not create parser", 0);
        return hp.parse();
    }

    /**
     * Parse the SIP Request Line
     *
     * @param requestLine
     *            a String containing the request line to be parsed.
     * @return a RequestLine structure that has the parsed RequestLine
     * @exception ParseException
     *                if there was an error parsing the requestLine.
     */

    public RequestLine parseSIPRequestLine(String requestLine)
            throws ParseException {
        requestLine += "\n";
        return new RequestLineParser(requestLine).parse();
    }

    /**
     * Parse the SIP Response message status line
     *
     * @param statusLine
     *            a String containing the Status line to be parsed.
     * @return StatusLine class corresponding to message
     * @exception ParseException
     *                if there was an error parsing
     * @see StatusLine
     */

    public StatusLine parseSIPStatusLine(String statusLine)
            throws ParseException {
        statusLine += "\n";
        return new StatusLineParser(statusLine).parse();
    }

    public static void setComputeContentLengthFromMessage(
            boolean computeContentLengthFromMessage) {
        GBStringMsgParser.computeContentLengthFromMessage = computeContentLengthFromMessage;
    }
}
