package de.dplatz.quarkus.livereload.watcher;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;

import io.quarkus.dev.spi.HotReplacementContext;
import io.quarkus.dev.spi.HotReplacementSetup;

public class FileModificationWatcher implements HotReplacementSetup {

    private static Logger log = Logger.getLogger(FileModificationWatcher.class);

    private HotReplacementContext hotReplacementContext;
    private Thread watchThread;
    private Map<WatchKey, Path> watchKeysForDirs = new HashMap<WatchKey, Path>();

    private WatchService watchService;

    @Override
    public void setupHotDeployment(HotReplacementContext context) {
        this.hotReplacementContext = context;
        
        // TODO: Use managed executor
        watchThread = new Thread(this::watch);
        watchThread.start();
    }
    
    @Override
    public void close() {
        if (watchThread != null) watchThread.interrupt();
    }
    
    private void registerWatcherForResourceDir(Path srcDir) throws IOException {
        log.info("Live-Reload is watching for changes @ "+ srcDir);

        Files.walkFileTree(srcDir, new SimpleFileVisitor<Path>() {
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
    }
    
    private void watch() {
        try {
            watchService = FileSystems.getDefault().newWatchService();

            List<Path> resourceDirs = this.hotReplacementContext.getResourcesDir();
            
            for (Path resourceDir : resourceDirs) {
                // Regular web resources
                Path webResourceDir = resourceDir.resolve("META-INF/resources");
                if (!webResourceDir.toFile().exists()) continue;
                
                this.registerWatcherForResourceDir(webResourceDir);

                // Qute templates
                Path quteTemplatesDir = resourceDir.resolve("templates");
                if (!quteTemplatesDir.toFile().exists()) continue;

                this.registerWatcherForResourceDir(quteTemplatesDir);
            }

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

    private void onFileChanged(Path p) {
       log.debug("Detected file-change that triggers a reload of the browser: "+ p);

       CDI.current().getBeanManager().fireEvent(new FileModificationEvent(p));
    }
}
