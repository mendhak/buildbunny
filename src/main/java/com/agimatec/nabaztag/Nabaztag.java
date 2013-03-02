package com.agimatec.nabaztag;

import java.io.DataInputStream;
import java.net.URL;

/**
 * User: Simon Tiffert
 * Copyright: Agimatec GmbH 2008
 */
public class Nabaztag {
    // API helper
    private static final String API_URL = "http://api.nabaztag.com/vl/FR/api.jsp?sn=";
    private static final String TIME_TO_LIVE_BIT = "&ttlive=";
    private static final String TOKEN_BIT = "&token=";
    private static final String TEXT_BIT = "&tts=";
    private static final String VOICE_BIT = "&voice=";
    private static final String ACTION_BIT = "&action=";

    // Pre-defined ear commands
    public static final String EARS_HAPPY = "&posright=0&posleft=0";
    public static final String EARS_SAD = "&posright=10&posleft=10";
    public static final String EARS_SPECIAL = "&posright=5&posleft=5";

    // available voices
    public static final String VOICE_DE = "DE-Otto";
    public static final String VOICE_US_BILLYE = "US-Billye";
    public static final String VOICE_US_DARLEEN = "US-Darleen";
    public static final String VOICE_US_BETHANY = "US-Bethany";
    public static final String VOICE_US_LILIAN = "US-Lilian";
    public static final String VOICE_US_LIBERTY = "US-Liberty";

    // action definitions
    public static final String ACTION_SUPPORTED_VOICES = "9";
    public static final String ACTION_SELECTED_LANGUAGES = "11";

    // your rabbit id
    private String rabbitID;
    // your rabbit token
    private String token;
    // time to live of the text message
    private String timeToLive;
    // text to be spoken
    private String text;
    // ear position
    private String ears;
    // voice of the rabbit
    private String voice;
    // action to fetch data like supported voices or selected languages
    private String action;


    /**
     * This method sends the call to the Nabaztag
     */
    public void publish() {
        call(constructUrl());
    }

    /**
     * This methods constructs the url from the given fields
     *
     * @return complete URL to call the Nabaztag
     */
    public String constructUrl() {
        StringBuffer url = new StringBuffer();

        url.append(constructBaseUrl());

        if (ears != null) {
            url.append(ears);
        }
        if (text != null) {
            url.append(TEXT_BIT);
            url.append(text);
        }
        if (voice != null) {
            url.append(VOICE_BIT);
            url.append(voice);
        }
        if (action != null) {
            url.append(ACTION_BIT);
            url.append(action);
        }
        return url.toString();
    }

    /**
     * This method creates a base url, which is the same for all Nabaztag actions
     *
     * @return A base URL which could completed by the given fields in constructURL
     */
    public String constructBaseUrl() {
        StringBuffer baseUrl = new StringBuffer();
        baseUrl.append(API_URL);
        baseUrl.append(rabbitID);
        baseUrl.append(TOKEN_BIT);
        baseUrl.append(token);

        if (timeToLive == null) {
            timeToLive = "600";
        }

        baseUrl.append(TIME_TO_LIVE_BIT);
        baseUrl.append(timeToLive);

        return baseUrl.toString();
    }

    /**
     * This method makes the call to the Nabaztag with the given url. It also displays the result of the Nabaztag
     *
     * @param urlString A constructed URL with Nabaztag actions
     */
    private void call(String urlString) {
        URL url;

        debug("-- BEGIN PUBLISH --");
        try {
            url = new URL(urlString.replaceAll(" ", "%20"));
            debug("-- NABAZTAG PUBLISH: " + url.toString() + "--");
            DataInputStream dis = new DataInputStream(url.openStream());
            int i;
            while ((i = dis.read()) != -1) {
                System.out.print((char) i);
            }
            dis.close();
        } catch (Exception e) {
            error("-- PUBLISH URL EXCEPTION --", e);
        }
        debug("-- END PUBLISH --");
    }

    /**
     * This method is used for logging errors
     *
     * @param error     The error message
     * @param exception The thrown exception
     */
    private void error(String error, Exception exception) {
        System.err.println(error);
        if (exception != null) exception.printStackTrace();
    }

    /**
     * This method is used for logging
     *
     * @param message The message to be logged
     */
    private void debug(String message) {
        System.out.println(message);
    }

    public String getEars() {
        return ears;
    }

    public void setEars(String ears) {
        this.ears = ears;
    }

    public String getVoice() {
        return voice;
    }

    public void setVoice(String voice) {
        this.voice = voice;
    }

    /**
     * The id of the rabbit you wish to talk to
     */
    public void setRabbitID(String ID) {
        this.rabbitID = ID;
    }

    /**
     * The token that allows you to talk to bunnies
     */
    public void setToken(String token) {
        this.token = token;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTimeToLive() {
        return timeToLive;
    }

    public void setTimeToLive(String timeToLive) {
        this.timeToLive = timeToLive;
    }
}
