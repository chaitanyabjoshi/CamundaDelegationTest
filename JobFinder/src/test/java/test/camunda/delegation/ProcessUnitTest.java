package test.camunda.delegation;

import org.apache.ibatis.logging.LogFactory;
import org.camunda.bpm.engine.runtime.JobQuery;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.extension.process_test_coverage.junit.rules.TestCoverageProcessEngineRuleBuilder;
import org.camunda.bpm.engine.test.Deployment;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.*;
import static org.junit.Assert.*;

/**
 * Test case starting an in-memory database-backed Process Engine.
 */
public class ProcessUnitTest {

  @ClassRule
  @Rule
  public static ProcessEngineRule rule = TestCoverageProcessEngineRuleBuilder.create().build();

  private static final String PROCESS_DEFINITION_KEY = "JobFinder";

  static {
    LogFactory.useSlf4jLogging(); // MyBatis
  }

  @Before
  public void setup() {
    init(rule.getProcessEngine());
  }

  /**
   * Just tests if the process definition is deployable.
   */
  @Test
  @Deployment(resources = "process.bpmn")
  public void testParsingAndDeployment() {
    // nothing is done here, as we just want to check for exceptions during deployment
  }

  @Test
  @Deployment(resources = "process.bpmn")
  public void testHappyPath() {
    // Start instance 1
    processEngine().getRuntimeService().startProcessInstanceByKey(PROCESS_DEFINITION_KEY);
    // Start instance 2
    processEngine().getRuntimeService().startProcessInstanceByKey(PROCESS_DEFINITION_KEY);
    // Start instance 3
    processEngine().getRuntimeService().startProcessInstanceByKey(PROCESS_DEFINITION_KEY);
	  // ProcessInstance processInstance = processEngine().getRuntimeService().startProcessInstanceByKey(PROCESS_DEFINITION_KEY);
	  // JobQuery query = processEngine().getManagementService().createJobQuery().processInstanceId(processInstance.getId());
    // System.out.println("===========================================================");
    // System.out.println("\nJob Count: " + query.count());
    // for (var job : query.list()) {
    //   System.out.println("\nJob Details: " + job.toString());      
    // }
    // System.out.println("===========================================================");
	  // Now: Drive the process by API and assert correct behavior by camunda-bpm-assert
  }

}
