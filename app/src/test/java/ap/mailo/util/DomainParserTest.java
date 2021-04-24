package ap.mailo.util;

import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class DomainParserTest {

    @Parameters(name = "{index}: parseDomain({0}) = {1}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {"example@example.com", "example.com"}, {"example@example", "example"}, {"example\"@\"example@example.com", "example.com"}
        });
    }

    private String mail;
    private String domainExpected;

    public DomainParserTest(String mail, String domainExpected) {
        this.mail = mail;
        this.domainExpected = domainExpected;
    }

    @Test
    public void testParseDomain() {
        Assert.assertEquals(domainExpected, DomainParser.parseDomain(mail));
    }

}