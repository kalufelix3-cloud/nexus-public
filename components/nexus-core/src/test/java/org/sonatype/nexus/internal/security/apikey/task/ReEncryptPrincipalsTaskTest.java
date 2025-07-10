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
package org.sonatype.nexus.internal.security.apikey.task;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.crypto.internal.PbeCipherFactory;
import org.sonatype.nexus.crypto.internal.PbeCipherFactory.PbeCipher;
import org.sonatype.nexus.crypto.secrets.EncryptedSecret;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.internal.security.secrets.tasks.ReEncryptTaskDescriptor;
import org.sonatype.nexus.scheduling.TaskConfiguration;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ReEncryptPrincipalsTaskTest
    extends TestSupport
{
  private static final String SELECT = """
      SELECT principals, domain, username, access_key
      FROM api_key_v2
      ORDER BY username, principals, domain
      LIMIT ? OFFSET ?
      """;

  private static final String UPDATE = """
      UPDATE api_key_v2
      SET principals = ?
      WHERE domain = ? AND username = ? AND access_key = ?
      """;

  @Mock
  private DataSessionSupplier sessionSupplier;

  @Mock
  private PbeCipherFactory pbeCipherFactory;

  private ReEncryptPrincipalsTask underTest;

  @Before
  public void setup() {
    underTest =
        spy(new ReEncryptPrincipalsTask(sessionSupplier, pbeCipherFactory, "password", "salt", "iv", "algorithm"));
    doReturn(setupTaskConfig()).when(underTest).taskConfiguration();
  }

  @Test
  public void testExecute_WhenThereAreNotChange() throws Exception {
    TaskConfiguration taskConfigurationEmpty = new TaskConfiguration();
    taskConfigurationEmpty.setId(UUID.randomUUID().toString());
    taskConfigurationEmpty.setTypeId(ReEncryptTaskDescriptor.TYPE_ID);
    when(underTest.taskConfiguration()).thenReturn(taskConfigurationEmpty);

    Object result = underTest.execute();

    assertThat(result).isEqualTo(0);
    verify(pbeCipherFactory, never()).create(any(), any(), any());
    verify(sessionSupplier, never()).openConnection(anyString());
  }

  @Test
  public void testExecute() throws Exception {
    Connection mockConnection = mock(Connection.class);
    when(sessionSupplier.openConnection("nexus")).thenReturn(mockConnection);

    PreparedStatement mockPreparedStatementSelect = mock(PreparedStatement.class);
    when(mockConnection.prepareStatement(SELECT)).thenReturn(mockPreparedStatementSelect);

    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockPreparedStatementSelect.executeQuery()).thenReturn(mockResultSet);

    PreparedStatement mockPreparedStatementUpdate = mock(PreparedStatement.class);
    when(mockConnection.prepareStatement(UPDATE)).thenReturn(mockPreparedStatementUpdate);

    // only one page of results
    when(mockResultSet.isBeforeFirst()).thenReturn(true, false);
    // 3 rows in the result set
    when(mockResultSet.next()).thenReturn(true, true, true, false);
    when(mockResultSet.getBytes("principals")).thenReturn("new-principals1".getBytes(), "new-principals2".getBytes(),
        "new-principals3".getBytes());
    when(mockResultSet.getString("domain")).thenReturn("domain1", "domain2", "domain3");
    when(mockResultSet.getString("username")).thenReturn("user1", "user2", "user3");
    when(mockResultSet.getString("access_key")).thenReturn("access-key-1", "access-key-2", "access-key-3");

    PbeCipher mockPbeCipherToEncrypt = mock(PbeCipher.class);
    when(pbeCipherFactory.create(any(), any(), any())).thenReturn(mockPbeCipherToEncrypt);
    when(mockPbeCipherToEncrypt.encrypt(any()))
        .thenReturn(EncryptedSecret.parse("$test2$kv1=v1,kv2=v2$test-salt2$dGVzdA=="));
    PbeCipher mockPbeCipherToDecrypt = mock(PbeCipher.class);
    when(pbeCipherFactory.create(any(), any())).thenReturn(mockPbeCipherToDecrypt);
    when(mockPbeCipherToDecrypt.decrypt()).thenReturn("decrypted".getBytes());

    Object result = underTest.execute();

    assertThat(result).isEqualTo(3);
    verify(sessionSupplier, times(1)).openConnection(anyString());
    verify(pbeCipherFactory, times(1)).create(any(), any(), any());
    verify(pbeCipherFactory, times(3)).create(any(), any());
    verify(mockPreparedStatementSelect, times(2)).executeQuery();
    verify(mockPbeCipherToDecrypt, times(3)).decrypt();
    verify(mockPbeCipherToEncrypt, times(3)).encrypt(any());
    verify(mockPreparedStatementUpdate, times(3)).executeUpdate();
  }

  @Test
  public void testExecute_WhenSecondPrincipalsFails() throws Exception {
    Connection mockConnection = mock(Connection.class);
    when(sessionSupplier.openConnection("nexus")).thenReturn(mockConnection);

    PreparedStatement mockPreparedStatementSelect = mock(PreparedStatement.class);
    when(mockConnection.prepareStatement(SELECT)).thenReturn(mockPreparedStatementSelect);

    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockPreparedStatementSelect.executeQuery()).thenReturn(mockResultSet);

    PreparedStatement mockPreparedStatementUpdate = mock(PreparedStatement.class);
    when(mockConnection.prepareStatement(UPDATE)).thenReturn(mockPreparedStatementUpdate);

    // only one page of results
    when(mockResultSet.isBeforeFirst()).thenReturn(true, false);
    // 3 rows in the result set
    when(mockResultSet.next()).thenReturn(true, true, true, false);
    when(mockResultSet.getBytes("principals")).thenReturn("new-principals1".getBytes(), "new-principals2".getBytes(),
        "new-principals3".getBytes());
    when(mockResultSet.getString("domain")).thenReturn("domain1", "domain2", "domain3");
    when(mockResultSet.getString("username")).thenReturn("user1", "user2", "user3");
    when(mockResultSet.getString("access_key")).thenReturn("access-key-1", "access-key-2", "access-key-3");

    PbeCipher mockPbeCipherToEncrypt = mock(PbeCipher.class);
    when(pbeCipherFactory.create(any(), any(), any())).thenReturn(mockPbeCipherToEncrypt);
    when(mockPbeCipherToEncrypt.encrypt(any()))
        .thenReturn(EncryptedSecret.parse("$test2$kv1=v1,kv2=v2$test-salt2$dGVzdA=="));
    PbeCipher mockPbeCipherToDecrypt = mock(PbeCipher.class);
    when(pbeCipherFactory.create(any(), any())).thenReturn(mockPbeCipherToDecrypt);

    // Simulate decryption failure for the second one
    when(mockPbeCipherToDecrypt.decrypt())
        .thenReturn("decrypted1".getBytes())
        .thenThrow(new RuntimeException("Decryption failed"))
        .thenReturn("decrypted3".getBytes());

    Object result = underTest.execute();

    assertThat(result).isEqualTo(2);
    verify(sessionSupplier, times(1)).openConnection(anyString());
    verify(pbeCipherFactory, times(1)).create(any(), any(), any());
    verify(pbeCipherFactory, times(3)).create(any(), any());
    verify(mockPreparedStatementSelect, times(2)).executeQuery();
    verify(mockPbeCipherToDecrypt, times(3)).decrypt();
    verify(mockPbeCipherToEncrypt, times(2)).encrypt(any());
    verify(mockPreparedStatementUpdate, times(2)).executeUpdate();
  }

  private static TaskConfiguration setupTaskConfig() {
    TaskConfiguration taskConfiguration = new TaskConfiguration();
    taskConfiguration.setId(UUID.randomUUID().toString());
    taskConfiguration.setTypeId(ReEncryptTaskDescriptor.TYPE_ID);
    taskConfiguration.setString("password", "old-password");
    taskConfiguration.setString("salt", "old-salt");
    taskConfiguration.setString("iv", "old-iv");
    taskConfiguration.setString("algorithm", "old-algorithm");
    return taskConfiguration;
  }
}
