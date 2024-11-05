package com.example.demo.exception;

public class CustomExceptions {
    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String message) {
            super(message);
        }
    }

    // Nueva excepción para manejar problemas en Firebase
    public static class ProcessingException extends RuntimeException {
        public ProcessingException(String message) {
            super(message);
        }
    }


    public static class EmailAlreadyExistsException extends RuntimeException {
        public EmailAlreadyExistsException(String message) {
            super(message);
        }
    }

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }

    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException(String message) {
            super(message);
        }
    }
    public static class AuthenticationException extends RuntimeException {
        public AuthenticationException(String message) {
            super(message);
        }
    }
    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) {
            super(message);
        }
    }
    public static class InvalidPasswordException extends InvalidCredentialsException{
        public InvalidPasswordException(String message){
            super(message);
        }
    }


}