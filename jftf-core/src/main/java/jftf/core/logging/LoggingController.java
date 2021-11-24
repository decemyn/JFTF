package jftf.core.logging;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LoggingController {
    private Logger LOGGER = null;
    private ch.qos.logback.classic.Logger rootLogger = null;
    private LoggingContextInformation currentLoggerContextInformation;
    private LoggerContext currentLoggerContext = null;
    private boolean faultMode = false;
    private final List<String> faultModeErrorMessages = new ArrayList<>();
    private static LoggingController loggerInstance;
    private static boolean internalLogs = false;

    private LoggingController(LoggingContextInformation currentLoggerContextInformation) {
        try {
            if(!Objects.equals(currentLoggerContextInformation.getApplicationID(), ""))
                this.LOGGER = LoggerFactory.getLogger(currentLoggerContextInformation.getApplicationID());
            else {
                this.LOGGER = LoggerFactory.getLogger(LoggingController.class.getName());
                currentLoggerContextInformation.setApplicationID(LoggingController.class.getName());
            }
            this.currentLoggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            this.rootLogger = this.currentLoggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
            this.currentLoggerContextInformation = currentLoggerContextInformation;
            this.shiftToAttachedContextInformation();
        }
        catch(Exception e){
            this.switchToFaultMode(e);
        }
    }

    public static LoggingController LoggerFactory(LoggingContextInformation currentLoggerContextInformation){
        if(loggerInstance == null)
            loggerInstance = new LoggingController(currentLoggerContextInformation);
        return loggerInstance;
    }

    public static LoggingController LoggerFactory(LoggingContextInformation currentLoggerContextInformation, boolean internalLogs){
        if(loggerInstance == null) {
            LoggingController.internalLogs = internalLogs;
            loggerInstance = new LoggingController(currentLoggerContextInformation);
        }
        return loggerInstance;
    }

    public static void disableInternalLogs(){
        internalLogs = false;
    }

    public  static void enableInternalLogs(){
        internalLogs = true;
    }

    public void LogDebug(String logMessage){
        if(!this.faultMode){
            this.LOGGER.debug(logMessage);
        }
    }

    public void LogInfo(String logMessage){
        if(!this.faultMode){
            this.LOGGER.info(logMessage);
        }
    }

    public void LogError(String logMessage){
        if(!this.faultMode){
            this.LOGGER.error(logMessage);
        }
    }

    public void LogToMinimumLogLevel(String logMessage){
        if(!this.faultMode){
            if(this.rootLogger.getLevel() == LoggingContextInformation.debugLogLevel)
                this.LogDebug(logMessage);
            else if(this.rootLogger.getLevel() == LoggingContextInformation.infoLogLevel)
                this.LogInfo(logMessage);
            if(this.rootLogger.getLevel() == LoggingContextInformation.errorLogLevel)
                this.LogError(logMessage);
        }
    }

    private void announceContextRegistration(){
        if(!this.faultMode){
            if(internalLogs)
                this.LogToMinimumLogLevel(String.format("Shifted to attached logging context for application ID: '%s' | Log level: '%s' | Appender type: '%s' (INTERNAL / IGNORE)'", this.currentLoggerContextInformation.getApplicationID(),this.currentLoggerContextInformation.getLogLevel().levelStr,this.currentLoggerContextInformation.getAppender()));
        }
    }

    private void announceNewContextRegistration(LoggingContextInformation newContextInformation){
        if(!this.faultMode){
            if(internalLogs)
                this.LogToMinimumLogLevel(String.format("Shifted to new logging context for application ID: '%s' | Log level: '%s' | Appender type: '%s' (INTERNAL / IGNORE)", this.currentLoggerContextInformation.getApplicationID(),newContextInformation.getLogLevel().levelStr,newContextInformation.getAppender()));
        }
    }

    private void switchToFaultMode(Exception e){
        this.faultMode = true;
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        this.faultModeErrorMessages.add(sw.toString());
    }

    private void printFaultErrorMessages(){
        if(this.faultMode)
            System.out.println(this.faultModeErrorMessages);
        else
            System.out.println("Logger not currently in fault mode!");
    }

    private void shiftToContextInformationLogLevel(){
        if(!this.faultMode){
            if(this.rootLogger.getLevel() != this.currentLoggerContextInformation.getLogLevel()) {
                if(internalLogs)
                    this.LogInfo(String.format("Shifting log level for logging context: '%s' --> '%s' (INTERNAL / IGNORE)", this.rootLogger.getLevel().levelStr, this.currentLoggerContextInformation.getLogLevel().levelStr));
                try {
                    this.rootLogger.setLevel(this.currentLoggerContextInformation.getLogLevel());
                }
                catch(Exception e){
                    this.switchToFaultMode(e);
                }
                if(internalLogs)
                    this.LogToMinimumLogLevel(String.format("Shifted to --> '%s' Logging level shift confirmation message! (INTERNAL / IGNORE)",this.rootLogger.getLevel().levelStr));
            }
        }
    }

    private void shiftToLogLevel(LoggingContextInformation newContextInformation){
        if (!this.faultMode) {
            if (this.rootLogger.getLevel() != newContextInformation.getLogLevel()) {
                if(internalLogs)
                    this.LogToMinimumLogLevel(String.format("Shifting log level for logging context: '%s' --> '%s' (INTERNAL / IGNORE)", this.rootLogger.getLevel().levelStr, newContextInformation.getLogLevel()));
                try {
                    this.rootLogger.setLevel(newContextInformation.getLogLevel());
                } catch (Exception e) {
                    this.switchToFaultMode(e);
                }
                if(internalLogs)
                    this.LogToMinimumLogLevel(String.format("Shifted to --> '%s' Logging level shift confirmation message! (INTERNAL / IGNORE)",this.rootLogger.getLevel().levelStr));
            }
        }
    }

    private void shiftToContextInformationAppender(){
        if(!this.faultMode){
            JoranConfigurator jc = new JoranConfigurator();
            jc.setContext(this.currentLoggerContext);
            this.currentLoggerContext.reset();
            InputStream is = LoggingController.class.getClassLoader().getResourceAsStream(this.currentLoggerContextInformation.getAppenderConfiguration());
            try {
                jc.doConfigure(is);
                this.announceContextRegistration();
            }
            catch (JoranException e){
                this.switchToFaultMode(e);
            }
        }
    }

    private void shiftToAppender(LoggingContextInformation newContextInformation){
        if(!this.faultMode){
            JoranConfigurator jc = new JoranConfigurator();
            jc.setContext(this.currentLoggerContext);
            this.currentLoggerContext.reset();
            InputStream is = LoggingController.class.getClassLoader().getResourceAsStream(newContextInformation.getAppenderConfiguration());
            try {
                jc.doConfigure(is);
                this.announceNewContextRegistration(newContextInformation);
            }
            catch (JoranException e){
                this.switchToFaultMode(e);
            }
        }
    }

    public void shiftToAttachedContextInformation(){
        if(!this.faultMode) {
            if(internalLogs)
                this.LogToMinimumLogLevel(String.format("Shifting to attached context information! Log level: '%s' | Appender type: '%s' (INTERNAL / IGNORE)",this.currentLoggerContextInformation.getLogLevel(),this.currentLoggerContextInformation.getAppender()));
            this.shiftToContextInformationAppender();
            this.shiftToContextInformationLogLevel();
        }
    }

    public void shiftToNewContextInformation(LoggingContextInformation newContextInformation){
        if(!this.faultMode) {
            if(internalLogs)
                this.LogToMinimumLogLevel(String.format("Shifting to new logging context information! Log level: '%s' | Appender type: '%s' (INTERNAL / IGNORE)",newContextInformation.getLogLevel(),newContextInformation.getAppender()));
            this.shiftToAppender(newContextInformation);
            this.shiftToLogLevel(newContextInformation);
        }
    }

    public void attachNewContextInformation(LoggingContextInformation newContextInformation){
        if(!this.faultMode) {
            newContextInformation.setApplicationID(this.currentLoggerContextInformation.getApplicationID());
            if(internalLogs)
                this.LogToMinimumLogLevel(String.format("New logging context information attached to logger! Log level: '%s' | Appender type: '%s' (INTERNAL / IGNORE)",newContextInformation.getLogLevel(),newContextInformation.getAppender()));
            this.currentLoggerContextInformation = newContextInformation;
        }
    }

}
