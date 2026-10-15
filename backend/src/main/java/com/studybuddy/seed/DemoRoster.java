package com.studybuddy.seed;

import java.util.Map;
import java.util.Set;

/**
 * The demo students' account ids by handle, and which of them this seed run created.
 *
 * @param ids     every demo student's id, keyed by handle
 * @param created handles created in this run; the others already existed and were left alone
 */
public record DemoRoster(Map<String, Long> ids, Set<String> created) {

    public Long id(String handle) {
        Long id = ids.get(handle);
        if (id == null) {
            throw new IllegalStateException("Demo data refers to unknown student " + handle);
        }
        return id;
    }

    /** True when no demo student existed before this run, so replaying their history is safe. */
    public boolean isFresh() {
        return created.size() == ids.size();
    }
}
