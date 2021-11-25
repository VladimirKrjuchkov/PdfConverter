/* Decompiler 4ms, total 145ms, lines 68 */
package com.pb.ppls.converter.utils;

import java.io.File;
import java.nio.file.Paths;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Helper {
    public static File renameFile(File baseFile) throws Exception {
        Logger logger = LoggerFactory.getLogger(Helper.class);
        String basename = baseFile.getName();
        String dir = baseFile.getParent();
        String type = (new Tika()).detect(baseFile);
        byte var6 = -1;
        switch(type.hashCode()) {
            case -1719571662:
                if (type.equals("application/vnd.oasis.opendocument.text")) {
                    var6 = 1;
                }
                break;
            case -1248332507:
                if (type.equals("application/rtf")) {
                    var6 = 4;
                }
                break;
            case -1248325150:
                if (type.equals("application/zip")) {
                    var6 = 2;
                }
                break;
            case 342869833:
                if (type.equals("application/x-tika-ooxml")) {
                    var6 = 3;
                }
                break;
            case 803157392:
                if (type.equals("application/x-tika-msoffice")) {
                    var6 = 0;
                }
        }

        switch(var6) {
            case 0:
                basename = basename + ".doc";
                break;
            case 1:
                basename = basename + ".odt";
                break;
            case 2:
            case 3:
                basename = basename + ".docx";
                break;
            case 4:
                basename = basename + ".rtf";
                break;
            default:
                logger.error("Тип файла не определен.");
                return null;
        }

        File newFile = Paths.get(dir, basename).toFile();
        baseFile.renameTo(newFile);
        logger.debug("Файл {} был создан.", newFile.getName());
        return newFile;
    }
}