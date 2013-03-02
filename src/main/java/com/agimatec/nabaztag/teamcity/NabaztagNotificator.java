package com.agimatec.nabaztag.teamcity;

import jetbrains.buildServer.serverSide.mute.MuteInfo;
import jetbrains.buildServer.tests.TestName;
import jetbrains.buildServer.vcs.VcsRoot;
import com.agimatec.nabaztag.Nabaztag;
import jetbrains.buildServer.Build;
import jetbrains.buildServer.BuildType;
import jetbrains.buildServer.notification.Notificator;
import jetbrains.buildServer.notification.NotificatorRegistry;
import jetbrains.buildServer.responsibility.ResponsibilityEntry;
import jetbrains.buildServer.responsibility.TestNameResponsibilityEntry;
import jetbrains.buildServer.serverSide.*;
//import jetbrains.buildServer.tests.SuiteTestName;
import jetbrains.*;
import jetbrains.buildServer.users.NotificatorPropertyKey;
import jetbrains.buildServer.users.PropertyKey;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;
import jetbrains.buildServer.vcs.VcsModification;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.Set;

/**
 * This teamcity plugins is configured as a new notificator. The notifications are text
 * messages, which are sended to the text to speech engine of the Nabaztag.
 * For each teamcity user it is possible to configure his Nabaztag settings and notification events. In this
 * way multiple Nabaztags could be accessed for different projects or developers.
 * The configuration could be found in the same place, where mail or ide notifications are configured.
 * <p/>
 * User: Simon Tiffert
 * Copyright: Viaboxx GmbH 2010
 * <p/>
 * Changes made by Daniel Wellman (dwellman@cyrusinnovation.com):
 * - Renamed the Rabbit ID display field to clearly indicate it's the serial number
 * - Fixed a spelling error in successful
 * - You can now specify a voice to use (e.g. UK-Penelope).  For a list of
 * voices, send your rabbit this command:
 * http://api.nabaztag.com/vl/FR/api.jsp?sn=MYSERIALNUMBER&token=MYTOKEN&action=9
 * <p/>
 * Changes made by Robert Moran:
 * - Exposed all build messages to the user for customisation
 * - Added #PROJECT#, #USER# and #COMMENT# placeholders which can be used
 * in the messages and are replaced with the project name, user or username of
 * the last person to make a change and their commit comments
 * <p/>
 * - if no voice is specified a random voice is picked
 * - now returns multiple users and comments for build
 * - replaces '#' in message as this truncates what nabaztag says
 * - added message length limit and text to add to end of truncated message
 */
public class NabaztagNotificator implements Notificator {

    private static final String TYPE = "nabaztagNotifier";
    private static final String TYPE_NAME = "Nabaztag Notifier";
    private static final String NABAZTAG_RABBIT_ID = "rabbitId";
    private static final String NABAZTAG_RABBIT_TOKEN = "rabbitToken";
    private static final String NABAZTAG_RABBIT_VOICE = "rabbitVoice";
    private static final String NABAZTAG_BUILD_STARTED = "buildStarted";
    private static final String NABAZTAG_BUILD_SUCCESSFUL = "buildSuccessful";
    private static final String NABAZTAG_BUILD_FAILED = "buildFailed";
    private static final String NABAZTAG_BUILD_START_FAILED = "buildStartFailed";
    private static final String NABAZTAG_BUILD_LABELING = "buildLabeling";
    private static final String NABAZTAG_BUILD_FAILING = "buildFailing";
    private static final String NABAZTAG_BUILD_HANGING = "buildHanging";
    private static final String NABAZTAG_BUILD_RESPONSIBLE_CHANGED = "buildResponsibleChanged";
    private static final String NABAZTAG_BUILD_RESPONSIBLE_ASSIGNED = "buildResponsibleAssigned";
    private static final String NABAZTAG_HASH_TEXT = "hashText";
    private static final String NABAZTAG_MAX_MESSAGE_LENGTH = "maxMessageLength";
    private static final String NABAZTAG_ELLIPSES = "ellipses";

