package com.cooperativa.votacao.client;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class RandomCpfValidationClient implements CpfValidationClient {

    @Override
    public CpfValidationResult validate(String cpf) {
        boolean cpfValido = ThreadLocalRandom.current().nextBoolean();
        if (!cpfValido) {
            return new CpfValidationResult(false, null);
        }

        VotingPermissionStatus status = ThreadLocalRandom.current().nextBoolean()
                ? VotingPermissionStatus.ABLE_TO_VOTE
                : VotingPermissionStatus.UNABLE_TO_VOTE;
        return new CpfValidationResult(true, status);
    }
}
