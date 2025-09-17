package com.RBA.Service;

import com.RBA.Model.CreditDecisionRequest;
import com.RBA.Model.Limit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.runAsync;

@Service
public class DataPopulationService {

    @Autowired
    private final TaskExecutor forkJoinPool;
    private final MrcaPopulationService mrcaPopulationService;

    private final PartyDataPopulationService partyDataPopulationService;

    private final DalPopulationService dalPopulationService;

    public DataPopulationService(TaskExecutor forkJoinPool, MrcaPopulationService mrcaPopulationService, PartyDataPopulationService partyDataPopulationService, DalPopulationService dalPopulationService) {
        this.forkJoinPool = forkJoinPool;
        this.mrcaPopulationService = mrcaPopulationService;
        this.partyDataPopulationService = partyDataPopulationService;
        this.dalPopulationService = dalPopulationService;
    }

    public void populate(CreditDecisionRequest creditDecisionRequest) {

        for (Limit limit : creditDecisionRequest.getLimits()) {
            // Business Logic For Populate Common Fields.
            limit.setMaturityDays(limit.getLiveDetails().length());
            limit.setMaturityDays(limit.getLiveDetails().length());
        }
        // Main Data Population Using Multiple Services
        allOf(
                runAsync(() -> mrcaPopulationService.populate(creditDecisionRequest), forkJoinPool),
                runAsync(() -> partyDataPopulationService.populate(creditDecisionRequest), forkJoinPool),
                runAsync(() -> dalPopulationService.populate(creditDecisionRequest), forkJoinPool)
        ).join();
    }
}
