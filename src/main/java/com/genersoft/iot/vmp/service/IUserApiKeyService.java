package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.storager.dao.dto.UserApiKey;
import com.github.pagehelper.PageInfo;

public interface IUserApiKeyService {
    int addApiKey(UserApiKey userApiKey);

    boolean isApiKeyExists(String apiKey);

    PageInfo<UserApiKey> getUserApiKeys(int page, int count);

    int enable(Integer id);

    int disable(Integer id);

    int remark(Integer id, String remark);

    int delete(Integer id);

    UserApiKey getUserApiKeyById(Integer id);

    int reset(Integer id, String apiKey);

}
