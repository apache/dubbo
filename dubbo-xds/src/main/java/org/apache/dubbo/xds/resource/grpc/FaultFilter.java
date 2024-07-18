/*
 * Copyright 2021 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.xds.resource.grpc;

import org.apache.dubbo.xds.resource.grpc.FaultConfig.FaultAbort;
import org.apache.dubbo.xds.resource.grpc.FaultConfig.FaultDelay;
import org.apache.dubbo.xds.resource.grpc.Filter.ClientInterceptorBuilder;
import org.apache.dubbo.xds.resource.grpc.ThreadSafeRandom.ThreadSafeRandomImpl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.Durations;
import io.envoyproxy.envoy.extensions.filters.http.fault.v3.HTTPFault;
import io.envoyproxy.envoy.type.v3.FractionalPercent;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.Context;
import io.grpc.Deadline;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener.SimpleForwardingClientCallListener;
import io.grpc.LoadBalancer.PickSubchannelArgs;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.Status.Code;
import io.grpc.internal.DelayedClientCall;
import io.grpc.internal.GrpcUtil;

import javax.annotation.Nullable;

import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

/** HttpFault filter implementation. */
final class FaultFilter implements Filter, ClientInterceptorBuilder {

  static final FaultFilter INSTANCE =
      new FaultFilter(ThreadSafeRandomImpl.instance, new AtomicLong());
  @VisibleForTesting
  static final Metadata.Key<String> HEADER_DELAY_KEY =
      Metadata.Key.of("x-envoy-fault-delay-request", Metadata.ASCII_STRING_MARSHALLER);
  @VisibleForTesting
  static final Metadata.Key<String> HEADER_DELAY_PERCENTAGE_KEY =
      Metadata.Key.of("x-envoy-fault-delay-request-percentage", Metadata.ASCII_STRING_MARSHALLER);
  @VisibleForTesting
  static final Metadata.Key<String> HEADER_ABORT_HTTP_STATUS_KEY =
      Metadata.Key.of("x-envoy-fault-abort-request", Metadata.ASCII_STRING_MARSHALLER);
  @VisibleForTesting
  static final Metadata.Key<String> HEADER_ABORT_GRPC_STATUS_KEY =
      Metadata.Key.of("x-envoy-fault-abort-grpc-request", Metadata.ASCII_STRING_MARSHALLER);
  @VisibleForTesting
  static final Metadata.Key<String> HEADER_ABORT_PERCENTAGE_KEY =
      Metadata.Key.of("x-envoy-fault-abort-request-percentage", Metadata.ASCII_STRING_MARSHALLER);
  static final String TYPE_URL =
      "type.googleapis.com/envoy.extensions.filters.http.fault.v3.HTTPFault";

  private final ThreadSafeRandom random;
  private final AtomicLong activeFaultCounter;

  @VisibleForTesting
  FaultFilter(ThreadSafeRandom random, AtomicLong activeFaultCounter) {
    this.random = random;
    this.activeFaultCounter = activeFaultCounter;
  }

  @Override
  public String[] typeUrls() {
    return new String[] { TYPE_URL };
  }

  @Override
  public ConfigOrError<FaultConfig> parseFilterConfig(Message rawProtoMessage) {
    HTTPFault httpFaultProto;
    if (!(rawProtoMessage instanceof Any)) {
      return ConfigOrError.fromError("Invalid config type: " + rawProtoMessage.getClass());
    }
    Any anyMessage = (Any) rawProtoMessage;
    try {
      httpFaultProto = anyMessage.unpack(HTTPFault.class);
    } catch (InvalidProtocolBufferException e) {
      return ConfigOrError.fromError("Invalid proto: " + e);
    }
    return parseHttpFault(httpFaultProto);
  }

  private static ConfigOrError<FaultConfig> parseHttpFault(HTTPFault httpFault) {
    FaultDelay faultDelay = null;
    FaultAbort faultAbort = null;
    if (httpFault.hasDelay()) {
      faultDelay = parseFaultDelay(httpFault.getDelay());
    }
    if (httpFault.hasAbort()) {
      ConfigOrError<FaultAbort> faultAbortOrError = parseFaultAbort(httpFault.getAbort());
      if (faultAbortOrError.errorDetail != null) {
        return ConfigOrError.fromError(
            "HttpFault contains invalid FaultAbort: " + faultAbortOrError.errorDetail);
      }
      faultAbort = faultAbortOrError.config;
    }
    Integer maxActiveFaults = null;
    if (httpFault.hasMaxActiveFaults()) {
      maxActiveFaults = httpFault.getMaxActiveFaults().getValue();
      if (maxActiveFaults < 0) {
        maxActiveFaults = Integer.MAX_VALUE;
      }
    }
    return ConfigOrError.fromConfig(FaultConfig.create(faultDelay, faultAbort, maxActiveFaults));
  }

