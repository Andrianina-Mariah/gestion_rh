package tovo.gestion.rh.poste.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tovo.gestion.rh.common.exception.ResourceNotFoundException;
import tovo.gestion.rh.departement.entity.Departement;
import tovo.gestion.rh.departement.repository.DepartementRepository;
import tovo.gestion.rh.poste.dto.PosteRequest;
import tovo.gestion.rh.poste.dto.PosteResponse;
import tovo.gestion.rh.poste.entity.Poste;
import tovo.gestion.rh.poste.mapper.PosteMapper;
import tovo.gestion.rh.poste.repository.PosteRepository;
import tovo.gestion.rh.demanderecrutement.repository.DemandeRecrutementRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PosteService {

    private final PosteRepository posteRepository;
    private final DepartementRepository departementRepository;
    private final PosteMapper posteMapper;
    private final DemandeRecrutementRepository demandeRepository;

    @Transactional(readOnly = true)
    public List<PosteResponse> findAll(Integer departementId) {
        List<Poste> postes;

        if (departementId != null) {
            postes = posteRepository.findByDepartementId(departementId);
        } else {
            postes = posteRepository.findAll();
        }

        return postes.stream()
                .map(posteMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PosteResponse findById(Integer id) {
        Poste poste = posteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Poste", id));
        return posteMapper.toResponse(poste);
    }

    public PosteResponse create(PosteRequest request) {
        Departement departement = departementRepository.findById(request.getDepartementId())
                .orElseThrow(() -> new ResourceNotFoundException("Département", request.getDepartementId()));

        Poste poste = posteMapper.toEntity(request, departement);
        Poste saved = posteRepository.save(poste);
        return posteMapper.toResponse(saved);
    }

    public PosteResponse update(Integer id, PosteRequest request) {
        Poste poste = posteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Poste", id));

        Departement departement = departementRepository.findById(request.getDepartementId())
                .orElseThrow(() -> new ResourceNotFoundException("Département", request.getDepartementId()));

        posteMapper.updateEntity(poste, request, departement);
        Poste updated = posteRepository.save(poste);
        return posteMapper.toResponse(updated);
    }

    public void delete(Integer id) {
        Poste poste = posteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Poste", id));

        // TODO: vérifier qu'aucune demande de recrutement n'utilise ce poste
        // (à faire dans le module DemandeRecrutement)
        if (demandeRepository.existsByPosteId(id)) {
            throw new IllegalStateException("Impossible de supprimer ce poste : il est utilisé par une ou plusieurs demandes");
        }

        posteRepository.delete(poste);
    }
}