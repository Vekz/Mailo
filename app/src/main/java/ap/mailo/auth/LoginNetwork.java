package ap.mailo.auth;

import java.io.InputStream;
import java.net.URL;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import ap.mailo.R;
import ap.mailo.util.DomainParser;
import ap.mailo.util.UserDomainHandler;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;

public class LoginNetwork {
    private LoginNetwork() {}

    public static Single<LoginResult> loginUser(String mail, String password) {
        return Single.fromCallable(() -> {
            try {
                // Get domain for user's mail
                String domain = DomainParser.parseDomain(mail);

                // Check ISPDB for MX server settings
                String url = "https://autoconfig.thunderbird.net/v1.1/" + domain;

                // Setup SAX Parser and our XML handler
                SAXParserFactory factory = SAXParserFactory.newInstance();

                // Protect from XXE attacks
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

                SAXParser saxParser = factory.newSAXParser();
                UserDomainHandler handler = new UserDomainHandler(mail, password, domain);

                // Then parse XML
                InputStream uri = new URL(url).openStream();
                saxParser.parse(uri, handler);

                LoggedInUser user = handler.getUser();

                Session session = Session.getInstance(user.getSMTPProperties(),
                        new jakarta.mail.Authenticator() {
                            @Override
                            protected PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(mail, password);
                            }
                        }
                );

                Transport transport = session.getTransport("smtp");
                transport.connect(user.getHostSMTP(), mail, password);
                transport.close();

                return new LoginResult(user);
            } catch (Exception e) {
                return new LoginResult(R.string.login_failed);
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }
}
