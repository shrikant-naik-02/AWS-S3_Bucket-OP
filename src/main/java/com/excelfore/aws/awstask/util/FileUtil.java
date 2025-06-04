package com.excelfore.aws.awstask.util;

import com.excelfore.aws.awstask.exception.HashAlgorithmNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class FileUtil {

    private FileUtil() {
        // Prevent instantiation
        throw new AssertionError("Utility class - do not instantiate");
    }

    private static final String HASH_ALGORITHM = "SHA-256";

    public static String getHashAlgorithmName() {
        return HASH_ALGORITHM;
    }

    public static long mbToBytes(int mb) {
        return mb * 1024L * 1024L;
    }

    public static boolean isFileNameValid(String originalFileName) {
        int dotIndex = originalFileName.lastIndexOf('.');

        return dotIndex > 0 && originalFileName.substring(0, dotIndex).matches(".*[a-zA-Z].*");
    }

    public static String computeSHA256Hash(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }

            byte[] hashBytes = digest.digest();

            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();

        } catch (NoSuchAlgorithmException e) {
            log.error("Hash algorithm not found: {}", e.getMessage());
            throw new HashAlgorithmNotFoundException("SHA-256 algorithm not available");

        } catch (IOException e) {
            log.error("IOException while reading file: {}", e.getMessage());
            throw new RuntimeException("Failed to read uploaded file", e);
        }
    }


    public static Map<String, String> extractFolderShaKeyAndObjName(String presignedUrl) {
        URI uri = URI.create(presignedUrl);

        String path = uri.getPath(); // e.g. /myBucket/646d4fdf4sdf45dv

        // Remove leading slash
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        // Split into folderName and shaKey
        String[] parts = path.split("/", 2); // Split into folderName and shaKey
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid path format: " + path);
        }

        // Create the map to store folderName, shaKey, and objName
        Map<String, String> result = new HashMap<>();
        result.put("folderName", parts[0]);   // first part is folder
        result.put("shaKey", parts[1]);       // second part is shaKey

        // Construct objName by combining folderName and shaKey
        result.put("objName", parts[0] + "/" + parts[1]);

        return result;
    }

    public static String generateNewFileNameWithUUID(String originalFileName) {
        String uuid = UUID.randomUUID().toString();

        // Handle files with extensions (e.g., myphoto.jpg)
        int dotIndex = originalFileName.lastIndexOf(".");
        if (dotIndex != -1) {
            String name = originalFileName.substring(0, dotIndex);
            String extension = originalFileName.substring(dotIndex); // includes the dot
            return name + "_" + uuid + extension;
        }

        // For files with no extension
        return originalFileName + "_" + uuid;
    }



}
