package de.dplatz.quarkus.livereload.watcher;

import java.nio.file.Path;

public class FileModificationEvent {

    private Path file;

    public FileModificationEvent(Path file) {
        super();
        this.file = file;
    }

    public Path getFile() {
        return file;
    }
}
