package com.cine.cinelog.core.domain.policy;

import com.cine.cinelog.core.domain.model.Media;

public interface MediaPolicy {
    void validateInvariants(Media media);
}