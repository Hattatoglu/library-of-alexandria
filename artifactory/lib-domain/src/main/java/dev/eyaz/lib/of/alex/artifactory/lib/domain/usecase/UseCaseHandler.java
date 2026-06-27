package dev.eyaz.lib.of.alex.artifactory.lib.domain.usecase;

public interface UseCaseHandler <T extends UseCase>{
    T handle(T usecase);
}
