package com.example.pulsarworkshop.common.conf;

import com.example.pulsarworkshop.common.PulsarConnCfgConf;
import com.example.pulsarworkshop.common.exception.WorkshopRuntimException;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class S4jCfgConf {
    private final static Logger logger = LoggerFactory.getLogger(PulsarConnCfgConf.class);

    private final Map<String, Object> s4jConfMap = new HashMap<>();
    public S4jCfgConf(File s4jCfgFile) throws WorkshopRuntimException {
    }

    public String toString() {
        return new ToStringBuilder(this).
                append("s4jConfMap", s4jConfMap.toString()).
                toString();
    }

    public Map<String, Object> getS4jConfMap() { return this.s4jConfMap; }
}
