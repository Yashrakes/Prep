package com.RBA;

import com.RBA.Model.*;
import com.RBA.Service.DataPopulationService;
import com.RBA.Service.RuleExecutionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
public class CreditDecisionProcessorImpl implements CreditDecisionProcessor {

    Logger logger = LoggerFactory.getLogger(CreditDecisionProcessorImpl.class);
    @Autowired
    private TaskExecutor forkJoinPool;

    private final RuleExecutionService ruleExecutionService;

    private DataPopulationService dataPopulationService;

    // @RequiredArgsConstructor can also be used.
    public CreditDecisionProcessorImpl(TaskExecutor forkJoinPool, RuleExecutionService ruleExecutionService) {
        this.forkJoinPool = forkJoinPool;
        this.ruleExecutionService = ruleExecutionService;
    }

    public CreditDecisionResponse executeRules(CreditDecisionRequest creditDecisionRequest) {
        List<Rule> rules; // ruleservice.getRules() -> to get rules here.
        // Convert data from List of rules to rulelist on basis of ruletype.
        RuleList ruleList = new RuleList();

        /*
        Check creditDecisionRequest.getLimits() if limits are valid or not on basis of validation.
        Not Writing Code For Now...
        */

        /*
        Populating Credit Decision Request
        */
        dataPopulationService.populate(creditDecisionRequest);

        /*
        Prepare Request for Each Limit.
        */
        List<LimitSummary> limitSummaryList = new ArrayList<>();
        try {
            for(Limit limit : creditDecisionRequest.getLimits()){
                LimitSummary limitSummary = new LimitSummary();
                limitSummary.setLimitId(String.valueOf(limit.getLimitId()));
                ObjectMapper mapper = new ObjectMapper();
                CreditDecisionRequest copyCLR = mapper.readValue(creditDecisionRequest.toString(),CreditDecisionRequest.class);
                copyCLR.setLimits(List.of(limit));
                limitSummary.setCreditDecisionRequestForLimit(creditDecisionRequest);
                limitSummary.setRulesSummaryForLimit(ruleList);
                limitSummaryList.add(limitSummary);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        /*
        Execute Rule for each Limit
        */
        List<CompletableFuture<LimitSummary>> afterExecutionLimitSummaryList = limitSummaryList.stream().map(
                limitSummary -> CompletableFuture.supplyAsync(() -> {
                    return executeRulesAndUpdateLimitSummary(limitSummary);
                }, forkJoinPool)
        ).collect(Collectors.toList());
        limitSummaryList = afterExecutionLimitSummaryList.stream().map(CompletableFuture::join).collect(Collectors.toList());

        /*
        Prepare response for each Limit
        */
        prepareResposeForEachLimit(limitSummaryList);

        CreditDecisionResponse creditDecisionResponse = prepareFinalResponse(limitSummaryList);

        return creditDecisionResponse;
    }

    private  LimitSummary executeRulesAndUpdateLimitSummary(LimitSummary limitSummary){
        CreditDecisionRequest creditDecisionRequest = limitSummary.getCreditDecisionRequestForLimit();
        RuleList ruleList = limitSummary.getRulesSummaryForLimit();
        List<ExecutionSummary> differentiatorExecutionSummary = null;
        List<ExecutionSummary> increasingExecutionSummary = null;
        List<ExecutionSummary> decreasingExecutionSummary = null;
        differentiatorExecutionSummary = executeRulesFromRulesExecuteService(ruleList.getDifferentiatorRules(),creditDecisionRequest);
        List<ExecutionSummary> failedDifferentiatorRules = differentiatorExecutionSummary.stream().filter(rule -> rule.getRule().getResult().equals(Boolean.FALSE)).collect(Collectors.toList());

        if(!CollectionUtils.isEmpty(failedDifferentiatorRules)){
            //Any Differentiator Rules Failed
            ruleList.setLimitType("Limit Increase");
            increasingExecutionSummary = executeRulesFromRulesExecuteService(ruleList.getIncresingRules(),creditDecisionRequest);
        } else {
            //All Differentiator Rules Passed
            ruleList.setLimitType("Limit Decrease");
            decreasingExecutionSummary = executeRulesFromRulesExecuteService(ruleList.getDecreasingRules(),creditDecisionRequest);
        }

        decreasingExecutionSummary.addAll(differentiatorExecutionSummary);
        ruleList.setExecutionSummaryIncrease(increasingExecutionSummary);
        ruleList.setExecutionSummaryDecrease(decreasingExecutionSummary);
        return limitSummary;
    }

    private void prepareResposeForEachLimit(List<LimitSummary> limitSummaryList){
        List<CompletableFuture<LimitSummary>> responsePrepLimitSummary = limitSummaryList.stream().map(
                limitSummary -> CompletableFuture.supplyAsync(() -> {
                    return setResponseInLimitSummary(limitSummary);
                }, forkJoinPool)).collect(Collectors.toList());
    }

    private LimitSummary setResponseInLimitSummary(LimitSummary limitSummary){
        CreditDecisionResponse creditDecisionResponse = new CreditDecisionResponse();
        if(limitSummary.getRulesSummaryForLimit().getLimitType().equals("Limit Increase")){
            setDataUsingExecutionSummary(limitSummary.getRulesSummaryForLimit().getExecutionSummaryIncrease(), creditDecisionResponse);
        } else {
            setDataUsingExecutionSummary(limitSummary.getRulesSummaryForLimit().getExecutionSummaryDecrease(), creditDecisionResponse);
        }
        limitSummary.setCreditDecisionResponse(creditDecisionResponse);
        return limitSummary;
    }

    private void setDataUsingExecutionSummary(List<ExecutionSummary> executionSummary, CreditDecisionResponse creditDecisionResponse){
        if(executionSummary.stream().anyMatch(rule -> rule.getEvaluationResult().equals(Boolean.FALSE))){
            creditDecisionResponse.setOverallOutcome("Fail");
        } else {
            creditDecisionResponse.setOverallOutcome("Pass");
        }
        List<RuleResponse> fetchedRules = new ArrayList<>();
        for(ExecutionSummary summary : executionSummary){
            RuleResponse ruleResponse = new RuleResponse();
            ruleResponse.setRuleId(summary.getRule().getRuleId());
            ruleResponse.setRuleName(summary.getRule().getRuleName());
            ruleResponse.setResult(summary.getRule().getResult());
            fetchedRules.add(ruleResponse);
        }
        creditDecisionResponse.setRules(fetchedRules);
    }

    private CreditDecisionResponse prepareFinalResponse(List<LimitSummary> limitSummaryList){
        CreditDecisionResponse creditDecisionResponse = new CreditDecisionResponse();
        if(limitSummaryList.stream().anyMatch(summary -> summary.getCreditDecisionResponse().getOverallOutcome().equals(Boolean.FALSE))){
            creditDecisionResponse.setOverallOutcome("Fail");
        } else {
            creditDecisionResponse.setOverallOutcome("Pass");
        }
        Map<Long, RuleResponse> overallRulesResponse = new HashMap<>();
        for(LimitSummary limitSummary : limitSummaryList){
            List<RuleResponse> rules = limitSummary.getCreditDecisionResponse().getRules();
            for(RuleResponse rule : rules){
                if(overallRulesResponse.containsKey(rule.getRuleId()) &&
                        (!rule.getResult().equals(overallRulesResponse.get(rule.getRuleId()).getResult()))){
                    rule.setResult("True");
                }
                overallRulesResponse.put(rule.getRuleId(), rule);
            }
        }
        creditDecisionResponse.setRules(new ArrayList(overallRulesResponse.values()));
        return creditDecisionResponse;
    }

    private List<ExecutionSummary> executeRulesFromRulesExecuteService(List<Rule> rules, Object context){
        List<ExecutionSummary> executionSummary = ruleExecutionService.executeAllRules(rules,context);
        if(CollectionUtils.isEmpty(executionSummary)){
            logger.info("No rules return from rules service");
        }
        return executionSummary;
    }
}
