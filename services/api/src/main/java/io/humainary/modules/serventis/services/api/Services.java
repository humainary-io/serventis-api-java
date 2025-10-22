// Copyright (c) 2025 William David Louth

package io.humainary.modules.serventis.services.api;

import static io.humainary.modules.serventis.services.api.Services.Orientation.RECEIPT;
import static io.humainary.modules.serventis.services.api.Services.Orientation.RELEASE;
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


    /// A signal released indicating the request (call) for work to be done (executed)

    default void call () { emit ( Signal.CALL ); }


    /// A signal received indicating the request (call) for work to be done (executed)

    default void called () { emit ( Signal.CALLED ); }


    /// A signal released indicating the delay of work

    default void delay () { emit ( Signal.DELAY ); }


    /// A signal received indicating the delay of work

    default void delayed () { emit ( Signal.DELAYED ); }


    /// A signal released indicating the dropping of work

    default void discard () { emit ( Signal.DISCARD ); }


    /// A signal received indicating the dropping of work

    default void discarded () { emit ( Signal.DISCARDED ); }


    /// A signal released indicating the disconnection of work

    default void disconnect () { emit ( Signal.DISCONNECT ); }


    /// A signal received indicating the disconnection of work

    default void disconnected () { emit ( Signal.DISCONNECTED ); }


    /// A method that emits the appropriate signals for this service in the calling of a function.
    ///
    /// @param fn  the function to be called
    /// @param <R> the return type of the function
    /// @param <T> the throwable class type
    /// @return The return value of the function
    /// @throws T                    the checked exception type of the function
    /// @throws NullPointerException if the function param is `null`

    default < R, T extends Throwable > R dispatch (
      @NotNull final Fn < R, T > fn
    ) throws T {

      requireNonNull ( fn );

      call ();

      try {

        final var result =
          fn.eval ();

        success ();

        return
          result;

      } catch (
        final Throwable t
      ) {

        fail ();

        throw t;

      }

    }


    /// A method that emits the appropriate signals for this service in the calling of an operation.
    ///
    /// @param op  the operation to be called
    /// @param <T> the throwable class type
    /// @throws T                    the checked exception type of the operation
    /// @throws NullPointerException if the operation param is `null`

    default < T extends Throwable > void dispatch (
      @NotNull final Op < T > op
    ) throws T {

      requireNonNull ( op );

      call ();

      try {

        op.exec ();

        success ();

      } catch (
        final Throwable t
      ) {

        fail ();

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

    default < R, T extends Throwable > R execute (
      final Fn < R, T > fn
    ) throws T {

      requireNonNull ( fn );

      start ();

      try {

        final var result =
          fn.eval ();

        success ();

        return
          result;

      } catch (
        final Throwable t
      ) {

        fail ();

        throw t;

      } finally {

        stop ();

      }

    }


    /// A method that emits the appropriate signals for this service in the execution of an operation.
    ///
    /// @param op  the operation to be executed
    /// @param <T> the throwable class type
    /// @throws T                    the checked exception type of the operation
    /// @throws NullPointerException if the operation param is `null`

    default < T extends Throwable > void execute (
      final Op < T > op
    ) throws T {

      requireNonNull ( op );

      start ();

      try {

        op.exec ();

        success ();

      } catch (
        final Throwable t
      ) {

        fail ();

        throw t;

      } finally {

        stop ();

      }

    }


    /// A signal released indicating the expiration of work

    default void expire () { emit ( Signal.EXPIRE ); }


    /// A signal received indicating the expiration of work

    default void expired () { emit ( Signal.EXPIRED ); }


    /// A signal released indicating failure to complete a unit of work

    default void fail () { emit ( Signal.FAIL ); }


    /// A signal received indicating failure to complete a unit of work

    default void failed () { emit ( Signal.FAILED ); }


    /// A signal released indicating activation of some recourse strategy for work

    default void recourse () { emit ( Signal.RECOURSE ); }


    /// A signal received indicating activation of some recourse strategy for work

    default void recoursed () { emit ( Signal.RECOURSED ); }


    /// A signal released indicating the redirection of work to another service

    default void redirect () { emit ( Signal.REDIRECT ); }


    /// A signal received indicating the redirection of work to another service

    default void redirected () { emit ( Signal.REDIRECTED ); }


    /// A signal released indicating the rejection of work

    default void reject () { emit ( Signal.REJECT ); }


    /// A signal received indicating the rejection of work

    default void rejected () { emit ( Signal.REJECTED ); }


    /// A signal released indicating the resumption of work

    default void resume () { emit ( Signal.RESUME ); }


    /// A signal received indicating the resumption of work

    default void resumed () { emit ( Signal.RESUMED ); }


    /// A signal received indicating the retry of work

    default void retried () { emit ( Signal.RETRIED ); }


    /// A signal released indicating the retry of work

    default void retry () { emit ( Signal.RETRY ); }


    /// A signal released indicating the scheduling of work

    default void schedule () { emit ( Signal.SCHEDULE ); }


    /// A signal received indicating the scheduling of work

    default void scheduled () { emit ( Signal.SCHEDULED ); }


    /// A signal released indicating the start of work to be done

    default void start () { emit ( Signal.START ); }


    /// A signal received indicating the start of work to be done

    default void started () { emit ( Signal.STARTED ); }


    /// A signal released indicating the completion of work

    default void stop () { emit ( Signal.STOP ); }


    /// A signal received indicating the completion of work

    default void stopped () { emit ( Signal.STOPPED ); }


    /// A signal received indicating successful completion of work

    default void succeeded () { emit ( Signal.SUCCEEDED ); }


    /// A signal released indicating successful completion of work

    default void success () { emit ( Signal.SUCCESS ); }


    /// A signal released indicating the suspension of work

    default void suspend () { emit ( Signal.SUSPEND ); }


    /// A signal received indicating the suspension of work

    default void suspended () { emit ( Signal.SUSPENDED ); }


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
  /// - `RELEASE`: Indicates the emission of a sign from a self-perspective.
  ///   The service is informing other observers (services) of an operation or outcome.
  /// - `RECEIPT`: Indicates the reception of a sign observed within some message response or event notification.
  ///   Receipt of a sign should be taken as being generated in the past, whereas release is in the present.
  ///
  /// This classification helps to understand the context and timing of signals in service interactions.
  /// Receipt of a sign should be taken as being generated in the past, whereas release is in the present.

  enum Orientation {

    /// the emission of a sign from a self-perspective

    RELEASE,

    /// the reception of a sign observed from an other-perspective

    RECEIPT,

  }

}
