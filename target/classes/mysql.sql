DROP TABLE IF EXISTS `spring_azure_blob`.`users` ;

CREATE TABLE IF NOT EXISTS `spring_azure_blob`.`users` (
    `user_id` VARCHAR(255) NOT NULL,
    `username` VARCHAR(255) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `access_token` VARCHAR(255) NULL DEFAULT NULL,
    `sas_directory` VARCHAR(255) NOT NULL,
    PRIMARY KEY (`user_id`),
    UNIQUE INDEX `user_id_UNIQUE` (`user_id` ASC) VISIBLE,
    UNIQUE INDEX `username_UNIQUE` (`username` ASC) VISIBLE)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `spring_azure_blob`.`file_info`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `spring_azure_blob`.`file_info` ;

CREATE TABLE IF NOT EXISTS `spring_azure_blob`.`file_info` (
    `file_id` VARCHAR(255) NOT NULL,
    `sas_token` VARCHAR(255) NOT NULL,
    `expired_at` DATETIME NULL DEFAULT NULL,
    `file_name` VARCHAR(255) NOT NULL,
    `file_path` VARCHAR(255) NOT NULL,
    `own_file` VARCHAR(255) NOT NULL,
    PRIMARY KEY (`file_id`),
    UNIQUE INDEX `file_id_UNIQUE` (`file_id` ASC) VISIBLE,
    UNIQUE INDEX `sas_token_UNIQUE` (`sas_token` ASC) VISIBLE,
    INDEX `user_fk_idx` (`own_file` ASC) VISIBLE,
    CONSTRAINT `file_user_fk`
    FOREIGN KEY (`own_file`)
    REFERENCES `spring_azure_blob`.`users` (`user_id`))
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `spring_azure_blob`.`sas`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `spring_azure_blob`.`sas` ;

CREATE TABLE IF NOT EXISTS `spring_azure_blob`.`sas` (
    `sas_token` VARCHAR(255) NOT NULL,
    `own_permis_sas` VARCHAR(255) NOT NULL,
    PRIMARY KEY (`sas_token`, `own_permis_sas`),
    INDEX `sas_user_fk_idx` (`own_permis_sas` ASC) VISIBLE,
    CONSTRAINT `sas_token_fk`
    FOREIGN KEY (`sas_token`)
    REFERENCES `spring_azure_blob`.`file_info` (`sas_token`),
    CONSTRAINT `sas_user_fk`
    FOREIGN KEY (`own_permis_sas`)
    REFERENCES `spring_azure_blob`.`users` (`user_id`))
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci;