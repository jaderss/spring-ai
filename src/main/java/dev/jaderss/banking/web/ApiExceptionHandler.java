package dev.jaderss.banking.web;

import dev.jaderss.banking.core.TransferRejectedException;
import dev.jaderss.banking.insights.ModelOutputRejectedException;
import dev.jaderss.banking.observability.CorrelationIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(TransferRejectedException.class)
    ProblemDetail transferRejected(TransferRejectedException e) {
        HttpStatus status = switch (e.reason()) {
            case PROPOSAL_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ALREADY_CONFIRMED -> HttpStatus.CONFLICT;
            case PROPOSAL_EXPIRED -> HttpStatus.GONE;
            case ACCOUNT_NOT_FOUND,
                    SAME_ACCOUNT,
                    NOT_POSITIVE,
                    CURRENCY_MISMATCH,
                    INSUFFICIENT_FUNDS,
                    IDEMPOTENCY_KEY_REUSED -> HttpStatus.UNPROCESSABLE_CONTENT;
        };
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, e.getMessage());
        problem.setTitle("Transfer rejected");
        problem.setProperty("reason", e.reason().name());
        return problem;
    }

    @ExceptionHandler(ModelOutputRejectedException.class)
    ProblemDetail modelOutputRejected(ModelOutputRejectedException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY, "The language model did not return a usable answer. Try again later.");
        problem.setTitle("Model output invalid");
        return problem;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpectedFailure(Exception e) {
        log.error("Unexpected failure, correlationId={}", MDC.get(CorrelationIdFilter.MDC_KEY), e);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "The request could not be completed.");
        problem.setTitle("Internal error");
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalidInput(IllegalArgumentException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problem.setTitle("Invalid request");
        return problem;
    }
}
