package com.example.pulsarworkshop.common;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

public class CommonUtils {

    public static String getJsonStrForCsv(String csvTitleLine, String csvItemLine) {
        String[] fieldNames = StringUtils.split(csvTitleLine, ',');
        String[] itemValues = StringUtils.split(csvItemLine, ',');
        assert (fieldNames.length == itemValues.length);

        JSONObject jsonObject = new JSONObject();

        for (int i=0; i< fieldNames.length; i++) {
            jsonObject.put(fieldNames[i], itemValues[i]);
        }

        return jsonObject.toString();
    }

}
