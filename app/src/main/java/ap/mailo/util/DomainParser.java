package ap.mailo.util;

public class DomainParser {

    private DomainParser() {}

    public static String parseDomain(String email){
        return email.substring(email.lastIndexOf("@") + 1);
    }
}
