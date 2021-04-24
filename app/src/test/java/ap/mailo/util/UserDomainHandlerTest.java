package ap.mailo.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import ap.mailo.auth.LoggedInUser;

@RunWith(Parameterized.class)
public class UserDomainHandlerTest {

    @Parameterized.Parameters(name = "{index}: getUser() from {0}.mail")
    public static Object[] data() {
        return new Object[] {
                new LoggedInUser("TestUsername@mail.org", "TestPassword",
                        "smtp.mail.com", "587", "STARTTLS", "imap.mail.com",
                        "143", "STARTTLS", "mail.com"),
                new LoggedInUser("ItDoesntMatter@gmail.com", "UnImportant",
                        "smtp.gmail.com", "465", "SSL", "imap.gmail.com",
                        "993", "SSL", "GMail")
        };
    }

    private LoggedInUser expectedUser;

    public UserDomainHandlerTest(LoggedInUser expectedUser) {
        this.expectedUser = expectedUser;
    }

    @Test
    public void testGetUser() throws ParserConfigurationException, SAXException, IOException {
        //Given
        String domain = DomainParser.parseDomain(expectedUser.getMail());

        // Check ISPDB for MX server settings
        String url = "https://autoconfig.thunderbird.net/v1.1/" + domain;

        // Setup SAX Parser and our XML handler
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser = factory.newSAXParser();
        UserDomainHandler handler = new UserDomainHandler(expectedUser.getMail(), expectedUser.getPassword(), domain);

        // Then parse XML
        InputStream uri = new URL(url).openStream();
        saxParser.parse(uri, handler);

        // Assert are the settings correctly pulled from ISPDB and added to user instance
        Assert.assertEquals(expectedUser, handler.getUser());
    }
}