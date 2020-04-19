package de.dplatz.quarkus.livereload;

import javax.servlet.DispatcherType;

import de.dplatz.quarkus.livereload.scriptinjection.LiveReloadScriptInjectionFilter;
import de.dplatz.quarkus.livereload.watcher.FileModificationWatcher;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.undertow.deployment.FilterBuildItem;
import io.quarkus.undertow.websockets.deployment.AnnotatedWebsocketEndpointBuildItem;

class LiveReloadProcessor {

    static final String FEATURE_NAME = "livereload-extension";

    @BuildStep(onlyIf = IsDevelopment.class)
    void createAirhacksServlet(BuildProducer<FilterBuildItem> filter,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<AnnotatedWebsocketEndpointBuildItem> annotatedProducer,
            BuildProducer<AdditionalBeanBuildItem> beansProducer) {

        filter.produce(FilterBuildItem.builder(FEATURE_NAME, LiveReloadScriptInjectionFilter.class.getName()).setLoadOnStartup(1)
                .addFilterServletNameMapping("default", DispatcherType.REQUEST).setAsyncSupported(false)
                .build());
        //reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, WebResourceFilter.class.getName()));
        //reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, LiveReloadWebsocketEndpoint.class.getName()));
        
        beansProducer.produce(AdditionalBeanBuildItem.builder().setUnremovable()
                .addBeanClasses(FileModificationWatcher.class,LiveReloadEventsPushEndpoint.class).build());
        
        annotatedProducer
                .produce(new AnnotatedWebsocketEndpointBuildItem(LiveReloadEventsPushEndpoint.class.getName(), false));

    }

    @BuildStep(onlyIf = IsDevelopment.class)
    FeatureBuildItem createFeatureItem() {
        return new FeatureBuildItem(FEATURE_NAME);
    }
}