    private static final PropertyKey RABBIT_ID = new NotificatorPropertyKey(TYPE, NABAZTAG_RABBIT_ID);
    private static final PropertyKey RABBIT_TOKEN = new NotificatorPropertyKey(TYPE, NABAZTAG_RABBIT_TOKEN);
    private static final PropertyKey RABBIT_VOICE = new NotificatorPropertyKey(TYPE, NABAZTAG_RABBIT_VOICE);
    private static final PropertyKey BUILD_STARTED = new NotificatorPropertyKey(TYPE, NABAZTAG_BUILD_STARTED);
    private static final PropertyKey BUILD_SUCCESSFUL = new NotificatorPropertyKey(TYPE, NABAZTAG_BUILD_SUCCESSFUL);
    private static final PropertyKey BUILD_FAILED = new NotificatorPropertyKey(TYPE, NABAZTAG_BUILD_FAILED);
    private static final PropertyKey BUILD_START_FAILED = new NotificatorPropertyKey(TYPE, NABAZTAG_BUILD_START_FAILED);
    private static final PropertyKey BUILD_LABELING = new NotificatorPropertyKey(TYPE, NABAZTAG_BUILD_LABELING);
    private static final PropertyKey BUILD_FAILING = new NotificatorPropertyKey(TYPE, NABAZTAG_BUILD_FAILING);
    private static final PropertyKey BUILD_HANGING = new NotificatorPropertyKey(TYPE, NABAZTAG_BUILD_HANGING);
    private static final PropertyKey BUILD_RESPONSIBLE_CHANGED = new NotificatorPropertyKey(TYPE, NABAZTAG_BUILD_RESPONSIBLE_CHANGED);
    private static final PropertyKey BUILD_RESPONSIBLE_ASSIGNED = new NotificatorPropertyKey(TYPE, NABAZTAG_BUILD_RESPONSIBLE_ASSIGNED);
    private static final PropertyKey HASH_TEXT = new NotificatorPropertyKey(TYPE, NABAZTAG_HASH_TEXT);
    private static final PropertyKey MAX_MESSAGE_LENGTH = new NotificatorPropertyKey(TYPE, NABAZTAG_MAX_MESSAGE_LENGTH);
    private static final PropertyKey ELLIPSES = new NotificatorPropertyKey(TYPE, NABAZTAG_ELLIPSES);

    private static final String DEFAULT_STARTED_MESSAGE = "Build #PROJECT# started.";
    private static final String DEFAULT_SUCCESSFUL_MESSAGE = "Build #PROJECT# successfull.";
    private static final String DEFAULT_FAILED_MESSAGE = "Build #PROJECT# failed.";
    private static final String DEFAULT_START_FAILED_MESSAGE = "Start Build #PROJECT# failed.";
    private static final String DEFAULT_LABELING_MESSAGE = "Labeling of build #PROJECT# failed.";
    private static final String DEFAULT_FAILING_MESSAGE = "Build #PROJECT# is failing.";
    private static final String DEFAULT_HANGING_MESSAGE = "Build #PROJECT# is probably hanging.";
    private static final String DEFAULT_RESPONSIBLE_CHANGED_MESSAGE = "Responsibility of build #PROJECT# changed.";
    private static final String DEFAULT_RESPONSIBLE_ASSIGNED_MESSAGE = "#USER# is assigned responsible for build #PROJECT#.";
    private static final String DEFAULT_HASH_TEXT = "number ";
    private static final String DEFAULT_ELLIPSES = ", et cetera.";

    public NabaztagNotificator(NotificatorRegistry notificatorRegistry) throws IOException {
        ArrayList<UserPropertyInfo> userProps = new ArrayList<UserPropertyInfo>();
        userProps.add(new UserPropertyInfo(NABAZTAG_RABBIT_ID, "Nabaztag Serial #"));
        userProps.add(new UserPropertyInfo(NABAZTAG_RABBIT_TOKEN, "Nabaztag Token"));
        userProps.add(new UserPropertyInfo(NABAZTAG_RABBIT_VOICE, "Nabaztag Voice"));
        userProps.add(new UserPropertyInfo(NABAZTAG_BUILD_STARTED, "Started Message"));
        userProps.add(new UserPropertyInfo(NABAZTAG_BUILD_SUCCESSFUL, "Success Message"));
        userProps.add(new UserPropertyInfo(NABAZTAG_BUILD_FAILED, "Failed Message"));
        userProps.add(new UserPropertyInfo(NABAZTAG_BUILD_START_FAILED, "Start Failed Message"));
        userProps.add(new UserPropertyInfo(NABAZTAG_BUILD_LABELING, "Labeling Message"));
        userProps.add(new UserPropertyInfo(NABAZTAG_BUILD_FAILING, "Failing Message"));
        userProps.add(new UserPropertyInfo(NABAZTAG_BUILD_HANGING, "Hanging Message"));
        userProps.add(new UserPropertyInfo(NABAZTAG_BUILD_RESPONSIBLE_CHANGED, "Changed Message"));
        userProps.add(new UserPropertyInfo(NABAZTAG_BUILD_RESPONSIBLE_ASSIGNED, "Responsible Message"));
        userProps.add(new UserPropertyInfo(NABAZTAG_HASH_TEXT, "'#' Replacement"));
        userProps.add(new UserPropertyInfo(NABAZTAG_MAX_MESSAGE_LENGTH, "Max Message Length"));
        userProps.add(new UserPropertyInfo(NABAZTAG_ELLIPSES, "Ellipses"));
        notificatorRegistry.register(this, userProps);
    }



