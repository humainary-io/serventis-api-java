/*
 * Copyright (c) 2025 William David Louth
 */

package io.humainary.modules.serventis.services.api;

import static io.humainary.modules.serventis.services.api.Services.Orientation.RECEIPT;
import static io.humainary.modules.serventis.services.api.Services.Orientation.RELEASE;
import static io.humainary.modules.serventis.services.api.Services.Signal.*;
import static io.humainary.substrates.api.Substrates.*;
import static java.util.Objects.requireNonNull;


/// The [Services] class is the entry point into the Serventis Services API.
///
/// Services is a new novel approach to the monitoring (observability) of service-to-service interactions
/// based on signaling theory and social systems regulated in part by local and remote status assessment.
///
/// @author autoletics
/// @since 1.0

public interface Services
  extends Composer < Services.Service, Services.Signal > {


  /// An interface representing a service, which can be a composition of one or more functions or operations.
  ///
  /// A service is a subject precept (instrument) that emits signals.

  @Provided
  interface Service
    extends Pipe < Signal > {

    /// A method that emits the appropriate signals for this service in the calling of a function.
    ///
    /// @param fn  the function to be called
    /// @param <R> the return type of the function
    /// @param <T> the throwable class type
    /// @return The return value of the function
    /// @throws T                    the checked exception type of the function
    /// @throws NullPointerException if the function param is `null`

    default < R, T extends Throwable > R call (
      @NotNull final Fn < R, T > fn
    ) throws T {

      requireNonNull ( fn );

      emit (
        CALL
      );

      try {

        final var result =
          fn.eval ();

        emit (
          SUCCEED
        );

        return
          result;

      } catch (
        final Throwable t
      ) {

        emit (
          FAIL
        );

        throw t;

      }

    }


    /// A method that emits the appropriate signals for this service in the calling of an operation.
    ///
    /// @param op  the operation to be called
    /// @param <T> the throwable class type
    /// @throws T                    the checked exception type of the operation
    /// @throws NullPointerException if the operation param is `null`

    default < T extends Throwable > void call (
      @NotNull final Op < T > op
    ) throws T {

      requireNonNull ( op );

      emit (
        CALL
      );

      try {

        op.exec ();

        emit (
          SUCCEED
        );

      } catch (
        final Throwable t
      ) {

        emit (
          FAIL
        );

        throw t;

      }


    }


    /// Emit a signal for this service.
    ///
    /// @param signal the [Signal] to be emitted

    @Override
    void emit (
      Signal signal
    );


    /// A method that emits the appropriate signals for this service in the execution of a function.
    ///
    /// @param fn  the function to be executed
    /// @param <R> the return type of the function
    /// @param <T> the throwable class type
    /// @return The return value of the function
    /// @throws T                    the checked exception type of the function
    /// @throws NullPointerException if the function param is `null`

    default < R, T extends Throwable > R exec (
      final Fn < R, T > fn
    ) throws T {

      requireNonNull ( fn );

      emit (
        START
      );

      try {

        final var result =
          fn.eval ();

        emit (
          SUCCEED
        );

        return
          result;

      } catch (
        final Throwable t
      ) {

        emit (
          FAIL
        );

        throw t;

      } finally {

        emit (
          STOP
        );

      }

    }


    /// A method that emits the appropriate signals for this service in the execution of an operation.
    ///
    /// @param op  the operation to be executed
    /// @param <T> the throwable class type
    /// @throws T                    the checked exception type of the operation
    /// @throws NullPointerException if the operation param is `null`

    default < T extends Throwable > void exec (
      final Op < T > op
    ) throws T {

      requireNonNull ( op );

      emit (
        START
      );

      try {

        op.exec ();

        emit (
          SUCCEED
        );

      } catch (
        final Throwable t
      ) {

        emit (
          FAIL
        );

        throw t;

      } finally {

        emit (
          STOP
        );

      }

    }

  }


  /// The `Signal` enum represents various types of signals that services can emit.
  ///
  /// Each signal type indicates a specific operation or outcome, and orientation in a service-to-service interaction.

  enum Signal {

    /// A signal released indicating the start of a service's execution

    START ( Sign.START, RELEASE ),


    /// A signal received indicating the start of a service's execution

    STARTED ( Sign.START, RECEIPT ),


    /// A signal released indicating the completion of a service's execution.

    STOP ( Sign.STOP, RELEASE ),


    /// A signal received indicating the completion of a service's execution

    STOPPED ( Sign.STOP, RECEIPT ),


    /// A signal released indicating the calling of a service

    CALL ( Sign.CALL, RELEASE ),


    /// A signal received indicating a service was called

    CALLED ( Sign.CALL, RECEIPT ),


    /// A signal released indicating the successful completion of a service

    SUCCEED ( Sign.SUCCEED, RELEASE ),


    /// A signal received indicating the successful completion of a service

    SUCCEEDED ( Sign.SUCCEED, RECEIPT ),


    /// A signal released indicating the failure of a service execution.

    FAIL ( Sign.FAIL, RELEASE ),


    /// A signal received indicating the failure of a service execution.

    FAILED ( Sign.FAIL, RECEIPT ),


    /// A signal released indicating the recourse of a service execution.

    RECOURSE ( Sign.RECOURSE, RELEASE ),


    /// A signal received indicating the recourse of a service execution.

    RECOURSED ( Sign.RECOURSE, RECEIPT ),


    /// A signal released indicating the redirection of a service execution.

    REDIRECT ( Sign.REDIRECT, RELEASE ),


    /// A signal received indicating the redirection of a service execution.

    REDIRECTED ( Sign.REDIRECT, RECEIPT ),


    /// A signal released indicating the elapse of a service execution.

    ELAPSE ( Sign.ELAPSE, RELEASE ),


    /// A signal received indicating the elapse of a service execution.

    ELAPSED ( Sign.ELAPSE, RECEIPT ),


    /// A signal released indicating the retry of a service execution.

    RETRY ( Sign.RETRY, RELEASE ),


    /// A signal received indicating the retry of a service execution.

    RETRIED ( Sign.RETRY, RECEIPT ),


    /// A signal released indicating the rejection of a service execution.

    REJECT ( Sign.REJECT, RELEASE ),


    /// A signal received indicating the rejection of a service execution.

    REJECTED ( Sign.REJECT, RECEIPT ),


    /// A signal released indicating the dropping of a service execution.

    DROP ( Sign.DROP, RELEASE ),


    /// A signal received indicating the dropping of a service execution.

    DROPPED ( Sign.DROP, RECEIPT ),


    /// A signal released indicating the delay of a service execution.

    DELAY ( Sign.DELAY, RELEASE ),


    /// A signal received indicating the delay of a service execution.

    DELAYED ( Sign.DELAY, RECEIPT ),


    /// A signal released indicating the scheduling of a service execution.

    SCHEDULE ( Sign.SCHEDULE, RELEASE ),


    /// A signal received indicating the scheduling of a service execution.

    SCHEDULED ( Sign.SCHEDULE, RECEIPT ),


    /// A signal released indicating the suspension of a service execution.

    SUSPEND ( Sign.SUSPEND, RELEASE ),


    /// A signal received indicating the suspension of a service execution.

    SUSPENDED ( Sign.SUSPEND, RECEIPT ),


    /// A signal released indicating the resumption of a service execution.

    RESUME ( Sign.RESUME, RELEASE ),


    /// A signal received indicating the resumption of a service execution.
    RESUMED ( Sign.RESUME, RECEIPT ),


    /// A signal released indicating the disconnection of a service execution.

    DISCONNECT ( Sign.DISCONNECT, RELEASE ),


    /// A signal received indicating the disconnection of a service execution.

    DISCONNECTED ( Sign.DISCONNECT, RECEIPT );

    private final Orientation orientation;
    private final Sign        sign;

    Signal (
      final Sign sign,
      final Orientation orientation
    ) {

      this.orientation = requireNonNull ( orientation );
      this.sign = requireNonNull ( sign );

    }

    public Orientation orientation () {

      return orientation;

    }

    public Sign sign () {

      return sign;

    }

  }


  /// A [Sign] classifies operations, transitions, and outcomes that occur during service request
  /// execution and inter-service calling, such classifications enable analysis of service behavior.

  enum Sign {


    /// Indicates the initiation of a service execution.
    /// This is the first event in a service's lifecycle.
    START,

    /// Indicates the completion of a service execution, regardless of its outcome.
    /// This is typically the final signal in a service's interaction.

    STOP,

    /// Records service operation representing either:
    /// - Outbound: A caller initiating a request to another service
    /// - Inbound: A service receiving a request from a caller

    CALL,

    /// Indicates that a service execution or inter-service call completed
    /// successfully, meeting all expected criteria.

    SUCCEED,

    /// Indicates that a service execution or inter-service call encountered
    /// an error condition and did not complete as expected.

    FAIL,

    /// Indicates activation of a degraded service mode after a failure, such as
    /// - Serving cached data instead of live data
    /// - Routing to a backup service
    /// - Using a simplified alternative implementation
    RECOURSE,

    /// Records that a service request was forwarded to a different endpoint
    /// or handler than originally targeted.
    REDIRECT,

    /// Indicates that a service execution or call exceeded its allocated
    /// time budget and was terminated.
    ELAPSE,

    /// Records an automatic reattempt of a failed service call, typically
    /// as part of a retry policy with backoff.
    RETRY,

    /// Indicates that a service actively declined to process a request,
    /// typically due to policy violations or resource constraints.
    REJECT,

    /// Records that a service request was discarded without processing,
    /// typically due to overload conditions or circuit breaking.
    DROP,

    /// Indicates that processing of a request was intentionally
    /// postponed for later execution.
    DELAY,

    /// Records that a service request has been queued for
    /// execution at a future time.
    SCHEDULE,

    /// Indicates that an in-progress service execution was temporarily
    /// halted but may resume later.
    SUSPEND,

    /// Records that a previously suspended service execution
    /// has restarted processing.
    RESUME,

    /// Indicates a connection failure where the client could not
    /// establish or maintain network connectivity with the service.
    DISCONNECT,

  }


  /// The `Orientation` enum classifies the method of signal recording against a service.
  ///
  /// There are two ways a `Sign` can be recorded against a `Service`:
  ///
  /// - `RELEASE`: Indicates the projection (release) of a signal from the self-perspective.
  ///   The service is informing other observers (services) of an operation or outcome.
  /// - `RECEIPT`: Indicates the acknowledgment (receipt) of a signal observed within some message response or event notification.
  ///   Receipt of a signal should be taken as being generated in the past, whereas release is in the present.
  ///
  /// This classification helps to understand the context and timing of signals in service interactions.
  /// Receipt of a signal should be taken as being generated in the past, whereas release is in the present.

  enum Orientation {

    /// The projection (release) of a signal.
    RELEASE,

    /// The perception (receipt) of a signal.
    RECEIPT,

  }

}
