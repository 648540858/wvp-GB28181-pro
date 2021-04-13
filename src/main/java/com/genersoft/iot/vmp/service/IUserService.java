package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.storager.dao.dto.User;

public interface IUserService {

    User getUser(String username, String password);

    boolean changePassword(int id, String password);


}
