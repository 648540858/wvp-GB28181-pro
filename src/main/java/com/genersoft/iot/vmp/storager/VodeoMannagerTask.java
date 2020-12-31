package com.genersoft.iot.vmp.storager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class VodeoMannagerTask implements CommandLineRunner {

    @Autowired
    private IVideoManagerStorager redisStorager;

    @Override
    public void run(String... strings) throws Exception {
        redisStorager.updateCatch();
    }
}
