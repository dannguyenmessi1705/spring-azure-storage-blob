package com.didan.azure.entity.keys;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Id;

import java.io.Serializable;

@Embeddable
public class SasId implements Serializable {
    @Column(name = "sas_token", nullable = false)
    private String sasToken;

    @Column(name = "own_permis_sas", nullable = false)
    private String ownPermisSas;

    public SasId(String sasToken, String ownPermisSas) {
        this.sasToken = sasToken;
        this.ownPermisSas = ownPermisSas;
    }

    public SasId() {

    }

    public String getSasToken() {
        return sasToken;
    }

    public void setSasToken(String sasToken) {
        this.sasToken = sasToken;
    }

    public String getOwnPermisSas() {
        return ownPermisSas;
    }

    public void setOwnPermisSas(String ownPermisSas) {
        this.ownPermisSas = ownPermisSas;
    }
}
