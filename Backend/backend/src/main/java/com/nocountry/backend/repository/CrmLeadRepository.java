package com.nocountry.backend.repository;

import com.nocountry.backend.entity.CrmLead;
import com.nocountry.backend.enums.Stage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CrmLeadRepository extends JpaRepository<CrmLead, Long> {
        List<CrmLead> findByDeletedFalse();

        List<CrmLead> findByDeletedFalseAndNameContainingIgnoreCase(String name);

        List<CrmLead> findByDeletedFalseAndEmailContainingIgnoreCase(String email);

        List<CrmLead> findByDeletedFalseAndStage(Stage stage);

        List<CrmLead> findByDeletedFalseAndNameContainingIgnoreCaseAndStage(String name, Stage stage);

        List<CrmLead> findByDeletedFalseAndEmailContainingIgnoreCaseAndStage(String email, Stage stage);

        List<CrmLead> findByDeletedTrue();

        boolean existsByEmailIgnoreCase(String email);

        Optional<CrmLead> findFirstByEmailIgnoreCase(String email);

        /**
         * Busca un Lead por número de teléfono.
         * Utilizado para identificar o crear Leads desde mensajes de WhatsApp.
         */
        Optional<CrmLead> findByPhone(String phone);
}