    public void doNotification(SUser notifyUser, PropertyKey messageKey, String messageDefault, String projectName, String userName, String comment, String rabbitEars) {
        String message = notifyUser.getPropertyValue(messageKey);
        if (message == null || message.equals("")) {
            message = messageDefault;
        }

        message = replaceText(message, "#PROJECT#", projectName);
        message = replaceText(message, "#USER#", userName);
        message = replaceText(message, "#COMMENT#", comment);

        // Bunny stops talking when it encounters a #
        String hashText = notifyUser.getPropertyValue(HASH_TEXT);
        if (hashText == null || hashText.equals("")) {
            hashText = DEFAULT_HASH_TEXT;
        }
        message = replaceText(message, "#", hashText);

        // Truncate long commit messages
        String maxLengthText = notifyUser.getPropertyValue(MAX_MESSAGE_LENGTH);
        if (maxLengthText != null) {
            try {
                int maxLength = Integer.parseInt(maxLengthText);
                if (message.length() > maxLength) {
                    String ellipses = notifyUser.getPropertyValue(ELLIPSES);
                    if (ellipses == null || hashText.equals("")) {
                        ellipses = DEFAULT_ELLIPSES;
                    }
                    message = message.substring(0, maxLength).concat(ellipses);
                }
            }
            catch (Exception ignored) {
            }
        }

        Nabaztag nabaztag = new Nabaztag();

        nabaztag.setRabbitID(notifyUser.getPropertyValue(RABBIT_ID));
        nabaztag.setToken(notifyUser.getPropertyValue(RABBIT_TOKEN));
        nabaztag.setText(message);

        String voice = notifyUser.getPropertyValue(RABBIT_VOICE);
        if (voice != null && !voice.equals("")) {
            nabaztag.setVoice(voice);
        } else {
            nabaztag.setVoice(getRandomVoice());
        }

        if (rabbitEars != null) {
            nabaztag.setEars(rabbitEars);
        }

        nabaztag.publish();
    }

    private String replaceText(String source, String find, String replace) {
        String result = source;

        if (replace != null) {
            result = result.replaceAll(find, replace);
        }

        return result;
    }

    private String getUserNames(Build build) {
        // This returns the user's full name when a build is triggered from the team city interface
        try {
            if (build instanceof SBuild) {
                SBuild sBuild = (SBuild) build;
                if (sBuild.getTriggeredBy().isTriggeredByUser()) {
                    return sBuild.getTriggeredBy().getUser().getName();
                }
            }
        }
        catch (Exception ignored) {
        }

        StringBuffer buffer = new StringBuffer();
        String delimiter = ", ";

        // This gets the users full names if they are set in the user settings area
        if (build.getCommitters(SelectPrevBuildPolicy.SINCE_LAST_BUILD) != null) {
            for (User user : build.getCommitters(SelectPrevBuildPolicy.SINCE_LAST_BUILD).getUsers()) {
                if (!user.getName().equals("")) {
                    buffer.append(delimiter);
                    buffer.append(user.getName());
                }
            }
        }

        // If all else fails, this will get the usernames
        if (buffer.length() == 0) {
            for (VcsModification modification : build.getContainingChanges()) {
                if (!modification.getUserName().equals("")) {
                    buffer.append(delimiter);
                    buffer.append(modification.getUserName());
                }
            }
        }

        String result = buffer.toString();

        // Trim the first delimiter
        if (result.length() > delimiter.length()) {
            result = result.substring(delimiter.length());
        }

        return result;
    }

    private String getComments(Build build) {
        StringBuffer buffer = new StringBuffer();
        String delimiter = ". ";

        for (VcsModification modification : build.getContainingChanges()) {
            if (!modification.getDescription().equals("")) {
                buffer.append(delimiter);
                buffer.append(modification.getDescription());
            }
        }

        String result = buffer.toString();

        // Trim the first delimiter
        if (result.length() > delimiter.length()) {
            result = result.substring(delimiter.length());
        }

        return result;
    }

