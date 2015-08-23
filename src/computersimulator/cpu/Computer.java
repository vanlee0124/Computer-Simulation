
package computersimulator.cpu;

import computersimulator.components.*;

/**
 * Computer is the primary class used by the simulator. The core business logic
 * lives inside the components of computer. The primary components are CPU, IO,
 * and Memory.  The computer also is the primary controller of the clock cycle,
 * but delegates to the primary components.  Also, because our primary data 
 * type is an object, computer has one setter and getter for each register. This
 * prevents us from accidentally operating on stale references that are no longer
 * valid.
 */
public class Computer implements IClockCycle {
    
    private CentralProcessingUnit cpu;
    private MemoryControlUnit memory;
    private InputOutputController io;   

    public Computer() {        
        memory = new MemoryControlUnit();  
        cpu = new CentralProcessingUnit(memory); // contains ALU,  ControlUnit      
        io = new InputOutputController();
    }   
    
    /**
     * Clock Cycle for Computer
     * @throws Exception 
     */
    @Override
    public final void clockCycle() throws Exception{
        //System.out.println("-------- CLOCK CYCLE --------"); 
        /** @TODO: Turned off this message until part 2, we're running more 
        * clock cycles than necessary in part 1 because we not actually running 
        * a program yet. **/

        this.cpu.clockCycle();
        this.memory.clockCycle();
        this.io.clockCycle();                
    }

    /**
     *
     * @return CPU
     */
    public CentralProcessingUnit getCpu() {
        return cpu;
    }

    /**
     *
     * @return Memory Instance
     */
    public MemoryControlUnit getMemory() {
        return memory;
    }

    /**
     *
     * @return IO instance
     */
    public InputOutputController getIO() {
        return io;
    }

    /**
     * Since each Register contains a reference to a particular unit/word, 
     * we can't maintain that reference and instead must look it up every time.
     * @param name Name of Register/Variable
     * @return Unit value
     */
    public Unit getComponentValueByName(String name){
        switch(name){
            case "R0":
                return this.getCpu().getControlUnit().getGeneralPurposeRegister(0);
            case "R1":
                return this.getCpu().getControlUnit().getGeneralPurposeRegister(1);
            case "R2":
                return this.getCpu().getControlUnit().getGeneralPurposeRegister(2);        
            case "R3":
                return this.getCpu().getControlUnit().getGeneralPurposeRegister(3);
            case "X1":
                return this.getCpu().getControlUnit().getIndexRegister(1);
            case "X2":
                return this.getCpu().getControlUnit().getIndexRegister(2);
            case "X3":
                return this.getCpu().getControlUnit().getIndexRegister(3);
            case "MAR":
                return this.getMemory().getMAR();
            case "MBR":
                return this.getMemory().getMBR();
            case "PC":
                return this.getCpu().getControlUnit().getProgramCounter();
            case "CC":
                return this.getCpu().getControlUnit().getConditionCodeRegister();
            case "IR":
                return this.getCpu().getControlUnit().getInstructionRegister();
            default:
                return new Unit(13,0);
               
        }                  
    }
    
    /**
     * Since each Register contains a reference to a particular unit/word, 
     * we can't maintain that reference and instead must look it up every time.
     * @param name Name of Register/Variable
     * @param deposit Unit to Deposit
     */
    public void setComponentValueByName(String name, Unit deposit){
        
        switch(name){
            case "R0":
                this.getCpu().getControlUnit().getGeneralPurposeRegisters()[0].setValueBinary(deposit.getBinaryString());
                break;
            case "R1":
                this.getCpu().getControlUnit().getGeneralPurposeRegisters()[1].setValueBinary(deposit.getBinaryString());
                break;
            case "R2":
                this.getCpu().getControlUnit().getGeneralPurposeRegisters()[2].setValueBinary(deposit.getBinaryString());               
                break;
            case "R3":
                this.getCpu().getControlUnit().getGeneralPurposeRegisters()[3].setValueBinary(deposit.getBinaryString());                
                break;
            case "X1":
                this.getCpu().getControlUnit().getIndexRegisters()[0].setValueBinary(deposit.getBinaryString());
                break;
            case "X2":
                this.getCpu().getControlUnit().getIndexRegisters()[1].setValueBinary(deposit.getBinaryString());
                break;
            case "X3":
                this.getCpu().getControlUnit().getIndexRegisters()[2].setValueBinary(deposit.getBinaryString());
                break;
            case "MAR":                
                Unit depositUnit = new Unit(13);
                depositUnit.setValueBinary(deposit.getBinaryString());
                this.getMemory().setMAR(depositUnit);
                break;
            case "MBR":
                Word depositWord = new Word(deposit);
                this.getMemory().setMBR(depositWord);
                break;
            case "PC":
                this.getCpu().getControlUnit().getProgramCounter().setValueBinary(deposit.getBinaryString());
                break;
            case "CC":
                // This doesn't accept deposits
                break;
            case "IR":                
                this.getCpu().getControlUnit().getInstructionRegister().setValueBinary(deposit.getBinaryString());
                break;
        }                  
    }             
}
