package org.dominokit.domino.rest.android;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.dominokit.domino.api.shared.extension.ContextAggregator;
import org.dominokit.domino.rest.shared.RestfulRequest;
import org.dominokit.domino.rest.shared.request.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

public class AndroidRequestSender<R, S> implements RequestRestSender<R, S> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AndroidRequestSender.class);

    private final List<String> SEND_BODY_METHODS = Arrays.asList("POST", "PUT", "PATCH");

    @Override
    public void send(ServerRequest<R, S> request, ServerRequestCallBack callBack) {
        request.normalizeUrl();
        List<RequestInterceptor> interceptors = DominoRestContext.make().getConfig().getRequestInterceptors();

        if (nonNull(interceptors) && !interceptors.isEmpty()) {
            List<InterceptorRequestWait> interceptorsWaitList = interceptors.stream().map(InterceptorRequestWait::new)
                    .collect(Collectors.toList());
            ContextAggregator.waitFor(interceptorsWaitList)
                    .onReady(() -> onAfterInterception(request, callBack));
            interceptorsWaitList.forEach(interceptorWait -> interceptorWait.getInterceptor().interceptRequest(request, interceptorWait));
        } else {
            onAfterInterception(request, callBack);
        }
    }

    private void onAfterInterception(ServerRequest<R, S> request, ServerRequestCallBack callBack) {
        Observable
                .<S>create(emitter -> {
                    int[] retriesCounter = new int[]{0};
                    RestfulRequest restfulRequest = RestfulRequest.request(request.getUrl(), request.getHttpMethod().toUpperCase());
                    restfulRequest
                            .putHeaders(request.headers())
                            .putParameters(request.parameters())
                            .onSuccess(response -> {
                                if (Arrays.stream(request.getSuccessCodes()).anyMatch(code -> code.equals(response.getStatusCode()))) {
                                    emitter.onNext(request.getResponseReader().read(response.getBodyAsString()));
                                } else {
                                    emitter.onError(new FailedResponse(response.getStatusCode(), response.getStatusText(), response.getBodyAsString(), response.getHeaders()));
                                }
                            })
                            .onError(throwable -> {
                                if (throwable instanceof RequestTimeoutException && retriesCounter[0] < request.getMaxRetries()) {
                                    retriesCounter[0]++;
                                    LOGGER.info("Retrying request : " + retriesCounter[0]);
                                    doSendRequest(request, restfulRequest);
                                } else {
                                    emitter.onError(throwable);
                                }
                            });
                    setTimeout(request, restfulRequest);
                    doSendRequest(request, restfulRequest);
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new RequestObserver<>(callBack));
    }

    private void handleError(ServerRequestCallBack callBack, Throwable throwable) {
        FailedResponseBean failedResponseBean;
        if (throwable instanceof FailedResponse) {
            FailedResponse failedResponse = (FailedResponse) throwable;
            failedResponseBean = new FailedResponseBean(failedResponse.getStatusCode(),
                    failedResponse.getResponseText(),
                    failedResponse.getBodyAsString(),
                    failedResponse.getHeaders());
        } else {
            failedResponseBean = new FailedResponseBean(throwable);
        }

        LOGGER.info("Failed to execute request : " + failedResponseBean);
        callBack.onFailure(failedResponseBean);
    }

    private void setTimeout(ServerRequest<R, S> request, RestfulRequest restfulRequest) {
        if (request.getTimeout() > 0) {
            restfulRequest.timeout(request.getTimeout());
        }
    }

    private void doSendRequest(ServerRequest<R, S> request, RestfulRequest restfulRequest) {
        if (SEND_BODY_METHODS.contains(request.getHttpMethod().toUpperCase()) && !request.isVoidRequest()) {
            restfulRequest.send(request.getRequestWriter().write(request.requestBean()));
        } else {
            restfulRequest.send();
        }
    }

    private class RequestObserver<S> implements Observer<S> {

        private final ServerRequestCallBack callBack;
        private Disposable disposable;

        private RequestObserver(ServerRequestCallBack callBack) {
            this.callBack = callBack;
        }

        @Override
        public void onSubscribe(Disposable disposable) {
            this.disposable = disposable;
        }

        @Override
        public void onNext(S s) {
            callBack.onSuccess(s);
        }

        @Override
        public void onError(Throwable e) {
            handleError(callBack, e);
        }

        @Override
        public void onComplete() {
            disposable.dispose();
        }
    }
}
