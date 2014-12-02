package org.asu.apmg;

/**
 * Use this class to represent a metadata object that will be used to generate a member of the
 * package manifest in Salesforce
 */
public class APMGMetadataObject {
    private String extension;
    private String container;
    private String member;
    private String metadataType;
    private String path;
    private boolean destructible;
    private boolean valid;

    //Constructor
    public APMGMetadataObject(String extension, String container, String member,
                              String metadataType, String path, boolean destructible, boolean valid) {
        this.extension = extension;
        this.container = container;
        this.member = member;
        this.metadataType = metadataType;
        this.path = path;
        this.destructible = destructible;
        this.valid = valid;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getContainer() {
        return container;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMember() {
        return member;
    }

    public void setMember(String member) {
        this.member = member;
    }

    public String getMetadataType() {
        return metadataType;
    }

    public void setMetadataType(String metadata) {
        this.metadataType = metadata;
    }

    public boolean isDestructible() {
        return destructible;
    }

    public void setDestructible(boolean destructible) {
        this.destructible = destructible;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getFullName(){
        return member+"."+extension;
    }
}
