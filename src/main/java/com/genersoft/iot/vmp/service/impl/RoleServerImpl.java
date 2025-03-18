package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.service.IRoleService;
import com.genersoft.iot.vmp.storager.dao.RoleMapper;
import com.genersoft.iot.vmp.storager.dao.dto.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoleServerImpl implements IRoleService {

    @Autowired
    private RoleMapper roleMapper;

    @Override
    public Role getRoleById(int id) {
        return roleMapper.selectById(id);
    }

    @Override
    public int add(Role role) {
        return roleMapper.add(role);
    }

    @Override
    public int delete(int id) {
        return roleMapper.delete(id);
    }

    @Override
    public List<Role> getAll() {
        return roleMapper.selectAll();
    }

    @Override
    public int update(Role role) {
        return roleMapper.update(role);
    }
}
