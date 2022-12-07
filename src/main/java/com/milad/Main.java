package com.milad;

import akka.Done;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import org.apache.commons.lang3.RandomStringUtils;

import java.time.Duration;

public class Main {
    public static void main(String[] args) {
        var system = ActorSystem.create(Behaviors.empty(), "WordCounter");
        var sharding = ClusterSharding.get(system);

        WordCounter.init(system);
        WordCounterProjection.init(system);

        var wordCounterRef1 = sharding.entityRefFor(WordCounter.ENTITY_TYPE_KEY, "1");
        var wordCounterRef2 = sharding.entityRefFor(WordCounter.ENTITY_TYPE_KEY, "2");
        var wordCounterRef3 = sharding.entityRefFor(WordCounter.ENTITY_TYPE_KEY, "3");

        for (int i = 0; i < 999; i++) {
            var sentence = RandomStringUtils.randomAlphanumeric(3) + " " + RandomStringUtils.randomAlphanumeric(3);
            if (i % 3 == 0)
                wordCounterRef1.<Done>askWithStatus(v1 -> new WordCounter.CountWords(sentence, v1), Duration.ofMinutes(20));
            else if (i % 3 == 1)
                wordCounterRef2.<Done>askWithStatus(v1 -> new WordCounter.CountWords(sentence, v1), Duration.ofMinutes(20));
            else
                wordCounterRef3.<Done>askWithStatus(v1 -> new WordCounter.CountWords(sentence, v1), Duration.ofMinutes(20));
        }
    }
}