  private static FaultDelay parseFaultDelay(
      io.envoyproxy.envoy.extensions.filters.common.fault.v3.FaultDelay faultDelay) {
    FaultConfig.FractionalPercent percent = parsePercent(faultDelay.getPercentage());
    if (faultDelay.hasHeaderDelay()) {
      return FaultDelay.forHeader(percent);
    }
    return FaultDelay.forFixedDelay(Durations.toNanos(faultDelay.getFixedDelay()), percent);
  }

  @VisibleForTesting
  static ConfigOrError<FaultAbort> parseFaultAbort(
      io.envoyproxy.envoy.extensions.filters.http.fault.v3.FaultAbort faultAbort) {
    FaultConfig.FractionalPercent percent = parsePercent(faultAbort.getPercentage());
    switch (faultAbort.getErrorTypeCase()) {
      case HEADER_ABORT:
        return ConfigOrError.fromConfig(FaultAbort.forHeader(percent));
      case HTTP_STATUS:
        return ConfigOrError.fromConfig(FaultAbort.forStatus(
            GrpcUtil.httpStatusToGrpcStatus(faultAbort.getHttpStatus()), percent));
      case GRPC_STATUS:
        return ConfigOrError.fromConfig(FaultAbort.forStatus(
            Status.fromCodeValue(faultAbort.getGrpcStatus()), percent));
      case ERRORTYPE_NOT_SET:
      default:
        return ConfigOrError.fromError(
            "Unknown error type case: " + faultAbort.getErrorTypeCase());
    }
  }

  private static FaultConfig.FractionalPercent parsePercent(FractionalPercent proto) {
    switch (proto.getDenominator()) {
      case HUNDRED:
        return FaultConfig.FractionalPercent.perHundred(proto.getNumerator());
      case TEN_THOUSAND:
        return FaultConfig.FractionalPercent.perTenThousand(proto.getNumerator());
      case MILLION:
        return FaultConfig.FractionalPercent.perMillion(proto.getNumerator());
      case UNRECOGNIZED:
      default:
        throw new IllegalArgumentException("Unknown denominator type: " + proto.getDenominator());
    }
  }

  @Override
  public ConfigOrError<FaultConfig> parseFilterConfigOverride(Message rawProtoMessage) {
    return parseFilterConfig(rawProtoMessage);
  }

