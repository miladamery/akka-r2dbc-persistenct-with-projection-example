package com.milad;

import akka.Done;
import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.Entity;
import akka.cluster.sharding.typed.javadsl.EntityTypeKey;
import akka.pattern.StatusReply;
import akka.persistence.typed.PersistenceId;
import akka.persistence.typed.javadsl.CommandHandler;
import akka.persistence.typed.javadsl.EventHandler;
import akka.persistence.typed.javadsl.EventSourcedBehavior;
import akka.persistence.typed.javadsl.RetentionCriteria;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class WordCounter extends EventSourcedBehavior<WordCounter.Command, WordCounter.Event, Integer> {

    private Integer countedWords = 0;

    private WordCounter(PersistenceId persistenceId) {
        super(persistenceId);
    }

    public static Behavior<Command> create(PersistenceId persistenceId) {
        return Behaviors.setup(ctx -> new WordCounter(persistenceId));
    }

    public static final EntityTypeKey<Command> ENTITY_TYPE_KEY =
            EntityTypeKey.create(Command.class, "WordCounter");

    public static void init(ActorSystem<?> system) {
        ClusterSharding.get(system)
                .init(Entity.of(ENTITY_TYPE_KEY, entityContext -> create(PersistenceId.of(
                        entityContext.getEntityTypeKey().name(), entityContext.getEntityId()))));
    }

    @Override
    public Integer emptyState() {
        return countedWords;
    }

    @Override
    public CommandHandler<Command, Event, Integer> commandHandler() {
        var builder = newCommandHandlerBuilder();
        builder
                .forAnyState()
                .onCommand(CountWords.class, cmd -> Effect()
                        .persist(new WordCounted(cmd.sentence.split(" ").length))
                        .thenReply(cmd.replyTo, countedWords -> StatusReply.Ack())
                );
        return builder.build();
    }

    @Override
    public EventHandler<Integer, Event> eventHandler() {
        var builder = newEventHandlerBuilder();
        builder
                .forAnyState()
                .onEvent(WordCounted.class, wordCounted -> {
                    countedWords += wordCounted.counted;
                    return countedWords;
                });
        return builder.build();
    }

    @Override
    public RetentionCriteria retentionCriteria() {
        return RetentionCriteria.snapshotEvery(100, 1);
    }

    interface Command extends CborSerializer {
    }

    @Value
    @Jacksonized
    public static class CountWords implements Command {
        String sentence;
        ActorRef<StatusReply<Done>> replyTo;
    }

    interface Event extends CborSerializer {
    }

    @Value
    @NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
    @AllArgsConstructor
    public static class WordCounted implements Event {
        int counted;
    }
}
