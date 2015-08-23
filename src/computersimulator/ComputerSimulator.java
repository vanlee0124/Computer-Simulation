package computersimulator;

import computersimulator.components.*;
import javax.swing.SwingUtilities;
import computersimulator.cpu.Computer;
import computersimulator.gui.OperatorConsole;

/**
 * Computer Simulator Program - This controls the GUI and instantiates a 
 * Computer, which represents the main simulator. 
 * @TODO: It also contains a few debug conditions that are set to ease testing in Part 1.
 */
public class ComputerSimulator {
    
    private static Computer computer;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        computer = new Computer();  // (contains memory, cpu, and IO)
               
        /** @TODO: Load ROM on boot via simple ROM loader 
         *      ROM Loader should read a boot program from a virtual card 
         *      reader to memory, then transfer execution to program.
         *      The card reader is implemented as a file. (via IO Controller?)
         *  Question: Should this be instantiated by the I/O Controller, or does vice-versa?
         */
                          
        
        OperatorConsole opconsole = new OperatorConsole();        
        opconsole.setComputer(computer); // pass computer instance into GUI
        
        
     
        /***** Testing Data *****/
        // @TODO Remove this after Phase 1
      //  computer.getMemory().engineerSetMemoryLocation(new Unit(13, 52), new Word(100));
      //  computer.getMemory().engineerSetMemoryLocation(new Unit(13, 100), new Word(1023));
      //  computer.getMemory().engineerSetMemoryLocation(new Unit(13, 152), new Word(512));
       // computer.getMemory().engineerSetMemoryLocation(new Unit(13, 512), new Word(768));
        computer.getMemory().engineerSetMemoryLocation(new Unit(13, 223), new Word(500));
        computer.getCpu().getControlUnit().setGeneralPurposeRegister(0, new Word(2));
        computer.getCpu().getControlUnit().setIndexRegister(1, new Unit(13,100));
        computer.getMemory().setMAR(new Unit(13,1));
     //   computer.getMemory().setMBR(new Word(132731));
        computer.getCpu().getControlUnit().setProgramCounter(new Unit(13, 1)); // Start at 1
        //@TODO: Note for TESTING.. Make sure to set MAR FIRST, then MBR. (otherwise Fetch)
                
        
        
        
        SwingUtilities.invokeLater(opconsole);
        
        
    }
    
}
