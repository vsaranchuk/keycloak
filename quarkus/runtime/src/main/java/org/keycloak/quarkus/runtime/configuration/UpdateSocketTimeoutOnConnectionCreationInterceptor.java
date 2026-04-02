package org.keycloak.quarkus.runtime.configuration;

import io.agroal.api.AgroalPoolInterceptor;

import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Restores the {@code socketTimeout} of a MySQL/TiDB connection to its original value after the connection
 * has been successfully created. During connection creation, {@code socketTimeout} is temporarily overridden
 * with a tighter value to prevent the creation thread from hanging indefinitely.
 * <p>
 * See {@link org.keycloak.quarkus.runtime.configuration.JdbcUrlInterceptor} for the logic responsible
 * for overriding the {@code socketTimeout} during the connection creation phase.
 *
 * @see <a href="https://github.com/keycloak/keycloak/issues/42256">DB Connection Pool acquisition timeout errors on database failover</a>
 * @see <a href="https://github.com/keycloak/keycloak/issues/47174">MySQL/TiDB: Configure DB socket timeouts on the fly during connection creation phase</a>
 * @see <a href="https://github.com/keycloak/keycloak/issues/47140">Add CLI option for database connection timeout and provide it into quarkus.datasource.jdbc.login-timeout</a>
 */
public class UpdateSocketTimeoutOnConnectionCreationInterceptor extends SocketTimeoutInterceptor implements AgroalPoolInterceptor {

    private final Executor executor;

    public UpdateSocketTimeoutOnConnectionCreationInterceptor() {
        this.executor = Executors.newCachedThreadPool();
    }

    @Override
    public void onConnectionCreate(Connection connection) {
        if (isSupported) {
            String effectiveTimeout =
                    StringUtils.firstNonBlank(dbUrlSocketTimeout, dbUrlPropertiesSocketTimeout);

            try {
                connection.setNetworkTimeout(executor,
                        StringUtils.isNotBlank(effectiveTimeout) ? Integer.parseInt(effectiveTimeout) : 0);
            } catch (SQLException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }
}
