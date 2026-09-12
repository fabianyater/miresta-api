package com.miresta.table;

import java.util.List;

public interface ISalonService {
    List<SalonResponse> getSalons();

    SalonResponse createSalon(SalonRequest request);

    SalonResponse renameSalon(Long id, SalonRequest request);

    SalonResponse moveSalon(Long id, String direction);

    void deleteSalon(Long id);
}
