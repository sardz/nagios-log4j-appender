package edu.utah.logging.log4j

import com.googlecode.jsendnsca.Level
import com.googlecode.jsendnsca.MessagePayload
import com.googlecode.jsendnsca.NagiosPassiveCheckSender
import com.googlecode.jsendnsca.builders.MessagePayloadBuilder
import com.googlecode.jsendnsca.builders.NagiosSettingsBuilder
import com.googlecode.jsendnsca.encryption.Encryption
import org.apache.logging.log4j.core.Appender
import org.apache.logging.log4j.core.Core
import org.apache.logging.log4j.core.Filter
import org.apache.logging.log4j.core.Layout
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.plugins.Plugin
import org.apache.logging.log4j.core.config.plugins.PluginAttribute
import org.apache.logging.log4j.core.config.plugins.PluginElement
import org.apache.logging.log4j.core.config.plugins.PluginFactory
import org.apache.logging.log4j.core.layout.PatternLayout
import com.googlecode.jsendnsca.NagiosSettings

@Plugin(
        name = 'NagiosAppender',
        category = Core.CATEGORY_NAME,
        elementType = Appender.ELEMENT_TYPE,
        printObject = true
)
class NagiosAppender extends AbstractAppender {

    String nagiosHost
    int port = 5667
    String hostname
    String serviceName
    String password

    protected NagiosAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions, String nagiosHost, int port, String password, String hostname, String serviceName) {
        super(name, filter, layout, ignoreExceptions, null)
        this.nagiosHost = nagiosHost
        this.port = port
        this.hostname = hostname
        this.serviceName = serviceName
        this.password = password
    }

    @PluginFactory
    static NagiosAppender createAppender(
            @PluginAttribute('name') String name,
            @PluginElement('Layout') Layout<? extends Serializable> layout,
            @PluginElement('Filter') Filter filter,
            @PluginAttribute('nagiosHost') String nagiosHost,
            @PluginAttribute('port') int port,
            @PluginAttribute('password') String password,
            @PluginAttribute('hostname') String hostname,
            @PluginAttribute('serviceName') String serviceName
    ) {
        if (name == null) {
            LOGGER.error("NagiosAppender does not include 'name' attribute.")
            return null
        }
        if (nagiosHost == null) {
            LOGGER.error("NagiosAppender does not include 'nagiosHost' attribute.")
            return null
        }
        if (hostname == null) {
            LOGGER.error("NagiosAppender does not include 'hostName' attribute.")
            return null
        }
        if (password == null) {
            LOGGER.error("NagiosAppender does not include 'password' attribute.")
            return null
        }
        if (serviceName == null) {
            LOGGER.error("NagiosAppender does not include 'serviceName' attribute.")
            return null
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout()
        }
        return new NagiosAppender(name, filter, layout, true, nagiosHost, port, password, hostname, serviceName)
    }

    @Override
    void append(LogEvent event) {

        def level = resolveNagiosLevel(event)

        if (level == null) return

        NagiosSettings settings = new NagiosSettingsBuilder()
        .withNagiosHost(nagiosHost)
        .withPort(port)
        .withPassword(password)
        .withEncryption(Encryption.XOR)
        .create()

        MessagePayload payload = new MessagePayloadBuilder()
        .withHostname(hostname)
        .withLevel(level)
        .withServiceName(serviceName)
        .withMessage(event.message.formattedMessage)
        .create()

        NagiosPassiveCheckSender sender = new NagiosPassiveCheckSender(settings)

        try {
            sender.send(payload)
        } catch (Exception e) {
            e.printStackTrace()
        }

    }

    private static Level resolveNagiosLevel(LogEvent event) {
        switch (event.level) {
            case org.apache.logging.log4j.Level.DEBUG:
                return Level.UNKNOWN
            case org.apache.logging.log4j.Level.INFO:
                return Level.OK
            case org.apache.logging.log4j.Level.WARN:
                return Level.WARNING
            case org.apache.logging.log4j.Level.ERROR:
                return Level.CRITICAL
            case org.apache.logging.log4j.Level.FATAL:
                return Level.CRITICAL
            default:
                return null
        }
    }

}
