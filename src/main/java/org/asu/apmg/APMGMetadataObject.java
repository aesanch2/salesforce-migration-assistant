package org.asu.apmg;

/**
 * Creates an object representation of a Salesforce Metadata file.
 * @author aesanch2
 */
public class APMGMetadataObject {
    private String extension;
    private String container;
    private String member;
    private String metadataType;
    private String path;
    private boolean destructible;
    private boolean valid;
    private boolean metaxml;

    /**
     * Constructor for APMGMetdataObject
     * @param extension
     * @param container
     * @param member
     * @param metadataType
     * @param path
     * @param destructible
     * @param valid
     * @param metaxml
     */
    public APMGMetadataObject(String extension, String container, String member,
                              String metadataType, String path, boolean destructible, boolean valid, boolean metaxml) {
        this.extension = extension;
        this.container = container;
        this.member = member;
        this.metadataType = metadataType;
        this.path = path;
        this.destructible = destructible;
        this.valid = valid;
        this.metaxml = metaxml;
    }

    /**
     * Returns the extension for this metadata file.
     * @return A string representation of the extension type of the metadata file.
     */
    public String getExtension() {
        return extension;
    }

    /**
     * Sets the extension for this metadata object.
     * @param extension
     */
    public void setExtension(String extension) {
        this.extension = extension;
    }

    /**
     * Returns the parent container for this metadata file.
     * @return A string representation of the parent container for this metadata file.
     */
    public String getContainer() {
        return container;
    }

    /**
     * Sets the parent container for this metadata object.
     * @param container
     */
    public void setContainer(String container) {
        this.container = container;
    }

    /**
     * Returns the path of the metadata file.
     * @return A string representation of the path of the metadata file.
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets the path for this metadata object.
     * @param path
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Returns the name of the metadata file.
     * @return A string representation of the metadata file's name.
     */
    public String getMember() {
        return member;
    }

    /**
     * Sets the name of the metadata file.
     * @param member
     */
    public void setMember(String member) {
        this.member = member;
    }

    /**
     * Returns the metadata type of this metadata file.
     * @return A string representation of the metadata file's type.
     */
    public String getMetadataType() {
        return metadataType;
    }

    /**
     * Sets the metadata type of this metadata object.
     * @param metadata
     */
    public void setMetadataType(String metadata) {
        this.metadataType = metadata;
    }

    /**
     * Returns whether or not this metadata object can be deleted using the Salesforce API.
     * @return A boolean that describes whether or not this metadata object can be deleted using the Salesforce API.
     */
    public boolean isDestructible() {
        return destructible;
    }

    /**
     * Sets the boolean to allow this metadata object to be deleted in a destructive change.
     * @param destructible
     */
    public void setDestructible(boolean destructible) {
        this.destructible = destructible;
    }

    /**
     * Returns whether or not this metadata object is a valid member of the Salesforce API.
     * @return A boolean that describes wheter or not this metadata object is a valid member of the Salesforce API.
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Sets the boolean to determine if this metadata object is a valid member of the Salesforce API.
     * @param valid
     */
    public void setValid(boolean valid) {
        this.valid = valid;
    }

    /**
     * Returns whether or not this metadata object has an accompanying -meta.xml file.
     * @return
     */
    public boolean hasMetaxml() {
        return metaxml;
    }

    /**
     * Sets the boolean to determine if this metadata object has an accompanying -meta.xml file
     * @param metaxml
     */
    public void setMetaxml(boolean metaxml) {
        this.metaxml = metaxml;
    }

    /**
     * A toString() like method that returns a concatenation of the name and extension of the metadata object.
     * @return A string of the name and extension of the metadata object.
     */
    public String getFullName(){
        return member+"."+extension;
    }
}
