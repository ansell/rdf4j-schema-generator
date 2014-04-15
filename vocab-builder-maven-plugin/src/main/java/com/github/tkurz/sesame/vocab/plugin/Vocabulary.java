/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.tkurz.sesame.vocab.plugin;

import java.io.File;
import java.net.URL;

import com.google.common.base.CaseFormat;

/**
 * Configuration for a Vocabulary to be generated.
 *
 * @author Jakob Frank (jakob@apache.org)
 */
public class Vocabulary {

    private URL url;
    private File file;

    private String name;
    private String packageName;

    private String className;

    private String mimeType;

    private String preferredLanguage;
    private Boolean createResourceBundles;
	private CaseFormat caseFormat;
    
    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public void setPreferredLanguage(String language) {
    	this.preferredLanguage = language;
    }
    
    public String getPreferredLanguage() {
    	return preferredLanguage;
    }

    public void setConstantCase(CaseFormat caseFormat) {
    	this.caseFormat = caseFormat;
    }
    
    public CaseFormat getConstantCase() {
    	return caseFormat;
    }
    
    public static Vocabulary create(URL url, String name, String className) {
        Vocabulary v = new Vocabulary();
        v.url = url;
        v.name = name;
        v.className = className;
        return v;
    }

    public static Vocabulary create(File file, String name, String className) {
        Vocabulary v = new Vocabulary();
        v.file = file;
        v.name = name;
        v.className = className;
        return v;
    }

    public boolean isCreateResourceBundles() {
        return createResourceBundles;
    }

    public boolean isCreateResourceBundlesSet() {
        return createResourceBundles != null;
    }

    public void setCreateResourceBundles(boolean createResourceBundles) {
        this.createResourceBundles = createResourceBundles;
    }
}
