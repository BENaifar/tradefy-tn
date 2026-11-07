package tn.esprit.pi5eme.service.portfolio;

import tn.esprit.pi5eme.dto.portfolio.AllocationResponse;
import tn.esprit.pi5eme.dto.portfolio.AllocationTargetRequest;
import tn.esprit.pi5eme.dto.portfolio.AllocationTargetResponse;
import tn.esprit.pi5eme.dto.portfolio.RebalanceSuggestion;

import java.util.List;

public interface IAllocationService {

    /** Current allocation; cash is always a slice with key CASH. Null sectors are grouped as "Non classé". */
    AllocationResponse getAllocation(Long userId, AllocationGroupBy by);

    List<AllocationTargetResponse> getTargets(Long userId);

    /**
     * Replaces all targets. All targets must share one type, keys must be unique, each percent in (0,100],
     * the sum at most 100 (the rest is the implicit cash target). Violations give BadRequestException.
     */
    List<AllocationTargetResponse> setTargets(Long userId, List<AllocationTargetRequest> targets);

    /** INSTRUMENT targets only; other target types give an empty list. Suggestions only, nothing is executed. */
    List<RebalanceSuggestion> getRebalanceSuggestions(Long userId);
}
