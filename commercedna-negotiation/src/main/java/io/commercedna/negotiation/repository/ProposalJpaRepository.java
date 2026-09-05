package io.commercedna.negotiation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProposalJpaRepository extends JpaRepository<ProposalEntity, UUID> {

    Optional<ProposalEntity> findByProposalCode(String proposalCode);

    List<ProposalEntity> findByMerchantId(UUID merchantId);

    List<ProposalEntity> findByBuyerAgentDid(String buyerAgentDid);
}
