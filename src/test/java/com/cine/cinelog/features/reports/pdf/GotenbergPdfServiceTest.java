package com.cine.cinelog.features.reports.pdf;

import com.cine.cinelog.features.reports.config.ReportProperties;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GotenbergPdfService}.
 *
 * <p>
 * Uses a mock {@link ExchangeFunction} to intercept WebClient calls
 * to Gotenberg and a mock TemplateEngine for Thymeleaf rendering.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class GotenbergPdfServiceTest {

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private ExchangeFunction exchangeFunction;

    private GotenbergPdfService service;
    private ReportProperties props;

    @BeforeEach
    void setUp() {
        props = new ReportProperties();
        ReportProperties.Pdf pdf = new ReportProperties.Pdf();
        pdf.setEnabled(true);
        pdf.setGotenbergUrl("http://localhost:3001");
        pdf.setTimeoutSeconds(5);
        props.setPdf(pdf);

        WebClient webClient = WebClient.builder()
                .baseUrl(pdf.getGotenbergUrl())
                .exchangeFunction(exchangeFunction)
                .build();

        // Use a custom WebClient.Builder that returns our mock-backed WebClient
        WebClient.Builder mockBuilder = mock(WebClient.Builder.class, RETURNS_SELF);
        when(mockBuilder.build()).thenReturn(webClient);

        service = new GotenbergPdfService(mockBuilder, templateEngine, props);
    }

    private void enqueueSuccessResponse(byte[] pdfBytes) {
        ClientResponse response = ClientResponse.create(HttpStatusCode.valueOf(200))
                .header("Content-Type", "application/pdf")
                .body(new String(pdfBytes))
                .build();
        when(exchangeFunction.exchange(any())).thenReturn(Mono.just(response));
    }

    private void enqueueErrorResponse(int status, String body) {
        ClientResponse response = ClientResponse.create(HttpStatusCode.valueOf(status))
                .body(body)
                .build();
        when(exchangeFunction.exchange(any())).thenReturn(Mono.just(response));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // generate()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("generate(PdfOptions, Map)")
    class GenerateTests {

        @Test
        @DisplayName("should render template and return PDF bytes from Gotenberg")
        void shouldReturnPdfBytes() {
            // Arrange
            byte[] fakePdf = "%PDF-1.4 fake".getBytes();
            when(templateEngine.process(anyString(), any(Context.class)))
                    .thenReturn("<html><body>Hello</body></html>");
            enqueueSuccessResponse(fakePdf);

            PdfOptions options = PdfOptions.a4("weekly-digest");

            // Act
            byte[] result = service.generate(options, Map.of("data", "test"));

            // Assert
            assertThat(result).isEqualTo(fakePdf);
            verify(templateEngine).process(eq("pdf/weekly-digest"), any(Context.class));
            verify(exchangeFunction).exchange(any());
        }

        @Test
        @DisplayName("should pass template name prefixed with pdf/")
        void shouldUseCorrectTemplatePath() {
            byte[] fakePdf = "%PDF".getBytes();
            ArgumentCaptor<String> templateCaptor = ArgumentCaptor.forClass(String.class);
            when(templateEngine.process(templateCaptor.capture(), any(Context.class)))
                    .thenReturn("<html>trending</html>");
            enqueueSuccessResponse(fakePdf);

            service.generate(PdfOptions.a4("trending"), Map.of());

            assertThat(templateCaptor.getValue()).isEqualTo("pdf/trending");
        }

        @Test
        @DisplayName("should inject baseUrl and appName into template context")
        void shouldInjectContextVariables() {
            byte[] fakePdf = "%PDF".getBytes();
            ArgumentCaptor<Context> ctxCaptor = ArgumentCaptor.forClass(Context.class);
            when(templateEngine.process(anyString(), ctxCaptor.capture()))
                    .thenReturn("<html></html>");
            enqueueSuccessResponse(fakePdf);

            service.generate(PdfOptions.a4("test"), Map.of("data", "val"));

            Context ctx = ctxCaptor.getValue();
            assertThat(ctx.getVariable("baseUrl")).isEqualTo(props.getBaseUrl());
            assertThat(ctx.getVariable("appName")).isEqualTo(props.getFromName());
            assertThat(ctx.getVariable("data")).isEqualTo("val");
        }

        @Test
        @DisplayName("should throw GotenbergException when PDF generation is disabled")
        void shouldThrowWhenDisabled() {
            props.getPdf().setEnabled(false);

            assertThatThrownBy(() -> service.generate(PdfOptions.a4("test"), Map.of()))
                    .isInstanceOf(GotenbergException.class)
                    .hasMessageContaining("disabled");

            verifyNoInteractions(templateEngine, exchangeFunction);
        }

        @Test
        @DisplayName("should throw GotenbergException on Gotenberg 500 error")
        void shouldThrowOnServerError() {
            when(templateEngine.process(anyString(), any(Context.class)))
                    .thenReturn("<html>ok</html>");
            enqueueErrorResponse(500, "Internal Server Error");

            assertThatThrownBy(() -> service.generate(PdfOptions.a4("test"), Map.of()))
                    .isInstanceOf(GotenbergException.class);
        }

        @Test
        @DisplayName("should throw GotenbergException when template engine fails")
        void shouldThrowOnTemplateError() {
            when(templateEngine.process(anyString(), any(Context.class)))
                    .thenThrow(new RuntimeException("Template not found"));

            assertThatThrownBy(() -> service.generate(PdfOptions.a4("missing"), Map.of()))
                    .isInstanceOf(GotenbergException.class)
                    .hasMessageContaining("missing");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // generateFromHtml()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("generateFromHtml(String, PdfOptions)")
    class GenerateFromHtmlTests {

        @Test
        @DisplayName("should send raw HTML to Gotenberg and return PDF bytes")
        void shouldSendRawHtml() {
            byte[] fakePdf = "%PDF-raw".getBytes();
            enqueueSuccessResponse(fakePdf);

            byte[] result = service.generateFromHtml("<html><body>Raw</body></html>",
                    PdfOptions.a4("unused"));

            assertThat(result).isEqualTo(fakePdf);
            verifyNoInteractions(templateEngine);
        }

        @Test
        @DisplayName("should throw GotenbergException when disabled")
        void shouldThrowWhenDisabled() {
            props.getPdf().setEnabled(false);

            assertThatThrownBy(() -> service.generateFromHtml("<html></html>", PdfOptions.a4("x")))
                    .isInstanceOf(GotenbergException.class)
                    .hasMessageContaining("disabled");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PdfOptions record
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PdfOptions")
    class PdfOptionsTests {

        @Test
        @DisplayName("a4() should return portrait A4 dimensions")
        void a4ShouldBePortrait() {
            PdfOptions opts = PdfOptions.a4("test");
            assertThat(opts.paperWidth()).isEqualTo("8.27");
            assertThat(opts.paperHeight()).isEqualTo("11.69");
            assertThat(opts.landscape()).isFalse();
            assertThat(opts.templateName()).isEqualTo("test");
        }

        @Test
        @DisplayName("a4Landscape() should return landscape A4 dimensions")
        void a4LandscapeShouldBeLandscape() {
            PdfOptions opts = PdfOptions.a4Landscape("report");
            assertThat(opts.paperWidth()).isEqualTo("11.69");
            assertThat(opts.paperHeight()).isEqualTo("8.27");
            assertThat(opts.landscape()).isTrue();
        }
    }
}
