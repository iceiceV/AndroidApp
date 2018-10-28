package com.ice.android.icedevice;

import java.util.regex.Pattern;

public class InputValidations {
    static boolean isValidUsername(String name) {
        boolean isValid = true;

        if (name.isEmpty() || name.length() > 16) {
            isValid = false;
        }

        return isValid;
    }

    static boolean isValidEmail(String email) {
        boolean isValid = true;

        String emailRegex = "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\\\x\\ö0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])";
        final Pattern EMAIL_ADDRESS_REGEX = Pattern.compile(emailRegex);

        if (email.isEmpty() || !EMAIL_ADDRESS_REGEX.matcher(email).matches()) {
            isValid = false;
        }

        return isValid;
    }

    static boolean isValidPassword(String password) {
        boolean isValid = true;

        if (password.isEmpty() || password.length() < 6) {
            isValid = false;
        }

        return isValid;
    }

    public boolean isValidGameName(String gameName) {
        boolean isValid = true;

        if (gameName.isEmpty() || gameName.length() > 16) {
            isValid = false;
        }

        return isValid;
    }
}
