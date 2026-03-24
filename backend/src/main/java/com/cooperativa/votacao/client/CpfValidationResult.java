package com.cooperativa.votacao.client;

public record CpfValidationResult(boolean cpfValido, VotingPermissionStatus status) {
}
