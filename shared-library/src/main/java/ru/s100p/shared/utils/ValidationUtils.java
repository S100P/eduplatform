package ru.s100p.shared.utils;

public final class ValidationUtils {
    
    private ValidationUtils() {}
    
    public static boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@" +
                           "(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email != null && email.matches(emailRegex);
    }
    
    public static boolean isValidPassword(String password) {
        // Минимум 8 символов, хотя бы одна цифра и одна буква
        return password != null && 
               password.length() >= 8 && 
               password.matches(".*\\d.*") && 
               password.matches(".*[a-zA-Z].*");
    }
    
    public static String sanitizeString(String input) {
        return input != null ? input.trim().replaceAll("<[^>]*>", "") : null;
    }
}