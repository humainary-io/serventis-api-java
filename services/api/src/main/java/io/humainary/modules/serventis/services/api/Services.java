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
          SUCCESS
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
          SUCCESS
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
    /// @throws NullPointerException if signal param is `null`

    @Override
    void emit (
      @NotNull Signal signal
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
          SUCCESS
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
          SUCCESS
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
  ///
  /// Note: We use the term `work` here to mean either (remote) call or (local) execution.

  enum Signal {

    /// A signal released indicating the start of work to be done

    START ( Sign.START, RELEASE ),


    /// A signal received indicating the start of work to be done

    STARTED ( Sign.START, RECEIPT ),


    /// A signal released indicating the completion work

    STOP ( Sign.STOP, RELEASE ),


    /// A signal received indicating the completion work

    STOPPED ( Sign.STOP, RECEIPT ),


    /// A signal released indicating the request (call) for work to be done (executed)

    CALL ( Sign.CALL, RELEASE ),


    /// A signal received indicating the request (call) for work to be done (executed)

    CALLED ( Sign.CALL, RECEIPT ),


    /// A signal released indicating successful completion of work

    SUCCESS ( Sign.SUCCESS, RELEASE ),


    /// A signal received indicating successful completion of work

    SUCCEEDED ( Sign.SUCCESS, RECEIPT ),


    /// A signal released indicating failure to complete a unit of work

    FAIL ( Sign.FAIL, RELEASE ),


    /// A signal received indicating failure to complete a unit of work

    FAILED ( Sign.FAIL, RECEIPT ),


    /// A signal released indicating activation of some recourse strategy for work

    RECOURSE ( Sign.RECOURSE, RELEASE ),


    /// A signal received indicating activation of some recourse strategy for work

    RECOURSED ( Sign.RECOURSE, RECEIPT ),


    /// A signal released indicating the redirection of work to another service

    REDIRECT ( Sign.REDIRECT, RELEASE ),


    /// A signal received indicating the redirection of work to another service

    REDIRECTED ( Sign.REDIRECT, RECEIPT ),


    /// A signal released indicating the expiration of work

    EXPIRE ( Sign.EXPIRE, RELEASE ),


    /// A signal received indicating the expiration of work

    EXPIRED ( Sign.EXPIRE, RECEIPT ),


    /// A signal released indicating the retry of work

    RETRY ( Sign.RETRY, RELEASE ),


    /// A signal received indicating the retry of work

    RETRIED ( Sign.RETRY, RECEIPT ),


    /// A signal released indicating the rejection of work

    REJECT ( Sign.REJECT, RELEASE ),


    /// A signal received indicating the rejection of work

    REJECTED ( Sign.REJECT, RECEIPT ),


    /// A signal released indicating the dropping of work

    DISCARD ( Sign.DISCARD, RELEASE ),


    /// A signal received indicating the dropping work

    DISCARDED ( Sign.DISCARD, RECEIPT ),


    /// A signal released indicating the delay of work

    DELAY ( Sign.DELAY, RELEASE ),


    /// A signal received indicating the delay of work

    DELAYED ( Sign.DELAY, RECEIPT ),


    /// A signal released indicating the scheduling of work

    SCHEDULE ( Sign.SCHEDULE, RELEASE ),


    /// A signal received indicating the scheduling work

    SCHEDULED ( Sign.SCHEDULE, RECEIPT ),


    /// A signal released indicating the suspension of work

    SUSPEND ( Sign.SUSPEND, RELEASE ),


    /// A signal received indicating the suspension of work

    SUSPENDED ( Sign.SUSPEND, RECEIPT ),


    /// A signal released indicating the resumption of work

    RESUME ( Sign.RESUME, RELEASE ),


    /// A signal received indicating the resumption of work
    RESUMED ( Sign.RESUME, RECEIPT ),


    /// A signal released indicating the disconnection of work

    DISCONNECT ( Sign.DISCONNECT, RELEASE ),


    /// A signal received indicating the disconnection of work

    DISCONNECTED ( Sign.DISCONNECT, RECEIPT );

    private final Orientation orientation;
    private final Sign        sign;

    Signal (
      @NotNull final Sign sign,
      @NotNull final Orientation orientation
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
  ///
  /// Note: We use the term `work` here to mean either (remote) call or (local) execution.

  enum Sign {


    /// Indicates the start of work to be done

    START,

    /// Indicates the completion of work

    STOP,

    /// Indicates the request (call) for work to be done (executed)

    CALL,

    /// Indicates that successful completion of work

    SUCCESS,

    /// Indicates the failure to complete a unit of work

    FAIL,

    /// Indicates the activation of a degraded work mode after a failure

    RECOURSE,

    /// Indicates the forwarding of the work to another service

    REDIRECT,

    /// Indicates the expiration of a time budget for work

    EXPIRE,

    /// Indicates the automatic retry of work on an error

    RETRY,

    /// Indicates the rejection of work

    REJECT,

    /// Indicates the discarding of work

    DISCARD,

    /// Indicates the delaying of work.

    DELAY,

    /// Indicates the scheduling of work

    SCHEDULE,

    /// Indicates the suspension of work

    SUSPEND,

    /// Indicates the resumption of work

    RESUME,

    /// Indicates the inability to issue work

    DISCONNECT,

  }


  /// The `Orientation` enum classifies the method of signal recording against a service.
  ///
  /// There are two ways a `Sign` can be recorded against a `Service`:
  ///
  /// - `RELEASE`: Indicates the projection (release) of a signal from the self-perspective.
  ///   The service is informing other observers (services) of an operation or outcome.
  ///
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
