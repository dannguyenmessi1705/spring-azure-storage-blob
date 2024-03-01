package com.didan.azure.repository;

import com.didan.azure.entity.Sas;
import com.didan.azure.entity.keys.SasId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SasRepository extends JpaRepository<Sas, SasId> {
    List<Sas> findBySasId_OwnPermisSas(String ownPermisSas);
    List<Sas> findBySasId_SasToken(String sasToken);
    Sas findBySasId_OwnPermisSasAndSasId_OwnPermisSas(String ownPermisSas, String sasToken);


}
