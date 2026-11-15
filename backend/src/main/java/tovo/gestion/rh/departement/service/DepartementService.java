package tovo.gestion.rh.departement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tovo.gestion.rh.common.exception.ResourceNotFoundException;
import tovo.gestion.rh.departement.dto.DepartementRequest;
import tovo.gestion.rh.departement.dto.DepartementResponse;
import tovo.gestion.rh.departement.entity.Departement;
import tovo.gestion.rh.departement.mapper.DepartementMapper;
import tovo.gestion.rh.departement.repository.DepartementRepository;
import tovo.gestion.rh.poste.repository.PosteRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DepartementService {

    private final DepartementRepository departementRepository;
    private final DepartementMapper departementMapper;
    private final PosteRepository posteRepository;

    // Pour le moment on ne vérifie pas encore les postes rattachés
    // (on le fera quand le module Poste existera)

    @Transactional(readOnly = true)
    public List<DepartementResponse> findAll() {
        return departementRepository.findAll()
                .stream()
                .map(departementMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DepartementResponse findById(Integer id) {
        Departement departement = departementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Département", id));
        return departementMapper.toResponse(departement);
    }

    public DepartementResponse create(DepartementRequest request) {
        if (departementRepository.existsByNomIgnoreCase(request.getNom())) {
            throw new IllegalArgumentException("Un département avec ce nom existe déjà");
        }

        Departement departement = departementMapper.toEntity(request);
        Departement saved = departementRepository.save(departement);
        return departementMapper.toResponse(saved);
    }

    public DepartementResponse update(Integer id, DepartementRequest request) {
        Departement departement = departementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Département", id));

        if (departementRepository.existsByNomIgnoreCaseAndIdNot(request.getNom(), id)) {
            throw new IllegalArgumentException("Un département avec ce nom existe déjà");
        }

        departementMapper.updateEntity(departement, request);
        Departement updated = departementRepository.save(departement);
        return departementMapper.toResponse(updated);
    }

    public void delete(Integer id) {
        Departement departement = departementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Département", id));

        // Vérifie s'il y a des postes rattachés
        if (posteRepository.existsByDepartementId(id)) {
            throw new IllegalStateException("Impossible de supprimer ce département : des postes y sont encore rattachés");
        }

        departementRepository.delete(departement);
    }
}