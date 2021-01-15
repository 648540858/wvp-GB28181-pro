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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class SdpParser {

	public SessionDescription parse(byte[] body) throws IOException {
		if (body == null || body.length == 0) {
			return null;
		}
		ByteArrayInputStream in = new ByteArrayInputStream(body);
		InputStreamReader inputStreamReader = new InputStreamReader(in);
		BufferedReader reader = new BufferedReader(inputStreamReader);
		SessionDescription sessionDescription = new SessionDescription();
		
		//version
		
		String line = reader.readLine();
		if (line.length() < 3) {
			return null;
		}
		if (line.charAt(0) != RFC4566_28181.TYPE_VERSION
				|| line.charAt(1) != RFC4566_28181.SEPARATOR
				|| line.charAt(2) != RFC4566_28181.VERSION) {
			return null;
		}

		//origin
		
		line = reader.readLine();
		if (line.length() < 3) {
			return null;
		}
		if (line.charAt(0) != RFC4566_28181.TYPE_ORIGIN
				|| line.charAt(1) != RFC4566_28181.SEPARATOR) {
			return null;
		}
		line = line.substring(2);
		String[] originArr = line.split(" ");
		if (originArr == null || originArr.length != 6) {
			return null;
		}
		sessionDescription.setUsername(originArr[0]);
		sessionDescription.setId(Long.parseLong(originArr[1]));
		sessionDescription.setVersion(Long.parseLong(originArr[2]));
		sessionDescription.setIpAddress(InetAddress.getByName(originArr[5]));

		//name
		
		line = reader.readLine();
		if (line.length() < 3) {
			return null;
		}
		if (line.charAt(0) != RFC4566_28181.TYPE_SUBJECT
				|| line.charAt(1) != RFC4566_28181.SEPARATOR) {
			return null;
		}
		sessionDescription.setName(line.substring(2));
		
		//session connection and attributes
        Hashtable<String, String> sessionAttributes = new Hashtable<String, String>();
        sessionDescription.setAttributes(sessionAttributes);
		
		while ((line = reader.readLine()) != null
				&& line.charAt(0) != RFC4566_28181.TYPE_MEDIA) {
			if (line.length() > 3
					&& line.charAt(0) == RFC4566_28181.TYPE_CONNECTION
					&& line.charAt(1) == RFC4566_28181.SEPARATOR) {
				String connection = parseConnection(line.substring(2));
				if (connection == null) {
					continue;
				}
				sessionDescription.setIpAddress(InetAddress.getByName(connection));
			} else if (line.length() > 3
                    && line.charAt(0) == RFC4566_28181.TYPE_ATTRIBUTE
                    && line.charAt(1) == RFC4566_28181.SEPARATOR) {
                String value = line.substring(2);
                int pos = value.indexOf(RFC4566_28181.ATTR_SEPARATOR);
                if (pos > -1) {
                    sessionAttributes.put(value.substring(0, pos),
                            value.substring(pos + 1));
                } else {
                    sessionAttributes.put(value, "");
                }
            }
		}
		if (line == null) {
			return null;
		}
		//we are at the first media line
        
        ArrayList<SdpLine> mediaLines = new ArrayList<SdpLine>();
        do {
            if (line.length() < 2) {
                return null;
            }
            if (line.charAt(1) != RFC4566_28181.SEPARATOR) {
                return null;
            }
            if (line.charAt(0) == RFC4566_28181.TYPE_SSRC) {
                sessionDescription.setSsrc(line.length() >=2 ?line.substring(2):"");
            }else if (line.charAt(0) == RFC4566_28181.TYPE_MEDIA_DES) {
                sessionDescription.setGbMediaDescriptions(line.length() >=2 ?line.substring(2):"");
            }else {
                SdpLine mediaLine = new SdpLine();
                mediaLine.setType(line.charAt(0));
                mediaLine.setValue(line.substring(2));
                mediaLines.add(mediaLine);
            }

        }
        while ((line = reader.readLine()) != null );
        
        ArrayList<MediaDescription> mediaDescriptions = new ArrayList<MediaDescription>();
        sessionDescription.setMediaDescriptions(mediaDescriptions);
        
        for (SdpLine sdpLine : mediaLines) {
            MediaDescription mediaDescription;
            if (sdpLine.getType() == RFC4566_28181.TYPE_MEDIA) {
                String[] mediaArr = sdpLine.getValue().split(" ");
                if (mediaArr == null || mediaArr.length < 4) {
                    return null;
                }
                mediaDescription = new MediaDescription();
                mediaDescription.setType(mediaArr[0]);
                //TODO manage port range
                mediaDescription.setPort(Integer.parseInt(mediaArr[1]));
                mediaDescription.setAttributes(new Hashtable<String, String>());
                List<Codec> codecs = new ArrayList<Codec>();
                for (int i = 3; i < mediaArr.length; ++i) {
                    int payloadType = Integer.parseInt(mediaArr[i]);
                    Codec codec = new Codec();
                    codec.setPayloadType(payloadType);
                    codec.setName("unsupported");
                    codecs.add(codec);
                }
                mediaDescription.setCodecs(codecs);
                mediaDescriptions.add(mediaDescription);
            } else {
                mediaDescription = mediaDescriptions.get(mediaDescriptions.size() - 1);
                String sdpLineValue = sdpLine.getValue();
                if (sdpLine.getType() == RFC4566_28181.TYPE_CONNECTION) {
                    String ipAddress = parseConnection(sdpLineValue);
                    mediaDescription.setIpAddress(InetAddress.getByName(ipAddress));
                } else if (sdpLine.getType() == RFC4566_28181.TYPE_ATTRIBUTE) {
                    Hashtable<String, String> attributes = mediaDescription.getAttributes();
                    int pos = sdpLineValue.indexOf(RFC4566_28181.ATTR_SEPARATOR);
                    if (pos > -1) {
                        String name = sdpLineValue.substring(0, pos);
                        String value = sdpLineValue.substring(pos + 1);
                        pos = value.indexOf(" ");
                        if (pos > -1) {
                            int payloadType;
                            try {
                                payloadType = Integer.parseInt(value.substring(0, pos));
                                List<Codec> codecs = mediaDescription.getCodecs();
                                for (Codec codec: codecs) {
                                    if (codec.getPayloadType() == payloadType) {
                                        value = value.substring(pos + 1);
                                        pos = value.indexOf("/");
                                        if (pos > -1) {
                                            value = value.substring(0, pos);
                                            codec.setName(value);
                                        }
                                        break;
                                    }
                                }
                            } catch (NumberFormatException e) {
                                attributes.put(name, value);
                            }
                        } else {
                            attributes.put(name, value);
                        }
                    } else {
                        attributes.put(sdpLineValue, "");
                    }
                }
            }
        }
        sessionDescription.setMediaDescriptions(mediaDescriptions);

        for (MediaDescription description : mediaDescriptions) {
            if (description.getIpAddress() == null) {
                InetAddress sessionAddress = sessionDescription.getIpAddress();
                if (sessionAddress == null) {
                    return null;
                }
                description.setIpAddress(sessionAddress);
            }
        }


		return sessionDescription;
	}
	
	private String parseConnection(String line) {
		String[] connectionArr = line.split(" ");
		if (connectionArr == null || connectionArr.length != 3) {
			return null;
		}
		return connectionArr[2];
	}
	
}
