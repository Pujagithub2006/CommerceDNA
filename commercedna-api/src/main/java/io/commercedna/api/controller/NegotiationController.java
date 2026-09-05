package io.commercedna.api.controller;

import io.commercedna.negotiation.dto.NegotiateChatRequest;
import io.commercedna.negotiation.dto.NegotiateChatResponse;
import io.commercedna.negotiation.repository.ProposalEntity;
import io.commercedna.negotiation.service.NegotiationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/negotiate")
@Tag(name = "Negotiation Engine", description = "Agentic negotiation, intent compiler, and deterministic margin policy airgap APIs")
public class NegotiationController {

    private final NegotiationService negotiationService;

    public NegotiationController(NegotiationService negotiationService) {
        this.negotiationService = Objects.requireNonNull(negotiationService);
    }

    @PostMapping("/chat")
    @Operation(summary = "Converse with AI sales agent and negotiate terms with deterministic airgap safety")
    public ResponseEntity<NegotiateChatResponse> chat(@Valid @RequestBody NegotiateChatRequest request) {
        NegotiateChatResponse response = negotiationService.handleChatNegotiation(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/propose")
    @Operation(summary = "Submit a direct structured proposal with Ed25519 digital signature")
    public ResponseEntity<NegotiationService.ProposalResult> propose(
            @Valid @RequestBody NegotiationService.SubmitProposalCommand command
    ) {
        NegotiationService.ProposalResult result = negotiationService.submitProposal(command);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{code}")
    @Operation(summary = "Fetch proposal details and merchant counter-signature by proposal code")
    public ResponseEntity<ProposalEntity> getProposal(@PathVariable("code") String code) {
        ProposalEntity entity = negotiationService.getProposalByCode(code);
        return ResponseEntity.ok(entity);
    }
}