  @Nullable
  @Override
  public ClientInterceptor buildClientInterceptor(
      FilterConfig config, @Nullable FilterConfig overrideConfig, PickSubchannelArgs args,
      final ScheduledExecutorService scheduler) {
    checkNotNull(config, "config");
    if (overrideConfig != null) {
      config = overrideConfig;
    }
    FaultConfig faultConfig = (FaultConfig) config;
    Long delayNanos = null;
    Status abortStatus = null;
    if (faultConfig.maxActiveFaults() == null
        || activeFaultCounter.get() < faultConfig.maxActiveFaults()) {
      Metadata headers = args.getHeaders();
      if (faultConfig.faultDelay() != null) {
        delayNanos = determineFaultDelayNanos(faultConfig.faultDelay(), headers);
      }
      if (faultConfig.faultAbort() != null) {
        abortStatus = determineFaultAbortStatus(faultConfig.faultAbort(), headers);
      }
    }
    if (delayNanos == null && abortStatus == null) {
      return null;
    }
    final Long finalDelayNanos = delayNanos;
    final Status finalAbortStatus = getAbortStatusWithDescription(abortStatus);

    final class FaultInjectionInterceptor implements ClientInterceptor {
      @Override
      public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
          final MethodDescriptor<ReqT, RespT> method, final CallOptions callOptions,
          final Channel next) {
        Executor callExecutor = callOptions.getExecutor();
        if (callExecutor == null) { // This should never happen in practice because
          // ManagedChannelImpl.ConfigSelectingClientCall always provides CallOptions with
          // a callExecutor.
          // TODO(https://github.com/grpc/grpc-java/issues/7868)
          callExecutor = MoreExecutors.directExecutor();
        }
        if (finalDelayNanos != null) {
          Supplier<? extends ClientCall<ReqT, RespT>> callSupplier;
          if (finalAbortStatus != null) {
            callSupplier = Suppliers.ofInstance(
                new FailingClientCall<ReqT, RespT>(finalAbortStatus, callExecutor));
          } else {
            callSupplier = new Supplier<ClientCall<ReqT, RespT>>() {
              @Override
              public ClientCall<ReqT, RespT> get() {
                return next.newCall(method, callOptions);
              }
            };
          }
          final DelayInjectedCall<ReqT, RespT> delayInjectedCall = new DelayInjectedCall<>(
              finalDelayNanos, callExecutor, scheduler, callOptions.getDeadline(), callSupplier);

          final class DeadlineInsightForwardingCall extends ForwardingClientCall<ReqT, RespT> {
            @Override
            protected ClientCall<ReqT, RespT> delegate() {
              return delayInjectedCall;
            }

            @Override
            public void start(Listener<RespT> listener, Metadata headers) {
              Listener<RespT> finalListener =
                  new SimpleForwardingClientCallListener<RespT>(listener) {
                    @Override
                    public void onClose(Status status, Metadata trailers) {
                      if (status.getCode().equals(Code.DEADLINE_EXCEEDED)) {
                        // TODO(zdapeng:) check effective deadline locally, and
                        //   do the following only if the local deadline is exceeded.
                        //   (If the server sends DEADLINE_EXCEEDED for its own deadline, then the
                        //   injected delay does not contribute to the error, because the request is
                        //   only sent out after the delay. There could be a race between local and
                        //   remote, but it is rather rare.)
                        String description = String.format(
                            Locale.US,
                            "Deadline exceeded after up to %d ns of fault-injected delay",
                            finalDelayNanos);
                        if (status.getDescription() != null) {
                          description = description + ": " + status.getDescription();
                        }
                        status = Status.DEADLINE_EXCEEDED
                            .withDescription(description).withCause(status.getCause());
                        // Replace trailers to prevent mixing sources of status and trailers.
                        trailers = new Metadata();
                      }
                      delegate().onClose(status, trailers);
                    }
                  };
              delegate().start(finalListener, headers);
            }
          }

          return new DeadlineInsightForwardingCall();
        } else {
          return new FailingClientCall<>(finalAbortStatus, callExecutor);
        }
      }
    }

