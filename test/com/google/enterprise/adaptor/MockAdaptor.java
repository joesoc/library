// Copyright 2011 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.enterprise.adaptor;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Mock of {@link Adaptor}. All documents authz as PERMIT.
 */
class MockAdaptor extends AbstractAdaptor implements AuthzAuthority {
  public byte[] documentBytes = new byte[] {1, 2, 3};

  @Override
  public void init(AdaptorContext context) {
    context.setAuthzAuthority(this);
  }

  @Override
  public void getDocIds(DocIdPusher pusher) throws InterruptedException,
      IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void getDocContent(Request request, Response response)
      throws IOException, InterruptedException {
    response.getOutputStream().write(documentBytes);
  }

  @Override
  public Map<DocId, AuthzStatus> isUserAuthorized(AuthnIdentity identity,
      Collection<DocId> ids) {
    Map<DocId, AuthzStatus> result
        = new HashMap<DocId, AuthzStatus>(ids.size() * 2);
    for (DocId id : ids) {
      result.put(id, AuthzStatus.PERMIT);
    }
    return Collections.unmodifiableMap(result);
  }
}
