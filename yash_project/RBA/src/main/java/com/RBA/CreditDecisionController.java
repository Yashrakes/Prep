package com.RBA;

import com.RBA.Model.CreditDecisionRequest;
import com.RBA.Model.CreditDecisionResponse;
import jakarta.websocket.server.PathParam;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class CreditDecisionController {
    Logger logger = LoggerFactory.getLogger(CreditDecisionController.class);
    private final CreditDecisionProcessor creditDecisionProcessor;

    // @RequiredArgsConstructor can also be used.
    public CreditDecisionController(CreditDecisionProcessor creditDecisionProcessor) {
        this.creditDecisionProcessor = creditDecisionProcessor;
    }

    @PostMapping(value="/request/execute")
    public ResponseEntity<CreditDecisionResponse> executeRules(@RequestBody CreditDecisionRequest creditDecisionRequest){
        CreditDecisionResponse creditDecisionResponse = new CreditDecisionResponse();
        logger.info("Starting RBA for request : {}", creditDecisionRequest.getRequestId());
        creditDecisionProcessor.executeRules(creditDecisionRequest);
        return ResponseEntity.ok().body(creditDecisionResponse);
    }
    // /save/a?id  payload =CreditDecisionRequest
    @RequestMapping(method = RequestMethod.POST , path = "/save/{a}" , consumes = {"application/json"})
    public ResponseEntity<CreditDecisionResponse> exec(@PathVariable(value ="a") Long a, @RequestBody CreditDecisionRequest cd,
                                                       @RequestParam(name = "id") Long R){
        return ResponseEntity.ok().body(null);
    }
}
