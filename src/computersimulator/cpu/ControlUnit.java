package computersimulator.cpu;

import computersimulator.components.*;
import java.util.HashMap;

/**
 * It is the task of the control unit to fetch the next instruction from 
 *   memory to be executed, decode it (i.e., determine what is to be done), and 
 *   execute it by issuing the appropriate command to the ALU, memory, and the 
 *   I/O controllers.
 * 
 * @TODO We need some way to ensure that registers don't get converted to different size units. I accidentally cast PC to 4 and no error happened immediately.
 */
public class ControlUnit implements IClockCycle {

    // PC	13 bits	Program Counter: address of next instruction to be executed
    private Unit programCounter;
    
    //IR	20 bits	Instruction Register: holds the instruction to be executed
    private Word instructionRegister;
    
    private HashMap<String,Unit> instructionRegisterDecoded;
    
    //MSR	20 bits	Machine Status Register: certain bits record the status of the health of the machine
    private Word machineStatusRegister;

    //MFR	4 bits	Machine Fault Register: contains the ID code if a machine fault after it occurs
    private Unit machineFaultRegister;
    
    //X1…X3	13 bits	Index Register: contains a 13-bit base address that supports base register addressing of memory.
    private Unit[] indexRegisters = new Unit[3];
    
    //R1…R3	20 bits General Purpose Registers (GPRs) – each 20 bits in length
    private Word[] gpRegisters = new Word[4];    
    
    //CC	4 bits	Condition Code: set when arithmetic/logical operations are executed; 
    //          it has four 1-bit elements: overflow, underflow, division by zero, equal-or-not. 
    //          OVERFLOW[0], UNDERFLOW[1], DIVZERO[2], EQUALORNOT[3]
    private Unit conditionCode;    
    
    
    /**************************************
     * All the variables below are internal and used to maintain state of the control unit
     *************************************/
    
    // Effective Address   ENGINEER Console: Used to hold EA temporarily in microcycles
    private Unit effectiveAddress;
    
    // Engineer: Internal flag to signal that a blocking operation occurred (memory read) forcing a clock cycle
    private boolean blocked = false;
    
    // used to control state of EA
    private int eaState;
    private static final int EA_DIRECT=0;
    private static final int EA_REGISTER_INDIRECT=1;
    private static final int EA_INDEXED=2;
    private static final int EA_INDEXED_OFFSET=3;
     
    // used to control the instruction cycle
    private int state;
    private static final int STATE_NONE=0;
    private static final int STATE_FETCH_INSTRUCTION=1;
    private static final int STATE_DECODE_INSTRUCTION=2;
    private static final int STATE_EXECUTE_INSTRUCTION=3;
            
    //used for Condition Code Register
    public final static int CONDITION_REGISTER_OVERFLOW = 0;
    public final static int CONDITION_REGISTER_UNDERFLOW = 1;
    public final static int CONDITION_REGISTER_DIVZERO = 2;
    public final static int CONDITION_REGISTER_EQUALORNOT = 3;
    
    private static final int MICROSTATE_EXECUTE_COMPLETE=999;
    
    private static final int OPCODE_HLT=0;
    private static final int OPCODE_LDR=1;
    private static final int OPCODE_STR=2;
    private static final int OPCODE_LDA=3;
    private static final int OPCODE_LDX=41;
    private static final int OPCODE_STX=42;
    private static final int OPCODE_AMR=4;
    private static final int OPCODE_SMR=5;
    private static final int OPCODE_AIR=6;
    private static final int OPCODE_SIR=7;
    private static final int OPCODE_JMP=13;
    private static final int OPCODE_JZ=10;
    private static final int OPCODE_JNE=11;
    private static final int OPCODE_JGE=17;
    private static final int OPCODE_SOB=16;
    private static final int OPCODE_JCC=12;
    private static final int OPCODE_RFS=15;
    private static final int OPCODE_JSR=14;
    private static final int OPCODE_SRC=31;
    private static final int OPCODE_RRC=32;


    private static final int OPCODE_TRR=22;
    private static final int OPCODE_AND=23;

 



    private static final int OPCODE_ORR=24;
    private static final int OPCODE_NOT=25;

    
    // Engineer: used to control micro step, defined per state
    private Integer microState = null;
        
    // memory reference
    private MemoryControlUnit memory;
    
    // ALU Reference
    private ArithmeticLogicUnit alu;
    
    // Unit reference
    private Unit uRef;
    
    // nextPC	13 bits	Next Program Counter: Interal Register Used to signal program counter was adjusted by instruction
    private Unit nextProgramCounter;
    
    public ControlUnit(MemoryControlUnit mem, ArithmeticLogicUnit aluRef) {
        this.instructionRegister = new Word();
        this.programCounter = new Unit(13);
        this.machineStatusRegister = new Word();
        this.machineFaultRegister = new Unit(4);
        this.state = ControlUnit.STATE_NONE;
        this.memory = mem;        
        this.alu=aluRef;
        this.conditionCode = new Unit(4);  
        this.clearConditions();        
        
        for(int x=0;x<3;x++){
            this.indexRegisters[x] = new Unit(13);
        }        
        
        for(int x=0;x<4;x++){
            this.gpRegisters[x] = new Word();
        }                         
    }
    
   
    public int getConditionCode(int ConditionRegister) {
        Integer[] raw = this.conditionCode.getBinaryArray();        
        return raw[ConditionRegister];
    }

    /**
     * Used for GUI
     * @return ConditionCodeRegister Unit(4) - 
     */
    public Unit getConditionCodeRegister() {
        return conditionCode;
    }    
    
    /**
     * Set a Condition Flag
     * Usage:  this.setCondition(ArithmeticLogicUnit.CONDITION_REGISTER_OVERFLOW);
     * @param ConditionRegister (see static variables)
     */
    public void setCondition(int ConditionRegister){
        Integer[] raw = this.conditionCode.getBinaryArray();
        raw[ConditionRegister] = 1;
        
        StringBuilder ret = new StringBuilder();
        for (Integer el : raw) {
            ret.append(el);
        }
        this.conditionCode.setValueBinary(ret.toString());        
    }
    

    
    /**
     * Unset a Condition Flag
     * Usage:  this.unsetCondition(ArithmeticLogicUnit.CONDITION_REGISTER_OVERFLOW);
     * @param ConditionRegister (see static variables)
     */
    public void unsetCondition(int ConditionRegister){
        Integer[] raw = this.conditionCode.getBinaryArray();
        raw[ConditionRegister] = 0;
        
        StringBuilder ret = new StringBuilder();
        for (Integer el : raw) {
            ret.append(el);
        }
        this.conditionCode.setValueBinary(ret.toString());         
    }
    
    /**
     * Clear any previously set condition codes
     */
    public final void clearConditions(){
        this.conditionCode.setValueBinary("0000");
    }    
    
    public Unit getProgramCounter() {
        return programCounter;
    }

    public Word getInstructionRegister() {
        return instructionRegister;
    }

    public Word getMachineStatusRegister() {
        return machineStatusRegister;
    }

    public Unit getMachineFaultRegister() {
        return machineFaultRegister;
    }

