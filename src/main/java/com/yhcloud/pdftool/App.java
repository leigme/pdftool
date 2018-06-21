package com.yhcloud.pdftool;

import com.yhcloud.pdftool.logic.PdfUtil;
import com.yhcloud.pdftool.resolve.AnalyzeConfig;
import com.yhcloud.pdftool.resolve.ConfigBean;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 工程入口
 *
 */
public class App {

    private static Logger log = Logger.getLogger(App.class);

    public static void main( String[] args ) {

        File configFile = new File("config.json");

        if (!configFile.exists()) {
            log.error("配置文件不存在...");
            return;
        }

        ConfigBean configBean = new AnalyzeConfig().execute(configFile);

        Map<String, String> map = new HashMap<>();

        int i = 0;

        for (String content: configBean.getContent()) {
            map.put(String.valueOf(i), content);
            i += 1;
        }

        PdfUtil pdfUtil = new PdfUtil();

        Map<String, String> resullt = pdfUtil.execute(map);

        for (Map.Entry<String, String> set: resullt.entrySet()) {
            log.info(set.getValue());
        }

    }
}
