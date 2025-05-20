package com.sp.schedulerplatform.utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {
    public static boolean isStrongPassword(String password){
       if (password==null || password.length()<8){
           return false;
       }
       boolean hasUpper=false;
       boolean hasLower=false;
       boolean hasDigit=false;


       for ( char ch:password.toCharArray()){
           if (Character.isUpperCase(ch)){
               hasUpper=true;
           } else if (Character.isDigit(ch)) {
               hasDigit=true;


           } else if (Character.isLowerCase(ch)) {
               hasLower=true;

           }

       }
        return hasUpper && hasLower && hasDigit;
    }


    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("password must not be null or empty");
        }
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    public static boolean checkPassword(String password, String hashedPassword) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("password must not be null or empty");
        }
        if (hashedPassword == null || hashedPassword.isEmpty()) {
            throw new IllegalArgumentException("hashed password must not be null or empty");
        }
        return BCrypt.checkpw(password, hashedPassword);
    }
}
