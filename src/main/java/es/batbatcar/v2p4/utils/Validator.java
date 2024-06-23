package es.batbatcar.v2p4.utils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class Validator {

    public static boolean isValidName(String name) {
        if (isNullOrEmpty(name)) {
            return false;
        }
        return name.length() >= 5 && Character.isUpperCase(name.charAt(0));
    }

    private static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static boolean isValidRoute(String route) {
        if (isNullOrEmpty(route)) {
            return false;
        }
        return route.matches("^[a-zA-Z]+\\s?-\\s?[a-zA-Z]+$");
    }

    public static boolean isValidPropietario(String propietario) {
        if (isNullOrEmpty(propietario)) {
            return false;
        }
        return propietario.matches("^[A-Z][a-zA-Z]*\\s[A-Z][a-zA-Z]*$");
    }

    public static boolean isPositiveInt(int number) {
        return number > 0;
    }

    public static boolean isPositiveFloat(float number) {
        return number > 0.0f;
    }

    public static boolean isValidDate(String date) {
        return parseDate(date, "yyyy-MM-dd") != null;
    }

    public static boolean isValidTime(String time) {
        return parseTime(time, "HH:mm") != null;
    }

    public static boolean isValidDateTime(String dateTime) {
        return parseDateTime(dateTime, "yyyy-MM-dd HH:mm") != null;
    }

    public static boolean isValidPlazasOfertadas(int plazas) {
        return plazas >= 1 && plazas <= 6;
    }

    private static LocalDate parseDate(String date, String pattern) {
        try {
            return LocalDate.parse(date, DateTimeFormatter.ofPattern(pattern));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static LocalTime parseTime(String time, String pattern) {
        try {
            return LocalTime.parse(time, DateTimeFormatter.ofPattern(pattern));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static LocalDate parseDateTime(String dateTime, String pattern) {
        try {
            return LocalDate.parse(dateTime, DateTimeFormatter.ofPattern(pattern));
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
