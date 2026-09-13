package com.miresta.table;

import java.util.List;

public interface ISalonService {
    List<SalonResponse> getSalons();

    SalonResponse createSalon(SalonRequest request);

    SalonResponse renameSalon(Long id, SalonRequest request);

    SalonResponse moveSalon(Long id, String direction);

    void deleteSalon(Long id);

    List<SalonLayoutResponse> getLayouts(Long salonId);

    SalonLayoutResponse saveLayout(Long salonId, SalonLayoutRequest request, String actingUserEmail);

    SalonLayoutResponse renameLayout(Long salonId, Long layoutId, SalonLayoutRequest request);

    void deleteLayout(Long salonId, Long layoutId);

    void applyLayout(Long salonId, Long layoutId);
}
