package com.cine.cinelog.core.domain.common;

import java.util.Objects;

/**
 * Classe de configuração Spring para gerenciamento de ok.
 * 
 * <p>Define beans e configurações necessárias para o funcionamento
 * adequado da aplicação.</p>
 * 
 * @since 1.0
 */
public sealed interface Result<T> permits Result.Ok, Result.Err {
    record Ok<T>(T value) implements Result<T> {
    }

    record Err<T>(String message) implements Result<T> {
    }

    static <T> Result<T> ok(T v) {
        return new Ok<>(v);
    }

    static <T> Result<T> err(String m) {
        return new Err<>(Objects.requireNonNull(m));
    }

    default boolean isOk() {
        return this instanceof Ok<?>;
    }

    default boolean isErr() {
        return this instanceof Err<?>;
    }
}