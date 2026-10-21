package dev.jaderss.banking.observability;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class RecordingObservationHandler implements ObservationHandler<Observation.Context> {

    private final List<Observation.Context> stopped = new CopyOnWriteArrayList<>();

    @Override
    public void onStop(Observation.Context context) {
        stopped.add(context);
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return true;
    }

    public List<Observation.Context> stopped() {
        return List.copyOf(stopped);
    }
}
