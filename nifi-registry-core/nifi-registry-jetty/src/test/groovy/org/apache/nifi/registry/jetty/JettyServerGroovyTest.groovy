package org.apache.nifi.registry.jetty

import org.apache.nifi.registry.properties.NiFiRegistryProperties
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.eclipse.jetty.server.Server

@RunWith(MockitoJUnitRunner.class)
class JettyServerGroovyTest extends GroovyTestCase {

    private static final Logger logger = LoggerFactory.getLogger(JettyServerGroovyTest.class)

    private static final keyPassword = "keyPassword"
    private static final keystorePassword = "keystorePassword"
    private static final truststorePassword = "truststorePassword"
    private static final matchingPassword = "thePassword"

    @Test
    void testCreateSslContextFactoryWithKeystoreAndKeypassword() throws Exception {

        // Arrange
        NiFiRegistryProperties properties = new NiFiRegistryProperties()
        properties.setProperty(NiFiRegistryProperties.SECURITY_TRUSTSTORE, "src/test/resources/truststore.jks")
        properties.setProperty(NiFiRegistryProperties.SECURITY_TRUSTSTORE_PASSWD, truststorePassword)
        properties.setProperty(NiFiRegistryProperties.SECURITY_TRUSTSTORE_TYPE, "JKS")
        properties.setProperty(NiFiRegistryProperties.SECURITY_KEYSTORE, "src/test/resources/keystoreDifferentPasswords.jks")
        properties.setProperty(NiFiRegistryProperties.SECURITY_KEY_PASSWD, keyPassword)
        properties.setProperty(NiFiRegistryProperties.SECURITY_KEYSTORE_PASSWD, keystorePassword)
        properties.setProperty(NiFiRegistryProperties.SECURITY_KEYSTORE_TYPE, "JKS")

        Server internalServer = new Server()
        JettyServer testServer = new JettyServer(internalServer, properties)

        // Act
        SslContextFactory sslContextFactory = testServer.createSslContextFactory()
        sslContextFactory.start()

        // Assert
        assertNotNull(sslContextFactory)
        assertNotNull(sslContextFactory.getSslContext())
    }

    @Test
    void testCreateSslContextFactoryWithOnlyKeystorePassword() throws Exception {

        // Arrange
        NiFiRegistryProperties properties = new NiFiRegistryProperties()
        properties.setProperty(NiFiRegistryProperties.SECURITY_TRUSTSTORE, "src/test/resources/truststore.jks")
        properties.setProperty(NiFiRegistryProperties.SECURITY_TRUSTSTORE_PASSWD, truststorePassword)
        properties.setProperty(NiFiRegistryProperties.SECURITY_TRUSTSTORE_TYPE, "JKS")
        properties.setProperty(NiFiRegistryProperties.SECURITY_KEYSTORE, "src/test/resources/keystoreSamePassword.jks")
        properties.setProperty(NiFiRegistryProperties.SECURITY_KEYSTORE_PASSWD, matchingPassword)
        properties.setProperty(NiFiRegistryProperties.SECURITY_KEYSTORE_TYPE, "JKS")

        Server internalServer = new Server()
        JettyServer testServer = new JettyServer(internalServer, properties)

        // Act
        SslContextFactory sslContextFactory = testServer.createSslContextFactory()
        sslContextFactory.start()

        // Assert
        assertNotNull(sslContextFactory)
        assertNotNull(sslContextFactory.getSslContext())
    }

    @Test
    void testCreateSslContextFactoryWithMatchingPasswordsDefined() throws Exception {

        // Arrange
        NiFiRegistryProperties properties = new NiFiRegistryProperties()
        properties.setProperty(NiFiRegistryProperties.SECURITY_TRUSTSTORE, "src/test/resources/truststore.jks")
        properties.setProperty(NiFiRegistryProperties.SECURITY_TRUSTSTORE_PASSWD, truststorePassword)
        properties.setProperty(NiFiRegistryProperties.SECURITY_TRUSTSTORE_TYPE, "JKS")
        properties.setProperty(NiFiRegistryProperties.SECURITY_KEYSTORE, "src/test/resources/keystoreSamePassword.jks")
        properties.setProperty(NiFiRegistryProperties.SECURITY_KEY_PASSWD, matchingPassword)
        properties.setProperty(NiFiRegistryProperties.SECURITY_KEYSTORE_PASSWD, matchingPassword)
        properties.setProperty(NiFiRegistryProperties.SECURITY_KEYSTORE_TYPE, "JKS")

        Server internalServer = new Server()
        JettyServer testServer = new JettyServer(internalServer, properties)

        // Act
        SslContextFactory sslContextFactory = testServer.createSslContextFactory()
        sslContextFactory.start()

        // Assert
        assertNotNull(sslContextFactory)
        assertNotNull(sslContextFactory.getSslContext())
    }

    @Rule public ExpectedException exception = ExpectedException.none()

    @Test
    void testCreateSslContextFactoryWithNoKeystorePasswordFails() throws Exception {

        // Arrange
        exception.expect(IllegalArgumentException.class)
        exception.expectMessage("The keystore password cannot be null or empty")

        NiFiRegistryProperties properties = new NiFiRegistryProperties()
        properties.setProperty(NiFiRegistryProperties.SECURITY_TRUSTSTORE, "src/test/resources/truststore.jks")
        properties.setProperty(NiFiRegistryProperties.SECURITY_TRUSTSTORE_PASSWD, truststorePassword)
        properties.setProperty(NiFiRegistryProperties.SECURITY_TRUSTSTORE_TYPE, "JKS")
        properties.setProperty(NiFiRegistryProperties.SECURITY_KEYSTORE, "src/test/resources/keystoreSamePassword.jks")
        properties.setProperty(NiFiRegistryProperties.SECURITY_KEYSTORE_TYPE, "JKS")

        Server internalServer = new Server()
        JettyServer testServer = new JettyServer(internalServer, properties)

        // Act but expect exception
        SslContextFactory sslContextFactory = testServer.createSslContextFactory()
    }
}
