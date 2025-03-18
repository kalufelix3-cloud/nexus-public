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
package org.sonatype.nexus.elasticsearch.internal.repository;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.io.Hex;
import org.sonatype.nexus.rest.ValidationErrorsException;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;

public class TokenEncoderTest
    extends TestSupport
{
  private final TokenEncoder tokenEncoder = new TokenEncoder();

  private final int LAST_FROM = 100;

  private final int PAGE_SIZE = 50;

  private final QueryBuilder QUERY_BUILDER = QueryBuilders.termQuery("format", "maven2");

  @Test
  public void testEncodeAndDecode() {
    String token = tokenEncoder.encode(LAST_FROM, PAGE_SIZE, QUERY_BUILDER);
    int fromDecoded = tokenEncoder.decode(token, QUERY_BUILDER);
    assertThat(fromDecoded, is(LAST_FROM + PAGE_SIZE));
  }

  @Test
  public void testDecode_nullToken() {
    int fromDecoded = tokenEncoder.decode(null, QUERY_BUILDER);
    assertThat(fromDecoded, is(0));
  }

  @Test
  public void testEncodeAndDecode_nonMatchedQueryBuilder() {
    QueryBuilder queryBuilder1 = QueryBuilders.termQuery("format", "maven2");
    QueryBuilder queryBuilder2 = QueryBuilders.termQuery("format", "npm");
    String token = tokenEncoder.encode(LAST_FROM, PAGE_SIZE, queryBuilder1);
    assertExceptionOccurs(token, queryBuilder2, "Continuation token " + token + " does not match this query");
  }

  @Test
  public void testDecode_invalidToken() {
    String token = "this_is_not_a_valid_token";
    assertExceptionOccurs(token, QUERY_BUILDER, "Continuation token " + token + " is not valid.");
  }

  /**
   * Tokens are encoded as a string of the form "index:hashcode". Test case where this is not true
   */
  @Test
  public void testDecode_invalidLength() {
    String token = Hex.encode("string_without_colon".getBytes(UTF_8));
    assertExceptionOccurs(token, QUERY_BUILDER, "Unable to parse token " + token);
  }

  /**
   * Tokens are encoded as a string of the form "index:hashcode". Index is expected to be a valid int.
   */
  @Test
  public void testDecode_invalidIndex() {
    String queryBuilderHash = tokenEncoder.getHashCode(QUERY_BUILDER);
    String token = Hex.encode(("notANumber:" + queryBuilderHash).getBytes(UTF_8));
    assertExceptionOccurs(token, QUERY_BUILDER,
        "Continuation token " + token + " is not valid. index must be a valid integer.");
  }

  private void assertExceptionOccurs(
      final String token,
      final QueryBuilder queryBuilder,
      final String expectedMessage)
  {
    ValidationErrorsException exception = assertThrows(ValidationErrorsException.class, () -> {
      tokenEncoder.decode(token, queryBuilder);
    });
    assertThat(exception.getMessage(), is(expectedMessage));
  }
}
