/*-
 * ========================LICENSE_START=================================
 * flyway-database-oceanbase
 * ========================================================================
 * Copyright (C) 2010 - 2024 Red Gate Software Ltd
 * ========================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
/*
 * Copyright (C) Red Gate Software Ltd 2010-2023
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flywaydb.community.database.mysql.oceanbase;

import org.flywaydb.core.api.ResourceProvider;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.extensibility.Tier;
import org.flywaydb.core.internal.database.base.BaseDatabaseType;
import org.flywaydb.core.internal.database.base.Database;
import org.flywaydb.core.internal.jdbc.JdbcConnectionFactory;
import org.flywaydb.core.internal.jdbc.StatementInterceptor;
import org.flywaydb.core.internal.license.FlywayEditionUpgradeRequiredException;
import org.flywaydb.core.internal.parser.Parser;
import org.flywaydb.core.internal.parser.ParsingContext;
import org.flywaydb.core.internal.util.ClassUtils;

import java.sql.Connection;
import java.sql.Types;
import java.util.List;
import java.util.Properties;

public class OceanBaseDatabaseType extends BaseDatabaseType {

    public static final String JDBC_URL_PREFIX = "jdbc:oceanbase:";
    public static final String DRIVER_CLASS = "com.oceanbase.jdbc.Driver";
    public static final String LEGACY_DRIVER_CLASS = "com.alipay.oceanbase.jdbc.Driver";


    @Override
    public int getPriority() {
        // OceanBase needs to be checked in advance of MySql
        return 1;
    }

    @Override
    public String getName() {
        return "OceanBase";
    }

    @Override
    public List<String> getSupportedEngines() {
        //TODO - move TiDB to community plugin once this API is public
        return List.of(getName(), "PerconaXtraDbCluster", "TiDb", "AuroraMySql");
    }

    @Override
    public int getNullType() {
        return Types.VARCHAR;
    }

    @Override
    public boolean handlesJDBCUrl(String url) {
        if (url.startsWith(JDBC_URL_PREFIX)) {
            return true;
        }
        if (url.startsWith("jdbc-secretsmanager:mysql:")) {

            throw new FlywayEditionUpgradeRequiredException(Tier.ENTERPRISE, (Tier) null, "jdbc-secretsmanager");

        }
        return url.startsWith("jdbc:mysql:") || url.startsWith("jdbc:google:") ||
                url.startsWith("jdbc:p6spy:mysql:") || url.startsWith("jdbc:p6spy:google:");
    }

    @Override
    public String getDriverClass(String url, ClassLoader classLoader) {

        if (url.startsWith(JDBC_URL_PREFIX)) {
            return DRIVER_CLASS;
        }
        if (url.startsWith("jdbc:p6spy:mysql:") || url.startsWith("jdbc:p6spy:google:")) {
            return "com.p6spy.engine.spy.P6SpyDriver";
        }
        if (url.startsWith("jdbc:mysql:")) {
            return "com.mysql.cj.jdbc.Driver";
        } else {
            return "com.mysql.jdbc.GoogleDriver";
        }
    }

    @Override
    public String getBackupDriverClass(String url, ClassLoader classLoader) {
        if (ClassUtils.isPresent(LEGACY_DRIVER_CLASS, classLoader)) {
            return LEGACY_DRIVER_CLASS;
        }
        return super.getBackupDriverClass(url, classLoader);
    }

    @Override
    public boolean handlesDatabaseProductNameAndVersion(String databaseProductName, String databaseProductVersion, Connection connection) {
        // Google Cloud SQL returns different names depending on the environment and the SDK version.
        //   ex.: Google SQL Service/MySQL
        return databaseProductName.contains(getName());
    }

    @Override
    public Database createDatabase(Configuration configuration, JdbcConnectionFactory jdbcConnectionFactory, StatementInterceptor statementInterceptor) {
        return new OceanBaseDatabase(configuration, jdbcConnectionFactory, statementInterceptor);
    }

    @Override
    public Parser createParser(Configuration configuration, ResourceProvider resourceProvider, ParsingContext parsingContext) {
        return new OceanBaseParser(configuration, parsingContext);
    }

    @Override
    public void setDefaultConnectionProps(String url, Properties props, ClassLoader classLoader) {
        props.put("connectionAttributes", "program_name:" + APPLICATION_NAME);
    }

    @Override
    public boolean detectPasswordRequiredByUrl(String url) {
        return super.detectPasswordRequiredByUrl(url);
    }

    @Override
    public boolean externalAuthPropertiesRequired(String url, String username, String password) {

        return super.externalAuthPropertiesRequired(url, username, password);
    }

    @Override
    public Properties getExternalAuthProperties(String url, String username) {
        return super.getExternalAuthProperties(url, username);
    }

    @Override
    public String instantiateClassExtendedErrorMessage() {
        return "Failure probably due to inability to load dependencies. Please ensure you have downloaded 'https://dev.mysql.com/downloads/connector/j/' and extracted to 'flyway/drivers' folder";
    }
}
