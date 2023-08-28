CREATE INDEX index_channel_deviceId
    ON device_channel (deviceId);
CREATE INDEX index_deviceId
    ON device (deviceId);
CREATE INDEX index_deviceChannelId
    ON platform_gb_channel (deviceChannelId);