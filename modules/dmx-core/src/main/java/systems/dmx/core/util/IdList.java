package systems.dmx.core.util;

import java.util.ArrayList;



public class IdList extends ArrayList<Long> {

    public IdList() {
    }

    public IdList(String ids) {
        for (String id : ids.split(",")) {
            add(Long.parseLong(id));
        }
    }
}