    public Unit[] getIndexRegisters() {
        return indexRegisters;
    }
     /**
     *Use to set Index Register IX
     * @param ixid IndexRegisters Id(1~3)
     * @param IndexRegister data
     */
    public void setIndexRegister(int ixid,Unit IndexRegister)
    {
        if(ixid<4&&ixid>0){ // IX1-3, stored internally at 0-2
            this.indexRegisters[ixid-1]=IndexRegister;
        }
    }
    
    /**
     * Returns a translated index register value (1-3) becomes (0-2)
     * @param ixid IndexRegisters Id(1~3)
     * @return Unit value
     */
    public Unit getIndexRegister(int ixid){
        if(ixid<4&&ixid>0){ // IX1-3, stored internally at 0-2
            return this.indexRegisters[ixid-1];
        } else {
            return null;
        }
    }

    public Word[] getGeneralPurposeRegisters() {
        return gpRegisters;
    }  
     /**
     *Use to set General Purpose Register
     * @param RFI GeneralPurposeRegisterValuesId(0~3)
     * @param GeneralPurposeRegisterValue initial data
     */
    public void setGeneralPurposeRegister(int RFI,Word GeneralPurposeRegisterValue){
        if(RFI<4&&RFI>=0) // GPR 0-3
        {
            this.gpRegisters[RFI]=GeneralPurposeRegisterValue;
        }
    }
    
    /**
     * 
     * @param RFI
     * @return R(RFI)
     */
    public Word getGeneralPurposeRegister(int RFI){
        return this.gpRegisters[RFI];
    }

    public void setProgramCounter(Unit programCounter) {
        this.programCounter = programCounter;
    }

    public Word getIR() {
        return instructionRegister;
    }

    public void setIR(Word instructionRegister) {
        this.instructionRegister = instructionRegister;
    }    
    
    /**
     * Clock cycle. This is the main function which causes the ControlUnit to do work.
     *  This serves as a publicly accessible method, but calls the instruction cycle.
     * @throws java.lang.Exception
     */
    @Override
    public void clockCycle() throws Exception {
        // Used to run microcycles without causing a full clock cycle
        //boolean runningMicroCycles=true;
        //do {  // @TODO: Turned off until we're running a program (part 2)
        //  System.out.println("Micro!");
          
          this.instructionCycle();

          if(this.blocked == true){
              // A microcycle signaled it is blocking.
              //runningMicroCycles=false;
              this.blocked=false;
          }
        //} while(runningMicroCycles);                              
        
    }  
    
    /**
     * Used internally to signal that a micro-cycle needs a full clock cycle
     */
    private void signalBlockingMicroFunction(){
        this.blocked=true;
    }    
    
    /**       
    * These fundamental steps are repeated over and over until we reach the 
    * last instruction in the program, typically something called HALT, STOP, or QUIT. 
    */      
    private void instructionCycle() throws Exception {
        if(microState==null){
            microState=0;
        }
        
        switch(this.state){
            case ControlUnit.STATE_FETCH_INSTRUCTION: // takes 2 cycles
                this.fetchNextInstructionFromMemory();
                break;
            case ControlUnit.STATE_DECODE_INSTRUCTION:
                this.decodeInstructionRegister();
                break;
            case ControlUnit.STATE_EXECUTE_INSTRUCTION:
                this.executeInstructionRegister();
                break;
            default:
                this.state = ControlUnit.STATE_FETCH_INSTRUCTION;
                break;     
        }        
    }
    
    /**
     * fetch the next instruction from memory to be executed
     */
    private void fetchNextInstructionFromMemory(){
        
        switch(this.microState){            
            case 0:
                this.clearConditions();   // Clear CC on new instruction
                System.out.println("Micro-0: PC -> MAR");
                // Micro-0: PC -> MAR
                Unit pc = this.getProgramCounter();
                System.out.println("-- PC: "+pc);
                this.memory.setMAR(pc);
                this.microState=1;  
                this.signalBlockingMicroFunction();
                break;         
                
                // unwritten step: clock cycle causes memory to pull PC
            
            case 1:
                System.out.println("Micro-1: MDR -> IR");
                // Micro-1: MDR -> IR                
                this.setIR(this.memory.getMBR());              
                System.out.println("-- IR: "+this.memory.getMBR());
                this.microState=2;              

                // Set up for next major state
                this.microState=null;
                this.state=ControlUnit.STATE_DECODE_INSTRUCTION;
                break;
        }
    }
    
    /**
     *
     * @param IR Instruction Register 
     * @return HashMap of IR
     */
    private HashMap<String,Unit> decodeInstructionRegister(Word IR){
       HashMap<String,Unit> decoded = new HashMap();
       
       decoded.put("opcode",  IR.decomposeByOffset(0, 5  ));
       decoded.put("rfi",    IR.decomposeByOffset(6, 7  ));
       decoded.put("xfi",    IR.decomposeByOffset(8, 9  ));
       decoded.put("index",   IR.decomposeByOffset(10    ));
       decoded.put("trace",   IR.decomposeByOffset(11    ));
       decoded.put("address", IR.decomposeByOffset(12, 19));
       
       return decoded;
   }      
    
