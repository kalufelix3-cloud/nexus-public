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
package org.sonatype.nexus.repository.content.upgrades;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import static java.util.Objects.requireNonNull;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Creates composite index on {format}_asset_blob tables for blob_created DESC and asset_blob_id ASC.
 * Note that the 2_18_1 version is because this step is needed in a 3.86.1 patch release and there are already
 * like 20 new db migrations on main for the 3.87 release, so want to make sure we don't cause migration issues for
 * customers that go to the 3.86.1 release and then 3.87 or any other future release
 */
@Component
@Scope(SCOPE_SINGLETON)
public class AssetBlobMigrationStep_2_18_1
    extends ComponentSupport
    implements DatabaseMigrationStep
{
  private static final String CREATE_INDEX_PG = "CREATE INDEX CONCURRENTLY ";

  private static final String CREATE_INDEX_H2 = "CREATE INDEX IF NOT EXISTS ";

  private static final String INDEX_NAME =
      "idx_%s_asset_blob_blob_created_asset_id ON %s_asset_blob (blob_created DESC, asset_blob_id ASC);";

  private final List<Format> formats;

  @Inject
  public AssetBlobMigrationStep_2_18_1(final List<Format> formats) {
    this.formats = requireNonNull(formats);
  }

  @Override
  public Optional<String> version() {
    return Optional.of("2.18.1");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    String finalIndexClause = isH2(connection)
        ? CREATE_INDEX_H2 + INDEX_NAME
        : CREATE_INDEX_PG + INDEX_NAME;

    formats.forEach(format -> {
      String tableName = format.getValue() + "_asset_blob";
      try {
        if (tableExists(connection, tableName)) {
          executeStatement(connection,
              String.format(finalIndexClause, format.getValue(), format.getValue()));
        }
      }
      catch (SQLException e) {
        log.error("Failed to check if table '{}' exists", tableName, e);
        throw new RuntimeException(e);
      }
    });
  }

  private void executeStatement(final Connection connection, final String sqlStatement) {
    try (PreparedStatement statement = connection.prepareStatement(sqlStatement)) {
      statement.executeUpdate();
    }
    catch (SQLException e) {
      log.error("Failed to apply asset_blob index ('{}')", sqlStatement, e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean canExecuteInTransaction() {
    return false; // PostgreSQL does not support concurrent index creation in a transaction
  }
}
