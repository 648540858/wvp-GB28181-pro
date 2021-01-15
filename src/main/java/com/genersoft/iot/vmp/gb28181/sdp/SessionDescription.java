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

public class SessionDescription {

	private long id;
	private long version;
	private String name;
	private String username;
	private InetAddress ipAddress;
	private List<MediaDescription> mediaDescriptions;
    private Hashtable<String, String> attributes;
    private String ssrc;
    private String gbMediaDescriptions;

    public SessionDescription() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(InetAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

    public List<MediaDescription> getMediaDescriptions() {
        return mediaDescriptions;
    }

    public void setMediaDescriptions(List<MediaDescription> mediaDescriptions) {
        this.mediaDescriptions = mediaDescriptions;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public Hashtable<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Hashtable<String, String> attributes) {
        this.attributes = attributes;
    }

    public String getSsrc() {
        return ssrc;
    }

    public void setSsrc(String ssrc) {
        this.ssrc = ssrc;
    }

    public String getGbMediaDescriptions() {
        return gbMediaDescriptions;
    }

    public void setGbMediaDescriptions(String gbMediaDescriptions) {
        this.gbMediaDescriptions = gbMediaDescriptions;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("v=0\r\n");
        buf.append("o=").append(username).append(" ").append(id);
        buf.append(" ").append(version);
        int ipVersion;
        if (ipAddress instanceof Inet4Address) {
            ipVersion = 4;
        } else if (ipAddress instanceof Inet6Address) {
            ipVersion = 6;
        } else {
            throw new RuntimeException("unknown ip version: " + ipAddress);
        }
        buf.append(" IN IP").append(ipVersion).append(" ");
        String hostAddress = ipAddress.getHostAddress();
        buf.append(hostAddress).append("\r\n");
        buf.append("s=").append(name).append("\r\n");
        buf.append("c=IN IP").append(ipVersion).append(" ");
        buf.append(hostAddress).append("\r\n");
        buf.append("t=0 0\r\n");
        if (attributes != null){
            for (String attributeName: attributes.keySet()) {
                String attributeValue = attributes.get(attributeName);
                buf.append("a=").append(attributeName);
                if (attributeValue != null && !"".equals(attributeValue.trim())) {
                    buf.append(":");
                    buf.append(attributeValue);
                    buf.append("\r\n");
                }
            }
        }
        if (mediaDescriptions != null){
            for (MediaDescription mediaDescription: mediaDescriptions) {
                buf.append(mediaDescription.toString());
            }
        }

        if (ssrc != null){
            buf.append("y=").append(ssrc).append("\r\n");
        }
        if (gbMediaDescriptions != null){
            buf.append("f=").append(gbMediaDescriptions).append("\r\n");
        }
        return buf.toString();
    }

}
