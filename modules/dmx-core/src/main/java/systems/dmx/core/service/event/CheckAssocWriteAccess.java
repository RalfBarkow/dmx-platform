package systems.dmx.core.service.event;

import systems.dmx.core.service.EventListener;



public interface CheckAssocWriteAccess extends EventListener {

    void checkAssocWriteAccess(long assocId);
}
