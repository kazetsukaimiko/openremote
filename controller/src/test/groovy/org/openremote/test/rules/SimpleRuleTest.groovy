package org.openremote.test.rules

import org.kie.api.KieServices
import org.kie.api.io.Resource
import org.openremote.controller.ControllerService
import org.openremote.controller.event.SwitchEvent
import org.openremote.controller.rules.RuleEngine
import org.openremote.test.ContainerTrait
import org.openremote.test.util.EventGrabProcessor
import spock.lang.Specification

import java.util.stream.Stream

/**
 * Basic rule engine test, trigger a rule through an event.
 * The rule dispatches a new event, replacing the original event.
 */
class SimpleRuleTest extends Specification implements ContainerTrait {

    def "Sensor event replaced by rule-triggered event"() {

        given: "a controller deployment"
        def controllerDeploymentXml = getClass().getResourceAsStream(
                "/org/openremote/test/rules/simple/controller.xml"
        )

        and: "event processors and rules"
        def ruleEngineProcessor = new RuleEngine() {
            @Override
            protected Stream<Resource> getResources(KieServices kieServices) {
                Stream.of(
                        kieServices.getResources().newClassPathResource(
                                "org/openremote/test/rules/simple/EventMod.drl"
                        )
                )
            }
        }
        def grabProcessor = new EventGrabProcessor();

        and: "the started controller server"
        def controllerService = new ControllerService(
                controllerDeploymentXml,
                new TestCommandFactory(),
                ruleEngineProcessor,
                grabProcessor
        )
        def services = Stream.of(controllerService)
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), services)

        when: "we wait for initial state polling of sensor and rule execution"
        Thread.sleep(500);

        then: "the rules should immediately switch the sensor off again"
        def lastEvent = grabProcessor.lastEvent;
        lastEvent.getSourceID() == 123
        lastEvent.getSource() == "TestSensor"
        lastEvent instanceof SwitchEvent
        def grabbedSwitchEvent = (SwitchEvent) lastEvent;
        grabbedSwitchEvent.getValue() == "off"
        grabbedSwitchEvent.getState() == SwitchEvent.State.OFF

        and: "the total event count is 1, as the event fired in the rule terminates processing of our switch event"
        grabProcessor.totalEventCount == 1

        and: "the data context state of the sensor should be 'off'"
        controllerService.getDataContext().queryValue(123) == "off"

        and: "the deployment model should work"
        controllerService.getDeployment().getCommandDefinition(456).getCommandID() == 456
        controllerService.getDeployment().getCommandDefinition(123123) == null
        controllerService.getDeployment().getCommandDefinition("TestDevice", "TestCommand").getCommandID() == 456
        controllerService.getDeployment().getCommandDefinition("TestDevice", "NoSuchThing") == null
        controllerService.getDeployment().getCommandDefinition("NoSuchThing", "NoSuchThing") == null
        controllerService.getDeployment().getCommandDefinition("", "") == null
        controllerService.getDeployment().getCommandDefinition(null, "") == null
        controllerService.getDeployment().getCommandDefinition("", null) == null
        controllerService.getDeployment().getCommandDefinition(null, null) == null
        controllerService.getDeployment().getCommandDefinition("TestCommand").getCommandID() == 456
        controllerService.getDeployment().getCommandDefinition("NoSuchThing") == null
        controllerService.getDeployment().getCommandDefinition("") == null
        controllerService.getDeployment().getCommandDefinition(null) == null
        controllerService.getDeployment().getDevices().length == 1
        controllerService.getDeployment().getDevices()[0].getName() == "TestDevice"
        controllerService.getDeployment().getDevices()[0].getDeviceID() == 111
        controllerService.getDeployment().getDevice("TestDevice").getName() == "TestDevice"
        controllerService.getDeployment().getDevice("TestDevice").getDeviceID() == 111

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
