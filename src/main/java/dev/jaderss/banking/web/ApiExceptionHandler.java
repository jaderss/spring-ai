package dev.jaderss.banking.web;

import dev.jaderss.banking.core.TransferRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(TransferRejectedException.class)
    ProblemDetail transferRejected(TransferRejectedException e) {
        HttpStatus status = switch (e.reason()) {
            case PROPOSAL_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ALREADY_CONFIRMED -> HttpStatus.CONFLICT;
            case PROPOSAL_EXPIRED -> HttpStatus.GONE;
            case ACCOUNT_NOT_FOUND, SAME_ACCOUNT, NOT_POSITIVE, CURRENCY_MISMATCH, INSUFFICIENT_FUNDS ->
                HttpStatus.UNPROCESSABLE_CONTENT;
        };
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, e.getMessage());
        problem.setTitle("Transfer rejected");
        problem.setProperty("reason", e.reason().name());
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalidInput(IllegalArgumentException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problem.setTitle("Invalid request");
        return problem;
    }
}
