package com.cine.cinelog.core.application.ports.in.season;

import com.cine.cinelog.core.domain.model.Season;
import java.util.List;

public interface ListSeasonsUseCase {
    List<Season> execute();
}
