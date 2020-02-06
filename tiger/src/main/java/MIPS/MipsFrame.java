package MIPS;

import Frame.*;
import Temp.*;
import Assem.*;

import java.util.*;

public class MipsFrame extends Frame {
    public int allocDown = 0; // 栈偏移量
    public ArrayList saveArgs = new ArrayList(); // 用于保存参数
    private Temp fp = new Temp(0); // frame pointer
    private Temp sp = new Temp(1); // stack pointer
    private Temp ra = new Temp(2); // return address
    private Temp rv = new Temp(3); // return value
    private Temp zero = new Temp(4);
    // 寄存器$a0~$a3，为函数传入参数的寄存器
    public TempList argRegs = new TempList(new Temp(5),
            new TempList(new Temp(6),
                    new TempList(new Temp(7),
                            new TempList(new Temp(8), null))));
    private TempList calleeSaves = null; // 寄存器$s0~$s7
    public TempList callerSaves = null; // 寄存器$t0~$t9
    private int numOfcalleeSaves = 8; // $s寄存器的数量，一共8个

    // 初始化寄存器，并为它们统一编号
    public MipsFrame() {
        for (int i = 9; i <= 18; i++)
            callerSaves = new TempList(new Temp(i), callerSaves);
        for (int i = 19; i <= 26; i++)
            calleeSaves = new TempList(new Temp(i), calleeSaves);
    }

    public Frame newFrame(Label name, Util.BoolList formals) {
        MipsFrame ret = new MipsFrame();
        ret.name = name; // 传入帧名称
        TempList argReg = argRegs; // 传入参数列表
        for (Util.BoolList f = formals; f != null; f = f.tail, argReg = argReg.tail) {
            // 为每个参数分配存储空间
            Access a = ret.allocLocal(f.head);
            // 注意区分 Frame.formals 和本地的 formals，前者是 AccessList 类型
            ret.formals = new AccessList(a, ret.formals);
            if (argReg != null) {
                // 产生保存参数的汇编指令，把参数放入 frame 的 Access 中
                ret.saveArgs.add(new Tree.MOVE(a.exp(new Tree.TEMP(fp)), new Tree.TEMP(argReg.head)));
            }
        }
        return ret;
    }

    public Access allocLocal(boolean escape) {
        if (escape) {
            // 若逃逸，则在帧中分配空间
            Access ret = new InFrame(this, allocDown);
            // 增加一个机器字(32 bits)的存储空间，注意存储空间向下增长
            allocDown -= Translate.Library.WORDSIZE;
            return ret;
        } else {
            // 否则分配寄存器作为存储空间
            return new InReg();
        }
    }

    public Tree.Exp externalCall(String func, Tree.ExpList args) {
        // 调用标准库函数
        return new Tree.CALL(new Tree.NAME(new Label(func)), args);
    }

    public String string(Label label, String value) {
        // 产生字符串的数据段汇编代码
        String ret = label.toString() + ": " + System.getProperty("line.separator");
        if (value.equals("\n")) {
            ret = ret + ".word" + value.length() + System.getProperty("line.separator");
            ret = ret + ".asciiz \"" + System.getProperty("line.separator") + "\"";
            return ret;
        }
        ret = ret + ".word" + value.length() + System.getProperty("line.separator");
        ret = ret + ".asciiz \"" + value + "\"";
        return ret;
    }

    // 返回$fp、$sp、$ra、$rv寄存器
    public Temp FP() {
        return fp;
    }

    public Temp SP() {
        return sp;
    }

    public Temp RA() {
        return ra;
    }

    public Temp RV() {
        return rv;
    }

    public String tempMap(Temp t) {
        // 将传入的寄存器转换为寄存器名称
        if (t.toString().equals("t0")) return "$fp";
        if (t.toString().equals("t1")) return "$sp";
        if (t.toString().equals("t2")) return "$ra";
        if (t.toString().equals("t3")) return "$v0";
        if (t.toString().equals("t4")) return "$zero";

        for (int i = 5; i <= 8; i++)
            if (t.toString().equals("t" + i)) {
                int r = i - 5;
                return "$a" + r;
            }
        for (int i = 9; i <= 18; i++)
            if (t.toString().equals("t" + i)) {
                int r = i - 9;
                return "$t" + r;
            }
        for (int i = 19; i <= 26; i++)
            if (t.toString().equals("t" + i)) {
                int r = i - 19;
                return "$s" + r;
            }

        return null;
    }

    public java.util.HashSet registers() {
        // 返回寄存器集合
        java.util.HashSet ret = new java.util.HashSet();
        // 将calleeSave寄存器存入set集合中
        for (TempList tl = this.calleeSaves; tl != null; tl = tl.tail) ret.add(tl.head);
        // 将callerSave寄存器存入set集合中
        for (TempList tl = this.callerSaves; tl != null; tl = tl.tail) ret.add(tl.head);

        return ret;
    }

    // Callee 经过procEntryExit1 处理后增加了如下指令 保存原fp->计算新fp->保存
    // ra->保存Callee-save寄存器->保存参数->(函数体原指令)->恢复Callee-save寄存器->恢复返回地址->恢复fp
    // 在函数体原指令前加上保存参数的指令
    public Tree.Stm procEntryExit1(Tree.Stm body) {
        for (int i = 0; i < saveArgs.size(); ++i)
            body = new Tree.SEQ((Tree.MOVE)saveArgs.get(i), body);
        // 以下为加入保存CalleeSave的指令
        Access fpAcc = allocLocal(true); // 为 $fp 中的值分配空间
        Access raAcc = allocLocal(true); // 为 $ra 中的值分配空间
        Access[] calleeAcc = new Access[numOfcalleeSaves]; // 为寄存器 $s0 ~ $s7 分配空间
        TempList calleeTemp = calleeSaves; // 为寄存器 $t0 ~ $t9 分配空间
        for (int i = 0; i < numOfcalleeSaves; ++i, calleeTemp = calleeTemp.tail) {
            // 将 calleeSave 寄存器存入帧空间中
            calleeAcc[i] = allocLocal(true);
            body = new Tree.SEQ(new Tree.MOVE(calleeAcc[i].exp(new Tree.TEMP(fp)), new Tree.TEMP(calleeTemp.head)), body);
        }
        // 在 body 前面加上保存返回地址 $ra 的指令
        body = new Tree.SEQ(new Tree.MOVE(raAcc.exp(new Tree.TEMP(fp)), new Tree.TEMP(ra)), body);
        // 令 $fp = $sp - 帧空间 + 4 bytes
        body = new Tree.SEQ(new Tree.MOVE(new Tree.TEMP(fp), new Tree.BINOP(Tree.BINOP.PLUS, new Tree.TEMP(sp),
                new Tree.CONST(-allocDown - Translate.Library.WORDSIZE))), body);
        // 在 body 前保存 fp
        body = new Tree.SEQ(new Tree.MOVE(fpAcc.expFromStack(new Tree.TEMP(sp)), new Tree.TEMP(fp)), body);
        // 在 body 后恢复 callee
        calleeTemp = calleeSaves;
        for (int i = 0; i < numOfcalleeSaves; ++i, calleeTemp = calleeTemp.tail)
            body = new Tree.SEQ(body,
                    new Tree.MOVE(new Tree.TEMP(calleeTemp.head), calleeAcc[i].exp(new Tree.TEMP(fp))));
        // body 后恢复返回地址
        body = new Tree.SEQ(body, new Tree.MOVE(new Tree.TEMP(ra), raAcc.exp(new Tree.TEMP(fp))));
        // body 后恢复 fp
        body = new Tree.SEQ(body, new Tree.MOVE(new Tree.TEMP(fp), fpAcc.expFromStack(new Tree.TEMP(sp))));
        return body;
    }

    // 函数经 procEntryExit2 处理后保持不变，仅增加一条空指令
    public Assem.InstrList procEntryExit2(Assem.InstrList body) {
        return Assem.InstrList.append(body, new Assem.InstrList(
                new Assem.OPER("", null, new TempList(zero, new TempList(sp, new TempList(ra, calleeSaves)))), null));
    }

    public InstrList procEntryExit3(InstrList body) {
        // 分配帧空间：将 $sp 减去帧空间 (如32bytes)
        body = new InstrList(new OPER("subu $sp, $sp, " + (-allocDown), new TempList(sp, null), new TempList(sp, null)),
                body);
        // 设置函数体标号
        body = new InstrList(new OPER(name.toString() + ":", null, null), body);
        // 跳转到返回地址
        InstrList epilogue = new InstrList(new OPER("jr $ra", null, new TempList(ra, null)), null);
        // 将 $sp 加上相应的帧空间 (如32bytes)
        epilogue = new InstrList(
                new OPER("addu $sp, $sp, " + (-allocDown), new TempList(sp, null), new TempList(sp, null)), epilogue);
        body = InstrList.append(body, epilogue);
        return body;
    }

}
