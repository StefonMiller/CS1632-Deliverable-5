import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import gov.nasa.jpf.vm.Verify;
import java.util.Random;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Code by @author Wonsun Ahn
 * 
 * <p>Uses the Java Path Finder model checking tool to check BeanCounterLogic in
 * various modes of operation. It checks BeanCounterLogic in both "luck" and
 * "skill" modes for various numbers of slots and beans. It also goes down all
 * the possible random path taken by the beans during operation.
 */

public class BeanCounterLogicTest {
	private static BeanCounterLogic logic; // The core logic of the program
	private static Bean[] beans; // The beans in the machine
	private static String failString; // A descriptive fail string for assertions

	private static int slotCount; // The number of slots in the machine we want to test
	private static int beanCount; // The number of beans in the machine we want to test
	private static boolean isLuck; // Whether the machine we want to test is in "luck" or "skill" mode

	/**
	 * Sets up the test fixture.
	 */
	@BeforeClass
	public static void setUp() {
		if (Config.getTestType() == TestType.JUNIT) {
			slotCount = 5;
			beanCount = 3;
			isLuck = true;
		} else if (Config.getTestType() == TestType.JPF_ON_JUNIT) {
			/*
			 * TODO: Use the Java Path Finder Verify API to generate choices for slotCount,
			 * beanCount, and isLuck: slotCount should take values 1-5, beanCount should
			 * take values 0-3, and isLucky should be either true or false. For reference on
			 * how to use the Verify API, look at:
			 * https://github.com/javapathfinder/jpf-core/wiki/Verify-API-of-JPF
			 */
			slotCount = Verify.getInt(1, 5);
			beanCount = Verify.getInt(0, 3);
			isLuck = Verify.getBoolean();
		} else {
			assert (false);
		}

		// Create the internal logic
		logic = BeanCounterLogic.createInstance(slotCount);
		// Create the beans
		beans = new Bean[beanCount];
		for (int i = 0; i < beanCount; i++) {
			beans[i] = Bean.createInstance(slotCount, isLuck, new Random(42));
		}

		// A failstring useful to pass to assertions to get a more descriptive error.
		failString = "Failure in (slotCount=" + slotCount
				+ ", beanCount=" + beanCount + ", isLucky=" + isLuck + "):";
	}

	@AfterClass
	public static void tearDown() {
	}

	/**
	 * Test case for void void reset(Bean[] beans).
	 * Preconditions: None.
	 * Execution steps: Call logic.reset(beans).
	 * Invariants: If beanCount is greater than 0,
	 *             remaining bean count is beanCount - 1
	 *             in-flight bean count is 1 (the bean initially at the top)
	 *             in-slot bean count is 0.
	 *             If beanCount is 0,
	 *             remaining bean count is 0
	 *             in-flight bean count is 0
	 *             in-slot bean count is 0.
	 */
	@Test
	public void testReset() {
		/*
		 * Currently, it just prints out the failString to demonstrate to you all the
		 * cases considered by Java Path Finder. If you called the Verify API correctly
		 * in setUp(), you should see all combinations of machines
		 * (slotCount/beanCount/isLucky) printed here:
		 * 
		 * Failure in (slotCount=1, beanCount=0, isLucky=false):
		 * Failure in (slotCount=1, beanCount=0, isLucky=true):
		 * Failure in (slotCount=1, beanCount=1, isLucky=false):
		 * Failure in (slotCount=1, beanCount=1, isLucky=true):
		 * ...
		 * 
		 * PLEASE REMOVE when you are done implementing.
		 */
		logic.reset(beans);
		
		//Counter for in-flight beans 
		int inFlightCount = 0;
		if (beanCount > 0) {
			//Ensure remaining beans are beanCount - 1
			assertEquals("Remaining bean count incorrect", beanCount - 1, logic.getRemainingBeanCount());
			
			//Loop through slots
			for (int i = 0; i < slotCount; i++) {
				
				//Each additional slot is another y level,
				//so the number of levels is equal to the number of slots.
				//If there is an in flight bean for the given X position, increment the counter
				if (logic.getInFlightBeanXPos(i) != -1) {
					inFlightCount++;
				}
				
				//Ensure there are no beans in any slots
				assertEquals("No beans should be in any slots", 0, logic.getSlotBeanCount(i));
			}
			
			//Ensure there is only 1 bean in flight
			assertEquals("In flight bean count should be 1 but is not", 1, inFlightCount);
		} else if (beanCount == 0) {
			//Ensure there are no remaining beans
			assertEquals("Remaining bean count is not 0", 0, logic.getRemainingBeanCount());
			
			//Loop through slots
			for (int i = 0; i < slotCount; i++) {
				
				//Each additional slot is another y level,
				//so the number of levels is equal to the number of slots.
				//If there is an in flight bean for the given X position, increment the counter
				if (logic.getInFlightBeanXPos(i) != -1) {
					inFlightCount++;
				}
				
				//Ensure there are no beans in any slots
				assertEquals("No beans should be in any slots", 0, logic.getSlotBeanCount(i));
			}
			
			//Ensure there are no beans in flight
			assertEquals("In flight bean count should be 0 but is not", 0, inFlightCount);
		}
	}

