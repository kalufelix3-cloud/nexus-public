/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.upgrade.datastore.internal.steps;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.testdb.DataSessionRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

public class RemoveDistributedCooperationMigrationStep_2_14Test
    extends TestSupport
{
  @Rule
  public DataSessionRule sessionRule = new DataSessionRule(DEFAULT_DATASTORE_NAME);

  private RemoveDistributedCooperationMigrationStep_2_14 underTest;

  @Before
  public void setup() {
    underTest =
        new RemoveDistributedCooperationMigrationStep_2_14();
  }

  @Test
  public void testRemoveTable() throws Exception {
    try (Connection connection = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      createTable(connection);
      assertThat(tableExists(connection), is(true));
      underTest.migrate(connection);
      assertThat(tableExists(connection), is(false));
    }
  }

  @Test
  public void testRemoveTable_notExisting() throws Exception {
    try (Connection connection = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      assertThat(tableExists(connection), is(false));
      underTest.migrate(connection);
      assertThat(tableExists(connection), is(false));
    }
  }

  private void createTable(final Connection connection) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(
        "CREATE TABLE IF NOT EXISTS COOPERATION_LOCK\n" +
            "    (\n" +
            "      cooperation_key VARCHAR(256) NOT NULL,\n" +
            "      node_id VARCHAR(256) NOT NULL,\n" +
            "      created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,\n" +
            "      expiry TIMESTAMP WITH TIME ZONE NOT NULL,\n" +
            "      CONSTRAINT pk_cooperation_lock_cooperation_key PRIMARY KEY (cooperation_key)\n" +
            "    );")) {
      statement.execute();
    }
  }

  private boolean tableExists(final Connection connection) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(
        "SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE UPPER(TABLE_NAME) = ?")) {
      statement.setString(1, "COOPERATION_LOCK");
      try (ResultSet results = statement.executeQuery()) {
        if (!results.next()) {
          return false;
        }
      }
    }
    return true;
  }
}
