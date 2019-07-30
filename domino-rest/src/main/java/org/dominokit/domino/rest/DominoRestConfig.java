package org.dominokit.domino.rest;

import org.dominokit.domino.rest.android.AndroidRequestSender;
import org.dominokit.domino.rest.android.DefaultServiceRoot;
import org.dominokit.domino.rest.android.OnServerRequestEventFactory;
import org.dominokit.domino.rest.shared.request.AsyncRunner;
import org.dominokit.domino.rest.shared.request.DefaultRequestAsyncSender;
import org.dominokit.domino.rest.shared.request.DominoRestContext;
import org.dominokit.domino.rest.shared.request.DynamicServiceRoot;
import org.dominokit.domino.rest.shared.request.Fail;
import org.dominokit.domino.rest.shared.request.GlobalResponseHandler;
import org.dominokit.domino.rest.shared.request.RequestInterceptor;
import org.dominokit.domino.rest.shared.request.RequestRouter;
import org.dominokit.domino.rest.shared.request.RestConfig;
import org.dominokit.domino.rest.shared.request.ServerRequest;
import org.dominokit.domino.rest.shared.request.ServerRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class DominoRestConfig implements RestConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(DominoRestConfig.class);
    private static String defaultServiceRoot;
    private static String defaultResourceRootPath = "service";
    private static String defaultJsonDateFormat = null;

    private static RequestRouter<ServerRequest> serverRouter = new ServerRouter(new DefaultRequestAsyncSender(
            new OnServerRequestEventFactory(), new AndroidRequestSender<>()));
    private static List<DynamicServiceRoot> dynamicServiceRoots = new ArrayList<>();
    private static List<RequestInterceptor> interceptors = new ArrayList<>();
    private static final List<GlobalResponseHandler> globalResponseHandlers = new ArrayList<>();
    private static Fail defaultFailHandler = failedResponse -> {
        if (nonNull(failedResponse.getThrowable())) {
            LOGGER.debug("could not execute request on server: ", failedResponse.getThrowable());
        } else {
            LOGGER.debug("could not execute request on server: status [" + failedResponse.getStatusCode() + "], body [" + failedResponse.getBody() + "]");
        }
    };

    public static DominoRestConfig initDefaults() {
        RestfullRequestContext.setFactory(new AndroidRestfulRequestFactory());
        DominoRestContext.init(DominoRestConfig.getInstance());
        return DominoRestConfig.getInstance();
    }

    public static DominoRestConfig getInstance() {
        return new DominoRestConfig();
    }

    public DominoRestConfig setDefaultServiceRoot(String defaultServiceRoot) {
        this.defaultServiceRoot = defaultServiceRoot;
        return this;
    }

    public DominoRestConfig setDefaultJsonDateFormat(String defaultJsonDateFormat) {
        this.defaultJsonDateFormat = defaultJsonDateFormat;
        return this;
    }

    public DominoRestConfig addDynamicServiceRoot(DynamicServiceRoot dynamicServiceRoot) {
        dynamicServiceRoots.add(dynamicServiceRoot);
        return this;
    }

    public DominoRestConfig addRequestInterceptor(RequestInterceptor interceptor) {
        this.interceptors.add(interceptor);
        return this;
    }

    public DominoRestConfig removeRequestInterceptor(RequestInterceptor interceptor) {
        this.interceptors.remove(interceptor);
        return this;
    }

    public List<RequestInterceptor> getRequestInterceptors() {
        return interceptors;
    }

    public DominoRestConfig addGlobalResponseHandler(GlobalResponseHandler globalResponseHandler) {
        this.getGlobalResponseHandlers().add(globalResponseHandler);
        return this;
    }

    public DominoRestConfig removeGlobalResponseHandler(GlobalResponseHandler globalResponseHandler) {
        this.getGlobalResponseHandlers().remove(globalResponseHandler);
        return this;
    }

    @Override
    public List<GlobalResponseHandler> getGlobalResponseHandlers() {
        return globalResponseHandlers;
    }

    public String getDefaultServiceRoot() {
        if (isNull(defaultServiceRoot)) {
            return DefaultServiceRoot.get() + defaultResourceRootPath + "/";
        }
        return defaultServiceRoot;
    }

    public String getDefaultJsonDateFormat() {
        return defaultJsonDateFormat;
    }

    public List<DynamicServiceRoot> getServiceRoots() {
        return dynamicServiceRoots;
    }

    public DominoRestConfig setDefaultResourceRootPath(String rootPath) {
        if (nonNull(rootPath)) {
            this.defaultResourceRootPath = rootPath;
        }
        return this;
    }

    public RequestRouter<ServerRequest> getServerRouter() {
        return serverRouter;
    }

    public String getDefaultResourceRootPath() {
        if (nonNull(defaultResourceRootPath) && !defaultResourceRootPath.trim().isEmpty()) {
            return defaultResourceRootPath + "/";
        } else {
            return "";
        }
    }

    public DominoRestConfig setDefaultFailHandler(Fail fail) {
        if (nonNull(fail)) {
            this.defaultFailHandler = fail;
        }
        return this;
    }

    @Override
    public Fail getDefaultFailHandler() {
        return defaultFailHandler;
    }

    @Override
    public AsyncRunner asyncRunner() {
        return asyncTask -> {
            try {
                asyncTask.onSuccess();
            } catch (Throwable error) {
                asyncTask.onFailed(error);
            }
        };
    }

    public void setServerRouter(RequestRouter<ServerRequest> serverRouter) {
        DominoRestConfig.serverRouter = serverRouter;
    }
}