package com.cine.cinelog.core.domain.vo;

import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;

/**
 * Value Object para ano de lançamento.
 *
 * Regras:
 * - Ano mínimo: 1888 (primeiros filmes)
 * - Ano máximo: ano atual + FUTURE_SLACK
 *
 * Se o ano estiver fora do range, dispara MEDIA_YEAR_OUT_OF_RANGE (M4),
 * passando min, max e o valor como argumentos para i18n.
 */
public final class Year {

    public static final int MIN_YEAR = 1888;
    public static final int FUTURE_SLACK = 2; // tolerância para pré-cadastro de títulos

    private final int value;

    private Year(int value) {
        int current = java.time.Year.now().getValue();
        int maxYear = current + FUTURE_SLACK;

        if (value < MIN_YEAR || value > maxYear) {
            // args: min, max, value => podem ser usados em {0}, {1}, {2} na mensagem i18n
            throw DomainException.of(
                    ErrorCode.MEDIA_YEAR_OUT_OF_RANGE,
                    MIN_YEAR,
                    maxYear,
                    value);
        }
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static Year of(Integer v) {
        if (v == null) {
            // aqui você pode querer um erro mais específico no futuro, por enquanto
            // GEN_VALIDATION
            throw DomainException.of(ErrorCode.GEN_VALIDATION);
        }
        return new Year(v);
    }
}
