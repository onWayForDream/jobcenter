package cn.enn.portal.jobCenter.core;

import org.junit.Test;

import java.io.*;
import java.util.regex.Pattern;

public class TestGetLog {

    @Test
    public void testGetLog() throws IOException {

        String uuid = "2e61d5bb-9ac8-43f6-900f-e7fccc45bec2";
        File file = new File("C:\\Users\\zhude\\Downloads\\2018-07-11_15.log");
        BufferedReader reader = new BufferedReader(new FileReader(file));

        StringBuffer stringBuffer = new StringBuffer();

        String line = "";
        int notMatchLines = 0;

        Pattern exceptionNameRegex = Pattern.compile("^(java)|(com)|(org)|(cn)\\.");
        Pattern stackTraceRegex = Pattern.compile("^(Caused\\sby:)|(\\s+at\\s)");
        Pattern runIdRegex = Pattern.compile(uuid.replace("-", "\\-"));

        while ((line = reader.readLine()) != null) {

            if (runIdRegex.matcher(line).find()) {
                notMatchLines = 0;
                stringBuffer.append(line);
                stringBuffer.append('\n');
                System.out.println(line);
                continue;
            } else {
                notMatchLines++;
            }

            if (notMatchLines <= 1) {
                if (exceptionNameRegex.matcher(line).find()
                        || stackTraceRegex.matcher(line).find()) {
                    stringBuffer.append(line);
                    stringBuffer.append('\n');
                    System.out.println(line);
                    notMatchLines = 0;
                }
            }

        }

    }
}
