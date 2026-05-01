package com.example.server.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.server.entity.SystemConfig;
import com.example.server.mapper.SystemConfigMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SystemConfigService {
    private final SystemConfigMapper systemConfigMapper;

    public SystemConfigService(SystemConfigMapper systemConfigMapper) {
        this.systemConfigMapper = systemConfigMapper;
    }

    public List<SystemConfig> list() {
        QueryWrapper<SystemConfig> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("category", "config_key");
        return systemConfigMapper.selectList(wrapper);
    }

    public String getValue(String key, String defaultValue) {
        SystemConfig config = systemConfigMapper.selectById(key);
        return config != null && config.getConfigValue() != null ? config.getConfigValue() : defaultValue;
    }

    public int getIntValue(String key, int defaultValue) {
        try {
            return Integer.parseInt(getValue(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public SystemConfig update(String key, String value) {
        SystemConfig config = systemConfigMapper.selectById(key);
        if (config == null) {
            return null;
        }
        config.setConfigValue(value);
        systemConfigMapper.updateById(config);
        return config;
    }
}
