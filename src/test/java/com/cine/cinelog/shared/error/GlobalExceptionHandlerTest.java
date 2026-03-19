package com.cine.cinelog.shared.error;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class GlobalExceptionHandlerTest {

    private MessageSource messageSource;
    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        messageSource = Mockito.mock(MessageSource.class);
        // Make the MessageSource return the default message (3rd argument) for any key
        when(messageSource.getMessage(anyString(), any(), anyString(), any()))
                .thenAnswer(inv -> inv.getArgument(2));
        handler = new GlobalExceptionHandler(messageSource);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void handleMethodArgumentNotValid_shouldProduceValidationProblemDetail_withFieldErrorsAndCommonProps() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult br = mock(BindingResult.class);

        FieldError fe = new FieldError("dto", "name", "bad-value", false, null, null, "must not be blank");
        List<FieldError> errors = List.of(fe);

        when(ex.getBindingResult()).thenReturn(br);
        when(br.getFieldErrors()).thenReturn(errors);

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/api/test");

        MDC.put("traceId", "tid-123");
        MDC.put("spanId", "sid-456");

        ProblemDetail pd = handler.handleMethodArgumentNotValid(ex, req);

        assertEquals("Validation failed", pd.getTitle());
        assertEquals("Payload inválido.", pd.getDetail());
        assertEquals(URI.create("https://api.cinelog.com/errors/validation"), pd.getType());
        assertEquals("/api/test", pd.getProperties().get("path"));
        assertEquals("GEN-001", pd.getProperties().get("errorCode"));
        assertEquals("tid-123", pd.getProperties().get("traceId"));
        assertTrue(pd.getProperties().containsKey("timestamp"));

        Object fieldErrorsObj = pd.getProperties().get("fieldErrors");
        assertTrue(fieldErrorsObj instanceof List);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> feList = (List<Map<String, Object>>) fieldErrorsObj;
        assertEquals(1, feList.size());
        Map<String, Object> feMap = feList.get(0);
        assertEquals("name", feMap.get("field"));
        assertEquals("must not be blank", feMap.get("message"));
        assertEquals("bad-value", feMap.get("rejectedValue"));
        assertEquals("dto", feMap.get("object"));
    }

    @Test
    void handleConstraintViolation_shouldProduceValidationProblemDetail_withConstraintInfo() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> cv = mock(ConstraintViolation.class);
        Object path = "param.x";
        jakarta.validation.Path mockPath = new jakarta.validation.Path() {
            @Override
            public Iterator<jakarta.validation.Path.Node> iterator() {
                return Collections.emptyIterator();
            }

            @Override
            public String toString() {
                return path.toString();
            }
        };
        when(cv.getPropertyPath()).thenReturn(mockPath);
        when(cv.getMessage()).thenReturn("invalid param");
        when(cv.getInvalidValue()).thenReturn(42);

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(cv));
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/api/params");

        ProblemDetail pd = handler.handleConstraintViolation(ex, req);

        assertEquals("Constraint violation", pd.getTitle());
        assertEquals("Parâmetros inválidos.", pd.getDetail());
        assertEquals(URI.create("https://api.cinelog.com/errors/validation"), pd.getType());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> violations = (List<Map<String, Object>>) pd.getProperties().get("fieldErrors");
        assertEquals(1, violations.size());
        Map<String, Object> m = violations.get(0);
        assertEquals("param.x", m.get("field"));
        assertEquals("invalid param", m.get("message"));
        assertEquals(42, m.get("rejectedValue"));
    }

    @Test
    void handleBadJson_shouldProduceBadRequestProblemDetail() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/api/json");

        ProblemDetail pd = handler.handleBadJson(ex, req);

        assertEquals("Bad request", pd.getTitle());
        assertEquals("Requisição malformada ou tipos incompatíveis.", pd.getDetail());
        assertEquals(URI.create("https://api.cinelog.com/errors/bad-request"), pd.getType());
        assertEquals("GEN-001", pd.getProperties().get("errorCode"));
        assertEquals("/api/json", pd.getProperties().get("path"));
    }

    @Test
    void handleIntegrity_shouldReturnConflictWithoutExposingDbDetails() {
        String longMsg = "A".repeat(1000);
        DataIntegrityViolationException ex = new DataIntegrityViolationException("viol", new RuntimeException(longMsg));
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/api/db");

        ProblemDetail pd = handler.handleIntegrity(ex, req);

        assertEquals("Conflict", pd.getTitle());
        assertEquals("Operação violou uma restrição de integridade.", pd.getDetail());
        assertEquals(URI.create("https://api.cinelog.com/errors/conflict"), pd.getType());
        assertEquals("GEN-002", pd.getProperties().get("errorCode"));

        // Handler does NOT expose raw DB details to client (security: A02)
        assertNull(pd.getProperties().get("dbMessage"));
    }

    @Test
    void handleErrorResponseException_shouldFillMissingTypeAndTitle_andKeepBody() {
        ErrorResponseException ex = mock(ErrorResponseException.class);
        ProblemDetail body = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        // Spring sets "about:blank" type and "Bad Request" title by default; handler
        // will override type
        when(ex.getBody()).thenReturn(body);
        when(ex.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/api/spring");

        ProblemDetail pd = handler.handleErrorResponseException(ex, req);

        assertSame(body, pd);
        assertEquals(URI.create("https://api.cinelog.com/errors/bad-request"), pd.getType());
        // Spring's forStatus sets title to "Bad Request"; handler only overrides if
        // null
        assertNotNull(pd.getTitle());
        assertEquals("SPRING_ERROR", pd.getProperties().get("errorCode"));
        assertEquals("/api/spring", pd.getProperties().get("path"));
    }

    @Test
    void handleUnknown_shouldReturnInternalServerError() {
        RuntimeException ex = new RuntimeException("boom");
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/api/unknown");

        ProblemDetail pd = handler.handleUnknown(ex, req);

        assertEquals("Internal error", pd.getTitle());
        assertEquals("Ocorreu um erro inesperado.", pd.getDetail());
        assertEquals(URI.create("https://api.cinelog.com/errors/internal"), pd.getType());
        assertEquals("GEN-000", pd.getProperties().get("errorCode"));
        assertEquals("/api/unknown", pd.getProperties().get("path"));
    }
}
