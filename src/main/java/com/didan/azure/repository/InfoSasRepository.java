package com.didan.azure.repository;

import com.didan.azure.entity.InfoSass;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InfoSasRepository extends JpaRepository<InfoSass, String> {
    InfoSass findByFileName(String fileName);
    InfoSass findBySasToken(String sasToken);
    InfoSass findByOwnSas(String ownSas);
}
