package com.example.pulsarworkshop.common;

import com.example.pulsarworkshop.common.exception.WorkshopRuntimException;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PulsarConnCfgConf {

    private final static Logger logger = LoggerFactory.getLogger(PulsarConnCfgConf.class);

    private final Map<String, String> clientConfMap = new HashMap<>();
    public PulsarConnCfgConf(File clientConnFile) throws WorkshopRuntimException {
        String canonicalFilePath = "";

        try {
            canonicalFilePath = clientConnFile.getCanonicalPath();

            Parameters params = new Parameters();

            FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
                    new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
                            .configure(params.properties()
                                    .setFileName(canonicalFilePath));

            Configuration config = builder.getConfiguration();

            for (Iterator<String> it = config.getKeys(); it.hasNext(); ) {
                String confKey = it.next();
                String confVal = config.getProperty(confKey).toString();

                if (!StringUtils.isBlank(confVal)) {
                    clientConfMap.put(confKey, confVal);
                }
            }
        } catch (IOException ioe) {
            throw new WorkshopRuntimException("Can't read the specified properties file!");
        } catch (ConfigurationException cex) {
            throw new WorkshopRuntimException(
                    "Error loading configuration items from the specified properties file: " + canonicalFilePath);
        }
    }

    public String toString() {
        return new ToStringBuilder(this).
                append("clientConfMapRaw", clientConfMap.toString()).
                toString();
    }

    public Map<String, String> getClientConfMap() { return this.clientConfMap; }
}
