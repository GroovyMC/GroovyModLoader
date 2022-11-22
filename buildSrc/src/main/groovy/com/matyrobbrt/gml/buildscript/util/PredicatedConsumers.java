package com.matyrobbrt.gml.buildscript.util;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class PredicatedConsumers<T, R> implements Function<T, Consumer<R>> {
    private final Set<PredicateData<T, R>> datas = new HashSet<>();

    @Override
    public Consumer<R> apply(T t) {
        Consumer<R> cons = e -> {};
        for (final var data : this.datas) {
            if (data.predicate().test(t)) {
                cons = cons.andThen(e -> data.consumer.accept(t, e));
            }
        }
        return cons;
    }

    public void add(Predicate<T> predicate, BiConsumer<T, R> consumer) {
        this.datas.add(new PredicateData<>(predicate, consumer));
    }

    private record PredicateData<T, R>(Predicate<T> predicate, BiConsumer<T, R> consumer) {}
}
