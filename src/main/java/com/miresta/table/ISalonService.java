package com.miresta.table;

import java.util.List;

public interface ISalonService {
    List<SalonResponse> getSalons();

    SalonResponse createSalon(SalonRequest request);

    SalonResponse renameSalon(Long id, SalonRequest request);

    SalonResponse moveSalon(Long id, String direction);

    void deleteSalon(Long id);

    SalonLayoutResponse getLayout(Long salonId);

    SalonLayoutResponse saveLayout(Long salonId, String actingUserEmail);

    void applyLayout(Long salonId);
}
