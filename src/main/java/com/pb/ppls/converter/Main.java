/* Decompiler 6ms, total 150ms, lines 66 */
package com.pb.ppls.converter;

import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.Properties;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.pb.ppls.converter.handler.ConvertHandler;

public class Main {
    static final Properties property = new Properties();
    static HttpServer convertServer;

    public static void main(String[] args) throws IOException {
        Logger logger = LoggerFactory.getLogger(Main.class);
        logger.info("SERVER WAS STARTED..\n");

        try {
            File root = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File coreConfigPath;
            if (root.isDirectory()) {
                coreConfigPath = new File(root, "config.properties");
            } else {
                coreConfigPath = new File(root.getParent(), "config.properties");
            }

            try {
                InputStreamReader isr;
                if (Files.exists(coreConfigPath.toPath(), new LinkOption[0])) {
                    isr = new InputStreamReader(new FileInputStream(coreConfigPath), "UTF-8");
                    property.load(isr);
                } else {
                    isr = new InputStreamReader(Main.class.getResourceAsStream("/config.properties"), "UTF-8");
                    property.load(isr);
                }

                try {
                    isr.close();
                } catch (Exception var8) {
                }
            } catch (Exception var9) {
                logger.warn("Configs weren't uploaded.");
            }

            String pathConvert = property.getProperty("server.convert.path", "/services/pdfconvert");
            int port = Integer.valueOf(property.getProperty("server.convert.port", "3000"));
            String domain = property.getProperty("server.localDomain", "0.0.0.0");
            InetSocketAddress inetSocketAddress = new InetSocketAddress(domain, port);
            convertServer = HttpServer.create(inetSocketAddress, 0);
            convertServer.createContext(pathConvert, new ConvertHandler());
            convertServer.setExecutor((Executor)null);
            convertServer.start();
        } catch (Exception var10) {
            logger.error(var10.getMessage());
        }

    }
}