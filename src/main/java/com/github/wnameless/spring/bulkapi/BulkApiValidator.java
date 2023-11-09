/*
 *
 * Copyright 2015 Wei-Ming Wu
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package com.github.wnameless.spring.bulkapi;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;

import net.sf.rubycollect4j.Ruby;

/**
 * 
 * {@link BulkApiValidator} can check all bulk request paths and methods whether
 * bulkable or not.
 *
 */
public class BulkApiValidator {

  private final RoutingPathResolver pathRes;

  public BulkApiValidator(ApplicationContext appCtx) {
    Map<String, Object> bulkableBeans =
        appCtx.getBeansWithAnnotation(Bulkable.class);
    String[] basePackageNames = bulkableBeans.values().stream()
        .map(o -> o.getClass().getPackage().getName()).toArray(String[]::new);

    pathRes = new com.github.wnameless.spring.bulkapi.RoutingPathResolver(appCtx, basePackageNames);
  }

  /**
   * Checks if any request path with certain method is bulkable or not.
   * 
   * @param path
   *          a request bulk path
   * @param method
   *          a request bulk method
   * @return true if request path and method is bulkable, false otherwise
   */
  public PathValidationResult validatePath(String path, HttpMethod method) {
    com.github.wnameless.spring.bulkapi.RoutingPath rp = pathRes.findByRequestPathAndMethod(path,
        RequestMethod.valueOf(method.toString()));

    if (rp == null) return new PathValidationResult(false, false);

    Annotation bulkable = findBulkableAnno(rp);
    if (bulkable != null) {
      if (isAutoApply(bulkable) || isAcceptBulk(rp))
        return new PathValidationResult(true, hasRequestBody(rp));
    }

    return new PathValidationResult(false, false);
  }

  private boolean hasRequestBody(com.github.wnameless.spring.bulkapi.RoutingPath rp) {
    for (List<Annotation> annos : rp.getParameterAnnotations()) {
      if (Ruby.Array.of(annos).map(Annotation::annotationType)
          .contains(RequestBody.class))
        return true;
    }
    return false;
  }

  private boolean isAcceptBulk(RoutingPath rp) {
    return rp.getMethodAnnotations().stream().map(Annotation::annotationType)
        .collect(Collectors.toList()).contains(AcceptBulk.class);
  }

  private boolean isAutoApply(Annotation bulkable) {
    return ((Bulkable) bulkable).autoApply();
  }

  private Annotation findBulkableAnno(com.github.wnameless.spring.bulkapi.RoutingPath rp) {
    return rp.getClassAnnotations().stream()
        .filter(item -> item.annotationType().equals(Bulkable.class))
        .findFirst().orElse(null);
  }

}
