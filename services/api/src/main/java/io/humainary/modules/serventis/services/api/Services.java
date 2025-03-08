/*
 * Copyright (c) 2025 William David Louth
 */

package io.humainary.modules.serventis.services.api;

import static io.humainary.modules.serventis.services.api.Services.Orientation.RECEIPT;
import static io.humainary.modules.serventis.services.api.Services.Orientation.RELEASE;
import static io.humainary.modules.serventis.services.api.Services.Signal.*;
import static io.humainary.substrates.api.Substrates.*;


/// The [Services] class is the entry point into the signals for services API.
///
/// Services is a new novel approach to the monitoring (observability) of service-to-service interactions
/// based on signaling theory and social systems regulated in part by local and remote status assessment.
///
/// @author autoletics
/// @since 1.0

public interface Services
  extends Composer < Services.Service, Services.Signal > {


  /// An interface representing a composite-named service with the ability
  /// to inspect the availability status and emit or receive signals.
  ///
  /// Note: An SPI implementation of this interface is free to override
  /// the default methods implementation included here.

  @Provided
  interface Service
    extends Pipe < Signal > {

    /// @param fn  the task to be executed
    /// @param <R> the return type of the task
    /// @param <T> the throwable class type
    /// @return The return value of the task execution
    /// @throws T the throwable thrown by execution of the task
    default < R, T extends Throwable > R call (
      final Fn < R, T > fn
    ) throws T {

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


    /// @param op  the action to be executed
    /// @param <T> the throwable class type
    /// @throws T the throwable thrown by execution of the action

    default < T extends Throwable > void call (
      final Op < T > op
    ) throws T {


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

    void emit (
      Signal signal
    );


    /// @param fn  the task to be executed
    /// @param <R> the return type of the task
    /// @param <T> the throwable class type
    /// @return The return value of the task execution
    /// @throws T the throwable thrown by execution of the task
    default < R, T extends Throwable > R exec (
      final Fn < R, T > fn
    ) throws T {

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


    /// @param op  the action to be executed
    /// @param <T> the throwable class type
    /// @throws T the throwable thrown by execution of the action

    default < T extends Throwable > void exec (
      final Op < T > op
    ) throws T {

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

    /// A signal indicating the start of a local service execution.
    START (
      Sign.START,
      RELEASE
    ),

    /// A signal indicating that a remote service execution has started.
    STARTED (
      Sign.START,
      RECEIPT
    ),

    /// A signal indicating the completion of a local service execution.
    STOP (
      Sign.STOP,
      RELEASE
    ),

    /// A signal indicating the completion of a remote service execution
    STOPPED (
      Sign.STOP,
      RECEIPT
    ),

    /// A signal indicating a caller's outbound call to a service, where the subject is callee
    CALL (
      Sign.CALL,
      RELEASE
    ),

    /// A signal indicating an inbound call to a service, where the subject is the caller
    CALLED (
      Sign.CALL,
      RECEIPT
    ),

    /// A signal indicating the successful completion of a 'local' service execution, where the subject is the caller
    SUCCEED (
      Sign.SUCCEED,
      RELEASE
    ),

    /// A signal indicating the successful completion of a 'remote' service execution, where the subject is the callee
    SUCCEEDED (
      Sign.SUCCEED,
      RECEIPT
    ),

    /// A signal emitted indicating the...
    FAIL (
      Sign.FAIL,
      RELEASE
    ),

    /// A signal received indicating...
    FAILED (
      Sign.FAIL,
      RECEIPT
    ),

    /// A signal emitted indicating the...
    RECOURSE (
      Sign.RECOURSE,
      RELEASE
    ),

    /// A signal received indicating...
    RECOURSED (
      Sign.RECOURSE,
      RECEIPT
    ),

    /// A signal emitted indicating the...
    REDIRECT (
      Sign.REDIRECT,
      RELEASE
    ),

    /// A signal received indicating...
    REDIRECTED (
      Sign.REDIRECT,
      RECEIPT
    ),

    /// A signal emitted indicating the...
    ELAPSE (
      Sign.ELAPSE,
      RELEASE
    ),

    /// A signal received indicating...
    ELAPSED (
      Sign.ELAPSE,
      RECEIPT
    ),

    /// A signal emitted indicating the...
    RETRY (
      Sign.RETRY,
      RELEASE
    ),

    /// A signal received indicating...
    RETRIED (
      Sign.RETRY,
      RECEIPT
    ),

    /// A signal emitted indicating the...
    REJECT (
      Sign.REJECT,
      RELEASE
    ),

    /// A signal received indicating...
    REJECTED (
      Sign.REJECT,
      RECEIPT
    ),

    /// A signal emitted indicating the...
    DROP (
      Sign.DROP,
      RELEASE
    ),

    /// A signal received indicating...
    DROPPED (
      Sign.DROP,
      RECEIPT
    ),

    /// A signal emitted indicating the...
    DELAY (
      Sign.DELAY,
      RELEASE
    ),

    /// A signal received indicating...
    DELAYED (
      Sign.DELAY,
      RECEIPT
    ),

    /// A signal emitted indicating the...
    SCHEDULE (
      Sign.SCHEDULE,
      RELEASE
    ),

    /// A signal received indicating...
    SCHEDULED (
      Sign.SCHEDULE,
      RECEIPT
    ),

    /// A signal emitted indicating the...
    SUSPEND (
      Sign.SUSPEND,
      RELEASE
    ),

    /// A signal received indicating...
    SUSPENDED (
      Sign.SUSPEND,
      RECEIPT
    ),

    /// A signal emitted indicating the...
    RESUME (
      Sign.RESUME,
      RELEASE
    ),

    /// A signal received indicating...
    RESUMED (
      Sign.RESUME,
      RECEIPT
    ),

    /// A signal emitted indicating the...
    DISCONNECT (
      Sign.DISCONNECT,
      RELEASE
    ),

    /// A signal received indicating...
    DISCONNECTED (
      Sign.DISCONNECT,
      RECEIPT
    );

    private final Orientation orientation;
    private final Sign        sign;

    Signal (
      final Sign sign,
      final Orientation orientation
    ) {

      this.orientation = orientation;
      this.sign = sign;

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
