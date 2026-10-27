package org.racedaydreams.domain.activity;

import java.util.List;

/** Local shoe definitions; changing a shoe must preserve existing run assignments. */
public interface ShoeRepository {
    List<Shoe> shoes();
    void saveShoe(Shoe shoe);
}
