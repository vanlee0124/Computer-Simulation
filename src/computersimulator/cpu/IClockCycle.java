
package computersimulator.cpu;

/**
 * This is the primary interface for the clock cycle. This method will be
 * called once per clock cycle. It should then accept control flow and run 
 * any micro clock cycles.  Upon completion it should call/set the 
 * clockCycleComplete (@TODO: TBD).  If a micro instruction is blocking,
 * that is it requires a clock cycle to finish, then it can call this 
 * function sooner to end the control flow and jump to the next cycle. 
 * @throws java.lang.Exception
 */
public interface IClockCycle {
    

    public void clockCycle() throws Exception;
}
