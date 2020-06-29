package org.dominokit.domino.rest.android;

import org.dominokit.domino.api.shared.extension.ContextAggregator;
import org.dominokit.domino.rest.shared.Response;
import org.dominokit.domino.rest.shared.RestfulRequest;
import org.dominokit.domino.rest.shared.request.DominoRestContext;
import org.dominokit.domino.rest.shared.request.FailedResponseBean;
import org.dominokit.domino.rest.shared.request.InterceptorRequestWait;
import org.dominokit.domino.rest.shared.request.RequestInterceptor;
import org.dominokit.domino.rest.shared.request.RequestRestSender;
import org.dominokit.domino.rest.shared.request.RequestTimeoutException;
import org.dominokit.domino.rest.shared.request.ServerRequest;
import org.dominokit.domino.rest.shared.request.ServerRequestCallBack;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static java.util.Objects.nonNull;

public class AndroidRequestSender<R, S> implements RequestRestSender<R, S> {

    private static final Logger LOGGER = Logger.getLogger(AndroidRequestSender.class.getCanonicalName());

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
                            .putParameters(request.queryParameters())
                            .onSuccess(response -> {
                                if (Arrays.stream(request.getSuccessCodes()).anyMatch(code -> code.equals(response.getStatusCode()))) {
                                    callSuccessGlobalHandlers(request, response);
                                    emitter.onNext(request.getResponseReader().read(response));
                                } else {
                                    FailedResponseBean failedResponse = new FailedResponseBean(request, response);
                                    callFailedResponseHandlers(request, failedResponse);
                                    emitter.onError(new FailedResponseException(request, response));
                                }
                            })
                            .onError(throwable -> {
                                if (throwable instanceof RequestTimeoutException && retriesCounter[0] < request.getMaxRetries()) {
                                    retriesCounter[0]++;
                                    LOGGER.info("Retrying request : " + retriesCounter[0]);
                                    doSendRequest(request, restfulRequest);
                                } else {
                                    FailedResponseBean failedResponse = new FailedResponseBean(throwable);
                                    LOGGER.log(Level.SEVERE, "Failed to execute request : ", failedResponse.getThrowable());
                                    callFailedResponseHandlers(request, failedResponse);
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

    private void callSuccessGlobalHandlers(ServerRequest<R, S> request, Response response) {
        DominoRestContext.make().getConfig()
                .getResponseInterceptors()
                .forEach(responseInterceptor -> responseInterceptor.interceptOnSuccess(request, response));
    }

    private void callFailedResponseHandlers(ServerRequest request, FailedResponseBean failedResponse) {
        DominoRestContext.make().getConfig()
                .getResponseInterceptors()
                .forEach(responseInterceptor -> responseInterceptor.interceptOnFailed(request, failedResponse));
    }

    private void handleError(ServerRequestCallBack callBack, Throwable throwable) {
        FailedResponseBean failedResponseBean;
        if (throwable instanceof FailedResponseException) {
            FailedResponseException failedResponseException = (FailedResponseException) throwable;
            failedResponseBean = new FailedResponseBean(failedResponseException.getRequest(), failedResponseException.getResponse());
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

    private static class FailedResponseException extends Throwable {
        private final ServerRequest request;
        private final Response response;

        public FailedResponseException(ServerRequest request, Response response) {
            this.request = request;
            this.response = response;
        }

        public ServerRequest getRequest() {
            return request;
        }

        public Response getResponse() {
            return response;
        }
    }
}
