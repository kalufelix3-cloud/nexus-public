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
package org.sonatype.nexus.elasticsearch.internal.repository.contributions;

import java.util.function.Consumer;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.elasticsearch.ElasticSearchContribution;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import static org.sonatype.nexus.common.app.FeatureFlags.ELASTIC_SEARCH_ENABLED;

/**
 * {@link ElasticSearchContribution} which adds query to identify specific field is empty
 */
@Named(BlankValueElasticSearchContribution.NAME)
@Singleton
@ConditionalOnProperty(name = ELASTIC_SEARCH_ENABLED, havingValue = "true", matchIfMissing = true)
public class BlankValueElasticSearchContribution
    extends ElasticSearchContributionSupport
{
  public static final String NAME = "blankValueContribution";

  @Override
  public void contribute(final Consumer<QueryBuilder> query, final String type, final String value) {
    query.accept(QueryBuilders.boolQuery()
        .should(QueryBuilders.boolQuery()
            .mustNot(QueryBuilders.wildcardQuery(type, "*")))
        .should(QueryBuilders.boolQuery()
            .mustNot(QueryBuilders.existsQuery(type))));
  }
}