    /**
     * decode instruction (i.e., determine what is to be done)
     * also calculates effective address if instruction requires it
     */
    private void decodeInstructionRegister(){        
        if(this.microState == 0){// Micro-4: Decode IR
            this.effectiveAddress=null;
            System.out.println("Micro-4: Decode IR");
            this.instructionRegisterDecoded = this.decodeInstructionRegister(this.getIR());     
            System.out.println("-- IR Decoded: "+this.instructionRegisterDecoded);
                        
            int opcode = this.instructionRegisterDecoded.get("opcode").getUnsignedValue();            
            if(opcode == ControlUnit.OPCODE_AIR || opcode ==ControlUnit.OPCODE_SIR){
                // These instructions don't require EA calculation. Skip ahead.
                this.microState=null;
                this.state=ControlUnit.STATE_EXECUTE_INSTRUCTION;            
            } else { // Every other instruction does. We'll progress through eaState and microState now.
                if(this.instructionRegisterDecoded.get("xfi").getUnsignedValue()==0 && this.instructionRegisterDecoded.get("rfi").getUnsignedValue()==0){                        
                    this.eaState = ControlUnit.EA_DIRECT;
                } else if(this.instructionRegisterDecoded.get("index").getUnsignedValue()==0 && this.instructionRegisterDecoded.get("xfi").getUnsignedValue()>=1 && this.instructionRegisterDecoded.get("xfi").getUnsignedValue()<=3){
                    this.eaState = ControlUnit.EA_REGISTER_INDIRECT;                    
                } else if(this.instructionRegisterDecoded.get("index").getUnsignedValue()==1 && this.instructionRegisterDecoded.get("xfi").getUnsignedValue()==0){
                    this.eaState = ControlUnit.EA_INDEXED;
                } else if(this.instructionRegisterDecoded.get("index").getUnsignedValue()==1 && this.instructionRegisterDecoded.get("xfi").getUnsignedValue()>=1 && this.instructionRegisterDecoded.get("xfi").getUnsignedValue()<=3){
                    this.eaState = ControlUnit.EA_INDEXED_OFFSET;
                }                                
                this.microState++;
            }         
        } else { //microState >= 1 & we're computing EA
            System.out.println("Micro-5."+this.microState+": Compute Effective Address (Type: "+this.eaState+")");            
            switch(this.eaState){
                case ControlUnit.EA_DIRECT: //EA <- ADDR                    
                    System.out.println("Absolute/Direct:" + this.instructionRegisterDecoded.get("address"));
                    this.effectiveAddress = this.instructionRegisterDecoded.get("address");                    
                    break;
                case ControlUnit.EA_REGISTER_INDIRECT: //EA <- c(Xi) + ADDR
                    switch(this.microState){
                        case 1:
                            Unit addr = this.instructionRegisterDecoded.get("address");  
                            int contentsOfX = this.getIndexRegister(this.instructionRegisterDecoded.get("xfi").getUnsignedValue()).getUnsignedValue(); //read Xi here  
                            this.effectiveAddress = new Unit(13, (contentsOfX + addr.getUnsignedValue()));
                            System.out.println("Register Indirect + Offset ("+contentsOfX+" + "+addr.getUnsignedValue()+"): "+this.effectiveAddress);
                            break;                            
                    }                           
                    break;
                case ControlUnit.EA_INDEXED: //EA <- c(ADDR)                         
                    switch(this.microState){
                        case 1: // Set ADDR onto MAR
                            Unit addr = this.instructionRegisterDecoded.get("address");
                            this.memory.setMAR(addr);                            
                            this.microState++;
                            break;
                        case 2: // c(ADDR) from MBR, set to MAR
                            Word contentsOfAddr = this.memory.getMBR();
                            this.effectiveAddress =  new Unit(13, (contentsOfAddr.getUnsignedValue()));
                            System.out.println("Indexed - c(ADDR) =  c("+this.instructionRegisterDecoded.get("address").getUnsignedValue()+") = "+this.effectiveAddress);                            
                            break;
                    }                           
                    break;                    
                case ControlUnit.EA_INDEXED_OFFSET: //EA <- c(c(Xi) + ADDR)
                    switch(this.microState){
                        case 1:
                            Unit addr = this.instructionRegisterDecoded.get("address");
                            int contentsOfX = this.getIndexRegister(this.instructionRegisterDecoded.get("xfi").getUnsignedValue()).getUnsignedValue();    //read Xi here                        
                            Unit location = new Unit(13, (contentsOfX + addr.getUnsignedValue()));
                            this.memory.setMAR(location);
                            this.microState++;    
                            break;
                        case 2:
                            Word contentsOfLocation = this.memory.getMBR();
                            this.effectiveAddress = new Unit(13, contentsOfLocation.getUnsignedValue());
                            System.out.println("Indexed + Offset --> "+this.effectiveAddress);                                
                            break;
                    }                      
                    break;
                default:
                    // Unhandled address mode
            }            
            if(this.effectiveAddress != null){ // EA Calculated. Completed!
                System.out.println("-- Effective Address Calculated: "+this.effectiveAddress);                     
                this.microState=null;
                this.state=ControlUnit.STATE_EXECUTE_INSTRUCTION;                    
            }
        }
    }
    
    private void signalMicroStateExecutionComplete(){
        this.microState=ControlUnit.MICROSTATE_EXECUTE_COMPLETE;
    }
    
    /**
     * execute instruction by issuing the appropriate command to the ALU, memory, and the I/O controllers
     * Each instruction will receive the microstate at 0 and is responsible for taking action.
     * When the instruction completes, it sets the microState to MICROSTATE_EXECUTE_COMPLETE
     */
    private void executeInstructionRegister() throws Exception {
        if(microState==null){
            microState=0;
        }
        /* This delegates the microstate to the instruction, but does handle a special
            microstate (999) to signal completion. */
        if(this.microState < ControlUnit.MICROSTATE_EXECUTE_COMPLETE){
            int opcode = this.instructionRegisterDecoded.get("opcode").getUnsignedValue();
            System.out.println("--EXECUTING OPCODE: "+ opcode);
            switch(opcode){
                case ControlUnit.OPCODE_HLT:
                    this.executeOpcodeHLT();                    
                    break;                    
                case ControlUnit.OPCODE_LDR:
                    this.executeOpcodeLDR();
                    break;
                case ControlUnit.OPCODE_STR:
                    this.executeOpcodeSTR();
                    break;
                case ControlUnit.OPCODE_LDA:
                    this.executeOpcodeLDA();
                    break;    
                case ControlUnit.OPCODE_LDX:
                    this.executeOpcodeLDX();
                    break;
                case ControlUnit.OPCODE_STX:
                    this.executeOpcodeSTX();
                    break;
                case ControlUnit.OPCODE_AMR:
                    this.executeOpcodeAMR();
                    break;
                case ControlUnit.OPCODE_SMR:
                    this.executeOpcodeSMR();
                    break;
                case ControlUnit.OPCODE_AIR:
                    this.executeOpcodeAIR();
                    break;
                case ControlUnit.OPCODE_SIR:
                    this.executeOpcodeSIR();
                    break;
                case ControlUnit.OPCODE_JMP:  //TESTED
                    this.executeOpcodeJMP();
                    break;
                case ControlUnit.OPCODE_JZ:  //TESTED
                    this.executeOpcodeJZ();
                    break;
                case ControlUnit.OPCODE_JNE: //TESTED
                    this.executeOpcodeJNE();
                    break;
                case ControlUnit.OPCODE_JGE: //Tested 
                    this.executeOpcodeJGE();
                    break;                    
                case ControlUnit.OPCODE_SOB: //Tested
                    this.executeOpcodeSOB();
                    break;
                case ControlUnit.OPCODE_JCC:
                    this.executeOpcodeJCC();
                    break;
                case ControlUnit.OPCODE_RFS:
                    this.executeOpcodeRFS();
                    break;
                case ControlUnit.OPCODE_JSR://Partially implemented
                    this.executeOpcodeJSR();
                    break;
                case ControlUnit.OPCODE_SRC:
                    this.executeOpcodeSRC();
                    break;
                case ControlUnit.OPCODE_RRC:
                    this.executeOpcodeRRC();

                    break;     

                case ControlUnit.OPCODE_ORR:
                    this.executeOpcodeORR();
                    break;
                case ControlUnit.OPCODE_NOT:
                    this.executeOpcodeNOT();

                 case ControlUnit.OPCODE_TRR:
                    this.executeOpcodeTRR();
                    break;
                case ControlUnit.OPCODE_AND:
                    this.executeOpcodeAND();
                    break; 
                default: // Unhandle opcode. Crash!
                    throw new Exception("Unhandled Opcode: "+opcode);                        
            }            
            this.microState++; 
        } else { // MICROSTATE_EXECUTE_COMPLETE            
            if(this.nextProgramCounter==null){
                // Micro-N: c(PC) + 1 -> PC  --- Increment PC
                System.out.println("Micro-Final: c(PC) + 1 -> PC (Increment PC)");
                this.getProgramCounter().setValue(this.getProgramCounter().getUnsignedValue() + 1); 
            } else { 
                // Micro-N PC <- tempPC (internal to our simulator)
                this.getProgramCounter().setValue(this.nextProgramCounter.getUnsignedValue());
            }
            System.out.println("-- PC: "+this.getProgramCounter());
            this.state = ControlUnit.STATE_NONE;     
            this.microState = null;
            this.signalBlockingMicroFunction();            

        }        
    }
   
    
     

