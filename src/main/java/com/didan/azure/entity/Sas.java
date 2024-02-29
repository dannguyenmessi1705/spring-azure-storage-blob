package com.didan.azure.entity;

import com.didan.azure.entity.keys.SasId;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity(name = "sas")
public class Sas {
    @EmbeddedId
    private SasId sasId;

    @ManyToOne
    @JoinColumn(name = "own_permis_sas", updatable = false, insertable = false)
    private Users users;

    @ManyToOne
    @JoinColumn(name = "sas_token", updatable = false, insertable = false)
    private FileInfo fileInfo;

    public SasId getSasId() {
        return sasId;
    }

    public void setSasId(SasId sasId) {
        this.sasId = sasId;
    }

    public Users getUsers() {
        return users;
    }

    public void setUsers(Users users) {
        this.users = users;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public void setFileInfo(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }
}
