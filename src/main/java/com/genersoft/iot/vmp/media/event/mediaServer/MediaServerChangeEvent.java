package com.genersoft.iot.vmp.media.event.mediaServer;

import com.genersoft.iot.vmp.media.bean.MediaServer;
import org.springframework.context.ApplicationEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MediaServerChangeEvent extends ApplicationEvent {

    public MediaServerChangeEvent(Object source) {
        super(source);
    }

    private List<MediaServer> mediaServerItemList;

    public List<MediaServer> getMediaServerItemList() {
        return mediaServerItemList;
    }

    public void setMediaServerItemList(List<MediaServer> mediaServerItemList) {
        this.mediaServerItemList = mediaServerItemList;
    }

    public void setMediaServerItemList(MediaServer... mediaServerItemArray) {
        this.mediaServerItemList = new ArrayList<>();
        this.mediaServerItemList.addAll(Arrays.asList(mediaServerItemArray));
    }

    public void setMediaServerItem(List<MediaServer> mediaServerItemList) {
        this.mediaServerItemList = mediaServerItemList;
    }
}