    /***************** OPCODE IMPLEMENTATIONS BELOW ******************/
    
    /**
     * Execute Load Data Into Register
     */
    private void executeOpcodeLDR(){
        switch(this.microState){            

            case 0:
                // Micro-6: MAR <- EA
                System.out.println("Micro-6: MAR <- EA");
                memory.setMAR(this.effectiveAddress);  
                this.signalBlockingMicroFunction();
                break;
                
            case 1:
                // Micro-7: MBR <- M(MAR)
                System.out.println("Micro-7: MBR <- M(MAR)");
                // do nothing, done by memory
                break;

            case 2:
                // Micro-8: RF(RFI) <- MBR   
                System.out.println("Micro-8: RF(RFI) <- MBR");
                int RFI = this.instructionRegisterDecoded.get("rfi").getUnsignedValue();
                this.setGeneralPurposeRegister(RFI, this.memory.getMBR());

                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                System.out.println("COMPLETED INSTRUCTION: LDR - rfi["+RFI+"] is now: "+ this.memory.getMBR());
                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                
                this.signalMicroStateExecutionComplete();
                break;            
            
        }
            
                                
    }
    
    /**
     * Execute Store Register to Memory
     */
    private void executeOpcodeSTR() {
        switch(this.microState){    
            
            case 0:
              // Micro-6: MAR <- EA
              System.out.println("Micro-6: MAR <- EA");
              memory.setMAR(this.effectiveAddress);         
              
              // Micro-7: MBR <- RF(RFI)
              System.out.println("Micro-7: MBR <- RF(RFI)");
              int RFI = this.instructionRegisterDecoded.get("rfi").getUnsignedValue();
              memory.setMBR(this.getGeneralPurposeRegister(RFI));
              this.signalBlockingMicroFunction();
            break;
                
            case 1:   
              System.out.println("Micro-8: M(MAR) <- MBR");
              // do nothing, done by memory in this clock cycle    
              
              System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
              System.out.println("COMPLETED INSTRUCTION: STR - M(MAR): "+ this.memory.engineerFetchByMemoryLocation(this.effectiveAddress));
              System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
              
              this.signalMicroStateExecutionComplete();
            break;
        }
    }
    
    /**
     * Execute Load Register with Address
     */
    private void executeOpcodeLDA() {
        switch(this.microState){
                
            case 0:
                // Micro-6: MAR <- EA
                System.out.println("Micro-6: MAR <- EA");
                memory.setMAR(this.effectiveAddress);    
                this.signalBlockingMicroFunction();
                break;
                
            case 1:
                // Micro-7: MBR <- M(MAR)
                System.out.println("Micro-7: MBR <- M(MAR)");
                // do nothing, done by memory
            break;
                
            case 2:
                // Micro-8: RF(RFI) <- MBR   
                System.out.println("Micro-8: RF(RFI) <- MBR");
                int RFI = this.instructionRegisterDecoded.get("rfi").getUnsignedValue();
                this.setGeneralPurposeRegister(RFI, this.memory.getMBR());

                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                System.out.println("COMPLETED INSTRUCTION: LDA - rfi["+RFI+"] is now: "+ this.memory.getMBR());
                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                
                this.signalMicroStateExecutionComplete();

            break;            
        }
    }
    
    /**
     * Execute Load Index Register from Memory
     */
    private void executeOpcodeLDX() {
        switch(this.microState){            
            case 0:
              // Micro-6: MAR <- EA
              System.out.println("Micro-6: MAR<-EA");
              memory.setMAR(this.effectiveAddress);
              this.signalBlockingMicroFunction();
            break;
                
            case 1:
              // Micro-7: MBR <- M(MAR)
              System.out.println("Micro-7: MBR <- M(MAR)");
              // do nothing, done by memory
            break;

            case 2:
              // Micro 8: c(XFI) <- MBR              
              int XFI = this.instructionRegisterDecoded.get("xfi").getUnsignedValue();
              this.setIndexRegister(XFI, this.memory.getMBR());
              
              System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
              System.out.println("COMPLETED INSTRUCTION: LDX - M(MAR): "+ this.memory.engineerFetchByMemoryLocation(this.effectiveAddress));
              System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                
              this.signalMicroStateExecutionComplete();
            break;
        }
    }
    
    /**
     * Execute Store Index Register to Memory
     */
    private void executeOpcodeSTX() {
        switch(this.microState){
            case 0:
              // Micro-6: MAR <- EA
              System.out.println("Micro-6: MAR <- EA");
              memory.setMAR(this.effectiveAddress);
              this.signalBlockingMicroFunction();
            break;
                
            case 1:
              // Micro 7: MBR <- c(XFI)
              System.out.println("Micro 7: MBR <- c(XFI)");
              int XFI = this.instructionRegisterDecoded.get("xfi").getUnsignedValue();
              memory.setMBR(this.getIndexRegister(XFI));
            break;
                
            case 2:
              // Micro 8: M(MAR) <- MBR
              System.out.println("Micro 8: M(MAR) <- MBR");
              // do nothing, done by memory
                
              System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
              System.out.println("COMPLETED INSTRUCTION: STX - M(MAR): "+ this.memory.engineerFetchByMemoryLocation(this.effectiveAddress));
              System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                
              this.signalMicroStateExecutionComplete();
            break;
        }
    }
    
    /**
     * Execute Add Memory to Register
     */
    private void executeOpcodeAMR() {
        switch(this.microState){
            
            case 0:
              // Micro-6: MAR <- EA
              System.out.println("Micro-6: MAR <- EA");
              memory.setMAR(this.effectiveAddress);
              this.signalBlockingMicroFunction();
            break;
                
            case 1:
              // Micro-7: MBR <- M(MAR)
              System.out.println("Micro-7: MBR <- M(MAR)");
              // do nothing, done by memory
            break;
                
            case 2:
              // Micro-8: OP1 <- MBR
              System.out.println("Micro-8: OP1 <- MBR");
              alu.setOperand1(this.memory.getMBR());  // This might be possible to run in cycle 1
            break;
                
            case 3:
              // Micro-9: OP2 <- RF(RFI)
              System.out.println("Micro-9: OP2 <- RF(RFI)");
              int RFI = this.instructionRegisterDecoded.get("rfi").getUnsignedValue();
              alu.setOperand2(this.getGeneralPurposeRegister(RFI));
            break;
                
            case 4:
              // Micro-10: CTRL <- OPCODE
              System.out.println("Micro-10: CTRL <- OPCODE");  
              alu.setControl(ArithmeticLogicUnit.CONTROL_ADD); // @TODO: Should this come from IR somehow?
              alu.signalReadyToStartComputation();
            break;
                
            case 5:
              // Micro-11: RES <- c(OP1) + c(OP2)
              System.out.println("Micro-11: RES <- c(OP1) + c(OP2)");
              // Do nothing. (occurs automatically one clock cycle after signaled ready to compute)
            break;
                
            case 6:
              // Micro-12: RF(RFI) <- RES
              System.out.println("Micro-12: RF(RFI) <- RES");
              RFI = this.instructionRegisterDecoded.get("rfi").getUnsignedValue(); 
              this.setGeneralPurposeRegister(RFI, new Word(alu.getResult()));
              System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
              System.out.println("COMPLETED INSTRUCTION: AMR - RF("+RFI+"): "+  this.getGeneralPurposeRegister(RFI));
              System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                
              this.signalMicroStateExecutionComplete();
            break;          
        }
    }
    
