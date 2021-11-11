package com.genersoft.iot.vmp.vmanager.record;

import com.genersoft.iot.vmp.common.reponse.ResponseData;
import com.genersoft.iot.vmp.service.IRecordInfoServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/record")
public class RecordController {

    @Autowired
    private IRecordInfoServer recordInfoServer;

    @PostMapping(value = "/resetRecords")
    @ResponseBody
    public ResponseData resetRecords(@RequestBody Map<String, Object> params) {
        return recordInfoServer.resetRecords(params);
    }

}
