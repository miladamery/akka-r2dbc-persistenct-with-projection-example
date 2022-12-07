package com.milad;

import akka.Done;
import akka.persistence.query.typed.EventEnvelope;
import akka.projection.r2dbc.javadsl.R2dbcHandler;
import akka.projection.r2dbc.javadsl.R2dbcSession;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class WordCounterProjectionHandler extends R2dbcHandler<EventEnvelope<WordCounter.Event>> {

    @Override
    public CompletionStage<Done> process(R2dbcSession session, EventEnvelope<WordCounter.Event> eventEnvelope) throws Exception, Exception {
        var event = eventEnvelope.event();

        if (event instanceof WordCounter.WordCounted wc) {
            var sql = """
                    INSERT INTO counted_words(actor_name, c_words) values ($1, $2)
                    ON CONFLICT(actor_name) DO UPDATE set c_words = counted_words.c_words + $3
                    """;
            var statement = session.createStatement(sql)
                    .bind(0, eventEnvelope.persistenceId())
                    .bind(1, ((WordCounter.WordCounted) event).getCounted())
                    .bind(2, ((WordCounter.WordCounted) event).getCounted());
            return session.updateOne(statement).thenApply(rowsUpdated -> Done.getInstance());
        }

        return CompletableFuture.completedFuture(Done.getInstance());
    }
}
