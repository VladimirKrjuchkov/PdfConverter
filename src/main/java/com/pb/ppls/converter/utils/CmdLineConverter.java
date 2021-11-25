/* Decompiler 4ms, total 143ms, lines 45 */
package com.pb.ppls.converter.utils;

import java.io.DataOutputStream;
import java.io.File;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CmdLineConverter {
    public static File convert(File file) throws Exception {
        Logger logger = LoggerFactory.getLogger(CmdLineConverter.class);
        String filename = file.getName();
        String dir = file.getParent();
        Process pqShell = Runtime.getRuntime().exec("sh");
        String shellCommand = "unoconv -f pdf -eSelectPdfVersion=1 " + filename;
        String[] parsedname = filename.split("\\.");
        String basename = parsedname[0];

        try {
            DataOutputStream dos = new DataOutputStream(pqShell.getOutputStream());
            dos.writeBytes("cd " + dir + "\n");
            dos.writeBytes(shellCommand + "\n");
            dos.writeBytes("exit\n");
            dos.flush();
            dos.close();
            pqShell.waitFor();
            logger.debug("Shell-команда исполнена.");
        } catch (Exception var14) {
            logger.error(var14.getMessage());
        } finally {
            pqShell.destroy();
            file.delete();
            logger.debug("Временный файл {} удален.", filename);
        }

        try {
            File newfile = Paths.get(dir, basename + ".pdf").toFile();
            return newfile;
        } catch (Exception var13) {
            logger.debug("Не возможно конвертировать {} в pdf.", filename);
            return null;
        }
    }
}