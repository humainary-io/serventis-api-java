// Copyright (c) 2025 William David Louth

package io.humainary.serventis.opt.data;

import io.humainary.serventis.api.Serventis;
import io.humainary.serventis.sdk.SignMap;
import io.humainary.serventis.sdk.SignSet;
import io.humainary.serventis.sdk.SignalSet;
import io.humainary.serventis.sdk.Statuses;
import io.humainary.serventis.sdk.SymbolSet;
import io.humainary.specs.api.Specs.SpecDoc;
import io.humainary.specs.api.Specs.SpecRef;
import io.humainary.substrates.api.Substrates.Utility;

import static io.humainary.serventis.api.Serventis.Kind.OPERATION;
import static io.humainary.serventis.api.Serventis.Kind.OUTCOME;
import static io.humainary.serventis.sdk.Statuses.Sign.DEFECTIVE;
import static io.humainary.serventis.sdk.Statuses.Sign.DEGRADED;
import static io.humainary.serventis.sdk.Statuses.Sign.STABLE;
import static java.util.Objects.requireNonNull;

/// # Messages API
///
/// The `Messages` API reports how a message delivery contract ended: whether a message was
/// acknowledged, refused, delivered again, set aside, or allowed to expire. It covers message
/// brokers, event streams, and any transport that promises delivery and reports settlement.
///
/// ## Purpose
///
/// The transport vocabularies say that something moved. None of them says whether the receiving end
/// accepted it. `Queues` describes a container's boundary, not a delivery contract. `Messages`
/// supplies the verdicts: `ACK`, `NACK`, `EXHAUST`, and `EXPIRE`, with the operations that
/// precede them.
///
/// ## Important: Reporting vs Implementation
///
/// This API is for **reporting delivery semantics**, not implementing a broker. When a client
/// library, broker, or consumer framework settles a message, it emits the matching sign. Every sign
/// reports a result the messaging system itself presents: an acknowledgement frame, a redelivery
/// flag, a delivery limit reached, an expiry. A producer never has to infer one.
///
/// ## Key Concepts
///
/// - **Message**: a named message destination (topic, queue, channel) whose deliveries are observed
/// - **Sign**: a delivery step or settlement (`PUBLISH`, `DELIVER`, `ACK`, `NACK`, `REDELIVER`,
///   `EXHAUST`, `EXPIRE`)
/// - **Dimension**: the party reporting (`PRODUCER`, `BROKER`, `CONSUMER`)
///
/// ## Signal Matrix
///
/// Each party reports what happens to a message on the legs it takes part in: the producer on the
/// leg to the broker, the consumer on the leg from it, and the broker on both. Every sign and
/// dimension pair has a reading.
///
/// | Sign        | PRODUCER                                             | BROKER                                                    | CONSUMER                                                    |
/// |-------------|------------------------------------------------------|-----------------------------------------------------------|-------------------------------------------------------------|
/// | `PUBLISH`   | I handed a message to the broker                     | A producer handed me a message                            | I handed a message I consumed on, as to a retry topic       |
/// | `DELIVER`   | A receipt says my message reached a consumer         | I handed a message to a consumer                          | A message was handed to me                                  |
/// | `ACK`       | The broker confirmed my message                      | I confirmed a publish, or a consumer confirmed a delivery | I confirmed a message handed to me                          |
/// | `NACK`      | The broker refused my message                        | I refused a publish, or a consumer refused a delivery     | I refused or failed a message handed to me                  |
/// | `REDELIVER` | I sent a message again after a refusal or no confirm | I handed a message to a consumer again                    | A message was handed to me again                            |
/// | `EXHAUST`   | I gave a message up when my sends ran out            | I gave a message up when its deliveries ran out           | I gave a message up when my attempts to process it ran out  |
/// | `EXPIRE`    | A message I held expired before I could send it      | A message I held expired before delivery                  | My client dropped a message that expired before reaching me |
///
/// The broker is the receiving end of a publish and the sending end of a delivery, so its `ACK` and
/// `NACK` settle either leg. An observer that needs to tell them apart reads the producer's and the
/// consumer's signals alongside the broker's.
///
/// ## Relationship to Other APIs
///
/// - **Queues API**: a queue reports its boundary (`OVERFLOW`, `UNDERFLOW`); Messages reports whether
///   the delivery contract was kept
/// - **Services API**: a request/response call is a Services interaction; a message whose handling is
///   acknowledged is a Messages interaction
/// - **Statuses API**: `ACK` reads stable; `NACK` and `EXPIRE` read degraded; `EXHAUST` reads
///   defective
///
/// ## Performance Considerations
///
/// See [io.humainary.serventis.api.Serventis.Signaler] for observation reuse and the
/// limits of performance guarantees. Measure the provider and pipeline for the intended workload.
///
/// @author William David Louth
/// @since 3.6

