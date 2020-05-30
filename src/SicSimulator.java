import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * 시뮬레이터로서의 작업을 담당한다. VisualSimulator에서 사용자의 요청을 받으면 이에 따라
 * ResourceManager에 접근하여 작업을 수행한다.
 * <p>
 * 작성중의 유의사항 : <br>
 * 1) 새로운 클래스, 새로운 변수, 새로운 함수 선언은 얼마든지 허용됨. 단, 기존의 변수와 함수들을 삭제하거나 완전히 대체하는 것은 지양할 것.<br>
 * 2) 필요에 따라 예외처리, 인터페이스 또는 상속 사용 또한 허용됨.<br>
 * 3) 모든 void 타입의 리턴값은 유저의 필요에 따라 다른 리턴 타입으로 변경 가능.<br>
 * 4) 파일, 또는 콘솔창에 한글을 출력시키지 말 것. (채점상의 이유. 주석에 포함된 한글은 상관 없음)<br>
 *
 * <br><br>
 * + 제공하는 프로그램 구조의 개선방법을 제안하고 싶은 분들은 보고서의 결론 뒷부분에 첨부 바랍니다. 내용에 따라 가산점이 있을 수 있습니다.
 */
public class SicSimulator {
    ResourceManager rMgr;
    InstLuncher instLuncher;
    private ArrayList<String> log;
    private ArrayList<String> instructions;
    private String codes;
    private int codeCur;

    /* bit 조작의 가독성을 위한 선언 */
    public static final byte nFlag = 32;
    public static final byte iFlag = 16;
    public static final byte xFlag = 8;
    public static final byte bFlag = 4;
    public static final byte pFlag = 2;
    public static final byte eFlag = 1;

    public SicSimulator(ResourceManager resourceManager) {
        // 필요하다면 초기화 과정 추가
        this.rMgr = resourceManager;
        this.instLuncher = new InstLuncher(resourceManager);
        this.log = new ArrayList<String>();
        this.instructions = new ArrayList<String>();
    }

    /**
     * 레지스터, 메모리 초기화 등 프로그램 load와 관련된 작업 수행.
     * 단, object code의 메모리 적재 및 해석은 SicLoader에서 수행하도록 한다.
     */
    public void load(File objectCode) {
        /* 메모리 초기화, 레지스터 초기화 등*/
        for (int i = 0; i < 10; i++)
            rMgr.register[i] = 0;
        rMgr.register_F = 0;
        this.codes = "";
        this.codeCur = 0;
        String line = "";
        BufferedReader bufReader;
        int codeCur = 0, secLen = 0;
        ArrayList<SicLoader.Modify> mRec = new ArrayList<SicLoader.Modify>();
        try {
            bufReader = new BufferedReader(new FileReader(objectCode));
            while ((line = bufReader.readLine()) != null)
                if (line.length() != 0 && line.substring(0, 1).equals("T"))
                    codes += line.substring(9, line.length());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 1개의 instruction이 수행된 모습을 보인다.
     */
    public Boolean oneStep() {/*
        System.out.println(String.format("%02X", rMgr.memory[rMgr.register[8]]));*/
        int format = -1;
        String code, inst = "";
        byte opcode = (byte) (rMgr.memory[rMgr.register[8]] & ~3);
        format = instLuncher.getInstFormat(opcode);
        inst = instLuncher.getInst(opcode);
/*        System.out.println(format);
        System.out.println(inst);*/
        if (format == 1) {
            code = String.format("%02X", rMgr.memory[rMgr.register[8]++]);
            if (!codes.substring(codeCur, codeCur + 2).equals(code))
                return false;
            codeCur += 2;
            instLuncher.executeInst(opcode, null, 1,null);
        } else if (format == 2) {
            code = String.format("%02X%02X", rMgr.memory[rMgr.register[8]++], rMgr.memory[rMgr.register[8]++]);
            instLuncher.executeInst(opcode, code.substring(2, 4), 2,null);
            codeCur += 4;
        } else {
            //check nixbpe flag
            byte ni = 3;
            int addressingMode = ni & rMgr.memory[rMgr.register[8]];
/*            if(addressingMode==1)
                System.out.println("immediate");
            if(addressingMode==2)
                System.out.println("indirect");
            if(addressingMode==3)
                System.out.println("simple");*/
            if ((rMgr.memory[rMgr.register[8] + 1] & 16) != 16) {
                code = String.format("%02X%02X%02X", rMgr.memory[rMgr.register[8]++], rMgr.memory[rMgr.register[8]++], rMgr.memory[rMgr.register[8]++]);
                instLuncher.executeInst(opcode, code.substring(3, 6), 0,addressingMode);
                codeCur += 6;
            } else {
                code = String.format("%02X%02X%02X%02X", rMgr.memory[rMgr.register[8]++], rMgr.memory[rMgr.register[8]++], rMgr.memory[rMgr.register[8]++], rMgr.memory[rMgr.register[8]++]);
                instLuncher.executeInst(opcode, code.substring(3, 8), 0,addressingMode);
                codeCur += 8;
            }
        }
        addLog(inst, code);
        return true;
    }

    /**
     * 남은 모든 instruction이 수행된 모습을 보인다.
     */
    public void allStep() {
        System.out.println(rMgr.register[8]);
        oneStep();
        while (!(rMgr.register[8] == rMgr.startAddr)) {
            oneStep();
        }
    }

    /**
     * 각 단계를 수행할 때 마다 관련된 기록을 남기도록 한다.
     */
    public void addLog(String inst, String code) {
        this.log.add(inst + "\n");
        this.instructions.add(code);
    }

    public ArrayList<String> getLog() {
        return this.log;
    }

    public ArrayList<String> getInstructions() {
        return this.instructions;
    }
}