    /**
     * Execute Subtract Memory from Register
     */
    private void executeOpcodeSMR() {
        switch(this.microState){
            
            case 0:
              // Micro-6: MAR <- EA
              System.out.println("Micro-6: MAR <- EA");
              memory.setMAR(this.effectiveAddress);
              this.signalBlockingMicroFunction();
            break;
                
            case 1:
              // Micro-7: MBR <- M(MAR)
              System.out.println("Micro-7: MBR <- M(MAR)");
              // do nothing, done by memory
            break;
                
            case 3:
              // Micro-8: OP1 <- MBR
              System.out.println("Micro-8: OP1 <- MBR");
              alu.setOperand1(this.memory.getMBR());
            break;
                
            case 4:
              // Micro-9: OP2 <- RF(RFI)
              System.out.println("Micro-9: OP2 <- RF(RFI)");
              int RFI = this.instructionRegisterDecoded.get("rfi").getUnsignedValue();
              alu.setOperand2(this.getGeneralPurposeRegister(RFI));
            break;
                
            case 5:
              // Micro-10: CTRL <- OPCODE
              System.out.println("Micro-10: CTRL <- OPCODE");  
              alu.setControl(ArithmeticLogicUnit.CONTROL_SUBTRACT); // @TODO: Should this come from IR somehow?
              alu.signalReadyToStartComputation();
            break;
                
            case 6:
              // Micro-11: RES <- c(OP1) - c(OP2)
              // Do nothing. (occurs automatically one clock cycle after signaled ready to compute)
            break;
                
            case 7:
              // Micro-12: RF(RFI) <- RES
              System.out.println("Micro-12: RF(RFI) <- RES");
              RFI = this.instructionRegisterDecoded.get("rfi").getUnsignedValue();
              
              this.setGeneralPurposeRegister(RFI, new Word(alu.getResult()));
              System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
              System.out.println("COMPLETED INSTRUCTION: SMR - RF("+RFI+"): "+  this.getGeneralPurposeRegister(RFI));
              System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");                                         
                
              this.signalMicroStateExecutionComplete();
            break;          
        }
    }
    
    /**
     * Execute Add Immediate to Register
     */
    private void executeOpcodeAIR() {
        switch(this.microState){
            case 0:
                // Micro-6: OP1 <- RF(RFI)                
                int RFI = this.instructionRegisterDecoded.get("rfi").getUnsignedValue();                
                alu.setOperand1(this.getGeneralPurposeRegister(RFI));
                System.out.println("Micro-6: OP1 <- RF(RFI) - "+alu.getOperand1());
            break;
                        
            case 1:
                // Micro-7: OP2 <- Immed   (Immed is stored in ADDR)                
                alu.setOperand2(this.instructionRegisterDecoded.get("address"));
                System.out.println("Micro-7: OP2 <- Immed - " + alu.getOperand2());
            break;
                
            case 2:
                // Micro-8: CTRL <- OPCODE
                System.out.println("Micro-8: CTRL <- OPCODE");  
                alu.setControl(ArithmeticLogicUnit.CONTROL_ADD);
                alu.signalReadyToStartComputation();
            break;
                
            case 3:
                // Micro-9: RES <- c(OP1) + c(OP2)
                System.out.println("Micro-9: RES <- c(OP1) + c(OP2)");
                // Do nothing. (occurs automatically one clock cycle after signaled ready to compute)
            break;
                
            case 4:
                // Micro-10: RF(RFI) <- RES
                System.out.println("Micro-10: RF(RFI) <- RES - "+alu.getResult());
                RFI = this.instructionRegisterDecoded.get("rfi").getUnsignedValue();
               
                this.setGeneralPurposeRegister(RFI, new Word(alu.getResult()));
                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                System.out.println("COMPLETED INSTRUCTION: AIR - RF("+RFI+"): "+  this.getGeneralPurposeRegister(RFI));
                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");                                         
                
                this.signalMicroStateExecutionComplete();
            break;          
        }
    }
    
    /**
     * Execute Subtract Immediate from Register
     */
    private void executeOpcodeSIR() {
        switch(this.microState){
            case 0:
                // Micro-6: OP1 <- RF(RFI)                
                int RFI = this.instructionRegisterDecoded.get("rfi").getUnsignedValue();
                alu.setOperand1(this.getGeneralPurposeRegister(RFI));
                System.out.println("Micro-6: OP1 <- RF(RFI) - "+alu.getOperand1());
            break;
                        
            case 1:
                // Micro-7: OP2 <- Immed  (Immed is stored in ADDR)                
                alu.setOperand2(this.instructionRegisterDecoded.get("address"));
                System.out.println("Micro-7: OP2 <- Immed - "+ alu.getOperand2());
            break;
                
            case 2:
                // Micro-8: CTRL <- OPCODE
                System.out.println("Micro-8: CTRL <- OPCODE");  
                alu.setControl(ArithmeticLogicUnit.CONTROL_SUBTRACT);
                alu.signalReadyToStartComputation();
            break;
                
            case 3:
                // Micro-9: RES <- c(OP1) - c(OP2)
                System.out.println("Micro-9: RES <- c(OP1) - c(OP2)");
                // Do nothing. (occurs automatically one clock cycle after signaled ready to compute)
            break;
                
            case 4:
                // Micro-10: RF(RFI) <- RES
                System.out.println("Micro-10: RF(RFI) <- RES - "+alu.getResult());
                RFI = this.instructionRegisterDecoded.get("rfi").getUnsignedValue();
               
                this.setGeneralPurposeRegister(RFI, new Word(alu.getResult()));
                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                System.out.println("COMPLETED INSTRUCTION: SIR - RF("+RFI+"): "+  this.getGeneralPurposeRegister(RFI));
                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");                                         
                
                this.signalMicroStateExecutionComplete();
            break;          
        }
        
    }

    /**
     * Execute Unconditional Jump to Address
     * PC <- EA, if I bit not set; PC <− c(EA), if I bit set
     * Note: r is ignored in this instruction
     * Test pased by Fan based on 001101 00 01 1 0 01111011
     * 
     */
    private void executeOpcodeJMP(){
        
        this.nextProgramCounter=new Unit(13,this.effectiveAddress.getUnsignedValue());
         System.out.println("Micro-6: PC <- EA - "+this.nextProgramCounter);
            this.signalMicroStateExecutionComplete();
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.out.println("COMPLETED INSTRUCTION: JMP - IND="+this.instructionRegisterDecoded.get("index").getUnsignedValue()+": " + this.nextProgramCounter);
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");  
     
    }
    
