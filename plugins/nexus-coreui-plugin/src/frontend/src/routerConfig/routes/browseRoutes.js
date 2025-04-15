/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import { Permissions } from '@sonatype/nexus-ui-plugin';

import Welcome from '../../components/pages/user/Welcome/Welcome';
import BrowseReactExt from '../../components/pages/browse/Browse/BrowseExt';
import Upload from '../../components/pages/browse/Upload/Upload';
import MalwareRemediation from '../../components/pages/maliciousrisk/MalwareRemediation';
import SearchGenericExt from '../../components/pages/browse/Search/SearchGenericExt';
import SearchAptExt from '../../components/pages/browse/Search/SearchAptExt';
import SearchCargoExt from '../../components/pages/browse/Search/SearchCargoExt';
import SearchCocoapodsExt from '../../components/pages/browse/Search/SearchCocoapodsExt';
import SearchComposerExt from '../../components/pages/browse/Search/SearchComposerExt';
import SearchConanExt from '../../components/pages/browse/Search/SearchConanExt';
import SearchCondaExt from '../../components/pages/browse/Search/SearchCondaExt';
import SearchCustomExt from '../../components/pages/browse/Search/SearchCustomExt';
import SearchDockerExt from '../../components/pages/browse/Search/SearchDockerExt';
import SearchGitLfsExt from '../../components/pages/browse/Search/SearchGitLfsExt';
import SearchGolangExt from '../../components/pages/browse/Search/SearchGolangExt';
import SearchHelmExt from '../../components/pages/browse/Search/SearchHelmExt';
import SearchHuggingFaceExt from '../../components/pages/browse/Search/SearchHuggingFaceExt';
import SearchMavenExt from '../../components/pages/browse/Search/SearchMavenExt';
import SearchNpmExt from '../../components/pages/browse/Search/SearchNpmExt';
import SearchNugetExt from '../../components/pages/browse/Search/SearchNugetExt';
import SearchP2Ext from '../../components/pages/browse/Search/SearchP2Ext';
import SearchPypiExt from '../../components/pages/browse/Search/SearchPypiExt';
import SearchRExt from '../../components/pages/browse/Search/SearchRExt';
import SearchRawExt from '../../components/pages/browse/Search/SearchRawExt';
import SearchRubygemsExt from '../../components/pages/browse/Search/SearchRubygemsExt';
import SearchYumExt from '../../components/pages/browse/Search/SearchYumExt';
;
import FeatureFlags from '../../constants/FeatureFlags';
import { UIView } from '@uirouter/react';
import Tags from '../../components/pages/browse/Tags/Tags';
import { ROUTE_NAMES } from '../routeNames/routeNames';

const { MALWARE_RISK_ENABLED } = FeatureFlags;

const BROWSE = ROUTE_NAMES.BROWSE;

