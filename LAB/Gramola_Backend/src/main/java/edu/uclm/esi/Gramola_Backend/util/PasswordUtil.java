package edu.uclm.esi.Gramola_Backend.util;
/* hash de las pass CHECK
 */
public class PasswordUtil {

    public static String hash(String pwd) {
        return Integer.toHexString(pwd.hashCode());
    }

    
}
