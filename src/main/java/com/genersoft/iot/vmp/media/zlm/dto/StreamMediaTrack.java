package com.genersoft.iot.vmp.media.zlm.dto;

public class StreamMediaTrack {

    /**
     * 音频通道数
     */
    private Integer channels;

    /**
     *  H264 = 0, H265 = 1, AAC = 2, G711A = 3, G711U = 4
     */
    private Integer codec_id;

    /**
     * 编码类型名称 CodecAAC CodecH264
     */
    private String codec_id_name;

    /**
     * Video = 0, Audio = 1
     */
    private Integer codec_type;

    /**
     * 轨道是否准备就绪
     */
    private Boolean ready;

    /**
     * 音频采样位数
     */
    private Integer sample_bit;

    /**
     * 音频采样率
     */
    private Integer sample_rate;

    /**
     * 视频fps
     */
    private Float fps;

    /**
     * 视频高
     */
    private Integer height;

    /**
     * 视频宽
     */
    private Integer width;

    /**
     * 帧数
     */
    private Integer frames;

    /**
     * 关键帧数
     */
    private Integer key_frames;

    /**
     * GOP大小
     */
    private Integer gop_size;

    /**
     * GOP间隔时长(ms)
     */
    private Integer gop_interval_ms;

    /**
     * 丢帧率
     */
    private Float loss;


    public Integer getChannels() {
        return channels;
    }

    public void setChannels(Integer channels) {
        this.channels = channels;
    }

    public Integer getCodec_id() {
        return codec_id;
    }

    public void setCodec_id(Integer codec_id) {
        this.codec_id = codec_id;
    }

    public String getCodec_id_name() {
        return codec_id_name;
    }

    public void setCodec_id_name(String codec_id_name) {
        this.codec_id_name = codec_id_name;
    }

    public Integer getCodec_type() {
        return codec_type;
    }

    public void setCodec_type(Integer codec_type) {
        this.codec_type = codec_type;
    }

    public Boolean getReady() {
        return ready;
    }

    public void setReady(Boolean ready) {
        this.ready = ready;
    }

    public Integer getSample_bit() {
        return sample_bit;
    }

    public void setSample_bit(Integer sample_bit) {
        this.sample_bit = sample_bit;
    }

    public Integer getSample_rate() {
        return sample_rate;
    }

    public void setSample_rate(Integer sample_rate) {
        this.sample_rate = sample_rate;
    }

    public Float getFps() {
        return fps;
    }

    public void setFps(Float fps) {
        this.fps = fps;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getFrames() {
        return frames;
    }

    public void setFrames(Integer frames) {
        this.frames = frames;
    }

    public Integer getKey_frames() {
        return key_frames;
    }

    public void setKey_frames(Integer key_frames) {
        this.key_frames = key_frames;
    }

    public Integer getGop_size() {
        return gop_size;
    }

    public void setGop_size(Integer gop_size) {
        this.gop_size = gop_size;
    }

    public Integer getGop_interval_ms() {
        return gop_interval_ms;
    }

    public void setGop_interval_ms(Integer gop_interval_ms) {
        this.gop_interval_ms = gop_interval_ms;
    }

    public Float getLoss() {
        return loss;
    }

    public void setLoss(Float loss) {
        this.loss = loss;
    }
}