	/**
	 * Test case for boolean advanceStep().
	 * Preconditions: None.
	 * Execution steps: Call logic.reset(beans).
	 *                  Call logic.advanceStep() in a loop until it returns false (the machine terminates).
	 * Invariants: After each advanceStep(),
	 *             all positions of in-flight beans are legal positions in the logical coordinate system.
	 */
	@Test
	public void testAdvanceStepCoordinates() {
		logic.reset(beans);
		
		boolean running = true;
		while (running) {
			running = logic.advanceStep();
			
			for (int i = 0; i < slotCount; i++) {
				int inFlightBeanXPos = logic.getInFlightBeanXPos(i);
				if (inFlightBeanXPos != -1) {
					//Ensure each bean at every X position is within the bounds of the slots
					assertTrue("In-flight bean too far to the right", inFlightBeanXPos < slotCount);
					assertTrue("In-flight bean too far to the left", inFlightBeanXPos >= 0);
				}
			}
		}
	}

	/**
	 * Test case for boolean advanceStep().
	 * Preconditions: None.
	 * Execution steps: Call logic.reset(beans).
	 *                  Call logic.advanceStep() in a loop until it returns false (the machine terminates).
	 * Invariants: After each advanceStep(),
	 *             the sum of remaining, in-flight, and in-slot beans is equal to beanCount.
	 */
	@Test
	public void testAdvanceStepBeanCount() {
		logic.reset(beans);
		
		int total = 0;
		int inFlight = 0;
		int inSlot = 0;
		
		// Run simulation until finished
		boolean running = true;
		while (running) {
			running = logic.advanceStep();
			
			inFlight = 0;
			inSlot = 0;
			
			for (int i = 0; i < slotCount; i++) {
				if (logic.getInFlightBeanXPos(i) >= 0) {
					inFlight++;
				}
				
				inSlot += logic.getSlotBeanCount(i);
			}
			
			total = logic.getRemainingBeanCount() + inFlight + inSlot;
			assertEquals("Logic has incorrect number of total beans accounted for", beanCount, total);
		}
	}

	/**
	 * Test case for boolean advanceStep().
	 * Preconditions: None.
	 * Execution steps: Call logic.reset(beans).
	 *                  Call logic.advanceStep() in a loop until it returns false (the machine terminates).
	 * Invariants: After the machine terminates,
	 *             remaining bean count is 0
	 *             in-flight bean count is 0
	 *             in-slot bean count is beanCount.
	 */
	@Test
	public void testAdvanceStepPostCondition() {
		logic.reset(beans);
		
		// Run simulation until finished
		boolean running = true;
		while (running) {
			running = logic.advanceStep();
		}
		
		int inFlight = 0;
		int inSlot = 0;
		
		// Check all slots and x positions for beans
		for (int i = 0; i < slotCount; i++) {
			if (logic.getInFlightBeanXPos(i) >= 0) {
				inFlight++;
			}
			
			inSlot += logic.getSlotBeanCount(i);
		}
		
		assertEquals("Remaining bean count is not zero", 0, logic.getRemainingBeanCount());
		assertEquals("In-flight bean count is not zero", 0, inFlight);
		assertEquals("In-slot bean count doesn't match total beans used", beanCount, inSlot);
	}
	
