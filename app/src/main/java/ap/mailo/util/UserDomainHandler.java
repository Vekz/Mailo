package ap.mailo.util;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import ap.mailo.auth.LoggedInUser;

public class UserDomainHandler extends DefaultHandler {
    //Variables to create user object
    private final String mail;
    private final String password;
    private String hostSMTP;
    private String portSMTP;
    private String hostIMAP;
    private String portIMAP;
    private String socketTypeSMTP;
    private String socketTypeIMAP;
    private String shortDisplayName;

    //Variables to parse xmls
    private final String siteURL;
    private boolean domain;
    private boolean correctDomain;
    private boolean displayShortName;
    private boolean incIMAP;
    private boolean outSMTP;
    private boolean hostname;
    private boolean port;
    private boolean socketT;


    public UserDomainHandler(String username, String password, String siteURL){
        this.mail = username;
        this.password = password;
        this.siteURL = siteURL;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if(localName.equalsIgnoreCase("domain") ||
                qName.equalsIgnoreCase("domain")){
            domain = true;
        }
        else if(localName.equalsIgnoreCase("displayShortName") ||
                qName.equalsIgnoreCase("displayShortName")){
            displayShortName = true;
        }
        else if(localName.equalsIgnoreCase("incomingServer") ||
                qName.equalsIgnoreCase("incomingServer")){
            if(attributes.getValue("type").equalsIgnoreCase("IMAP")){
                incIMAP = true;
            }
        }
        else if(localName.equalsIgnoreCase("outgoingServer") ||
                qName.equalsIgnoreCase("outgoingServer")){
            if(attributes.getValue("type").equalsIgnoreCase("SMTP")){
                outSMTP = true;
            }
        }
        else if(localName.equalsIgnoreCase("hostname") ||
                qName.equalsIgnoreCase("hostname")){
            hostname = true;
        }
        else if(localName.equalsIgnoreCase("port") ||
                qName.equalsIgnoreCase("port")){
            port = true;
        }
        else if(localName.equalsIgnoreCase("socketType") ||
                qName.equalsIgnoreCase("socketType")){
            socketT = true;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if(localName.equalsIgnoreCase("incomingServer") ||
                qName.equalsIgnoreCase("incomingServer")){
            incIMAP = false;
        }
        if(localName.equalsIgnoreCase("domain") ||
                qName.equalsIgnoreCase("domain")){
            outSMTP = false;
        }
        if(localName.equalsIgnoreCase("emailProvider") ||
                qName.equalsIgnoreCase("emailProvider")){
            if(!correctDomain){
                throw new SAXException("Wrong domain!");
            }
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if(domain){
            domain = false;
            if(siteURL.equalsIgnoreCase(new String(ch,start,length))){
                correctDomain = true;
            }
        }
        if(correctDomain){
            if(displayShortName){
                displayShortName = false;
                shortDisplayName = new String(ch,start,length);
            }
            if(incIMAP){
                if(hostname){
                    hostname = false;
                    hostIMAP = new String(ch,start,length);
                }
                if(port){
                    port = false;
                    portIMAP = new String(ch,start,length);
                }
                if(socketT){
                    socketT = false;
                    socketTypeIMAP = new String(ch,start,length);
                }
            }
            if(outSMTP){
                if(hostname){
                    hostname = false;
                    hostSMTP = new String(ch,start,length);
                }
                if(port){
                    port = false;
                    portSMTP = new String(ch,start,length);
                }
                if(socketT){
                    socketT = false;
                    socketTypeSMTP = new String(ch,start,length);
                }
            }
        }
    }

    public LoggedInUser getUser(){
        return new LoggedInUser(mail, password, hostSMTP, portSMTP, socketTypeSMTP, hostIMAP, portIMAP, socketTypeIMAP, shortDisplayName);
    }
}
