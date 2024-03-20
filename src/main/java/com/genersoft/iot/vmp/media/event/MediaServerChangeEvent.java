package com.genersoft.iot.vmp.media.event;

import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import org.springframework.context.ApplicationEvent;

import java.util.Arrays;
import java.util.List;

public class MediaServerChangeEvent extends ApplicationEvent {

    public MediaServerChangeEvent(Object source) {
        super(source);
    }

    private List<MediaServerItem> mediaServerItemList;

    public List<MediaServerItem> getMediaServerItemList() {
        return mediaServerItemList;
    }

    public void setMediaServerItemList(List<MediaServerItem> mediaServerItemList) {
        this.mediaServerItemList = mediaServerItemList;
    }

    public void setMediaServerItemList(MediaServerItem... mediaServerItemArray) {
        this.mediaServerItemList.addAll(Arrays.asList(mediaServerItemArray));
    }

    public void setMediaServerItem(List<MediaServerItem> mediaServerItemList) {
        this.mediaServerItemList = mediaServerItemList;
    }
}
