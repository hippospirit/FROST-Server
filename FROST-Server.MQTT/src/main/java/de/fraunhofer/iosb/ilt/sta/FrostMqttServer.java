/*
 * Copyright (C) 2016 Fraunhofer Institut IOSB, Fraunhoferstr. 1, D 76131
 * Karlsruhe, Germany.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.fraunhofer.iosb.ilt.sta;

import de.fraunhofer.iosb.ilt.sta.messagebus.MessageBusFactory;
import de.fraunhofer.iosb.ilt.sta.mqtt.MqttManager;
import de.fraunhofer.iosb.ilt.sta.persistence.PersistenceManagerFactory;
import de.fraunhofer.iosb.ilt.sta.settings.CoreSettings;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author scf
 */
public class FrostMqttServer {

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(FrostMqttServer.class);
    private static final String KEY_TEMP_PATH = "tempPath";
    private static final String CONFIG_FILE_NAME = "FrostMqtt.properties";
    private final CoreSettings coreSettings;

    public FrostMqttServer(CoreSettings coreSettings) {
        this.coreSettings = coreSettings;
    }

    public void start() {
        PersistenceManagerFactory.init(coreSettings);
        MessageBusFactory.init(coreSettings);
        MqttManager.init(coreSettings);
        MessageBusFactory.getMessageBus().addMessageListener(MqttManager.getInstance());
    }

    public void stop() {
        LOGGER.info("Shutting down threads...");
        MqttManager.shutdown();
        MessageBusFactory.getMessageBus().stop();
        try {
            Thread.sleep(3000L);
        } catch (InterruptedException ex) {
            LOGGER.debug("Rude wakeup?", ex);
        }
        LOGGER.info("Done shutting down threads.");
    }

    private static CoreSettings loadCoreSettings(String configFileName) throws IOException {
        FileInputStream input = new FileInputStream(configFileName);
        Properties properties = new Properties();
        properties.load(input);
        LOGGER.info("Read {} properties from {}.", properties.size(), configFileName);
        String serviceRootUri = URI.create(properties.getProperty(CoreSettings.TAG_SERVICE_ROOT_URL) + "/" + properties.getProperty(CoreSettings.TAG_API_VERSION)).normalize().toString();
        CoreSettings coreSettings = new CoreSettings(
                properties,
                serviceRootUri,
                properties.getProperty(KEY_TEMP_PATH, System.getProperty("java.io.tmpdir")));
        return coreSettings;
    }

    /**
     * @param args the command line arguments
     * @throws java.io.FileNotFoundException if the config file is not found.
     */
    public static void main(String[] args) throws IOException {
        String configFileName = CONFIG_FILE_NAME;
        if (args.length > 0) {
            configFileName = args[0];
        }
        CoreSettings coreSettings = loadCoreSettings(configFileName);
        FrostMqttServer server = new FrostMqttServer(coreSettings);
        server.start();

        try (BufferedReader input = new BufferedReader(new InputStreamReader(System.in, "UTF-8"))) {
            LOGGER.warn("Press Enter to exit.");
            input.read();
            LOGGER.warn("Exiting...");
            server.stop();
            System.exit(0);
        }
    }

}