	/**
	 * Test case for void lowerHalf()().
	 * Preconditions: None.
	 * Execution steps: Call logic.reset(beans).
	 *                  Call logic.advanceStep() in a loop until it returns false (the machine terminates).
	 *                  Call logic.lowerHalf().
	 * Invariants: After calling logic.lowerHalf(),
	 *             slots in the machine contain only the lower half of the original beans.
	 *             Remember, if there were an odd number of beans, (N+1)/2 beans should remain.
	 *             Check each slot for the expected number of beans after having called logic.lowerHalf().
	 */
	@Test
	public void testLowerHalf() {
		logic.reset(beans);
		
		// Run simulation until finished
		boolean running = true;
		while (running) {
			running = logic.advanceStep();
		}
		
		// If even: beanCount / 2
		// If odd: (beanCount + 1) / 2
		int expectedInSlot = (beanCount % 2 == 0 ? (beanCount / 2) : ((beanCount + 1) / 2));
		int leftOverBeans = 0;
		int beansNeeded = 0;
		int currentSlot = 0;
		
		int[] preHalfSlotCounts = new int[slotCount];
		
		// Record slot counts before taking lowerHalf()
		for(int i = 0; i < slotCount; i++) {
			preHalfSlotCounts[i] = logic.getSlotBeanCount(i);
		}
		
		// Take lower half of beans only
		logic.lowerHalf();
		
		// Check all slots for remaining beans
		for (int i = 0; i < slotCount; i++) {
			currentSlot = logic.getSlotBeanCount(i);
			
			// If all leftover beans have been counted, the rest of the slots should be empty
			if (leftOverBeans >= expectedInSlot) {
				assertEquals("Too many beans: upper slots should be empty", 0, currentSlot);
			} else {
				
				// Add to the leftover bean count
				beansNeeded = expectedInSlot - leftOverBeans;
				leftOverBeans += currentSlot;
				
				// Check for overflow:
				// beans left in this slot should be
				// the pre-half slot count, minus
				// the number of beans needed to reach the expected amount
				if (leftOverBeans >= expectedInSlot) {
					assertEquals("Wrong number of overflow beans in slot " + i,
							currentSlot, preHalfSlotCounts[i] - beansNeeded);
				} else {
					
					// Otherwise, the number of beans in this slot should be
					// the same as it was previously
					assertEquals("Wrong number of beans in slot " + i, 
							preHalfSlotCounts[i], currentSlot);
				}
			}
		}
		
		assertEquals("Leftover beans should be half of the original count", expectedInSlot, leftOverBeans);
	}
	