// for more info on how to define routes see private/developer-documentation/frontend/client-side-routing.md
export const browseRoutes = [
  {
    name: BROWSE.DIRECTORY,
    url: 'browse',
    component: UIView,
    visibilityRequirements: {
      ignoreForMenuVisibilityCheck: true
    }
  },
  {
    name: BROWSE.WELCOME,
    url: '/welcome',
    component: Welcome,
    data: {
      // make sure we don't inherit from BROWSE
      visibilityRequirements: {}
    },
  },
  {
    name: BROWSE.SEARCH.ROOT,
    url: '/search',
    component: UIView,
    data: {
      // make sure we don't inherit from BROWSE
      visibilityRequirements: {}
    }
  },
  {
    name: BROWSE.SEARCH.GENERIC,
    url: '/generic:keyword',
    component: SearchGenericExt,
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: ['nexus:search:read'],
      },
    },
  },
  {
    name: BROWSE.SEARCH.CUSTOM,
    url: '/custom/:keyword',
    component: SearchCustomExt,
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: ['nexus:search:read'],
      },
    },
  },
  {
    name: BROWSE.SEARCH.APT,
    url: '/apt/:keyword',
    component: SearchAptExt,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: ['nexus:search:read'],
        browseableFormat: 'apt',
      },
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.CARGO,
    url: '/cargo/:keyword',
    component: SearchCargoExt,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: ['nexus:search:read'],
        browseableFormat: 'cargo',
      },
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.COCOAPODS,
    url: '/cocoapods/:keyword',
    component: SearchCocoapodsExt,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: ['nexus:search:read'],
        browseableFormat: 'cocoapods',
      },
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.COMPOSER,
    url: '/composer/:keyword',
    component: SearchComposerExt,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: ['nexus:search:read'],
        browseableFormat: 'composer',
      },
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.CONAN,
    url: '/conan/:keyword',
    component: SearchConanExt,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: ['nexus:search:read'],
        browseableFormat: 'conan',
      },
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.CONDA,
    url: '/conda/:keyword',
    component: SearchCondaExt,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: ['nexus:search:read'],
        browseableFormat: 'conda',
      },
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.DOCKER,
    url: '/docker/:keyword',
    component: SearchDockerExt,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: ['nexus:search:read'],
        browseableFormat: 'docker',
      },
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.GITLFS,
    url: '/gitlfs/:keyword',
    component: SearchGitLfsExt,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: ['nexus:search:read'],
        browseableFormat: 'gitlfs',
      },
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.GOLANG,
    url: '/golang/:keyword',
    component: SearchGolangExt,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: ['nexus:search:read'],
        browseableFormat: 'go',
      },
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.HELM,
    url: '/helm/:keyword',
    component: SearchHelmExt,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: ['nexus:search:read'],
        browseableFormat: 'helm',
      },
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.HUGGING_FACE,
    url: '/hugging_face/:keyword',
    component: SearchHuggingFaceExt,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: ['nexus:search:read'],
        browseableFormat: 'huggingface',
      },
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.MAVEN,
    url: '/maven/:keyword',
    component: SearchMavenExt,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: ['nexus:search:read'],
        browseableFormat: 'maven2',
      },
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.NPM,
    url: '/npm/:keyword',
    component: SearchNpmExt,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: ['nexus:search:read'],
        browseableFormat: 'npm',
      },
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.NUGET,
    url: '/nuget/:keyword',
    component: SearchNugetExt,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: ['nexus:search:read'],
        browseableFormat: 'nuget',
      },
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.P2,
    url: '/p2/:keyword',
    component: SearchP2Ext,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: ['nexus:search:read'],
        browseableFormat: 'p2',
      },
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.PYPI,
    url: '/pypi/:keyword',
    component: SearchPypiExt,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: ['nexus:search:read'],
        browseableFormat: 'pypi',
      },
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.R,
    url: '/r/:keyword',
    component: SearchRExt,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: ['nexus:search:read'],
        browseableFormat: 'r',
      },
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.RAW,
    url: '/raw/:keyword',
    component: SearchRawExt,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: ['nexus:search:read'],
        browseableFormat: 'raw',
      },
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.RUBYGEMS,
    url: '/rubygems/:keyword',
    component: SearchRubygemsExt,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: ['nexus:search:read'],
        browseableFormat: 'rubygems',
      },
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.YUM,
    url: '/yum/:keyword',
    component: SearchYumExt,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: ['nexus:search:read'],
        browseableFormat: 'yum',
      },
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.BROWSE,
    url: '/browse:repo',
    component: BrowseReactExt,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        statesEnabled: [
          {
            key: 'browseableformats',
            defaultValue: [],
          },
        ],
      },
    },
    params: {
      repo: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.UPLOAD,
    url: '/upload:itemId',
    component: Upload,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: [Permissions.COMPONENT.CREATE],
      },
    },
    params: {
      itemId: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.TAGS,
    url: '/tags:itemId',
    component: Tags,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: [Permissions.TAGS.READ],
        editions: ['PRO'],
      },
    },
    params: {
      itemId: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.MALWARERISK,
    url: '/malwarerisk',
    component: MalwareRemediation,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: [Permissions.ADMIN],
        statesEnabled: [
          {
            key: MALWARE_RISK_ENABLED,
            defaultValue: false,
          },
        ],
        editions: ['PRO'],
      },
    },
  },
];
