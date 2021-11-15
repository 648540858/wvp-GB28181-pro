package com.genersoft.iot.vmp.vmanager.videoSquare;

import com.genersoft.iot.vmp.common.reponse.ResponseData;
import com.genersoft.iot.vmp.service.IVideoSquareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/square")
public class VideoSquareController {

    @Autowired
    private IVideoSquareService videoSquareService;

    @GetMapping(value = "/video/tree")
    @ResponseBody
    public ResponseData queryVideoTree() {
        return ResponseData.success(videoSquareService.selectVideoTree());
    }
}