    /**
     * Jump If Zero:
     *  If c(r) = 0, then PC <− EA or c(EA), if I bit set;
     *  Else PC <- PC+1
     * Test pased by Fan based on 001010  00  01  1  0  01111011(given by professor)
     */
    private void  executeOpcodeJZ(){        
        int RFI = this.instructionRegisterDecoded.get("rfi").getUnsignedValue();
        if(this.getGeneralPurposeRegister(RFI).getUnsignedValue()==0)
        { // c(r)==0, jump
         this.nextProgramCounter=new Unit(13,this.effectiveAddress.getUnsignedValue());
         System.out.println("Micro-6: PC <- EA - "+this.nextProgramCounter);              
         this.signalMicroStateExecutionComplete();
         System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
         System.out.println("COMPLETED INSTRUCTION: JZ - R("+RFI+") was Zero -- JUMPING: "+this.nextProgramCounter);
         System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"); 
           
        }
        else
        {
             // not zero->PC++
            this.signalMicroStateExecutionComplete();
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.out.println("COMPLETED INSTRUCTION: JZ - R("+RFI+") was NOT Zero -- Continuing.");
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");  
            
        }
    }
        

  
    
    /**
     * Jump If Not Equal (to Zero) -- really JNZ.
     * If c(r) != 0, then PC <−- EA or c(EA) , if I bit set;
     * Else PC <- PC + 1
     * Test pased by Fan based on 001011 00 01 1 0 01111011(R(0)==0 AND R(0)=1)
     */
    private void executeOpcodeJNE(){
            int RFI = this.instructionRegisterDecoded.get("rfi").getUnsignedValue();
        if(this.getGeneralPurposeRegister(RFI).getUnsignedValue()!=0)
        { // c(r)!=0, jump
         this.nextProgramCounter=new Unit(13,this.effectiveAddress.getUnsignedValue());
         System.out.println("Micro-6: PC <- EA - "+this.nextProgramCounter);              
         this.signalMicroStateExecutionComplete();
         System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
         System.out.println("COMPLETED INSTRUCTION: JNE - R("+RFI+") was NOT Zero -- JUMPING: "+this.nextProgramCounter);
         System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"); 
           
        }
        else
        {
             // not zero->PC++
            this.signalMicroStateExecutionComplete();
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.out.println("COMPLETED INSTRUCTION: JNE - R("+RFI+") was  Zero -- Continuing.");
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");  
            
        }
    }
    
    
    /**
     * Subtract One And Branch. SOB allows you to support simple loops. 
     * R = 0..3
     * r <− c(r) – 1
     * If c(r) > 0,  PC <- EA; but PC <− c(EA), if I bit set;
     * Else PC <- PC + 1
     * Test pased by Fan based on 010000 00 01 1 0 01111011 (R0=1 or r0=2)
     */
     private void executeOpcodeSOB(){        
        int RFI = this.instructionRegisterDecoded.get("rfi").getUnsignedValue();
        
        switch(this.microState){
            case 0: // case 0, we decrement c(r)
                System.out.println("Micro-6:RF("+RFI+")=c("+RFI+")-1");  
                this.setGeneralPurposeRegister(RFI, new Word(this.getGeneralPurposeRegister(RFI).getUnsignedValue()-1));
                break;
            default: // case >= 1
                if(this.getGeneralPurposeRegister(RFI).getUnsignedValue()>0)
                { // c(r)>0, jump
                    this.nextProgramCounter=new Unit(13,this.effectiveAddress.getUnsignedValue());
                    System.out.println("Micro-7: PC <- EA - "+this.nextProgramCounter);              
                    this.signalMicroStateExecutionComplete();
                    System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                    System.out.println("COMPLETED INSTRUCTION: SOB - R("+RFI+") was GREATER than Zero after minus 1 -- JUMPING: "+this.nextProgramCounter);
                    System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"); 
           
                }
                else
                {
                    // not zero->PC++
                    this.signalMicroStateExecutionComplete();
                    System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                    System.out.println("COMPLETED INSTRUCTION: SOB - R("+RFI+") was NOT GREATER than Zero after minus 1  -- Continuing.");
                    System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");  
            
                }
                
        }
     }
   
    /**
     * 
     * Jump Greater Than or Equal To:
     * If c(r) >= 0, then PC <- EA or c(EA) , if I bit set;
     * Else PC <- PC + 1
     * Test pased by Fan based on 010001 00 01 1 0 01111011
    */
   private void executeOpcodeJGE()
   {
        int RFI = this.instructionRegisterDecoded.get("rfi").getUnsignedValue();
        if(this.getGeneralPurposeRegister(RFI).getUnsignedValue()>=0)
        { // c(r)>=0, jump
         this.nextProgramCounter=new Unit(13,this.effectiveAddress.getUnsignedValue());
         System.out.println("Micro-6: PC <- EA - "+this.nextProgramCounter);              
         this.signalMicroStateExecutionComplete();
         System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
         System.out.println("COMPLETED INSTRUCTION: JGE - R("+RFI+") was NOT LESS than Zero -- JUMPING: "+this.nextProgramCounter);
         System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"); 
           
        }
        else
        {
             // not zero->PC++
            this.signalMicroStateExecutionComplete();
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.out.println("COMPLETED INSTRUCTION: JGE - R("+RFI+") was LESS Zero -- Continuing.");
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");  
            
        }
    } 
   
    
    /**
     * 
     * Jump If Condition Code:
     * If CC = 1, then PC <- EA or c(EA) , if I bit set;
     * Else PC <- PC + 1
    */
  private void executeOpcodeJCC(){
        int CC = this.instructionRegisterDecoded.get("rfi").getUnsignedValue();         //CC replaces RFI for the JCC instruction.
        if(this.getConditionCode(CC)==1){
            this.nextProgramCounter=new Unit(13,this.effectiveAddress.getUnsignedValue());
            System.out.println("Micro-6: PC <- EA - "+this.nextProgramCounter);              
            this.signalMicroStateExecutionComplete();
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.out.println("COMPLETED INSTRUCTION: JCC("+CC+") - Jumping: "+this.nextProgramCounter);
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");              
        } else { // not zero->PC++             CC != 1
            this.signalMicroStateExecutionComplete();
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.out.println("COMPLETED INSTRUCTION: JCC("+CC+") - Not Jumping.");
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");  
        }
            
    }
 
    /**
     * 
     * Return From Subroutine w/ return code as Immediate portion (optional) 
     * stored in R0’s address field. 
     * R0 <− Immed; PC <− c(R3)
     * IX, I fields are ignored.
     * 
    */    
    private void executeOpcodeRFS(){
      switch(this.microState){
            case 0:
                // R0 <- Immed (Immed is stored in ADDR)        
                this.setGeneralPurposeRegister(0, new Word(this.instructionRegisterDecoded.get("address")));
                System.out.println("Micro-6: R0 <- Immediate");
            break;
                
            case 1:
                // PC <- c(R3)
                this.nextProgramCounter = new Unit(13, this.getGeneralPurposeRegister(3).getUnsignedValue());
                System.out.println("Micro-7: PC <- c(R3)");
            
                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                System.out.println("COMPLETED INSTRUCTION: RFS - Ret: "+this.getGeneralPurposeRegister(0)+" - Jump: "+  this.nextProgramCounter);
                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");                                         
                
                this.signalMicroStateExecutionComplete();
            break;          
        }
                
    }
    
