package ap.mailo.util;

public class DomainParser {
    public static String parseDomain(String email){
        return email.substring(email.lastIndexOf("@") + 1);
    }
}
