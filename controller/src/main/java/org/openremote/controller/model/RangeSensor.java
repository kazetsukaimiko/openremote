package org.openremote.controller.model;

import org.openremote.controller.command.EventProducerCommand;
import org.openremote.controller.deploy.SensorDefinition;
import org.openremote.controller.event.Event;
import org.openremote.controller.event.RangeEvent;

import java.util.logging.Logger;

public class RangeSensor extends Sensor {

    private static final Logger LOG = Logger.getLogger(RangeSensor.class.getName());

    private int minValue;
    private int maxValue;

    public RangeSensor(SensorDefinition sensorDefinition, EventProducerCommand eventProducerCommand, int minValue, int maxValue) {
        super(sensorDefinition, eventProducerCommand);
        if (minValue > maxValue)
            throw new IllegalArgumentException("Sensor value min '" + minValue + "' is larger than max '" + maxValue + "'");
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public int getMaxValue() {
        return maxValue;
    }

    public int getMinValue() {
        return minValue;
    }

    @Override
    protected Event processEvent(String value) {
        try {
            return new RangeEvent(
                getSensorDefinition().getSensorID(),
                getSensorDefinition().getName(),
                new Integer(value.trim()),
                getMinValue(),
                getMaxValue()
            );
        } catch (NumberFormatException exception) {
            if (!isUnknownSensorValue(value)) {
                LOG.warning("Range sensor '" + getSensorDefinition() + "' produced a non-integer value: " + value);
            }
            return new UnknownEvent(this);
        }

    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "sensorDefinition=" + getSensorDefinition() +
            ", minValue=" + minValue +
            ", maxValue=" + maxValue +
            '}';
    }
}


