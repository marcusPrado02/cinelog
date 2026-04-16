package com.cine.cinelog.features.reports.pdf;

/**
 * Opções de layout para geração de PDF via Gotenberg.
 *
 * <p>
 * Dimensões em polegadas (padrão Gotenberg/Chromium).
 * Valores defaults correspondem a A4 com margens de 1 polegada.
 * </p>
 *
 * @param templateName identificador do template Thymeleaf (ex:
 *                     {@code "weekly-digest"})
 * @param marginTop    margem superior em polegadas
 * @param marginBottom margem inferior em polegadas
 * @param marginLeft   margem esquerda em polegadas
 * @param marginRight  margem direita em polegadas
 * @param paperWidth   largura do papel em polegadas (A4 = 8.27)
 * @param paperHeight  altura do papel em polegadas (A4 = 11.69)
 * @param landscape    se {@code true}, gera em orientação paisagem
 * @since 2.1
 */
public record PdfOptions(
        String templateName,
        String marginTop,
        String marginBottom,
        String marginLeft,
        String marginRight,
        String paperWidth,
        String paperHeight,
        boolean landscape) {

    /**
     * Opções padrão para A4 retrato com margens de 0.8 polegadas.
     */
    public static PdfOptions a4(String templateName) {
        return new PdfOptions(templateName, "0.8", "0.8", "0.6", "0.6", "8.27", "11.69", false);
    }

    /**
     * A4 paisagem — útil para relatórios com tabelas largas.
     */
    public static PdfOptions a4Landscape(String templateName) {
        return new PdfOptions(templateName, "0.6", "0.6", "0.8", "0.8", "11.69", "8.27", true);
    }
}
