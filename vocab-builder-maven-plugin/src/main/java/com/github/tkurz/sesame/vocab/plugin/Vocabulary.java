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
