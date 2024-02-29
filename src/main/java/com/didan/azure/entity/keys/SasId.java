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

}