    /**
     * 
     * Jump and Save Return Address:
     * R3 <- PC + 1, PC <- EA or c(EA) , if I bit set;
     * R0 should contain pointer to arguments. Argument list should end with -17777 value.
    */
    private void executeOpcodeJSR(){
        switch(this.microState){
            case 0:
                // RFI1 <- 3
                System.out.println("Micro-6: RFI1 <- 3");
                 
            break;
                
            case 1:
                //RF(RFI1) <- PC + 1
                int RFI=3;
                this.setGeneralPurposeRegister(RFI, new Word(this.getProgramCounter().getUnsignedValue()+1));
                System.out.println("Micro-7: RF(RFI1) <- PC + 1");
            break;
                
            case 2:
                //PC<-EA
                 this.nextProgramCounter=new Unit(13,this.effectiveAddress.getUnsignedValue());
                 System.out.println("Micro-8: PC <- EA - "+this.nextProgramCounter);
                 
                break;
            case 3:
                System.out.println("Now Store the pointer of the Arguments into R0 here");
                this.signalMicroStateExecutionComplete();
                break;
                
              
        }
    }
    
    /**
     * Shift Register by Count
     * c(r) is shifted left (L/R =1) or right (L/R = 0) either logically (A/L = 1) or arithmetically (A/L = 0)
     * XX, XXX are ignored
     * Count = 0…20
     * If Count = 0, no shift occurs
     */
    private void executeOpcodeSRC(){
        int RFI = this.getIR().decomposeByOffset(8, 9).getUnsignedValue();
        
        int algorithmicLogical = this.getIR().decomposeByIndex(10).getUnsignedValue();
        int leftRight = this.getIR().decomposeByIndex(11).getUnsignedValue();
        int count = this.getIR().decomposeByOffset(15, 19).getUnsignedValue();
        
        // Shift functionality is implemented in Unit
        this.getGeneralPurposeRegister(RFI).shiftByCount(leftRight, count, algorithmicLogical);
        
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.println("COMPLETED INSTRUCTION: SRC - Shift Register "+RFI+" "+((leftRight==1) ? "Left" : "Right") +" "+((algorithmicLogical==1) ? "Logical" : "Algorithmic")+" by "+count+": "+this.getGeneralPurposeRegister(RFI));
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");                                                 
        
    }
    
    /**
     * Rotate Register by Count
     * c(r) is rotated left (L/R = 1) or right (L/R =0) either logically (A/L =1)
     * XX, XXX is ignored
     * Count = 0…20
     * If Count = 0, no rotate occurs
     */
    private void executeOpcodeRRC(){
        int RFI = this.getIR().decomposeByOffset(8, 9).getUnsignedValue();
        
        int leftRight = this.getIR().decomposeByIndex(11).getUnsignedValue();
        int count = this.getIR().decomposeByOffset(15, 19).getUnsignedValue();
        
        // Rotate functionality is implemented in Unit
        this.getGeneralPurposeRegister(RFI).rotateByCount(leftRight, count);
        
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.println("COMPLETED INSTRUCTION: RRC - Rotate Register "+RFI+" "+((leftRight==1) ? "Left" : "Right") +" by "+count+": "+this.getGeneralPurposeRegister(RFI));
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");            
    }
    /*
    Test the Equality of Register and Register

If c(rx) = c(ry), set cc(4) <- 1; else, cc(4) <- 0


    */
    private void executeOpcodeTRR()
    {
        int RFI1=this.getIR().decomposeByOffset(6, 7).getUnsignedValue();
        int RFI2=this.getIR().decomposeByOffset(8, 9).getUnsignedValue();
        if(this.getGeneralPurposeRegister(RFI1).getUnsignedValue()==this.getGeneralPurposeRegister(RFI2).getUnsignedValue())
        {
            this.setCondition(3);
        }
        else
        {
            this.unsetCondition(3);
        }
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.println("COMPLETED INSTRUCTION:TRR RF("+RFI1+")and RF("+RFI2+")is "+((this.getConditionCode(3)==1)?"equal":"unequal")+"  CC is"+this.getConditionCode(3));
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");      
    }
    /*
    Logical And of Register and Register

c(rx) <- c(rx) AND c(ry)


    */
    private void executeOpcodeAND()
    {
        int RFI1=this.getIR().decomposeByOffset(6, 7).getUnsignedValue();
        int RFI2=this.getIR().decomposeByOffset(8, 9).getUnsignedValue();
        Unit ContentOfRFI1=new Unit(13,this.getGeneralPurposeRegister(RFI1).getUnsignedValue());
        Unit ContentOfRFI2=new Unit(13,this.getGeneralPurposeRegister(RFI2).getUnsignedValue());
        alu.setOperand1(ContentOfRFI1);
        alu.setOperand2(ContentOfRFI2);
        alu.setControl(ArithmeticLogicUnit.CONTROL_AND);
        alu.signalReadyToStartComputation();
        this.setGeneralPurposeRegister(RFI1, new Word(alu.getResult()));
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.println("COMPLETED INSTRUCTION:AND RF("+RFI1+"), RF("+RFI2+")is "+this.getGeneralPurposeRegister(RFI1).getBinaryString());
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");      
 
    }
    
    /**
     * Logical OR of Register and Register
     * c(rx) <- c(rx) OR c(ry)
     */
    private void executeOpcodeORR(){
        Integer[] RFI1 = this.getIR().decomposeByOffset(6, 7).getBinaryArray();         //Get the contents of RFI as an integer array.
        Integer[] RFI2 = this.getIR().decomposeByOffset(8, 9).getBinaryArray();         //Get the contents of RFI as an integer array. (XFI bits act as RFI)
        int register = this.instructionRegisterDecoded.get("rfi").getUnsignedValue();   //Get the register number as an integer.
        
        Integer[] RFI = uRef.logicalOR(RFI1, RFI2);                                     //Perform logical OR.
        String res = uRef.IntArrayToBinaryString(RFI);                                  //Obtain Logical OR value as a string.
        Unit result = uRef.UnitFromBinaryString(res);                                   //Conver the string to unit.
        
        this.setGeneralPurposeRegister(register, new Word(result));
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.println("COMPLETED INSTRUCTION: ORR rx, ry");
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");                                         
        this.signalMicroStateExecutionComplete();
    }
    
    /**

     * Logical Not of Register To Register
     * C(rx) <- NOT c(rx)
     */
    private void executeOpcodeNOT(){
        Integer[] RFI = this.getIR().decomposeByOffset(6, 7).getBinaryArray();          //Get the contents of RFI as an integer array.
        int register = this.instructionRegisterDecoded.get("rfi").getUnsignedValue();   //Get the register number as an integer.
        
        RFI = uRef.logicalNOT(RFI);                                                         //Perform Logical NOT.
        String res = uRef.IntArrayToBinaryString(RFI);                                  //Obtain negated value as a string.
        Unit result = uRef.UnitFromBinaryString(res);                                   //Conver the string to unit.
        
        this.setGeneralPurposeRegister(register, new Word(result));
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.println("COMPLETED INSTRUCTION: NOT rx");
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");                                         
        this.signalMicroStateExecutionComplete();
        
    }
    
