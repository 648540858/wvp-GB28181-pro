/*
    This file is part of Peers, a java SIP softphone.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright 2007, 2008, 2009, 2010 Yohann Martineau 
 */

package com.genersoft.iot.vmp.gb28181.sdp;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Hashtable;
import java.util.List;

public class MediaDescription {

    private String type;
    private InetAddress ipAddress;
    // attributes not codec-related
    private Hashtable<String, String> attributes;
    private int port;
    private List<Codec> codecs;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Hashtable<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Hashtable<String, String> attributes) {
        this.attributes = attributes;
    }

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(InetAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public List<Codec> getCodecs() {
        return codecs;
    }

    public void setCodecs(List<Codec> codecs) {
        this.codecs = codecs;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(RFC4566_28181.TYPE_MEDIA).append(RFC4566_28181.SEPARATOR);
        buf.append(type).append(" ").append(port);
        buf.append(" RTP/AVP");
        if (codecs != null) {
            for (Codec codec: codecs) {
                buf.append(" ");
                buf.append(codec.getPayloadType());
            }
            buf.append("\r\n");
        }
        if (ipAddress != null) {
            int ipVersion;
            if (ipAddress instanceof Inet4Address) {
                ipVersion = 4;
            } else if (ipAddress instanceof Inet6Address) {
                ipVersion = 6;
            } else {
                throw new RuntimeException("unknown ip version: " + ipAddress);
            }
            buf.append(RFC4566_28181.TYPE_CONNECTION).append(RFC4566_28181.SEPARATOR);
            buf.append("IN IP").append(ipVersion).append(" ");
            buf.append(ipAddress.getHostAddress()).append("\r\n");
        }
        if (codecs != null) {
            for (Codec codec: codecs) {
                buf.append(codec.toString());
            }
        }

        if (attributes != null) {
            for (String attributeName: attributes.keySet()) {
                buf.append(RFC4566_28181.TYPE_ATTRIBUTE).append(RFC4566_28181.SEPARATOR);
                buf.append(attributeName);
                String attributeValue = attributes.get(attributeName);
                if (attributeValue != null && !"".equals(attributeValue.trim())) {
                    buf.append(":").append(attributeValue);
                }
                buf.append("\r\n");
            }
        }
        return buf.toString();
    }

}