    return new FaultInjectionInterceptor();
  }

  private static Status getAbortStatusWithDescription(Status abortStatus) {
    Status finalAbortStatus = null;
    if (abortStatus != null) {
      String abortDesc = "RPC terminated due to fault injection";
      if (abortStatus.getDescription() != null) {
        abortDesc = abortDesc + ": " + abortStatus.getDescription();
      }
      finalAbortStatus = abortStatus.withDescription(abortDesc);
    }
    return finalAbortStatus;
  }

  @Nullable
  private Long determineFaultDelayNanos(FaultDelay faultDelay, Metadata headers) {
    Long delayNanos;
    FaultConfig.FractionalPercent fractionalPercent = faultDelay.percent();
    if (faultDelay.headerDelay()) {
      try {
        int delayMillis = Integer.parseInt(headers.get(HEADER_DELAY_KEY));
        delayNanos = TimeUnit.MILLISECONDS.toNanos(delayMillis);
        String delayPercentageStr = headers.get(HEADER_DELAY_PERCENTAGE_KEY);
        if (delayPercentageStr != null) {
          int delayPercentage = Integer.parseInt(delayPercentageStr);
          if (delayPercentage >= 0 && delayPercentage < fractionalPercent.numerator()) {
            fractionalPercent = FaultConfig.FractionalPercent.create(
                delayPercentage, fractionalPercent.denominatorType());
          }
        }
      } catch (NumberFormatException e) {
        return null; // treated as header_delay not applicable
      }
    } else {
      delayNanos = faultDelay.delayNanos();
    }
    if (random.nextInt(1_000_000) >= getRatePerMillion(fractionalPercent)) {
      return null;
    }
    return delayNanos;
  }

  @Nullable
  private Status determineFaultAbortStatus(FaultAbort faultAbort, Metadata headers) {
    Status abortStatus = null;
    FaultConfig.FractionalPercent fractionalPercent = faultAbort.percent();
    if (faultAbort.headerAbort()) {
      try {
        String grpcCodeStr = headers.get(HEADER_ABORT_GRPC_STATUS_KEY);
        if (grpcCodeStr != null) {
          int grpcCode = Integer.parseInt(grpcCodeStr);
          abortStatus = Status.fromCodeValue(grpcCode);
        }
        String httpCodeStr = headers.get(HEADER_ABORT_HTTP_STATUS_KEY);
        if (httpCodeStr != null) {
          int httpCode = Integer.parseInt(httpCodeStr);
          abortStatus = GrpcUtil.httpStatusToGrpcStatus(httpCode);
        }
        String abortPercentageStr = headers.get(HEADER_ABORT_PERCENTAGE_KEY);
        if (abortPercentageStr != null) {
          int abortPercentage =
              Integer.parseInt(headers.get(HEADER_ABORT_PERCENTAGE_KEY));
          if (abortPercentage >= 0 && abortPercentage < fractionalPercent.numerator()) {
            fractionalPercent = FaultConfig.FractionalPercent.create(
                abortPercentage, fractionalPercent.denominatorType());
          }
        }
      } catch (NumberFormatException e) {
        return null; // treated as header_abort not applicable
      }
    } else {
      abortStatus = faultAbort.status();
    }
    if (random.nextInt(1_000_000) >= getRatePerMillion(fractionalPercent)) {
      return null;
    }
    return abortStatus;
  }

  private static int getRatePerMillion(FaultConfig.FractionalPercent percent) {
    int numerator = percent.numerator();
    FaultConfig.FractionalPercent.DenominatorType type = percent.denominatorType();
    switch (type) {
      case TEN_THOUSAND:
        numerator *= 100;
        break;
      case HUNDRED:
        numerator *= 10_000;
        break;
      case MILLION:
      default:
        break;
    }
    if (numerator > 1_000_000 || numerator < 0) {
      numerator = 1_000_000;
    }
    return numerator;
  }

  /** A {@link DelayedClientCall} with a fixed delay. */
  private final class DelayInjectedCall<ReqT, RespT> extends DelayedClientCall<ReqT, RespT> {
    final Object lock = new Object();
    ScheduledFuture<?> delayTask;
    boolean cancelled;

    DelayInjectedCall(
        long delayNanos, Executor callExecutor, ScheduledExecutorService scheduler,
        @Nullable Deadline deadline,
        final Supplier<? extends ClientCall<ReqT, RespT>> callSupplier) {
      super(callExecutor, scheduler, deadline);
      activeFaultCounter.incrementAndGet();
      ScheduledFuture<?> task = scheduler.schedule(
          new Runnable() {
            @Override
            public void run() {
              synchronized (lock) {
                if (!cancelled) {
                  activeFaultCounter.decrementAndGet();
                }
              }
              Runnable toRun = setCall(callSupplier.get());
              if (toRun != null) {
                toRun.run();
              }
            }
          },
          delayNanos,
          NANOSECONDS);
      synchronized (lock) {
        if (!cancelled) {
          delayTask = task;
          return;
        }
      }
      task.cancel(false);
    }

    @Override
    protected void callCancelled() {
      ScheduledFuture<?> savedDelayTask;
      synchronized (lock) {
        cancelled = true;
        activeFaultCounter.decrementAndGet();
        savedDelayTask = delayTask;
      }
      if (savedDelayTask != null) {
        savedDelayTask.cancel(false);
      }
    }
  }

  /** An implementation of {@link ClientCall} that fails when started. */
  private final class FailingClientCall<ReqT, RespT> extends ClientCall<ReqT, RespT> {
    final Status error;
    final Executor callExecutor;
    final Context context;

    FailingClientCall(Status error, Executor callExecutor) {
      this.error = error;
      this.callExecutor = callExecutor;
      this.context = Context.current();
    }

    @Override
    public void start(final Listener<RespT> listener, Metadata headers) {
      activeFaultCounter.incrementAndGet();
      callExecutor.execute(
          new Runnable() {
            @Override
            public void run() {
              Context previous = context.attach();
              try {
                listener.onClose(error, new Metadata());
                activeFaultCounter.decrementAndGet();
              } finally {
                context.detach(previous);
              }
            }
          });
    }

    @Override
    public void request(int numMessages) {}

    @Override
    public void cancel(String message, Throwable cause) {}

    @Override
    public void halfClose() {}

    @Override
    public void sendMessage(ReqT message) {}
  }
}