    /**
     * Multiply Register by Register
     * rx, rx+1 <- c(rx) * c(ry)
     */
    private void executeOpcodeMLT() {
     /*    Integer RFI1=this.getIR().decomposeByOffset(6, 7).getUnsignedValue();
        Integer RFI2=this.getIR().decomposeByOffset(8, 9).getUnsignedValue();
        Unit contentOfRFI1=new Unit(13,this.getGeneralPurposeRegister(RFI1).getUnsignedValue());
        Unit contentOfRFI2=new Unit(13,this.getGeneralPurposeRegister(RFI2).getUnsignedValue());
        alu.setOperand1(contentOfRFI1);
        alu.setOperand2(contentOfRFI2);
        alu.setControl(ArithmeticLogicUnit.CONTROL_MULTIPLY);
        alu.signalReadyToStartComputation();
        this.setGeneralPurposeRegister(RFI1, new Word(alu.getResult()));
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.println("COMPLETED INSTRUCTION:MLT RF("+RFI1+"), RF("+RFI2+")is "+this.getGeneralPurposeRegister(RFI1).getBinaryString());
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");  
    */
        switch(this.microState){
            case 0:
                // Micro-6: OP1 <- RF(RFI1)                
                Integer RFI1=this.getIR().decomposeByOffset(6, 7).getUnsignedValue();
                Unit contentOfRFI1=new Unit(13,this.getGeneralPurposeRegister(RFI1).getUnsignedValue());
                alu.setOperand1(contentOfRFI1);
                System.out.println("Micro-6: OP1 <- RF(RFI1) - "+alu.getOperand1());
            break;
                        
            case 1:
                // Micro-7: OP2 <- RF(RFI2)   
                Integer RFI2=this.getIR().decomposeByOffset(8, 9).getUnsignedValue();
                Unit contentOfRFI2=new Unit(13,this.getGeneralPurposeRegister(RFI2).getUnsignedValue());
                alu.setOperand2(contentOfRFI2);
            break;
                
            case 2:
                // Micro-8: CTRL <- OPCODE
                System.out.println("Micro-8: CTRL <- OPCODE");
                alu.setControl(ArithmeticLogicUnit.CONTROL_MULTIPLY);
                alu.signalReadyToStartComputation();
            break;
                
            case 3:
                // Micro-9: RES <- c(OP1) * c(OP2)      
                System.out.println("Micro-9: RES <- c(OP1) * c(OP2)");
                // Do nothing. (occurs automatically one clock cycle after signaled ready to compute)
            break;
                
            case 4:
                // Micro-10: RF(RFI1) <- RES
                //           RF(RFI2) <- RES
                System.out.println("Micro-10: RF(RFI1) <- RES - "+alu.getResult());
                System.out.println("        : RF(RFI2) <- RES - "+alu.getResult());
                RFI1=this.getIR().decomposeByOffset(6, 7).getUnsignedValue();
                RFI2=this.getIR().decomposeByOffset(8, 9).getUnsignedValue();
                this.setGeneralPurposeRegister(RFI1, new Word(alu.getResult()));
                this.setGeneralPurposeRegister(RFI2, new Word(alu.getResult()));
                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                System.out.println("COMPLETED INSTRUCTION:MLT RF("+RFI1+"), RF("+RFI2+")is "+this.getGeneralPurposeRegister(RFI1).getBinaryString());
                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");  
                this.signalMicroStateExecutionComplete();
            break;          
        }  
    }
    
    /**
     * Divide Register by Register
     * rx, rx+1 <- c(rx) / c(ry)
     */
    private void executeOpcodeDVD() {
        /*
        Integer RFI1=this.getIR().decomposeByOffset(6, 7).getUnsignedValue();
        Integer RFI2=this.getIR().decomposeByOffset(8, 9).getUnsignedValue();
        Unit contentOfRFI1=new Unit(13,this.getGeneralPurposeRegister(RFI1).getUnsignedValue());
        Unit contentOfRFI2=new Unit(13,this.getGeneralPurposeRegister(RFI2).getUnsignedValue());
        alu.setOperand1(contentOfRFI1);
        alu.setOperand2(contentOfRFI2);
        alu.setControl(ArithmeticLogicUnit.CONTROL_DIVIDE);
        alu.signalReadyToStartComputation();
        this.setGeneralPurposeRegister(RFI1, new Word(alu.getResult()));
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.println("COMPLETED INSTRUCTION:DVD RF("+RFI1+"), RF("+RFI2+")is "+this.getGeneralPurposeRegister(RFI1).getBinaryString());
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        */
        switch(this.microState){
            case 0:
                // Micro-6: OP1 <- RF(RFI1)                
                Integer RFI1=this.getIR().decomposeByOffset(6, 7).getUnsignedValue();
                Unit contentOfRFI1=new Unit(13,this.getGeneralPurposeRegister(RFI1).getUnsignedValue());
                alu.setOperand1(contentOfRFI1);
                System.out.println("Micro-6: OP1 <- RF(RFI1) - "+alu.getOperand1());
            break;
                        
            case 1:
                // Micro-7: OP2 <- RF(RFI2)   
                Integer RFI2=this.getIR().decomposeByOffset(8, 9).getUnsignedValue();
                Unit contentOfRFI2=new Unit(13,this.getGeneralPurposeRegister(RFI2).getUnsignedValue());
                alu.setOperand2(contentOfRFI2);
            break;
                
            case 2:
                // Micro-8: CTRL <- OPCODE
                System.out.println("Micro-8: CTRL <- OPCODE");
                alu.setControl(ArithmeticLogicUnit.CONTROL_DIVIDE);
                alu.signalReadyToStartComputation();
            break;
                
            case 3:
                // Micro-9: RES <- c(OP1) * c(OP2)      
                System.out.println("Micro-9: RES <- c(OP1) / c(OP2)");
                // Do nothing. (occurs automatically one clock cycle after signaled ready to compute)
            break;
                
            case 4:
                // Micro-10: RF(RFI1) <- RES
                //           RF(RFI2) <- RES
                System.out.println("Micro-10: RF(RFI1) <- RES - "+alu.getResult());
                System.out.println("        : RF(RFI2) <- RES - "+alu.getResult());
                RFI1=this.getIR().decomposeByOffset(6, 7).getUnsignedValue();
                RFI2=this.getIR().decomposeByOffset(8, 9).getUnsignedValue();
                this.setGeneralPurposeRegister(RFI1, new Word(alu.getResult()));
                this.setGeneralPurposeRegister(RFI2, new Word(alu.getResult()));
                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                System.out.println("COMPLETED INSTRUCTION:DVD RF("+RFI1+"), RF("+RFI2+")is "+this.getGeneralPurposeRegister(RFI1).getBinaryString());
                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");  
                this.signalMicroStateExecutionComplete();
            break;          
        }  
    }
        
    /**
     * Stop the machine
     */
    private void executeOpcodeHLT() throws HaltSystemException {
        throw new HaltSystemException();
    }   
    
  
}
