package com.agimatec.nabaztag.test;

import com.agimatec.nabaztag.Nabaztag;
import junit.framework.TestCase;

import java.io.IOException;

/**
 * User: Simon Tiffert
 * Copyright: Agimatec GmbH 2008 
 */
public class NabaztagTest extends TestCase {
    private final String RABBIT_ID;
    private final String TOKEN;

    public NabaztagTest() throws IOException {
        RABBIT_ID = "12345";
        TOKEN = "54321";
    }

    public void testBaseUrl() {
        Nabaztag nabaztag = new Nabaztag();
        nabaztag.setRabbitID(RABBIT_ID);
        nabaztag.setToken(TOKEN);
        String url = nabaztag.constructBaseUrl();
        assertEquals("http://api.nabaztag.com/vl/FR/api.jsp?sn="+RABBIT_ID+"&token="+TOKEN+"&ttlive=600", url);
        System.out.println("BaseUrl: " + url);
    }

    public void testActionUrl() {
        Nabaztag nabaztag = new Nabaztag();
        nabaztag.setRabbitID(RABBIT_ID);
        nabaztag.setToken(TOKEN);
        nabaztag.setAction(Nabaztag.ACTION_SUPPORTED_VOICES);
        String url = nabaztag.constructUrl();
        assertEquals(nabaztag.constructBaseUrl()+"&action=9",url);
        System.out.println("ActionUrl: " + url);
    }

    public void testEventUrlVoice() {
        Nabaztag nabaztag = new Nabaztag();
        nabaztag.setRabbitID(RABBIT_ID);
        nabaztag.setToken(TOKEN);
        nabaztag.setVoice(Nabaztag.VOICE_DE);
        String url = nabaztag.constructUrl();
        assertEquals(nabaztag.constructBaseUrl()+"&voice=DE-Otto",url);
    }

    public void testEventUrlText() {
        Nabaztag nabaztag = new Nabaztag();
        nabaztag.setRabbitID(RABBIT_ID);
        nabaztag.setToken(TOKEN);
        nabaztag.setText("Ich bin ein Hase");
        String url = nabaztag.constructUrl();
        assertEquals(nabaztag.constructBaseUrl()+"&tts=Ich bin ein Hase",url);
    }

    public void testEventUrlEars() {
        Nabaztag nabaztag = new Nabaztag();
        nabaztag.setRabbitID(RABBIT_ID);
        nabaztag.setToken(TOKEN);
        nabaztag.setEars(Nabaztag.EARS_HAPPY);
        String url = nabaztag.constructUrl();
        assertEquals(nabaztag.constructBaseUrl()+"&posright=0&posleft=0",url);
    }

    public void testEventUrl() {
        Nabaztag nabaztag = new Nabaztag();
        nabaztag.setRabbitID(RABBIT_ID);
        nabaztag.setToken(TOKEN);
        nabaztag.setVoice(Nabaztag.VOICE_DE);
        nabaztag.setEars(Nabaztag.EARS_HAPPY);
        nabaztag.setText("Ich bin ein Hase");
        String url = nabaztag.constructUrl();
        assertEquals(nabaztag.constructBaseUrl()+"&posright=0&posleft=0"+"&tts=Ich bin ein Hase"+"&voice=DE-Otto",url);

    }

    public void testTimeToLive() {
        Nabaztag nabaztag = new Nabaztag();
        nabaztag.setRabbitID(RABBIT_ID);
        nabaztag.setToken(TOKEN);
        nabaztag.setTimeToLive(""+(800));
        String url = nabaztag.constructBaseUrl();
        assertEquals("http://api.nabaztag.com/vl/FR/api.jsp?sn="+RABBIT_ID+"&token="+TOKEN+"&ttlive=800", url);
    }

//    Used for real testing
//    
//    public void testNabaztagCall() {
//        Nabaztag nabaztag = new Nabaztag();
//        nabaztag.setRabbitID(RABBIT_ID);
//        nabaztag.setToken(TOKEN);
//        nabaztag.setAction(Nabaztag.ACTION_SUPPORTED_VOICES);
//        nabaztag.publish();
//    }
//
//    public void testNabatagEvent() {
//        Nabaztag nabaztag = new Nabaztag();
//        nabaztag.setRabbitID(RABBIT_ID);
//        nabaztag.setToken(TOKEN);
//        nabaztag.setVoice("UK-Shirley");
//        nabaztag.setText("Build Agimatec Ostium successful.");
//        nabaztag.publish();
//    }
}
