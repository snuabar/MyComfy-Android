package com.snuabar.mycomfy.common;

import java.io.File;
import java.net.URLConnection;

import okhttp3.MediaType;

public final class FileType {
    public static final int UNKNOWN = 0;
    public static final int JPEG = 1;
    public static final int PDF = 2;
    public static final int FOLDER = 3;
    public static final int TIFF = 4;
    public static final int ZIP = 5;
    public static final int HEIF = 6;

    public static final String MIME_JPEG = "image/jpeg";
    public static final String MIME_JPG = "image/jpg";
    public static final String MIME_TIFF= "image/tiff";
    public static final String MIME_TIF= "image/tif";
    public static final String MIME_PDF= "application/pdf";
    public static final String MIME_ZIP= "application/zip";
    public static final String MIME_HEIF = "image/heif";

    public static final String EXT_JPEG = ".jpeg";
    public static final String EXT_JPG = ".jpg";
    public static final String EXT_PDF = ".pdf";
    public static final String EXT_TIFF = ".tiff";
    public static final String EXT_TIF = ".tif";
    public static final String EXT_ZIP = ".zip";
    public static final String EXT_HEIF = ".heic";

    public static int mimeTypeToFileType(String mimeType) {
        int fileType = UNKNOWN;
        if (MIME_JPEG.equals(mimeType) || MIME_JPG.equals(mimeType)) {
            fileType = JPEG;
        } else if (MIME_TIFF.equals(mimeType) || MIME_TIF.equals(mimeType)) {
            fileType = TIFF;
        } else if (MIME_PDF.equals(mimeType)) {
            fileType = PDF;
        } else if (MIME_ZIP.equals(mimeType)) {
            fileType = ZIP;
        } else if (MIME_HEIF.equals(mimeType)) {
            fileType = HEIF;
        }
        return fileType;
    }

    public static String fileTypeToMimeType(int fileType) {
        if (fileType == JPEG) {
            return MIME_JPEG;
        }
        if (fileType == PDF) {
            return MIME_PDF;
        }
        if (fileType == TIFF) {
            return MIME_TIFF;
        }
        return null;
    }

    public static String fileNameToMimeType(String fileName) {
        final String ext = fileName.substring(fileName.lastIndexOf('.'));
        if (EXT_JPEG.equalsIgnoreCase(ext)) {
            return MIME_JPEG;
        }
        if (EXT_JPG.equalsIgnoreCase(ext)) {
            return MIME_JPG;
        }
        if (EXT_TIF.equalsIgnoreCase(ext)) {
            return MIME_TIF;
        }
        if (EXT_TIFF.equalsIgnoreCase(ext)) {
            return MIME_TIFF;
        }
        if (EXT_PDF.equalsIgnoreCase(ext)) {
            return MIME_PDF;
        }
        if (EXT_ZIP.equalsIgnoreCase(ext)) {
            return MIME_ZIP;
        }
        if (EXT_HEIF.equalsIgnoreCase(ext)) {
            return MIME_HEIF;
        }
        return "*/*";
    }

    public static int fileNameToFileType(String fileName) {
        final String mimeType = fileNameToMimeType(fileName);
        return mimeTypeToFileType(mimeType);
    }

    public static MediaType gguessMediaType(File file) {
        String mimeType = URLConnection.guessContentTypeFromName(file.getName());
        if (mimeType == null) {
            mimeType = "application/octet-stream"; // 默认类型
        }
        return MediaType.parse(mimeType);
    }
}
