package com.milad;

import akka.actor.typed.ActorSystem;
import akka.cluster.sharding.typed.javadsl.ShardedDaemonProcess;
import akka.japi.Pair;
import akka.persistence.query.Offset;
import akka.persistence.query.typed.EventEnvelope;
import akka.persistence.r2dbc.query.javadsl.R2dbcReadJournal;
import akka.projection.Projection;
import akka.projection.ProjectionBehavior;
import akka.projection.ProjectionId;
import akka.projection.eventsourced.javadsl.EventSourcedProvider;
import akka.projection.javadsl.SourceProvider;
import akka.projection.r2dbc.R2dbcProjectionSettings;
import akka.projection.r2dbc.javadsl.R2dbcProjection;

import java.util.List;
import java.util.Optional;

public class WordCounterProjection {

    public static void init(ActorSystem<?> system) {
        int numberOfSliceRanges = 4;
        List<Pair<Integer, Integer>> sliceRanges =
                EventSourcedProvider.sliceRanges(
                        system, R2dbcReadJournal.Identifier(), numberOfSliceRanges);

        ShardedDaemonProcess.get(system)
                .init(
                        ProjectionBehavior.Command.class,
                        "WordCounterProjection",
                        sliceRanges.size(),
                        i -> ProjectionBehavior.create(createProjection(system, sliceRanges.get(i))), //can use R2dbcHandler.fromFunction too
                        ProjectionBehavior.stopMessage()
                );
    }

    private static Projection<EventEnvelope<WordCounter.Event>> createProjection(
            ActorSystem<?> system,
            Pair<Integer, Integer> sliceRange
    ) {
        int minSlice = sliceRange.first();
        int maxSlice = sliceRange.second();

        String entityType = WordCounter.ENTITY_TYPE_KEY.name();
        SourceProvider<Offset, EventEnvelope<WordCounter.Event>> sourceProvider = EventSourcedProvider.eventsBySlices(
                system,
                R2dbcReadJournal.Identifier(),
                entityType,
                minSlice,
                maxSlice
        );

        Optional<R2dbcProjectionSettings> settings = Optional.empty();
        return R2dbcProjection.exactlyOnce(
                ProjectionId.of("WordCounter", "word-counter-" + minSlice + "-" + maxSlice), //nothing special, just an string
                settings,
                sourceProvider,
                WordCounterProjectionHandler::new,
                system
        );
    }
}
