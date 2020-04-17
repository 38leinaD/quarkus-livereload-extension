package de.dplatz.quarkus.livereload.watcher;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.configuration.ProfileManager;

@ApplicationScoped
public class FileModificationWatcher {

    private static Logger log = Logger.getLogger(FileModificationWatcher.class);
    
    @Inject
    Event<FileModificationEvent> fileChangedEvents;
    
    private Thread watchThread;
    private Path watchDir;
    private Map<WatchKey, Path> watchKeysForDirs = new HashMap<WatchKey, Path>();

    public void init(@Observes StartupEvent startup) {
        if (!ProfileManager.getActiveProfile().equals("dev")) return;

        // TODO: What is the best/reliable way to get to src/main/resources?
        watchDir = Paths.get(System.getProperty("user.dir")).getParent().resolve("src/main/resources/META-INF/resources");
        log.info("Live-Reload is watching for changes @ "+ watchDir);
        
        // TODO: Use managed executor
        watchThread = new Thread(this::watch);
        watchThread.start();
    }
    
    public void shutdown(@Observes ShutdownEvent shutdown) {
        watchThread.interrupt();
    }
    
    void watch() {
        try {
            WatchService watchService = FileSystems.getDefault().newWatchService();

            Files.walkFileTree(this.watchDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                        throws IOException {

                    WatchKey watchKey = dir.register(watchService,
                            StandardWatchEventKinds.ENTRY_CREATE,
                            // StandardWatchEventKinds.ENTRY_DELETE,
                            StandardWatchEventKinds.ENTRY_MODIFY);
                    // StandardWatchEventKinds.OVERFLOW);

                    watchKeysForDirs.put(watchKey, dir);

                    return FileVisitResult.CONTINUE;
                }
            });

            // WatchKey watchKey = watchService.take();

            WatchKey key;

            while (!Thread.interrupted() && (key = watchService.take()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    Path dir = watchKeysForDirs.get(key);
                    
                    Path changedPath = dir.resolve(event.context().toString());
                    if (changedPath.toFile().isDirectory()) continue;

                    // TODO: Dispatch to managed executor
                    this.onFileChanged(changedPath);
                }
                key.reset();
            }

        } catch (InterruptedException e) {
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    void onFileChanged(Path p) {
       log.debug("Detected file-change that triggers a reload of the browser: "+ p);

       fileChangedEvents.fire(new FileModificationEvent(p));
    }
}
