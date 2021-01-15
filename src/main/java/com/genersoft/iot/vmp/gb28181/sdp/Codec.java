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
    
    Copyright 2010 Yohann Martineau 
*/

package com.genersoft.iot.vmp.gb28181.sdp;

public class Codec {

    private int payloadType;
    private String name;

    public int getPayloadType() {
        return payloadType;
    }

    public void setPayloadType(int payloadType) {
        this.payloadType = payloadType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Codec)) {
            return false;
        }
        Codec codec = (Codec)obj;
        if (codec.getName() == null) {
            return name == null;
        }
        return codec.getName().equalsIgnoreCase(name);
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(RFC4566_28181.TYPE_ATTRIBUTE).append(RFC4566_28181.SEPARATOR);
        buf.append(RFC4566_28181.ATTR_RTPMAP).append(RFC4566_28181.ATTR_SEPARATOR);
        buf.append(payloadType).append(" ").append(name).append("/");
        buf.append(9000).append("\r\n");
        return buf.toString();
    }

}