    private String getRandomVoice() {
        String[] voices = new String[]{"AU-Colleen", "AU-Jon", "UK-Edwin", "UK-Leonard", "UK-Mistermuggles", "UK-Penelope", "UK-Rachel", "UK-Shirley", "US-Bethany", "US-Billye", "US-Clarence", "US-Darleen", "US-Ernest", "US-Liberty", "US-Lilian"};
        int index = new Random().nextInt(voices.length);
        return voices[index];
    }

    @Override
    public void notifyBuildStarted(@org.jetbrains.annotations.NotNull SRunningBuild sRunningBuild, @org.jetbrains.annotations.NotNull Set<SUser> sUsers)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }



    @Override
    public void notifyBuildSuccessful(@org.jetbrains.annotations.NotNull SRunningBuild sRunningBuild, @org.jetbrains.annotations.NotNull Set<SUser> sUsers)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void notifyBuildFailed(@org.jetbrains.annotations.NotNull SRunningBuild sRunningBuild, @org.jetbrains.annotations.NotNull Set<SUser> sUsers)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void notifyBuildFailedToStart(@org.jetbrains.annotations.NotNull SRunningBuild sRunningBuild, @org.jetbrains.annotations.NotNull Set<SUser> sUsers)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void notifyLabelingFailed(@org.jetbrains.annotations.NotNull Build build, @org.jetbrains.annotations.NotNull VcsRoot vcsRoot, @org.jetbrains.annotations.NotNull Throwable throwable, @org.jetbrains.annotations.NotNull Set<SUser> sUsers)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void notifyBuildFailing(@org.jetbrains.annotations.NotNull SRunningBuild sRunningBuild, @org.jetbrains.annotations.NotNull Set<SUser> sUsers)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void notifyBuildProbablyHanging(@org.jetbrains.annotations.NotNull SRunningBuild sRunningBuild, @org.jetbrains.annotations.NotNull Set<SUser> sUsers)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void notifyResponsibleChanged(@org.jetbrains.annotations.NotNull SBuildType sBuildType, @org.jetbrains.annotations.NotNull Set<SUser> sUsers)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void notifyResponsibleAssigned(@org.jetbrains.annotations.NotNull SBuildType sBuildType, @org.jetbrains.annotations.NotNull Set<SUser> sUsers)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void notifyResponsibleChanged(@org.jetbrains.annotations.Nullable TestNameResponsibilityEntry testNameResponsibilityEntry, @org.jetbrains.annotations.NotNull TestNameResponsibilityEntry testNameResponsibilityEntry2, @org.jetbrains.annotations.NotNull SProject sProject, @org.jetbrains.annotations.NotNull Set<SUser> sUsers)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void notifyResponsibleAssigned(@org.jetbrains.annotations.Nullable TestNameResponsibilityEntry testNameResponsibilityEntry, @org.jetbrains.annotations.NotNull TestNameResponsibilityEntry testNameResponsibilityEntry2, @org.jetbrains.annotations.NotNull SProject sProject, @org.jetbrains.annotations.NotNull Set<SUser> sUsers)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void notifyResponsibleChanged(@org.jetbrains.annotations.NotNull Collection<TestName> testNames, @org.jetbrains.annotations.NotNull ResponsibilityEntry responsibilityEntry, @org.jetbrains.annotations.NotNull SProject sProject, @org.jetbrains.annotations.NotNull Set<SUser> sUsers)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void notifyResponsibleAssigned(@org.jetbrains.annotations.NotNull Collection<TestName> testNames, @org.jetbrains.annotations.NotNull ResponsibilityEntry responsibilityEntry, @org.jetbrains.annotations.NotNull SProject sProject, @org.jetbrains.annotations.NotNull Set<SUser> sUsers)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void notifyTestsMuted(@org.jetbrains.annotations.NotNull Collection<STest> sTests, @org.jetbrains.annotations.NotNull MuteInfo muteInfo, @org.jetbrains.annotations.NotNull Set<SUser> sUsers)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void notifyTestsUnmuted(@org.jetbrains.annotations.NotNull Collection<STest> sTests, @org.jetbrains.annotations.NotNull MuteInfo muteInfo, @org.jetbrains.annotations.Nullable SUser sUser, @org.jetbrains.annotations.NotNull Set<SUser> sUsers)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getNotificatorType()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getDisplayName()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
