package org.apache.dubbo.errorcode.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utilities of iterating file.
 */
public final class FileUtils {
    private FileUtils() {
        throw new UnsupportedOperationException("No instance of FileUtils for you! ");
    }

    public static List<Path> getAllClassFilePaths(String rootPath) {
        List<Path> targetFolders;

        try (Stream<Path> filesStream = Files.walk(Paths.get(rootPath))) {
            targetFolders = filesStream.filter(x -> !x.toFile().isFile())
                    .filter(x -> x.toString().contains("classes") && !x.toString().contains("test-classes"))
                    .filter(x -> x.toString().contains("\\org\\apache\\dubbo"))
                    .collect(Collectors.toList());

            return targetFolders;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