@Utility
@SpecDoc ( "https://github.com/humainary-io/serventis-api-spec/blob/3.6.0/SPEC.md" )
@SpecRef ( {"8.2", "registry:messages"} )
public final class Messages
  implements Serventis {

  /// The sign set of this API — the captured [Sign] constants from which the canonical
  /// [#STATUS] and [#KIND] interpretations, and any caller-derived sign maps, are mapped.

  @SpecRef ( "4.5" )
  public static final SignSet < Sign > SIGNS =
    SignSet.of (
      Sign.class
    );

  /// The dimension set of this API — the captured [Dimension] constants used to build
  /// message signal instances and caller-derived signal maps.

  @SpecRef ( "4.5" )
  public static final SymbolSet < Dimension > DIMENSIONS =
    SymbolSet.of (
      Dimension.class
    );

  /// Canonical sign-to-status translation for messages — the default *immediate interpretant* of
  /// the upward ascent (sign-keyed; the reporting party does not change the reading; compose to
  /// override, see [SignMap]). An acknowledged message reads healthy; a refusal and an expiry read
  /// degraded; a message given up once its delivery attempts ran out reads defective. Publishing,
  /// delivery, and redelivery abstain: a redelivery is recovery, and a refusal that caused one is
  /// read on its own. The instrument emits `Signal`, so as a Scorecards ballot project the sign
  /// first — `Scorecards.flow ( STATUS.compose ( Signal::sign ) )`.
  ///
  /// Exhaustive without a `default`: a new [Sign] is a compile error here until its reading is decided.

  public static final SignMap < Sign, Statuses.Sign > STATUS =
    SIGNS.map (
      sign -> switch ( sign ) {
        case ACK -> STABLE;
        case NACK, EXPIRE -> DEGRADED;
        case EXHAUST -> DEFECTIVE;
        case PUBLISH, DELIVER, REDELIVER -> null;
      }
    );

  /// Canonical sign-to-kind classification for messages — each [Sign] tagged [Kind#OPERATION] or
  /// [Kind#OUTCOME]: publishing, delivery, and redelivery are operations; acknowledgement, refusal,
  /// exhaustion, and expiry are outcomes. The reporting party does not change the kind.
  /// Exhaustive without a `default`. See [Kind].

  public static final SignMap < Sign, Kind > KIND =
    SIGNS.map (
      sign -> switch ( sign ) {
        case ACK, NACK, EXHAUST, EXPIRE -> OUTCOME;
        case PUBLISH, DELIVER, REDELIVER -> OPERATION;
      }
    );

  private Messages () { }

  /// Creates a Message instrument wrapping the specified pipe.
  ///
  /// @param pipe the pipe from which to create the message instrument
  /// @return a new Message instrument for the specified pipe
  /// @throws NullPointerException if the pipe parameter is `null`

  @SpecRef ( "6.4" )
  @New
  @NotNull
  public static Message of (
    @NotNull final Pipe < ? super Signal > pipe
  ) {

    return
      new Message (
        requireNonNull ( pipe )
      );

  }

  /// Returns a pool that creates cached Message instruments from a conduit.
  ///
  /// Within the returned pool, repeated lookup of the same name returns the same instrument,
  /// created on first lookup. Separate pools have separate identity guarantees.
  ///
  /// @param conduit the conduit providing signal pipes
  /// @return a pool that creates Message instruments
  /// @throws NullPointerException if the conduit parameter is `null`

  @SpecRef ( {"6.4", "substrates:10.1"} )
  @New
  @NotNull
  public static Pool < Message > pool (
    @NotNull final Conduit < Signal > conduit
  ) {

    return
      conduit.pool (
        Messages::of
      );

  }


  /// The [Dimension] enum names the party reporting a message sign.
  ///
  /// A delivery has three parties: the producer that publishes, the broker that holds and delivers,
  /// and the consumer that receives. The same settlement reported from two of them produces two
  /// signals that differ only in dimension, which is what lets a consumer of the signals see the
  /// parties disagree.

  @SpecRef ( {"4.3", "registry:messages"} )
  public enum Dimension
    implements Category {

    /// Emitted by the party that publishes messages.
    ///
    /// **Mental model**: "I am sending this message"

    PRODUCER,

    /// Emitted by the intermediary that holds messages and delivers them.
    ///
    /// **Mental model**: "I am relaying this message"

    BROKER,

    /// Emitted by the party that receives and processes messages.
    ///
    /// **Mental model**: "I am receiving this message"

    CONSUMER

  }


  /// The [Sign] enum represents a message delivery step or settlement.
  ///
  /// A message is published, delivered, and then settled: acknowledged, refused, or, once its
  /// delivery attempts run out, given up. A refused or unacknowledged message may be delivered again
  /// first. A message whose time to live runs out is settled by expiry instead.

  @SpecRef ( {"4.2", "registry:messages"} )
  public enum Sign
    implements Serventis.Sign {

    /// A message was handed on for delivery.

    PUBLISH,

    /// A message was handed to a receiver.

    DELIVER,

    /// The receiving end confirmed the message.
    ///
    /// From a producer this is a publish confirmation; from a consumer it is a processing
    /// acknowledgement.

    ACK,

    /// The receiving end refused or failed the message.

    NACK,

    /// A message was delivered again because an earlier delivery was refused or never settled.
    ///
    /// Reported whenever the messaging system marks a delivery as a redelivery: a broker requeueing
    /// a message that was refused, timed out unacknowledged, or left unsettled when its consumer's
    /// channel closed, and a consumer seeing the redelivered flag that results.

    REDELIVER,

    /// A message's delivery attempts ran out, and it was set aside or dropped.
    ///
    /// Reported when the messaging system's delivery limit is reached, by whichever party gives the
    /// message up, whether it is then routed to a dead-letter destination or discarded.

    EXHAUST,

    /// A message's time to live ran out before delivery.

    EXPIRE

  }

  /// The [Message] class emits message delivery observations for one named destination.
  ///
  /// ## Usage
  ///
  /// ```java
  /// // Producer publishes and receives a confirm
  /// message.publish(PRODUCER);
  /// message.ack(PRODUCER);
  ///
  /// // Broker delivers, the consumer refuses, the broker redelivers
  /// message.deliver(BROKER);
  /// message.nack(CONSUMER);
  /// message.redeliver(BROKER);
  ///
  /// // Delivery exhausted
  /// message.exhaust(BROKER);
  /// ```

  @SpecRef ( {"6.2", "6.3", "6.5", "substrates:6.1"} )
  @Queued
  @Provided
  public static final class Message
    implements Signaler < Sign, Dimension > {

    private static final SignalSet < Sign, Dimension, Signal > SIGNALS =
      SIGNS.signals (
        DIMENSIONS,
        Signal::new
      );

    private final Pipe < ? super Signal > pipe;

    private Message (
      final Pipe < ? super Signal > pipe
    ) {

      this.pipe =
        pipe;

    }

    /// Emits an `ACK` sign from the specified party.
    ///
    /// @param dimension the reporting party
    /// @throws NullPointerException if the dimension is `null`

    public void ack (
      @NotNull final Dimension dimension
    ) {

      signal (
        Sign.ACK,
        dimension
      );

    }

    /// Emits a `DELIVER` sign from the specified party.
    ///
    /// @param dimension the reporting party
    /// @throws NullPointerException if the dimension is `null`

    public void deliver (
      @NotNull final Dimension dimension
    ) {

      signal (
        Sign.DELIVER,
        dimension
      );

    }

    /// Emits an `EXHAUST` sign from the specified party.
    ///
    /// @param dimension the reporting party
    /// @throws NullPointerException if the dimension is `null`

    public void exhaust (
      @NotNull final Dimension dimension
    ) {

      signal (
        Sign.EXHAUST,
        dimension
      );

    }

    /// Emits an `EXPIRE` sign from the specified party.
    ///
    /// @param dimension the reporting party
    /// @throws NullPointerException if the dimension is `null`

    public void expire (
      @NotNull final Dimension dimension
    ) {

      signal (
        Sign.EXPIRE,
        dimension
      );

    }

    /// Emits a `NACK` sign from the specified party.
    ///
    /// @param dimension the reporting party
    /// @throws NullPointerException if the dimension is `null`

    public void nack (
      @NotNull final Dimension dimension
    ) {

      signal (
        Sign.NACK,
        dimension
      );

    }

    /// Emits a `PUBLISH` sign from the specified party.
    ///
    /// @param dimension the reporting party
    /// @throws NullPointerException if the dimension is `null`

    public void publish (
      @NotNull final Dimension dimension
    ) {

      signal (
        Sign.PUBLISH,
        dimension
      );

    }

    /// Emits a `REDELIVER` sign from the specified party.
    ///
    /// @param dimension the reporting party
    /// @throws NullPointerException if the dimension is `null`

    public void redeliver (
      @NotNull final Dimension dimension
    ) {

      signal (
        Sign.REDELIVER,
        dimension
      );

    }

    @SpecRef ( "6.2" )
    @Override
    public void signal (
      @NotNull final Sign sign,
      @NotNull final Dimension dimension
    ) {

      pipe.emit (
        SIGNALS.get (
          sign,
          dimension
        )
      );

    }

  }

  /// The [Signal] record represents a message observation.
  ///
  /// Each signal combines a sign with the party that reported it.
  ///
  /// @param sign      the delivery step or settlement
  /// @param dimension the reporting party

  @Provided
  @Immutable
  @SpecRef ( "4.4" )
  public record Signal(
    @NotNull Sign sign,
    @NotNull Dimension dimension
  ) implements Serventis.Signal < Sign, Dimension > {

    /// @throws NullPointerException if `sign` or `dimension` is `null`

    public Signal {

      requireNonNull ( sign );
      requireNonNull ( dimension );

    }

  }

}
