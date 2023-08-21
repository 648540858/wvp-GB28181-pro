package com.genersoft.iot.vmp.media.zlm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ServerKeepaliveData {
    @JsonProperty("Buffer")
    public int buffer;
    @JsonProperty("BufferLikeString")
    public int bufferLikeString;
    @JsonProperty("BufferList")
    public int bufferList;
    @JsonProperty("BufferRaw")
    public int bufferRaw;
    @JsonProperty("Frame")
    public int frame;
    @JsonProperty("FrameImp")
    public int frameImp;
    @JsonProperty("MediaSource")
    public int mediaSource;
    @JsonProperty("MultiMediaSourceMuxer")
    public int multiMediaSourceMuxer;
    @JsonProperty("RtmpPacket")
    public int rtmpPacket;
    @JsonProperty("RtpPacket")
    public int rtpPacket;
    @JsonProperty("Socket")
    public int socket;
    @JsonProperty("TcpClient")
    public int tcpClient;
    @JsonProperty("TcpServer")
    public int tcpServer;
    @JsonProperty("TcpSession")
    public int tcpSession;
    @JsonProperty("UdpServer")
    public int udpServer;
    @JsonProperty("UdpSession")
    public int udpSession;

    public int getBuffer() {
        return buffer;
    }

    public void setBuffer(int buffer) {
        this.buffer = buffer;
    }

    public int getBufferLikeString() {
        return bufferLikeString;
    }

    public void setBufferLikeString(int bufferLikeString) {
        this.bufferLikeString = bufferLikeString;
    }

    public int getBufferList() {
        return bufferList;
    }

    public void setBufferList(int bufferList) {
        this.bufferList = bufferList;
    }

    public int getBufferRaw() {
        return bufferRaw;
    }

    public void setBufferRaw(int bufferRaw) {
        this.bufferRaw = bufferRaw;
    }

    public int getFrame() {
        return frame;
    }

    public void setFrame(int frame) {
        this.frame = frame;
    }

    public int getFrameImp() {
        return frameImp;
    }

    public void setFrameImp(int frameImp) {
        this.frameImp = frameImp;
    }

    public int getMediaSource() {
        return mediaSource;
    }

    public void setMediaSource(int mediaSource) {
        this.mediaSource = mediaSource;
    }

    public int getMultiMediaSourceMuxer() {
        return multiMediaSourceMuxer;
    }

    public void setMultiMediaSourceMuxer(int multiMediaSourceMuxer) {
        this.multiMediaSourceMuxer = multiMediaSourceMuxer;
    }

    public int getRtmpPacket() {
        return rtmpPacket;
    }

    public void setRtmpPacket(int rtmpPacket) {
        this.rtmpPacket = rtmpPacket;
    }

    public int getRtpPacket() {
        return rtpPacket;
    }

    public void setRtpPacket(int rtpPacket) {
        this.rtpPacket = rtpPacket;
    }

    public int getSocket() {
        return socket;
    }

    public void setSocket(int socket) {
        this.socket = socket;
    }

    public int getTcpClient() {
        return tcpClient;
    }

    public void setTcpClient(int tcpClient) {
        this.tcpClient = tcpClient;
    }

    public int getTcpServer() {
        return tcpServer;
    }

    public void setTcpServer(int tcpServer) {
        this.tcpServer = tcpServer;
    }

    public int getTcpSession() {
        return tcpSession;
    }

    public void setTcpSession(int tcpSession) {
        this.tcpSession = tcpSession;
    }

    public int getUdpServer() {
        return udpServer;
    }

    public void setUdpServer(int udpServer) {
        this.udpServer = udpServer;
    }

    public int getUdpSession() {
        return udpSession;
    }

    public void setUdpSession(int udpSession) {
        this.udpSession = udpSession;
    }
}
