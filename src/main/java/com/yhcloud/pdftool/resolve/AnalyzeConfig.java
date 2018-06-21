package com.yhcloud.pdftool.resolve;

import com.google.gson.Gson;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

/**
 * 配置文件读取对象
 *
 * @author leig
 * @version 20180301
 *
 */

public class AnalyzeConfig {

    private Logger log = Logger.getLogger(AnalyzeConfig.class);

    public ConfigBean execute(File file) {

        FileInputStream is;

        StringBuilder stringBuilder;

        ConfigBean configBean = new ConfigBean();

        try {
            // 文件有内容才去读文件
            if (0 != file.length()) {

                is = new FileInputStream(file);

                InputStreamReader streamReader = new InputStreamReader(is);

                BufferedReader reader = new BufferedReader(streamReader);

                String line;

                stringBuilder = new StringBuilder();

                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }

                reader.close();
                is.close();

                Gson gson = new Gson();

                configBean = gson.fromJson(stringBuilder.toString(), ConfigBean.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("捕获异常: " + e.getMessage());
        }

        return configBean;
    }
}
