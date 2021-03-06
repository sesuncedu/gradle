/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.play.integtest.fixtures.app

import org.gradle.integtests.fixtures.SourceFile
import org.gradle.test.fixtures.file.TestFile
import org.gradle.util.GFileUtils

abstract class PlayApp {


    List<SourceFile> getAllFiles() {
        return appSources + testSources + viewSources + assetSources + confSources
    }

    List<SourceFile> getAppSources() {
        return sourceFiles("app").findAll {
            it.path != "app/views"
        }
    }

    abstract List<SourceFile> getViewSources();
    abstract List<SourceFile> getConfSources();
    abstract List<SourceFile> getAssetSources();


    protected SourceFile sourceFile(String path, String name, String baseDir = getClass().getSimpleName().toLowerCase()) {
        URL resource = getClass().getResource("$baseDir/$path/$name");
        File file = new File(resource.toURI())
        return new SourceFile(path, name, file.text);
    }

    void writeSources(TestFile sourceDir) {
        for (SourceFile srcFile : allFiles) {
            srcFile.writeToDir(sourceDir)
        }
    }

    List<SourceFile> sourceFiles(String baseDir, String rootDir = getClass().getSimpleName().toLowerCase()) {
        List sourceFiles = new ArrayList()

        URL resource = getClass().getResource("$rootDir/$baseDir")
        if(resource != null){
            File baseDirFile = new File(resource.toURI())
            baseDirFile.eachFileRecurse { File source ->
                if(!source.isDirectory()){
                    String fileName = source.getName()
                    def subpath = GFileUtils.relativePath(baseDirFile, source.parentFile);
                    sourceFiles.add(sourceFile("$baseDir/$subpath", fileName, rootDir))
                }
            }
        }

        return sourceFiles
    }
}
