package com.web.watr.controllers;

import com.web.watr.utils.FileUploadException;
import com.web.watr.utils.SuccessDeleteException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class ErrorController {

    @ExceptionHandler(FileUploadException.class)
    private ModelAndView handleFileUploadException(FileUploadException e, HttpServletRequest req, HttpServletResponse res) {
        Map<String, Object> model = new HashMap<>();
        model.put("message", e.getMessage());

        // Get the HTTP status from the exception
        HttpStatus statusCode = e.getStatus();

        // Check if it's an HTMX request
        if ("true".equals(req.getHeader("HX-Request"))) {
            res.addHeader("HX-Reswap", "outerHTML");
            res.addHeader("HX-Push-Url", "false");
            res.addHeader("HX-Trigger", "showToast");  // Custom event for showing the toast

            // Return a fragment (error toast) for HTMX requests
            return new ModelAndView("content/error-toast", model, statusCode);
        }

        // For non-HTMX requests, return the full error page
        return new ModelAndView("error", model, statusCode);
    }

    @ExceptionHandler(Exception.class)
    private ModelAndView handleGenericException(Exception e, HttpServletRequest req, HttpServletResponse res) {
        Map<String, Object> model = new HashMap<>();
        model.put("message", e.getMessage());

        HttpStatus statusCode = HttpStatus.INTERNAL_SERVER_ERROR;

        // Check if it's an HTMX request
        if ("true".equals(req.getHeader("HX-Request"))) {
            res.addHeader("HX-Reswap", "outerHTML");
            res.addHeader("HX-Push-Url", "false");
            res.addHeader("HX-Trigger", "showToast");  // Custom event for showing the toast

            // Return a fragment (error toast) for HTMX requests
            return new ModelAndView("content/error-toast", model, statusCode);
        }

        // For non-HTMX requests, return the full error page
        return new ModelAndView("error", model, statusCode);
    }

    @ExceptionHandler(SuccessDeleteException.class)
    private ModelAndView handleSuccessDeleteException(SuccessDeleteException e, HttpServletRequest req, HttpServletResponse res) {
        Map<String, Object> model = new HashMap<>();
        model.put("message", e.getMessage());

        // Get the HTTP status from the exception
        HttpStatus statusCode = e.getStatus();

        // Check if it's an HTMX request
        if ("true".equals(req.getHeader("HX-Request"))) {
            res.addHeader("HX-Reswap", "outerHTML");
            res.addHeader("HX-Push-Url", "false");
            res.addHeader("HX-Trigger", "showToast");  // Custom event for showing the toast

            // Return a fragment (succes toast) for HTMX requests
            return new ModelAndView("content/success-toast", model, statusCode);
        }

        // For non-HTMX requests, return the full error page
        return new ModelAndView("error", model, statusCode);
    }

}
