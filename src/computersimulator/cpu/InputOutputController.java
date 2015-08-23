package computersimulator.cpu;

/**
 * InputOutputController - I/O operations communicate with peripherals attached to the computer system. 
 * We need to simulate a card reader by reading a file from disk. We also need to simulate a GUI with a 
 * console printer and a pane that simulates a console keyboard.
 * @author george
 */
public class InputOutputController implements IClockCycle {

    public InputOutputController() {
        
    }
    
    
    /**
     * Clock cycle. This is the main function which causes the IOController to do work.
     *  This serves as a publicly accessible method, but delegates to other methods.
     */
    public void clockCycle(){
        // @TODO: Stubbed until Part 2
    }  
    
}
