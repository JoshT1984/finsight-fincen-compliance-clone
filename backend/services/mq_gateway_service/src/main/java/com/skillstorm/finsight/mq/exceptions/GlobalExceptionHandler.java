package com.skillstorm.finsight.mq.exceptions;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		pd.setTitle("Validation Failed");
		pd.setDetail("One or more fields are invalid.");
		pd.setProperty("path", request.getRequestURI());

		var errors = ex.getBindingResult().getFieldErrors().stream()
				.collect(Collectors.toMap(
						fe -> fe.getField(),
						fe -> fe.getDefaultMessage() == null ? "Invalid value" : fe.getDefaultMessage(),
						(a, b) -> a));

		pd.setProperty("errors", errors);
		return pd;
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
		ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		pd.setTitle("Validation Failed");
		pd.setDetail("One or more parameters are invalid.");
		pd.setProperty("path", request.getRequestURI());

		var errors = ex.getConstraintViolations().stream()
				.collect(Collectors.toMap(
						v -> v.getPropertyPath().toString(),
						v -> v.getMessage(),
						(a, b) -> a));

		pd.setProperty("errors", errors);
		return pd;
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ProblemDetail handleBadJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
		ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		pd.setTitle("Bad Request");
		pd.setDetail("Malformed JSON or invalid value.");
		pd.setProperty("path", request.getRequestURI());
		return pd;
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleAny(Exception ex, HttpServletRequest request) {
		log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

		ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
		pd.setTitle("Internal Server Error");
		pd.setDetail("An unexpected error occurred.");
		pd.setProperty("path", request.getRequestURI());
		return pd;
	}

	@ExceptionHandler(AmqpException.class)
	public ProblemDetail handleAmqp(AmqpException ex, HttpServletRequest request) {
		log.warn("AMQP publish failure at {}: {}", request.getRequestURI(), ex.getMessage());

		ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
		pd.setTitle("Message Broker Unavailable");
		pd.setDetail("Unable to publish message. Please retry.");
		pd.setProperty("path", request.getRequestURI());
		return pd;
	}

}
