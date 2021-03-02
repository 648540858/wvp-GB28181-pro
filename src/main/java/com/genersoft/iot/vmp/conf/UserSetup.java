package com.genersoft.iot.vmp.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration("userSetup")
public class UserSetup {
    @Value("${userSettings.savePositionHistory}")
    boolean savePositionHistory;

    public boolean getSavePositionHistory() {
        return savePositionHistory;
    }

    public void setSavePositionHistory(boolean savePositionHistory) {
        this.savePositionHistory = savePositionHistory;
    }
}
