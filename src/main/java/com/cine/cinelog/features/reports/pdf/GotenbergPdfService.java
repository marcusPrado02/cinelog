package com.cine.cinelog.features.reports.pdf;

import com.cine.cinelog.features.reports.config.ReportProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.Duration;
import java.util.Map;

/**
 * Serviço de geração de PDF via <a href="https://gotenberg.dev">Gotenberg</a>.
 *
 * <h2>Fluxo</h2>
 * <ol>
 * <li>Renderiza template Thymeleaf localizado em
 * {@code templates/pdf/{templateName}.html}</li>
 * <li>Envia o HTML ao Gotenberg via
 * {@code POST /forms/chromium/convert/html}</li>
 * <li>Retorna os bytes do PDF gerado</li>
 * </ol>
 *
 * <p>
 * Requer o container Gotenberg rodando (veja {@code docker-compose.yml}).
 * </p>
 *
 * @since 2.1
 * @see PdfOptions
 * @see ReportProperties.Pdf
 */
@Service
public class GotenbergPdfService {

    private static final Logger log = LoggerFactory.getLogger(GotenbergPdfService.class);

    private final WebClient webClient;
    private final TemplateEngine templateEngine;
    private final ReportProperties props;

    public GotenbergPdfService(WebClient.Builder webClientBuilder,
            TemplateEngine templateEngine,
            ReportProperties props) {
        // PDFs com imagens (posters, fotos) podem ultrapassar o limite padrão de 256KB
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) // 16 MB
                .build();
        this.webClient = webClientBuilder
                .baseUrl(props.getPdf().getGotenbergUrl())
                .exchangeStrategies(strategies)
                .build();
        this.templateEngine = templateEngine;
        this.props = props;
    }

    /**
     * Gera PDF a partir de um template Thymeleaf e variáveis de contexto.
     *
     * @param options   opções de layout do PDF (margens, tamanho de papel)
     * @param variables variáveis passadas ao template Thymeleaf
     * @return bytes do PDF gerado
     * @throws GotenbergException se o Gotenberg retornar erro ou estiver
     *                            indisponível
     */
    public byte[] generate(PdfOptions options, Map<String, Object> variables) {
        if (!props.getPdf().isEnabled()) {
            throw new GotenbergException("PDF generation is disabled (cinelog.reports.pdf.enabled=false)");
        }

        log.info("Generating PDF — template: {}, paper: {}x{}", options.templateName(),
                options.paperWidth(), options.paperHeight());
        long start = System.currentTimeMillis();

        try {
            // 1. Renderiza HTML via Thymeleaf
            String html = renderHtml(options.templateName(), variables);

            // 2. Monta request multipart para Gotenberg
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("files", new NamedByteArrayResource("index.html", html.getBytes()));
            builder.part("marginTop", options.marginTop());
            builder.part("marginBottom", options.marginBottom());
            builder.part("marginLeft", options.marginLeft());
            builder.part("marginRight", options.marginRight());
            builder.part("paperWidth", options.paperWidth());
            builder.part("paperHeight", options.paperHeight());
            builder.part("printBackground", "true");
            builder.part("preferCssPageSize", "false");
            builder.part("emulateMediaType", "print");

            if (options.landscape()) {
                builder.part("landscape", "true");
            }

            // 3. Chama Gotenberg
            byte[] pdf = webClient.post()
                    .uri("/forms/chromium/convert/html")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            response -> response.bodyToMono(String.class)
                                    .map(body -> new GotenbergException(
                                            "Gotenberg returned %d: %s".formatted(
                                                    response.statusCode().value(), body))))
                    .bodyToMono(byte[].class)
                    .timeout(Duration.ofSeconds(props.getPdf().getTimeoutSeconds()))
                    .block();

            long elapsed = System.currentTimeMillis() - start;
            log.info("PDF generated — template: {}, size: {} bytes, elapsed: {}ms",
                    options.templateName(),
                    pdf != null ? pdf.length : 0,
                    elapsed);

            return pdf;

        } catch (GotenbergException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate PDF — template: {}: {}", options.templateName(), e.getMessage(), e);
            throw new GotenbergException("PDF generation failed for template '%s': %s"
                    .formatted(options.templateName(), e.getMessage()), e);
        }
    }

    /**
     * Gera PDF a partir de uma string HTML já renderizada (sem template).
     *
     * @param rawHtml HTML completo como String
     * @param options opções de layout do PDF
     * @return bytes do PDF gerado
     */
    public byte[] generateFromHtml(String rawHtml, PdfOptions options) {
        if (!props.getPdf().isEnabled()) {
            throw new GotenbergException("PDF generation is disabled");
        }

        log.info("Generating PDF from raw HTML — size: {} chars", rawHtml.length());

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("files", new NamedByteArrayResource("index.html", rawHtml.getBytes()));
        builder.part("marginTop", options.marginTop());
        builder.part("marginBottom", options.marginBottom());
        builder.part("marginLeft", options.marginLeft());
        builder.part("marginRight", options.marginRight());
        builder.part("paperWidth", options.paperWidth());
        builder.part("paperHeight", options.paperHeight());
        builder.part("printBackground", "true");
        builder.part("emulateMediaType", "print");

        return webClient.post()
                .uri("/forms/chromium/convert/html")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(byte[].class)
                .timeout(Duration.ofSeconds(props.getPdf().getTimeoutSeconds()))
                .block();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String renderHtml(String templateName, Map<String, Object> variables) {
        Context ctx = new Context();
        variables.forEach(ctx::setVariable);
        ctx.setVariable("baseUrl", props.getBaseUrl());
        ctx.setVariable("appName", props.getFromName());
        return templateEngine.process("pdf/" + templateName, ctx);
    }

    /**
     * {@link ByteArrayResource} com nome de arquivo — obrigatório para
     * multipart file upload no Gotenberg.
     */
    private static class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        NamedByteArrayResource(String filename, byte[] byteArray) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
