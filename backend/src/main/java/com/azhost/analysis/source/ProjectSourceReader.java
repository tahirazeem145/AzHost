package com.azhost.analysis.source;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface ProjectSourceReader {

    boolean exists();

    boolean fileExists(String relativePath);

    Optional<String> readFileContent(String relativePath);

    List<String> listRootFiles();

    Path getRootPath();
}
