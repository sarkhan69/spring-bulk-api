package com.github.wnameless.spring.bulkapi;

import com.google.common.base.Predicate;
import com.google.common.collect.*;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RoutingPathResolver {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{[^}]+\\}");
    private static final Pattern PATH_VAR = Pattern.compile("\\{[^}]+\\}");
    private static final Pattern ANT_AA = Pattern.compile("\\*\\*");
    private static final Pattern ANT_A = Pattern.compile("\\*");
    private static final Pattern ANT_Q = Pattern.compile("\\?");
    private final Environment env;
    private final Set<RoutingPath> routingPaths = Sets.newLinkedHashSet();

    public RoutingPathResolver(ApplicationContext appCtx, String... basePackages) {
        this.env = appCtx.getEnvironment();
        Map<String, Object> beans = appCtx.getBeansWithAnnotation(Controller.class);
        beans.putAll(appCtx.getBeansWithAnnotation(RestController.class));
        this.retainBeansByPackageNames(beans, basePackages);
        Iterator var4 = beans.values().iterator();

        while (var4.hasNext()) {
            Object bean = var4.next();
            RequestMapping classMapping = bean.getClass().getSimpleName().contains("$$") ?
                    (RequestMapping) bean.getClass().getSuperclass().getAnnotation(RequestMapping.class) :
                    (RequestMapping) bean.getClass().getAnnotation(RequestMapping.class);
            List<Method> mappingMethods = this.getMethodsListWithAnnotation(bean.getClass(), RequestMapping.class);
            mappingMethods.addAll(this.getMethodsListWithAnnotation(bean.getClass(), GetMapping.class));
            mappingMethods.addAll(this.getMethodsListWithAnnotation(bean.getClass(), PostMapping.class));
            mappingMethods.addAll(this.getMethodsListWithAnnotation(bean.getClass(), DeleteMapping.class));
            mappingMethods.addAll(this.getMethodsListWithAnnotation(bean.getClass(), PutMapping.class));
            mappingMethods.addAll(this.getMethodsListWithAnnotation(bean.getClass(), PatchMapping.class));
            Iterator var8 = mappingMethods.iterator();

            while (var8.hasNext()) {
                Method method = (Method) var8.next();
                Annotation methodMapping = method.getAnnotation(RequestMapping.class);
                if (methodMapping == null) {
                    methodMapping = method.getAnnotation(GetMapping.class);
                }

                if (methodMapping == null) {
                    methodMapping = method.getAnnotation(PostMapping.class);
                }

                if (methodMapping == null) {
                    methodMapping = method.getAnnotation(DeleteMapping.class);
                }

                if (methodMapping == null) {
                    methodMapping = method.getAnnotation(PutMapping.class);
                }

                if (methodMapping == null) {
                    methodMapping = method.getAnnotation(PatchMapping.class);
                }

                Iterator var11 = this.computeRawPaths(classMapping, methodMapping).iterator();

                while (var11.hasNext()) {
                    Map.Entry<String, RequestMethod> rawPathAndMethod = (Map.Entry) var11.next();
                    String rawPath = (String) rawPathAndMethod.getKey();
                    String path = this.computePath(rawPath);
                    String regexPath = this.computeRegexPath(path);
                    this.routingPaths.add(new RoutingPath((RequestMethod) rawPathAndMethod.getValue(), rawPath, path, Pattern.compile(regexPath),
                            bean.getClass().getSimpleName().contains("$$") ? bean.getClass().getSuperclass().getAnnotations() : bean.getClass().getAnnotations(),
                            method.getAnnotations(), method.getParameterAnnotations()));
                }
            }
        }

    }

    public List<RoutingPath> getRoutingPaths() {
        return Lists.newArrayList(this.routingPaths);
    }

    public List<RoutingPath> findByAnnotationType(final Class<? extends Annotation> annoType) {
        List<RoutingPath> paths = Lists.newArrayList(this.routingPaths);
        Iterables.removeIf(paths, new Predicate<RoutingPath>() {
            public boolean apply(RoutingPath item) {
                return !Iterables.any(item.getClassAnnotations(), new Predicate<Annotation>() {
                    public boolean apply(Annotation item) {
                        return annoType.equals(item.annotationType());
                    }
                }) && !Iterables.any(item.getMethodAnnotations(), new Predicate<Annotation>() {
                    public boolean apply(Annotation item) {
                        return annoType.equals(item.annotationType());
                    }
                });
            }
        });
        return paths;
    }

    public List<RoutingPath> findByClassAnnotationType(final Class<? extends Annotation> annoType) {
        List<RoutingPath> paths = Lists.newArrayList(this.routingPaths);
        Iterables.removeIf(paths, new Predicate<RoutingPath>() {
            public boolean apply(RoutingPath input) {
                return !Iterables.any(input.getClassAnnotations(), new Predicate<Annotation>() {
                    public boolean apply(Annotation input) {
                        return annoType.equals(input.annotationType());
                    }
                });
            }
        });
        return paths;
    }

    public List<RoutingPath> findByMethodAnnotationType(final Class<? extends Annotation> annoType) {
        List<RoutingPath> paths = Lists.newArrayList(this.routingPaths);
        Iterables.removeIf(paths, new Predicate<RoutingPath>() {
            public boolean apply(RoutingPath input) {
                return !Iterables.any(input.getMethodAnnotations(), new Predicate<Annotation>() {
                    public boolean apply(Annotation input) {
                        return annoType.equals(input.annotationType());
                    }
                });
            }
        });
        return paths;
    }

    public com.github.wnameless.spring.bulkapi.RoutingPath findByRequestPathAndMethod(String requestPath, RequestMethod method) {
        Iterator var3 = this.routingPaths.iterator();

        com.github.wnameless.spring.bulkapi.RoutingPath routingPath;
        do {
            if (!var3.hasNext()) {
                var3 = this.routingPaths.iterator();

                do {
                    if (!var3.hasNext()) {
                        return null;
                    }

                    routingPath = (com.github.wnameless.spring.bulkapi.RoutingPath) var3.next();
                } while (!requestPath.matches(routingPath.getRegexPath().pattern()) || !routingPath.getMethod().equals(method));

                return routingPath;
            }

            routingPath = (com.github.wnameless.spring.bulkapi.RoutingPath) var3.next();
        } while (!routingPath.getPath().equals(requestPath) || !routingPath.getMethod().equals(method));

        return routingPath;
    }

    public List<RoutingPath> findByRequestPath(String requestPath) {
        List<RoutingPath> paths = Lists.newArrayList();
        Iterator var3 = this.routingPaths.iterator();

        while (var3.hasNext()) {
            RoutingPath routingPath = (RoutingPath) var3.next();
            if (routingPath.getPath().equals(requestPath)) {
                paths.add(routingPath);
            } else if (requestPath.matches(routingPath.getRegexPath().pattern())) {
                paths.add(routingPath);
            }
        }

        return paths;
    }

    private List<Map.Entry<String, RequestMethod>> computeRawPaths(RequestMapping classMapping, Annotation methodMapping) {
        List<Map.Entry<String, RequestMethod>> rawPathsAndMethods = Lists.newArrayList();
        List<String> prefixPaths = classMapping == null ? Lists.newArrayList(new String[]{""}) : (classMapping.value().length != 0 ? Lists.newArrayList(ImmutableSet.copyOf(classMapping.value())) : Lists.newArrayList(ImmutableSet.copyOf(classMapping.path())));
        if (prefixPaths.isEmpty()) {
            prefixPaths.add("");
        }

        List<String> suffixPaths = Lists.newArrayList();
        List<RequestMethod> requestMethods = Lists.newArrayList();
        if (methodMapping.annotationType().equals(RequestMapping.class)) {
            suffixPaths = ((RequestMapping) methodMapping).value().length != 0 ? Lists.newArrayList(ImmutableSet.copyOf(((RequestMapping) methodMapping).value())) : Lists.newArrayList(ImmutableSet.copyOf(((RequestMapping) methodMapping).path()));
            requestMethods.addAll(Arrays.asList(((RequestMapping) methodMapping).method()));
        } else if (methodMapping.annotationType().equals(GetMapping.class)) {
            suffixPaths = ((GetMapping) methodMapping).value().length != 0 ? Lists.newArrayList(ImmutableSet.copyOf(((GetMapping) methodMapping).value())) : Lists.newArrayList(ImmutableSet.copyOf(((GetMapping) methodMapping).path()));
            requestMethods.add(RequestMethod.GET);
        } else if (methodMapping.annotationType().equals(PostMapping.class)) {
            suffixPaths = ((PostMapping) methodMapping).value().length != 0 ? Lists.newArrayList(ImmutableSet.copyOf(((PostMapping) methodMapping).value())) : Lists.newArrayList(ImmutableSet.copyOf(((PostMapping) methodMapping).path()));
            requestMethods.add(RequestMethod.POST);
        } else if (methodMapping.annotationType().equals(DeleteMapping.class)) {
            suffixPaths = ((DeleteMapping) methodMapping).value().length != 0 ? Lists.newArrayList(ImmutableSet.copyOf(((DeleteMapping) methodMapping).value())) : Lists.newArrayList(ImmutableSet.copyOf(((DeleteMapping) methodMapping).path()));
            requestMethods.add(RequestMethod.DELETE);
        } else if (methodMapping.annotationType().equals(PutMapping.class)) {
            suffixPaths = ((PutMapping) methodMapping).value().length != 0 ? Lists.newArrayList(ImmutableSet.copyOf(((PutMapping) methodMapping).value())) : Lists.newArrayList(ImmutableSet.copyOf(((PutMapping) methodMapping).path()));
            requestMethods.add(RequestMethod.PUT);
        } else if (methodMapping.annotationType().equals(PatchMapping.class)) {
            suffixPaths = ((PatchMapping) methodMapping).value().length != 0 ? Lists.newArrayList(ImmutableSet.copyOf(((PatchMapping) methodMapping).value())) : Lists.newArrayList(ImmutableSet.copyOf(((PatchMapping) methodMapping).path()));
            requestMethods.add(RequestMethod.PATCH);
        }

        if (suffixPaths.isEmpty()) {
            suffixPaths.add("");
        }

        label103:
        while (!prefixPaths.isEmpty()) {
            String prefixPath = (String) prefixPaths.remove(0);

            while (true) {
                while (true) {
                    if (suffixPaths.isEmpty()) {
                        continue label103;
                    }

                    String suffixPath = (String) suffixPaths.remove(0);
                    if (requestMethods.isEmpty()) {
                        RequestMethod[] var13 = RequestMethod.values();
                        int var14 = var13.length;

                        for (int var11 = 0; var11 < var14; ++var11) {
                            RequestMethod m = var13[var11];
                            rawPathsAndMethods.add(Maps.immutableEntry(PathUtils.joinPaths(prefixPath, suffixPath), m));
                        }
                    } else {
                        Iterator var9 = requestMethods.iterator();

                        while (var9.hasNext()) {
                            RequestMethod m = (RequestMethod) var9.next();
                            rawPathsAndMethods.add(Maps.immutableEntry(PathUtils.joinPaths(prefixPath, suffixPath), m));
                        }
                    }
                }
            }
        }

        return rawPathsAndMethods;
    }

    private String computeRegexPath(String path) {
        path = Regexs.escapeSpecialCharacters(path, PLACEHOLDER, PATH_VAR, ANT_AA, ANT_A, ANT_Q);

        Matcher m;
        String match;
        for (m = PATH_VAR.matcher(path); m.find(); path = path.replaceFirst(Pattern.quote(match), "[^/]+")) {
            match = m.group();
        }

        for (m = ANT_AA.matcher(path); m.find(); path = path.replaceFirst(Pattern.quote(match), ".\"")) {
            match = m.group();
        }

        for (m = ANT_A.matcher(path); m.find(); path = path.replaceFirst(Pattern.quote(match), "[^/]*")) {
            match = m.group();
        }

        for (m = ANT_Q.matcher(path); m.find(); path = path.replaceFirst(Pattern.quote(match), ".")) {
            match = m.group();
        }

        path = path.replaceAll(Pattern.quote("\""), "*");
        path = path.startsWith("/") ? "/?" + path.substring(1) : "/?" + path;
        if (!path.endsWith("/")) {
            path = path + "/?";
        }

        return path;
    }

    private String computePath(String rawPath) {
        String path = rawPath;

        String placeholder;
        String key;
        String deFault;
        for (Matcher m = PLACEHOLDER.matcher(rawPath); m.find(); path = path.replaceFirst(Pattern.quote(placeholder), this.env.getProperty(key, deFault))) {
            placeholder = m.group();
            String trimmedPlaceholder = placeholder.substring(2, placeholder.length() - 1);
            String[] keyAndDefault = trimmedPlaceholder.split(":");
            key = keyAndDefault[0];
            deFault = "";
            if (keyAndDefault.length > 1) {
                deFault = keyAndDefault[1];
            }
        }

        return path;
    }

    private void retainBeansByPackageNames(Map<String, Object> beans, String... basePackages) {
        Iterator<Object> beansIter = beans.values().iterator();

        while (beansIter.hasNext()) {
            String beanPackage = beansIter.next().getClass().getPackage().getName();
            boolean isKeep = false;
            String[] var6 = basePackages;
            int var7 = basePackages.length;

            for (int var8 = 0; var8 < var7; ++var8) {
                String packageName = var6[var8];
                if (beanPackage.equals(packageName) || beanPackage.startsWith(packageName + ".")) {
                    isKeep = true;
                }
            }

            if (!isKeep) {
                beansIter.remove();
            }
        }

    }

    private List<Method> getMethodsListWithAnnotation(Class<?> cls, Class<? extends Annotation> annotationCls) {
        Method[] allMethods = cls.getSimpleName().contains("$$") ? cls.getSuperclass().getMethods() : cls.getMethods();
        List<Method> annotatedMethods = new ArrayList<>();
        Method[] var5 = allMethods;
        int var6 = allMethods.length;

        for (int var7 = 0; var7 < var6; ++var7) {
            Method method = var5[var7];
            if (method.getAnnotation(annotationCls) != null) {
                annotatedMethods.add(method);
            }
        }

        return annotatedMethods;
    }
}
