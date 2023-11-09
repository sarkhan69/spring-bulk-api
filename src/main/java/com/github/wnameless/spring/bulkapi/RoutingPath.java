package com.github.wnameless.spring.bulkapi;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ForwardingList;
import com.google.common.collect.Lists;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public final class RoutingPath {
    private final RequestMethod method;
    private final String rawPath;
    private final String path;
    private final Pattern regexPath;
    private final List<Annotation> classAnnotations;
    private final List<Annotation> methodAnnotations;
    private final List<List<Annotation>> parameterAnnotations = Lists.newArrayList();

    public RoutingPath(RequestMethod method, String rawPath, String path, Pattern regexPath, Annotation[] classAnnotations, Annotation[] methodAnnotations, Annotation[][] parameterAnnotations) {
        this.method = (RequestMethod) Preconditions.checkNotNull(method);
        this.rawPath = (String)Preconditions.checkNotNull(rawPath);
        this.regexPath = (Pattern)Preconditions.checkNotNull(regexPath);
        this.path = (String)Preconditions.checkNotNull(path);
        this.classAnnotations = Lists.newArrayList(classAnnotations);
        this.methodAnnotations = Lists.newArrayList(methodAnnotations);
        Annotation[][] var8 = parameterAnnotations;
        int var9 = parameterAnnotations.length;

        for(int var10 = 0; var10 < var9; ++var10) {
            Annotation[] annos = var8[var10];
            this.parameterAnnotations.add(Lists.newArrayList(annos));
        }

    }

    public RequestMethod getMethod() {
        return this.method;
    }

    public String getRawPath() {
        return this.rawPath;
    }

    public String getPath() {
        return this.path;
    }

    public Pattern getRegexPath() {
        return this.regexPath;
    }

    public List<Annotation> getClassAnnotations() {
        return Collections.unmodifiableList(this.classAnnotations);
    }

    public List<Annotation> getMethodAnnotations() {
        return Collections.unmodifiableList(this.methodAnnotations);
    }

    public List<List<Annotation>> getParameterAnnotations() {
        return this.unmodifiableList2(this.parameterAnnotations);
    }

    private <T> List<List<T>> unmodifiableList2(final List<List<T>> input) {
        return Collections.unmodifiableList(new ForwardingList<List<T>>() {
            protected List<List<T>> delegate() {
                return Collections.unmodifiableList(input);
            }

            public List<T> get(int index) {
                return Collections.unmodifiableList((List)this.delegate().get(index));
            }
        });
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (!(other instanceof RoutingPath)) {
            return false;
        } else {
            RoutingPath castOther = (RoutingPath) other;
            return Objects.equal(this.method, castOther.method) && Objects.equal(this.rawPath, castOther.rawPath) && Objects.equal(this.path, castOther.path) && Objects.equal(this.regexPath, castOther.regexPath) && Objects.equal(this.classAnnotations, castOther.classAnnotations) && Objects.equal(this.methodAnnotations, castOther.methodAnnotations);
        }
    }

    public int hashCode() {
        return Objects.hashCode(this.method, this.rawPath, this.path, this.regexPath, this.classAnnotations, this.methodAnnotations);
    }

    public String toString() {
        return MoreObjects.toStringHelper(this).add("method", this.method).add("rawPath", this.rawPath).add("path", this.path).add("regexPath", this.regexPath).add("classAnnotations", this.classAnnotations).add("methodAnnotations", this.methodAnnotations).toString();
    }
}
