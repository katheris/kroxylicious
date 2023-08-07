/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.kroxylicious.proxy.internal;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.kafka.common.message.ApiVersionsRequestData;
import org.apache.kafka.common.message.ApiVersionsResponseData;
import org.apache.kafka.common.message.FetchRequestData;
import org.apache.kafka.common.message.FetchResponseData;
import org.apache.kafka.common.message.ProduceRequestData;
import org.apache.kafka.common.message.RequestHeaderData;
import org.apache.kafka.common.message.ResponseHeaderData;
import org.apache.kafka.common.protocol.ApiMessage;
import org.apache.kafka.common.protocol.types.RawTaggedField;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.kroxylicious.proxy.filter.ApiVersionsRequestFilter;
import io.kroxylicious.proxy.filter.ApiVersionsResponseFilter;
import io.kroxylicious.proxy.filter.KrpcFilterContext;
import io.kroxylicious.proxy.filter.RequestFilterResult;
import io.kroxylicious.proxy.filter.ResponseFilterResult;
import io.kroxylicious.proxy.frame.DecodedRequestFrame;
import io.kroxylicious.proxy.frame.DecodedResponseFrame;
import io.kroxylicious.proxy.future.InternalCompletionStage;
import io.kroxylicious.proxy.internal.filter.RequestFilterResultBuilderImpl;
import io.kroxylicious.proxy.internal.filter.ResponseFilterResultBuilderImpl;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class FilterHandlerTest extends FilterHarness {

    public static final int ARBITRARY_TAG = 500;

    @Test
    public void testForwardRequest() {
        ApiVersionsRequestFilter filter = (apiVersion, header, request, context) -> context.requestFilterResultBuilder().forward(header, request)
                .completed();
        buildChannel(filter);
        var frame = writeRequest(new ApiVersionsRequestData());
        var propagated = channel.readOutbound();
        assertEquals(frame, propagated, "Expect it to be the frame that was sent");
    }

    @Test
    void testDeferredRequestTimeout() {
        var filterFuture = new CompletableFuture<RequestFilterResult>();
        ApiVersionsRequestFilter filter = (apiVersion, header, request, context) -> filterFuture;
        long timeoutMs = 50L;
        buildChannel(filter, timeoutMs);
        channel.freezeTime();
        writeRequest(new ApiVersionsRequestData());
        channel.advanceTimeBy(timeoutMs - 1, TimeUnit.MILLISECONDS);
        channel.runPendingTasks();
        assertThat(filterFuture).isNotDone();
        channel.advanceTimeBy(1, TimeUnit.MILLISECONDS);
        channel.runPendingTasks();

        assertThat(filterFuture).isCompletedExceptionally().isNotCancelled();
        assertThatThrownBy(filterFuture::get).hasCauseInstanceOf(TimeoutException.class);
        assertThat(channel.isOpen()).isFalse();
    }

    @Test
    void testDeferredResponseTimeout() {
        var filterFuture = new CompletableFuture<ResponseFilterResult>();
        ApiVersionsResponseFilter filter = (apiVersion, header, request, context) -> filterFuture;
        long timeoutMs = 50L;
        buildChannel(filter, timeoutMs);
        channel.freezeTime();
        writeResponse(new ApiVersionsResponseData());
        channel.advanceTimeBy(timeoutMs - 1, TimeUnit.MILLISECONDS);
        channel.runPendingTasks();
        assertThat(filterFuture).isNotDone();
        channel.advanceTimeBy(1, TimeUnit.MILLISECONDS);
        channel.runPendingTasks();

        assertThat(filterFuture).isCompletedExceptionally().isNotCancelled();
        assertThatThrownBy(filterFuture::get).hasCauseInstanceOf(TimeoutException.class);
        assertThat(channel.isOpen()).isFalse();
    }

    static Stream<Arguments> requestFilterClosesChannel() {
        return Stream.of(
                Arguments.of("completes exceptionally",
                        (BiFunction<RequestHeaderData, ApiMessage, CompletionStage<RequestFilterResult>>) (header, request) -> CompletableFuture
                                .failedStage(new RuntimeException("filter error")),
                        false),
                Arguments.of("filter result signals close",
                        (BiFunction<RequestHeaderData, ApiMessage, CompletionStage<RequestFilterResult>>) (header, request) -> new RequestFilterResultBuilderImpl()
                                .withCloseConnection().completed(),
                        false),
                Arguments.of("filter result signals close with forward",
                        (BiFunction<RequestHeaderData, ApiMessage, CompletionStage<RequestFilterResult>>) (header, request) -> new RequestFilterResultBuilderImpl()
                                .forward(header, request).withCloseConnection().completed(),
                        true));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void requestFilterClosesChannel(String name,
                                    BiFunction<RequestHeaderData, ApiMessage, CompletableFuture<RequestFilterResult>> stageFunction,
                                    boolean forwardExpected) {
        ApiVersionsRequestFilter filter = (apiVersion, header, request, context) -> stageFunction.apply(header, request);
        buildChannel(filter);
        var frame = writeRequest(new ApiVersionsRequestData());
        channel.runPendingTasks();

        assertThat(channel.isOpen()).isFalse();
        var propagated = channel.readOutbound();
        if (forwardExpected) {
            assertThat(propagated).isEqualTo(frame);
        }
        else {
            assertThat(propagated).isNull();
        }
    }

    static Stream<Arguments> responseFilterClosesChannel() {
        return Stream.of(
                Arguments.of("completes exceptionally",
                        (BiFunction<ResponseHeaderData, ApiMessage, CompletionStage<ResponseFilterResult>>) (header, response) -> CompletableFuture
                                .failedStage(new RuntimeException("filter error")),
                        false),
                Arguments.of("filter result signals close",
                        (BiFunction<ResponseHeaderData, ApiMessage, CompletionStage<ResponseFilterResult>>) (header, response) -> new ResponseFilterResultBuilderImpl()
                                .withCloseConnection().completed(),
                        false),
                Arguments.of("filter result signals close with forward",
                        (BiFunction<ResponseHeaderData, ApiMessage, CompletionStage<ResponseFilterResult>>) (header, response) -> new ResponseFilterResultBuilderImpl()
                                .forward(header, response).withCloseConnection().completed(),
                        true));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void responseFilterClosesChannel(String name,
                                     BiFunction<ResponseHeaderData, ApiMessage, CompletableFuture<ResponseFilterResult>> stageFunction,
                                     boolean forwardExpected) {
        ApiVersionsResponseFilter filter = (apiVersion, header, response, context) -> stageFunction.apply(header, response);
        buildChannel(filter);
        var frame = writeResponse(new ApiVersionsResponseData());
        channel.runPendingTasks();

        assertThat(channel.isOpen()).isFalse();
        var propagated = channel.readInbound();
        if (forwardExpected) {
            assertThat(propagated).isEqualTo(frame);
        }
        else {
            assertThat(propagated).isNull();
        }
    }

    @Test
    void closedChannelIgnoresDeferredPendingRequests() {
        var seen = new ArrayList<ApiMessage>();
        var filterFuture = new CompletableFuture<RequestFilterResult>();
        ApiVersionsRequestFilter filter = (apiVersion, header, request, context) -> {
            seen.add(request);
            return filterFuture;
        };
        buildChannel(filter);
        var frame1 = writeRequest(new ApiVersionsRequestData());
        writeRequest(new ApiVersionsRequestData().setClientSoftwareName("should not be processed"));
        // the filter handler will have queued up the second request, awaiting the completion of the first.
        filterFuture.complete(new RequestFilterResultBuilderImpl().withCloseConnection().build());
        channel.runPendingTasks();

        assertThat(channel.isOpen()).isFalse();
        var propagated = channel.readOutbound();
        assertThat(propagated).isNull();
        assertThat(seen).containsExactly(frame1.body());
    }

    @Test
    void closedChannelIgnoresDeferredPendingResponse() {
        var seen = new ArrayList<ApiMessage>();
        var filterFuture = new CompletableFuture<ResponseFilterResult>();
        ApiVersionsResponseFilter filter = (apiVersion, header, response, context) -> {
            seen.add(response);
            return filterFuture;
        };
        buildChannel(filter);
        var frame1 = writeResponse(new ApiVersionsResponseData().setErrorCode((short) 1));
        writeResponse(new ApiVersionsResponseData().setErrorCode((short) 2));
        // the filter handler will have queued up the second response, awaiting the completion of the first.
        filterFuture.complete(new ResponseFilterResultBuilderImpl().withCloseConnection().build());
        channel.runPendingTasks();

        assertThat(channel.isOpen()).isFalse();
        var propagated = channel.readInbound();
        assertThat(propagated).isNull();
        assertThat(seen).containsExactly(frame1.body());
    }

    @Test
    public void testShouldNotDeserialiseRequest() {
        ApiVersionsRequestFilter filter = new ApiVersionsRequestFilter() {
            @Override
            public boolean shouldHandleApiVersionsRequest(short apiVersion) {
                return false;
            }

            @Override
            public CompletionStage<RequestFilterResult> onApiVersionsRequest(short apiVersion, RequestHeaderData header, ApiVersionsRequestData request,
                                                                             KrpcFilterContext context) {
                fail("Should not be called");
                return null;
            }
        };
        buildChannel(filter);
        var frame = writeRequest(new ApiVersionsRequestData());
        var propagated = channel.readOutbound();
        assertEquals(frame, propagated, "Expect it to be the frame that was sent");
    }

    @Test
    public void testDropRequest() {
        ApiVersionsRequestFilter filter = (apiVersion, header, request, context) -> {
            /* don't call forwardRequest => drop the request */
            return context.requestFilterResultBuilder().drop().completed();
        };
        buildChannel(filter);
        var frame = writeRequest(new ApiVersionsRequestData());
        var propagated = channel.readOutbound();
        assertNull(propagated);
    }

    @Test
    public void testForwardResponse() {
        ApiVersionsResponseFilter filter = (apiVersion, header, response, context) -> context.responseFilterResultBuilder().forward(header, response)
                .completed();
        buildChannel(filter);
        var frame = writeResponse(new ApiVersionsResponseData());
        var propagated = channel.readInbound();
        assertEquals(frame, propagated, "Expect it to be the frame that was sent");
    }

    @Test
    public void testOtherFiltersInChainCanFilterOutOfBandResponse() {
        ApiVersionsResponseFilter recipientFilter = taggingApiVersionsResponseFilter("recipient");
        String filterName = "other-interested-filter";
        ApiVersionsResponseFilter filterUnderTest = taggingApiVersionsResponseFilter(filterName);
        buildChannel(filterUnderTest);
        CompletableFuture<Object> future = new CompletableFuture<>();
        var frame = writeInternalResponse(new ApiVersionsResponseData(), future, recipientFilter);
        var propagated = channel.readInbound();
        assertEquals(frame, propagated, "Expect it to be the frame that was sent");
        assertResponseMessageTaggedWith(filterName, (InternalResponseFrame<?>) propagated);
        assertFalse(future.isDone());
    }

    @Test
    public void testOtherFiltersInChainCanFilterOutOfBandRequest() {
        ApiVersionsRequestFilter recipientFilter = taggingApiVersionsRequestFilter("recipient");
        String filterName = "other-interested-filter";
        ApiVersionsRequestFilter filterUnderTest = taggingApiVersionsRequestFilter(filterName);
        buildChannel(filterUnderTest);
        CompletableFuture<Object> future = new CompletableFuture<>();
        var frame = writeInternalRequest(new ApiVersionsRequestData(), future, recipientFilter);
        var propagated = channel.readOutbound();
        assertEquals(frame, propagated, "Expect it to be the frame that was sent");
        assertRequestMessageTaggedWith(filterName, (DecodedRequestFrame<?>) propagated);
        assertFalse(future.isDone());
    }

    @Test
    public void testShouldNotDeserializeResponse() {
        ApiVersionsResponseFilter filter = new ApiVersionsResponseFilter() {
            @Override
            public boolean shouldHandleApiVersionsResponse(short apiVersion) {
                return false;
            }

            @Override
            public CompletionStage<ResponseFilterResult> onApiVersionsResponse(short apiVersion, ResponseHeaderData header, ApiVersionsResponseData response,
                                                                               KrpcFilterContext context) {
                fail("Should not be called");
                return null;
            }
        };
        buildChannel(filter);
        var frame = writeResponse(new ApiVersionsResponseData());
        var propagated = channel.readInbound();
        assertEquals(frame, propagated, "Expect it to be the frame that was sent");
    }

    @Test
    public void testDropResponse() {
        ApiVersionsResponseFilter filter = (apiVersion, header, response, context) -> {
            return context.responseFilterResultBuilder().drop().completed();
        };
        buildChannel(filter);
        var frame = writeResponse(new ApiVersionsResponseData());
        var propagated = channel.readInbound();
        assertNull(propagated);

    }

    @Test
    public void testSendRequest() {
        FetchRequestData body = new FetchRequestData();
        InternalCompletionStage<ApiMessage>[] fut = new InternalCompletionStage[]{ null };
        ApiVersionsRequestFilter filter = (apiVersion, header, request, context) -> {
            assertNull(fut[0],
                    "Expected to only be called once");
            fut[0] = (InternalCompletionStage<ApiMessage>) context.sendRequest((short) 3, body);
            return CompletableFuture.completedStage(null);
        };

        buildChannel(filter);

        var frame = writeRequest(new ApiVersionsRequestData());
        var propagated = channel.readOutbound();
        assertTrue(propagated instanceof InternalRequestFrame);
        assertEquals(body, ((InternalRequestFrame<?>) propagated).body(),
                "Expect the body to be the Fetch request");

        InternalCompletionStage<ApiMessage> completionStage = fut[0];
        CompletableFuture<ApiMessage> future = toCompletableFuture(completionStage);
        assertFalse(future.isDone(),
                "Future should not be finished yet");

        // test the response path
        CompletableFuture<ApiMessage> futu = new CompletableFuture<>();
        var responseFrame = writeInternalResponse(new FetchResponseData(), futu);
        assertTrue(futu.isDone(),
                "Future should be finished now");
        assertEquals(responseFrame.body(), futu.getNow(null),
                "Expect the body that was sent");
    }

    private static CompletableFuture<ApiMessage> toCompletableFuture(CompletionStage<ApiMessage> completionStage) {
        CompletableFuture<ApiMessage> future = new CompletableFuture<>();
        completionStage.whenComplete((o, throwable) -> {
            if (throwable != null) {
                future.completeExceptionally(throwable);
            }
            else {
                future.complete(o);
            }
        });
        return future;
    }

    @Test
    public void testSendRequestCompletionStageCannotBeConvertedToFuture() {
        FetchRequestData body = new FetchRequestData();
        CompletionStage<?>[] fut = { null };
        ApiVersionsRequestFilter filter = (apiVersion, header, request, context) -> {
            assertNull(fut[0],
                    "Expected to only be called once");
            fut[0] = context.sendRequest((short) 3, body);
            return CompletableFuture.completedStage(null);
        };

        buildChannel(filter);

        var frame = writeRequest(new ApiVersionsRequestData());
        var propagated = channel.readOutbound();
        assertTrue(propagated instanceof InternalRequestFrame);
        assertEquals(body, ((InternalRequestFrame<?>) propagated).body(),
                "Expect the body to be the Fetch request");

        assertThrows(UnsupportedOperationException.class, () -> {
            fut[0].toCompletableFuture();
        });
    }

    /**
     * Test the special case within {@link FilterHandler} for
     * {@link io.kroxylicious.proxy.filter.KrpcFilterContext#sendRequest(short, ApiMessage)}
     * with acks=0 Produce requests.
     */
    @Test
    public void testSendAcklessProduceRequest() throws ExecutionException, InterruptedException {
        ProduceRequestData body = new ProduceRequestData().setAcks((short) 0);
        CompletionStage<ApiMessage>[] fut = new CompletionStage[]{ null };
        ApiVersionsRequestFilter filter = (apiVersion, header, request, context) -> {
            assertNull(fut[0],
                    "Expected to only be called once");
            fut[0] = context.sendRequest((short) 3, body);
            return CompletableFuture.completedStage(null);
        };

        buildChannel(filter);

        var frame = writeRequest(new ApiVersionsRequestData());
        var propagated = channel.readOutbound();
        assertTrue(propagated instanceof InternalRequestFrame);
        assertEquals(body, ((InternalRequestFrame<?>) propagated).body(),
                "Expect the body to be the Fetch request");

        CompletableFuture<ApiMessage> future = toCompletableFuture(fut[0]);
        assertTrue(future.isDone(),
                "Future should be done");
        assertFalse(future.isCompletedExceptionally(),
                "Future should be successful");
        CompletableFuture<Object> blocking = new CompletableFuture<>();
        fut[0].thenApply(blocking::complete);
        assertNull(blocking.get(),
                "Value should be null");
    }

    @Test
    void testSendRequestTimeout() {
        FetchRequestData body = new FetchRequestData();
        CompletionStage<ApiMessage>[] fut = new CompletionStage[]{ null };
        ApiVersionsRequestFilter filter = (apiVersion, header, request, context) -> {
            assertNull(fut[0],
                    "Expected to only be called once");
            fut[0] = context.sendRequest((short) 3, body);
            return CompletableFuture.completedStage(null);
        };

        buildChannel(filter, 50L);
        channel.freezeTime();

        var frame = writeRequest(new ApiVersionsRequestData());
        var propagated = channel.readOutbound();
        assertTrue(propagated instanceof InternalRequestFrame);
        assertEquals(body, ((InternalRequestFrame<?>) propagated).body(),
                "Expect the body to be the Fetch request");

        CompletionStage<ApiMessage> p = fut[0];
        CompletableFuture<ApiMessage> q = toCompletableFuture(p);
        assertFalse(q.isDone(),
                "Future should not be finished yet");

        // advance to 1ms before timeout
        channel.advanceTimeBy(49, TimeUnit.MILLISECONDS);
        channel.runPendingTasks();
        assertThat(q).isNotDone();

        // advance to timeout
        channel.advanceTimeBy(1, TimeUnit.MILLISECONDS);
        channel.runPendingTasks();

        assertTrue(q.isDone(),
                "Future should be finished yet");
        assertTrue(q.isCompletedExceptionally(),
                "Future should be finished yet");
        assertThrows(ExecutionException.class, q::get);
    }

    private static void assertResponseMessageTaggedWith(String filterName, DecodedResponseFrame<?> propagated) {
        String tag = collectTagsToStrings(propagated.body(), ARBITRARY_TAG);
        assertEquals(tag, filterName);
    }

    private static void assertRequestMessageTaggedWith(String filterName, DecodedRequestFrame<?> propagated) {
        String tag = collectTagsToStrings(propagated.body(), ARBITRARY_TAG);
        assertEquals(tag, filterName);
    }

    private static ApiVersionsResponseFilter taggingApiVersionsResponseFilter(String tag) {
        return (apiVersion, header, response, context) -> {
            response.unknownTaggedFields().add(new RawTaggedField(ARBITRARY_TAG, tag.getBytes(UTF_8)));
            return context.responseFilterResultBuilder().forward(header, response).completed();
        };
    }

    private static String collectTagsToStrings(ApiMessage body, int tag) {
        return body.unknownTaggedFields().stream().filter(f -> f.tag() == tag)
                .map(RawTaggedField::data).map(f -> new String(f, UTF_8)).collect(Collectors.joining(","));
    }

    private static ApiVersionsRequestFilter taggingApiVersionsRequestFilter(String tag) {
        return (apiVersion, header, request, context) -> {
            request.unknownTaggedFields().add(new RawTaggedField(ARBITRARY_TAG, tag.getBytes(UTF_8)));
            return context.requestFilterResultBuilder().forward(header, request).completed();
        };
    }

}
