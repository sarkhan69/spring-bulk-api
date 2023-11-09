package com.github.wnameless.spring.bulkapi;

import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.Arrays;

public final class PathUtils {
    private PathUtils() {
    }

    public static String joinPaths(String... paths) {
        return joinPaths('/', paths);
    }

    public static String joinPaths(Character separator, String... paths) {
        String pathSeprator = separator.toString();
        ArrayList<String> pathList = new ArrayList(Arrays.asList(paths));
        pathList.remove("");

        for(int i = 1; i < pathList.size(); ++i) {
            int predecessor = i - 1;

            while(((String)pathList.get(predecessor)).endsWith(pathSeprator)) {
                pathList.set(predecessor, ((String)pathList.get(predecessor)).substring(0, ((String)pathList.get(predecessor)).length() - 1));
            }

            while(((String)pathList.get(i)).startsWith(pathSeprator)) {
                pathList.set(i, ((String)pathList.get(i)).substring(1));
            }

            pathList.set(i, pathSeprator + (String)pathList.get(i));
        }

        return Joiner.on("").join(pathList);
    }
}
