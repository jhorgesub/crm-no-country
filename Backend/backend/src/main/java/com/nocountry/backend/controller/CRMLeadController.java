package com.nocountry.backend.controller;

import com.nocountry.backend.dto.*;
import com.nocountry.backend.enums.Stage;
import com.nocountry.backend.services.CrmLeadService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/crmleads")
@RequiredArgsConstructor
public class CRMLeadController {

    private final CrmLeadService crmLeadService;

    @PostMapping
    public CrmLeadDTO create(@Valid @RequestBody CreateCrmLeadDTO dto) {
        return crmLeadService.create(dto);
    }

    @GetMapping("/{id}")
    public CrmLeadDTO getById(@PathVariable Long id) {
        return crmLeadService.getById(id);
    }

    @PutMapping("/{id}")
    public CrmLeadDTO update(@PathVariable Long id, @RequestBody UpdateCrmLeadDTO dto) {
        return crmLeadService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        crmLeadService.delete(id);
    }

    @GetMapping
    public List<CrmLeadDTO> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Stage stage) {
        return crmLeadService.getAll(name, email, stage);
    }

    @GetMapping("/deleted")
    public List<CrmLeadDTO> getDeleted() {
        return crmLeadService.getDeleted();
    }

}