	/**
	 * Test case for void upperHalf().
	 * Preconditions: None.
	 * Execution steps: Call logic.reset(beans).
	 *                  Call logic.advanceStep() in a loop until it returns false (the machine terminates).
	 *                  Call logic.lowerHalf().
	 * Invariants: After calling logic.upperHalf(),
	 *             slots in the machine contain only the upper half of the original beans.
	 *             Remember, if there were an odd number of beans, (N+1)/2 beans should remain.
	 *             Check each slot for the expected number of beans after having called logic.upperHalf().
	 */
	@Test
	public void testUpperHalf() {
		logic.reset(beans);
		
		// Run simulation until finished
		boolean running = true;
		while (running) {
			running = logic.advanceStep();
		}
		
		// If even: beanCount / 2
		// If odd: (beanCount + 1) / 2
		int expectedInSlot = (beanCount % 2 == 0 ? (beanCount / 2) : ((beanCount + 1) / 2));
		int leftOverBeans = 0;
		int beansNeeded = 0;
		int currentSlot = 0;
		
		int[] preHalfSlotCounts = new int[slotCount];
		
		// Record slot counts before taking upperHalf()
		for(int i = 0; i < slotCount; i++) {
			preHalfSlotCounts[i] = logic.getSlotBeanCount(i);
		}
		
		// Take upper half of beans only
		logic.upperHalf();
		
		// Check all slots for remaining beans
		// starting from the upper-most slot
		for (int i = slotCount - 1; i >= 0; i--) {
			currentSlot = logic.getSlotBeanCount(i);
			
			if (leftOverBeans >= expectedInSlot) {
				assertEquals("Too many beans: lower slots should be empty", 0, currentSlot);
			} else {
				
				// Add to the leftover bean count
				beansNeeded = expectedInSlot - leftOverBeans;
				leftOverBeans += currentSlot;
				
				// Check for overflow:
				// beans left in this slot should be
				// the pre-half slot count, minus
				// the number of beans needed to reach the expected amount
				if (leftOverBeans >= expectedInSlot) {
					assertEquals("Wrong number of overflow beans in slot " + i,
							currentSlot, preHalfSlotCounts[i] - beansNeeded);
				} else {
					
					// Otherwise, the number of beans in this slot should be
					// the same as it was previously
					assertEquals("Wrong number of beans in slot " + i, 
							preHalfSlotCounts[i], currentSlot);
				}
			}
		}
		
		assertEquals("Leftover beans should be half of the original count", expectedInSlot, leftOverBeans);
	}
	
	/**
	 * Test case for void repeat().
	 * Preconditions: None.
	 * Execution steps: Call logic.reset(beans).
	 *                  Call logic.advanceStep() in a loop until it returns false (the machine terminates).
	 *                  Call logic.repeat();
	 *                  Call logic.advanceStep() in a loop until it returns false (the machine terminates).
	 * Invariants: If the machine is operating in skill mode,
	 *             bean count in each slot is identical after the first run and second run of the machine. 
	 */
	@Test
	public void testRepeat() {
		logic.reset(beans);
		
		// Run simulation until finished
		boolean running = true;
		while (running) {
			running = logic.advanceStep();
		}
		
		int[] firstSlotCount = new int[slotCount];
		
		// Count beans in all slots
		for (int i = 0; i < slotCount; i++) {
			firstSlotCount[i] = logic.getSlotBeanCount(i);
		}
		
		// Repeat above simulation results
		logic.repeat();
		
		// Run simulation again until finished
		running = true;
		while (running) {
			running = logic.advanceStep();
		}
		
		// Check beans in all slots against original
		int newCount = 0;
		for (int i = 0; i < slotCount; i++) {
			newCount = logic.getSlotBeanCount(i);
			if (!isLuck) {
				assertEquals("Bean count in slot " + i 
						+ " was not the same", firstSlotCount[i], newCount);
			}
		}
	}
	
	/**
	 * Test case for void repeat().
	 * Preconditions: None.
	 * Execution steps: Call logic.reset(beans).
	 *                  Call logic.advanceStep() in a loop until it returns false (the machine terminates).
	 *                  Call logic.repeat();
	 *                  Call logic.advanceStep() in a loop until it returns false (the machine terminates).
	 * Invariants: The number of remaining beans after reset(beans) should be the same as after repeat()
	 */
	@Test
	public void testRepeatSize() {
		logic.reset(beans);
		int resetBeans = logic.getRemainingBeanCount();
		
		// Run simulation until finished
		boolean running = true;
		while (running) {
			running = logic.advanceStep();
		}
		
		// Repeat above simulation results
		logic.repeat();
		int repeatBeans = logic.getRemainingBeanCount();
		
		assertEquals(" Bean count not the same after calling reset", resetBeans, repeatBeans);
	}
}
