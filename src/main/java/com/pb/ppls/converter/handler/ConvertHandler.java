/* Decompiler 43ms, total 187ms, lines 185 */
package com.pb.ppls.converter.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import com.pb.ppls.converter.utils.CmdLineConverter;
import com.pb.ppls.converter.utils.Helper;

public class ConvertHandler implements HttpHandler {
    private static final String typeerrorResponse = "Unsupported Media Type";
    private static final String toolargeResponse = "Too large request";
    private static final String incorrectDataResponse = "Incorrect data";
    private static final String servererrorResponse = "Internal server error";
    private static final int maxInputSize = 10485760;
    private static final int maxOutputSize = 2097152;
    Logger logger = LoggerFactory.getLogger(ConvertHandler.class);

    public void handle(HttpExchange t) throws IOException {
        String realIP = t.getRequestHeaders().getFirst("x-forwarded-for");
        Properties property = new Properties();
        boolean multipart = false;

        try {
            MDC.remove("message");
            MDC.remove("request");
            MDC.put("real_ip", realIP == null ? "undef_ip" : realIP);
        } catch (Exception var24) {
            this.logger.warn("error working with MDC:{}", var24);
        }
        this.logger.info("Соединение принято от {}.", realIP == null ? "неизвестного ip" : realIP);
        long startTime = System.currentTimeMillis();
        if("GET".equals(t.getRequestMethod())){
            this.logger.error("Проверка работоспособности.");
            this.throwResponse(t, 200, (String)"Health check.", startTime);
        }

        try {
            File root;
            try {
                root = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            } catch (URISyntaxException var22) {
                root = null;
            }

            File coreConfigPath;
            if (root.isDirectory()) {
                coreConfigPath = new File(root, "config.properties");
            } else {
                coreConfigPath = new File(root.getParent(), "config.properties");
            }

            InputStreamReader isr;
            if (Files.exists(coreConfigPath.toPath(), new LinkOption[0])) {
                isr = new InputStreamReader(new FileInputStream(coreConfigPath), "UTF-8");
                property.load(isr);
            } else {
                isr = new InputStreamReader(this.getClass().getResourceAsStream("/config.properties"), "UTF-8");
                property.load(isr);
            }

            try {
                isr.close();
            } catch (Exception var21) {
            }
        } catch (Exception var23) {
            this.logger.warn("Конфигурации не подгружены.");
        }

        if (t.getRequestHeaders().containsKey("Content-Type") && (t.getRequestHeaders().get("Content-Type").toString().contains("multipart/form-data") || t.getRequestHeaders().get("Content-Type").toString().contains("application/octet-stream"))) {
            multipart = true;
        }

        String dir = property.getProperty("workdir", "/home/output/");
        File uploadedFileLocation = File.createTempFile("temp_", Long.toString(System.currentTimeMillis()), new File(dir));
        OutputStream fos = null;
        int firstLineSize = 0;
        byte[] preBuffer = new byte[16384];
        this.logger.debug("Считывание начато.");
        InputStream is = t.getRequestBody();
        int l = is.read(preBuffer);
        this.logger.info("*** *** *** BEFORE *** *** ***");
        if (l > -1) {
            fos = new FileOutputStream(uploadedFileLocation);
            int startFrom = 0;

            byte[] buffer;
            int len;
            for(boolean win = true; l != -1; fos.write(buffer, startFrom, len - startFrom)) {
                len = l;
                buffer = (byte[])preBuffer.clone();
                if (multipart) {
                    if (firstLineSize != 0) {
                        startFrom = 0;
                    } else {
                        for(int i = 0; i < l - 2; ++i) {
                            if (firstLineSize == 0 && buffer[i] == 10) {
                                firstLineSize = i;
                            } else {
                                if (buffer[i] == 10 && buffer[i + 1] == 10) {
                                    startFrom = i + 2;
                                    win = false;
                                    break;
                                }

                                if (buffer[i] == 10 && buffer[i + 1] == 13 && buffer[i + 2] == 10) {
                                    startFrom = i + 3;
                                    break;
                                }
                            }
                        }
                    }
                }

                l = is.read(preBuffer);
                if (l == -1 && firstLineSize > 0) {
                    len -= firstLineSize;
                    if (win) {
                        len -= 5;
                    } else {
                        len -= 3;
                    }
                }
            }

            fos.flush();
            this.logger.debug("Считывание закончено.");

        }else {
            this.logger.error("Пустой запрос.");
            this.throwResponse(t, 400, (String)"Incorrect data", startTime);
        }

        if (uploadedFileLocation.length() > 10485760L) {
            this.logger.error("Слишком большой запрос.");
            this.throwResponse(t, 413, (String)"Too large request", startTime);
        } else {
            try {
                File renamedFile = Helper.renameFile(uploadedFileLocation);
                if (renamedFile == null) {
                    this.throwResponse(t, 415, (String)"Unsupported Media Type", startTime);
                } else {
                    File outputfile = CmdLineConverter.convert(renamedFile);
                    String filename = outputfile.getName();
                    byte[] result = Files.readAllBytes(outputfile.toPath());
                    outputfile.delete();
                    this.logger.debug("Файл {} удален.", filename);
                    if (result.length > 2097152) {
                        this.throwResponse(t, 413, (String)"Too large request", startTime);
                    } else {
                        this.throwResponse(t, 200, (byte[])result, startTime);
                    }
                }
            } catch (Exception var20) {
                this.logger.error(var20.getMessage());
                var20.printStackTrace();
                this.throwResponse(t, 500, (String)"Internal server error", startTime);
            }
        }
    }

    private void throwResponse(HttpExchange t, int code, String response, long startTime) throws IOException {
        this.throwResponse(t, code, response.getBytes(), startTime);
    }

    private void throwResponse(HttpExchange t, int code, byte[] response, long startTime) throws IOException {
        boolean isOK = code == 200;
        t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        t.getResponseHeaders().add("Access-Control-Allow-Methods", "POST");
        t.getResponseHeaders().add("Content-Type", isOK ? "application/pdf" : "text/html");
        t.sendResponseHeaders(code, (long)response.length);
        OutputStream os = t.getResponseBody();
        os.write(response);
        os.close();
        this.logger.info("Отправлен ответ с кодом {} за {} мс.\n", code, Long.toString(System.currentTimeMillis() - startTime));
    }
}